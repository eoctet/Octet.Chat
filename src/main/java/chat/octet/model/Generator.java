package chat.octet.model;

import chat.octet.model.beans.CompletionResult;
import chat.octet.model.beans.Status;
import chat.octet.model.beans.Token;
import chat.octet.model.enums.FinishReason;
import chat.octet.model.exceptions.DecodeException;
import chat.octet.model.exceptions.ModelException;
import chat.octet.model.parameters.GenerateParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;

/**
 * Model inference generator,
 * Supports streaming output text and generating complete text.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Slf4j
public class Generator implements Iterable<Token> {
    private Inference inference;
    private Status chatStatus;

    /**
     * Create inference generator.
     *
     * @param generateParams Specify a generation parameter.
     * @param prompt         Prompt text.
     * @param chatStatus     Source status.
     */
    public Generator(GenerateParameter generateParams, String prompt, Status chatStatus) {
        this.chatStatus = chatStatus;
        this.inference = new Inference(generateParams, prompt, chatStatus);
    }

    /**
     * Create inference generator.
     *
     * @param generateParams Specify a generation parameter.
     * @param prompt         Prompt text.
     */
    public Generator(GenerateParameter generateParams, String prompt) {
        new Generator(generateParams, prompt, null);
    }

    /**
     * Return inference iterator.
     *
     * @return Iterator
     */
    @Nonnull
    @Override
    public Iterator<Token> iterator() {
        return inference;
    }

    @Override
    public Spliterator<Token> spliterator() {
        throw new RuntimeException("Unsupported operation.");
    }

    /**
     * Stream outputs the generated text.
     */
    public void output() {
        try {
            for (Token token : this) {
                System.out.print(token.getText());
            }
        } catch (Exception e) {
            throw new ModelException("Generate next token error ", e);
        } finally {
            if (chatStatus != null) {
                //copy last generated status
                chatStatus.copyToStatus(inference.getStatus());
            } else {
                inference.clearCache();
            }
        }
    }

    /**
     * Return the generated complete text.
     *
     * @return CompletionResult, generated text and completion reason.
     * @see CompletionResult
     */
    public CompletionResult result() {
        StringBuilder builder = new StringBuilder();
        FinishReason finishReason = FinishReason.UNKNOWN;
        while (inference.hasNext()) {
            Token token = inference.next();
            builder.append(token.getText());
            finishReason = token.getFinishReason();
        }
        return CompletionResult.builder().content(builder.toString()).finishReason(finishReason).build();
    }

    /**
     * Iterator-based inference implementation.
     */
    private static class Inference implements Iterator<Token> {
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
         * Create inference iterator.
         *
         * @param generateParams Specify a generation parameter.
         * @param prompt         Prompt
         * @param srcStatus      Source status.
         */
        protected Inference(GenerateParameter generateParams, String prompt, Status srcStatus) {
            this.generateParams = generateParams;
            this.multiByteTokenBuffer = new byte[8];
            this.contextSize = LlamaService.getContextSize();
            this.status = srcStatus == null ? new Status() : new Status(srcStatus);

            int[] tokens = StringUtils.isNotBlank(prompt) ? LlamaService.tokenize(prompt, true, true) : new int[]{LlamaService.getTokenBOS()};
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
         * Create inference iterator.
         *
         * @param generateParams Specify a generation parameter.
         * @param text           Input text or prompt.
         */
        protected Inference(GenerateParameter generateParams, String text) {
            this(generateParams, text, null);
        }

        /**
         * Batch decode prompt text.
         */
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

        /**
         * Check that inference continue or should be break.
         *
         * @param token  Next new token.
         * @param logits Logits.
         * @return boolean
         */
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

        /**
         * Converts the specified token id to text.
         *
         * @param token Token id.
         * @return String
         */
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

        @Override
        public boolean hasNext() {
            return !finished;
        }

        /**
         * Inference the next token.
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
            int[] lastTokens = null;
            if (generateParams.getLastTokensSize() != 0) {
                int startIndex = Math.max(0, status.getInputLength() - generateParams.getLastTokensSize());
                lastTokens = status.subInputIds(startIndex);
            }
            int tokenId = LlamaService.sampling(
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
            Token token = new Token(tokenId, LlamaService.getLlamaTokenType(tokenId), tokenToText(tokenId));
            //update generate status
            status.appendNextToken(token);
            finished = breakOrContinue(token, logits);
            return token;
        }

        /**
         * Clear context cache at the end of generation
         */
        public void clearCache() {
            status.reset();
            log.debug("Cache clear completed, sequence id: {}.", status.getId());
        }
    }

}
