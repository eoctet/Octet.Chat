package chat.octet.model;

import chat.octet.model.beans.FinishReason;
import chat.octet.model.beans.LlamaTokenType;
import chat.octet.model.beans.Token;
import chat.octet.model.parameters.GenerateParameter;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Generation iterator,
 * which outputs tokens one by one in a stream format.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Slf4j
public class Generator implements Iterator<Token> {
    private final GenerateParameter generateParams;
    private final List<Token> generateTokens;
    private final int[] inputIds;
    private final byte[] multiByteTokenBuffer;
    private int multiByteTokenLength;
    private int multiByteTokenIndex;
    private boolean finished = false;
    private int maxNewTokensSize;
    private int inputLength;
    private int pastTokensSize;
    private final int contextSize;
    private final int lastTokensSize;
    private final int sequenceId;

    protected Generator(GenerateParameter generateParams, String text, int lastTokensSize) {
        this.generateParams = generateParams;
        this.contextSize = LlamaService.getContextSize();
        this.inputIds = new int[contextSize];
        this.multiByteTokenBuffer = new byte[8];
        this.lastTokensSize = lastTokensSize;

        int[] tokens = StringUtils.isNotBlank(text) ? LlamaService.tokenize(text, true) : new int[]{LlamaService.getTokenBOS()};
        if (tokens.length >= contextSize) {
            throw new IllegalArgumentException(MessageFormat.format("Requested tokens ({0}) exceed context window of {1}.", tokens.length, contextSize));
        }
        if (generateParams.isVerbosePrompt()) {
            log.info("Print prompt text:\n{}", text);
        }
        System.arraycopy(tokens, 0, inputIds, 0, tokens.length);
        inputLength += tokens.length;

        maxNewTokensSize = (generateParams.getMaxNewTokensSize() <= 0) ? contextSize - tokens.length : generateParams.getMaxNewTokensSize();
        if (maxNewTokensSize + tokens.length > contextSize) {
            maxNewTokensSize = contextSize - tokens.length;
        }
        if (StringUtils.isNotBlank(generateParams.getGrammarRules())) {
            boolean status = LlamaService.loadLlamaGrammar(generateParams.getGrammarRules());
            if (!status) {
                log.error("Grammar rule parsing failed, Please check the grammar rule format.");
            }
        }
        generateTokens = Lists.newArrayList();
        log.debug("Generate starting, input tokens size: {}.", tokens.length);
        //batch decode input tokens
        sequenceId = LlamaService.batchDecode(tokens);
        pastTokensSize += tokens.length;
        log.debug("Batch decode completed, sequence id: {}.", sequenceId);
    }

    private boolean breakOrContinue(Token token, float[] logits) {
        if (token.getId() == LlamaService.getTokenEOS()) {
            token.updateFinishReason(FinishReason.FINISHED);
            return true;
        }
        if (generateParams.getStoppingCriteriaList() != null) {
            boolean matched = generateParams.getStoppingCriteriaList().criteria(inputIds, logits);
            if (matched) {
                token.updateFinishReason(FinishReason.STOP);
                return true;
            }
        }
        if (generateTokens.size() > maxNewTokensSize) {
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

    private void truncate(int keepSize) {
        if (keepSize <= 0 || keepSize >= contextSize) {
            keepSize = contextSize / 2;
        }
        //check multibyte token
        for (int truncateIndex = keepSize; truncateIndex > 0; truncateIndex--) {
            int size = TokenDecoder.isMultiByte(inputIds[truncateIndex]);
            if (size >= 0) {
                keepSize -= size;
                break;
            }
        }
        //clear truncated cache tokens
        LlamaService.clearCache(sequenceId, 0, keepSize);
        //
        int[] newTokensBuffer = ArrayUtils.subarray(inputIds, keepSize, inputIds.length);
        Arrays.fill(inputIds, 0);
        System.arraycopy(newTokensBuffer, 0, inputIds, 0, newTokensBuffer.length);
        //reset size position
        pastTokensSize = keepSize;
        inputLength = keepSize;
        log.debug("Generation exceeds the current context size {}, truncated to {}.", contextSize, keepSize);
    }

    private int doSampling(GenerateParameter generateParams, float[] logits) {
        int startIndex = Math.max(0, inputLength - lastTokensSize);
        int[] lastTokens = ArrayUtils.subarray(inputIds, startIndex, inputLength);
        return LlamaService.sampling(
                logits,
                lastTokens,
                lastTokensSize,
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
                sequenceId,
                pastTokensSize
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
        int index = generateTokens.isEmpty() ? pastTokensSize - 1 : 0;
        float[] logits = LlamaService.getLogits(index);

        //execute logits processor
        if (generateParams.getLogitsProcessorList() != null) {
            logits = generateParams.getLogitsProcessorList().processor(inputIds, logits);
        }
        //do sampling
        int tokenId = doSampling(generateParams, logits);
        Token token = new Token(tokenId, LlamaTokenType.valueOfType(LlamaService.getTokenType(tokenId)), tokenToText(tokenId));
        //save token to the generate list
        generateTokens.add(token);
        ++pastTokensSize;
        //truncation is required when the generation exceeds the current context size
        if (inputLength + 1 > contextSize) {
            truncate(generateParams.getKeepContextTokensSize());
        }
        //update current input token
        inputIds[inputLength] = tokenId;
        ++inputLength;

        if (breakOrContinue(token, logits)) {
            finished = true;
        }
        return token;
    }

    /**
     * Return the generated complete text.
     *
     * @return String
     */
    public String getFullGenerateText() {
        StringBuilder builder = new StringBuilder();
        generateTokens.forEach(token -> builder.append(token.getText()));
        return builder.toString();
    }

    /**
     * Return the finished reason for the last token.
     *
     * @return FinishReason, Last token finished reason.
     * @see FinishReason
     */
    public FinishReason getFinishReason() {
        return (generateTokens == null || generateTokens.isEmpty()) ? FinishReason.UNKNOWN : generateTokens.get(generateTokens.size() - 1).getFinishReason();
    }

    /**
     * Clear KV cache at the end of generation
     */
    public void clearCache() {
        LlamaService.clearCache(sequenceId);
        log.debug("Cache clear completed, sequence id: {}.", sequenceId);
    }

}
