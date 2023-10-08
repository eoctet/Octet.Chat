package chat.octet.model;

import chat.octet.model.beans.FinishReason;
import chat.octet.model.beans.LlamaTokenType;
import chat.octet.model.beans.Status;
import chat.octet.model.beans.Token;
import chat.octet.model.exceptions.DecodeException;
import chat.octet.model.parameters.GenerateParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Generation iterator,
 * which outputs tokens one by one in a stream format.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Slf4j
public class Generator implements Iterator<Token> {
    private final GenerateParameter generateParams;
    private final Status status;
    private final byte[] multiByteTokenBuffer;
    private int multiByteTokenLength;
    private int multiByteTokenIndex;
    private boolean finished = false;
    private final int maxNewTokenSize;
    private final int contextSize;

    protected Status getStatus() {
        return status;
    }

    /**
     * Create continuous conversation generator
     *
     * @param generateParams Specify a generation parameter.
     * @param prompt         Prompt
     * @param srcStatus      Source status.
     */
    protected Generator(GenerateParameter generateParams, String prompt, Status srcStatus) {
        this.generateParams = generateParams;
        this.multiByteTokenBuffer = new byte[8];
        this.contextSize = LlamaService.getContextSize();
        this.status = srcStatus == null ? new Status() : new Status(srcStatus);

        int[] tokens = StringUtils.isNotBlank(prompt) ? LlamaService.tokenize(prompt, true) : new int[]{LlamaService.getTokenBOS()};
        if (tokens.length >= contextSize) {
            throw new IllegalArgumentException(MessageFormat.format("Requested tokens ({0}) exceed context window of {1}.", tokens.length, contextSize));
        }
        if (generateParams.isVerbosePrompt()) {
            log.info("Print prompt text:\n{}", prompt);
        }
        this.status.appendTokens(tokens);

        this.maxNewTokenSize = (generateParams.getMaxNewTokenSize() <= 0) ? contextSize - this.status.getInputLength() : generateParams.getMaxNewTokenSize();
        if (StringUtils.isNotBlank(generateParams.getGrammarRules())) {
            boolean status = LlamaService.loadLlamaGrammar(generateParams.getGrammarRules());
            if (!status) {
                log.error("Grammar rule parsing failed, Please check the grammar rule format.");
            }
        }
        log.debug("Generate starting, input token size: {}, past token size: {}.", tokens.length, this.status.getPastTokenSize());
        decodePrompt();
    }

    /**
     * Create regular generator
     *
     * @param generateParams Specify a generation parameter.
     * @param text           Input text or prompt.
     */
    protected Generator(GenerateParameter generateParams, String text) {
        this(generateParams, text, null);
    }

    private void decodePrompt() {
        //batch decode input tokens
        int decodeStatus = LlamaService.batchDecode(status.getId(), status.getInputIds(), status.getInputLength(), status.getPastTokenSize());
        if (decodeStatus != 0) {
            throw new DecodeException(MessageFormat.format("Failed to decode, return code: {0}.", decodeStatus));
        }
        int size = status.getInputLength() - status.getPastTokenSize();
        status.addPastTokensSize(size);
        log.debug("Batch decode prompt completed, decode token size: {}, sequence id: {}.", size, status.getId());
    }

    private boolean breakOrContinue(Token token, float[] logits) {
        if (token.getId() == LlamaService.getTokenEOS()) {
            token.updateFinishReason(FinishReason.FINISHED);
            return true;
        }
        if (generateParams.getStoppingCriteriaList() != null) {
            boolean matched = generateParams.getStoppingCriteriaList().criteria(status.getInputIds(), logits);
            if (matched) {
                token.updateFinishReason(FinishReason.STOP);
                return true;
            }
        }
        if (status.getInputLength() >= contextSize) {
            token.updateFinishReason(FinishReason.TRUNCATED);
            log.warn("Context size has been exceeded. Truncate and reset the context cache, sequence id: {}.", status.getId());
            return true;
        }
        if (status.getGenerateTokens().size() >= maxNewTokenSize) {
            token.updateFinishReason(FinishReason.LENGTH);
            return true;
        }
        return false;
    }

    private String tokenToText(int token) {
        byte[] buffer = new byte[64];
        int length = LlamaService.tokenToPiece(token, buffer, buffer.length);
        byte code = buffer[0];

        if (length == 1 && !Character.isValidCodePoint(code)) {
            if (multiByteTokenLength == 0) {
                multiByteTokenLength = TokenDecoder.getUtf8ByteLength(code);
            }
            multiByteTokenBuffer[multiByteTokenIndex] = code;
            ++multiByteTokenIndex;
            if (multiByteTokenIndex == multiByteTokenLength) {
                String text = new String(multiByteTokenBuffer, 0, multiByteTokenLength, StandardCharsets.UTF_8);
                multiByteTokenIndex = 0;
                multiByteTokenLength = 0;
                Arrays.fill(multiByteTokenBuffer, (byte) 0);
                return text;
            }
            return StringUtils.EMPTY;
        }
        return new String(buffer, 0, length, StandardCharsets.UTF_8);
    }

    private int doSampling(float[] logits) {
        int[] lastTokens = null;
        if (generateParams.getLastTokensSize() != 0) {
            int startIndex = Math.max(0, status.getInputLength() - generateParams.getLastTokensSize());
            lastTokens = status.subInputIds(startIndex);
        }
        return LlamaService.sampling(
                logits,
                lastTokens,
                generateParams.getLastTokensSize(),
                generateParams.getRepeatPenalty(),
                generateParams.getFrequencyPenalty(),
                generateParams.getPresencePenalty(),
                generateParams.isPenalizeNl(),
                generateParams.getMirostatMode().ordinal(),
                generateParams.getMirostatTAU(),
                generateParams.getMirostatETA(),
                generateParams.getTemperature(),
                generateParams.getTopK(),
                generateParams.getTopP(),
                generateParams.getTsf(),
                generateParams.getTypical(),
                status.getId(),
                status.getPastTokenSize()
        );
    }

    @Override
    public boolean hasNext() {
        return !finished;
    }

    /**
     * Output next token.
     *
     * @return Token
     * @see Token
     */
    @Override
    public Token next() {
        float[] logits = LlamaService.getLogits(status.getLogitsIndex());
        //execute logits processor
        if (generateParams.getLogitsProcessorList() != null) {
            logits = generateParams.getLogitsProcessorList().processor(status.getInputIds(), logits);
        }
        //do sampling
        int tokenId = doSampling(logits);
        Token token = new Token(tokenId, LlamaTokenType.valueOfType(LlamaService.getTokenType(tokenId)), tokenToText(tokenId));
        //update generate status
        status.appendNextToken(token);
        finished = breakOrContinue(token, logits);
        return token;
    }

    /**
     * Return the generated complete text.
     *
     * @return String
     */
    public String getGeneratedCompleteText() {
        return status.getGeneratedCompleteText();
    }

    /**
     * Return the finished reason for the last token.
     *
     * @return FinishReason, Last token finished reason.
     * @see FinishReason
     */
    public FinishReason getFinishReason() {
        return status.getFinishReason();
    }

    /**
     * Clear context cache at the end of generation
     */
    public void clearCache() {
        status.reset();
        log.debug("Cache clear completed, sequence id: {}.", status.getId());
    }

}
