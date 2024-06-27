package chat.octet.model;

import chat.octet.model.beans.CompletionResult;
import chat.octet.model.beans.Status;
import chat.octet.model.beans.Token;
import chat.octet.model.enums.FinishReason;
import chat.octet.model.exceptions.DecodeException;
import chat.octet.model.exceptions.GenerationException;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.utils.ColorConsole;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Model inference generator,
 * Supports streaming output text and generating complete text.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Slf4j
public class Generator implements Iterable<Token> {
    private final Inference inference;
    private final Status chatStatus;

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
        this(generateParams, prompt, null);
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

    /**
     * Stream outputs the generated text.
     */
    public void output() {
        try {
            for (Token token : this) {
                System.out.print(ColorConsole.cyan(token.getText()));
            }
        } catch (Exception e) {
            throw new GenerationException("Generate next token error ", e);
        } finally {
            close();
        }
    }

    /**
     * Return the generated complete text.
     *
     * @return CompletionResult, generated text and completion reason.
     * @see CompletionResult
     */
    public CompletionResult result() {
        List<Token> tokens = tokens();

        StringBuilder builder = new StringBuilder();
        tokens.forEach(t -> builder.append(t.getText()));
        FinishReason finishReason = tokens.get(tokens.size() - 1).getFinishReason();

        return CompletionResult.builder()
                .promptTokens(inference.getPromptTokens())
                .completionTokens(tokens.size())
                .content(builder.toString())
                .finishReason(finishReason)
                .build();
    }

    /**
     * Return the generated tokens.
     *
     * @return A list of tokens will be returned.
     */
    public List<Token> tokens() {
        try {
            return Lists.newArrayList(this);
        } catch (Exception e) {
            throw new GenerationException("Generate next token error ", e);
        } finally {
            close();
        }
    }

    /**
     * Close inference generator.
     */
    public void close() {
        if (chatStatus != null && inference.isSessionCache()) {
            chatStatus.copyToStatus(inference.getStatus());
        } else {
            inference.clearCache();
        }
    }

    /**
     * Iterator-based inference implementation.
     */
    private static class Inference implements Iterator<Token> {
        private final GenerateParameter generateParams;
        private final Status status;
        private final byte[] multiByteTokenBuffer;
        private final int maxNewTokenSize;
        private final int contextSize;
        private final int promptTokens;
        private int multiByteTokenLength;
        private int multiByteTokenIndex;
        private boolean finished = false;

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

            //format prompt text
            String bosToken = StringUtils.EMPTY;
            if (LlamaService.addBosToken() && LlamaService.getBosToken() != -1) {
                bosToken = TokenDecoder.decodeToken(true, LlamaService.getBosToken());
                if (StringUtils.startsWithIgnoreCase(prompt.trim(), bosToken)) {
                    log.warn("Detect duplicate {} leading in prompt, automatically remove it now.", bosToken);
                    prompt = StringUtils.removeStartIgnoreCase(prompt, bosToken);
                }
            }
            String eosToken = StringUtils.EMPTY;
            if (LlamaService.addEosToken() && LlamaService.getEosToken() != -1) {
                eosToken = TokenDecoder.decodeToken(true, LlamaService.getEosToken());
                if (StringUtils.endsWithIgnoreCase(prompt, eosToken)) {
                    log.warn("Detect duplicate ending {} in prompt, automatically remove it now.", eosToken);
                    prompt = StringUtils.removeEndIgnoreCase(prompt, eosToken);
                }
            }
            if (generateParams.isInfill()) {
                prompt = formatPromptInfill(prompt);
            }
            if (LlamaService.addSpacePrefix() && StringUtils.isNotBlank(prompt) && !StringUtils.startsWith(prompt, StringUtils.SPACE)) {
                prompt = StringUtils.SPACE + prompt;
            }
            String finalPrompt = bosToken + prompt + eosToken;
            if (generateParams.isVerbosePrompt()) {
                log.info("Final prompt text:\n{}", finalPrompt);
            }

            //prompt tokenization
            int[] tokens = StringUtils.isNotBlank(prompt) ? LlamaService.tokenize(finalPrompt, false, true) : new int[]{LlamaService.getBosToken()};
            this.promptTokens = tokens.length;
            if (tokens.length >= contextSize) {
                throw new IllegalArgumentException(MessageFormat.format("Requested tokens ({0}) exceed context window of {1}.", tokens.length, contextSize));
            }
            this.status.appendTokens(tokens);

            this.maxNewTokenSize = (generateParams.getMaxNewTokenSize() <= 0) ? contextSize - this.status.getInputLength() : generateParams.getMaxNewTokenSize();
            if (StringUtils.isNotBlank(generateParams.getGrammarRules())) {
                boolean status = LlamaService.loadLlamaGrammar(generateParams.getGrammarRules());
                if (!status) {
                    log.error("Grammar rule parsing failed, Please check the grammar rule format.");
                }
            }
            log.debug("Inference starting, input token size: {}, past token size: {}.", tokens.length, this.status.getPastTokenSize());
            batchDecode();
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

        protected Status getStatus() {
            return status;
        }

        protected int getPromptTokens() {
            return promptTokens;
        }

        protected boolean isSessionCache() {
            return generateParams.isSessionCache();
        }

        private String formatPromptInfill(String prompt) {
            StringBuilder buffer = new StringBuilder(prompt);
            // prefix token
            String prefixToken = Optional.ofNullable(generateParams.getPrefixToken()).orElse(StringUtils.EMPTY);
            if (StringUtils.isBlank(prefixToken) && LlamaService.getPrefixToken() != -1) {
                prefixToken = TokenDecoder.decodeToken(true, LlamaService.getPrefixToken());
            }
            if (StringUtils.containsIgnoreCase(prompt, prefixToken.trim())) {
                prefixToken = StringUtils.EMPTY;
            }

            // suffix token
            String suffixToken = Optional.ofNullable(generateParams.getSuffixToken()).orElse(StringUtils.EMPTY);
            if (StringUtils.isBlank(suffixToken) && LlamaService.getSuffixToken() != -1) {
                suffixToken = TokenDecoder.decodeToken(true, LlamaService.getSuffixToken());
            }
            if (StringUtils.containsIgnoreCase(prompt, suffixToken.trim())) {
                suffixToken = StringUtils.EMPTY;
            }

            // middle token
            String middleToken = Optional.ofNullable(generateParams.getMiddleToken()).orElse(StringUtils.EMPTY);
            if (StringUtils.isBlank(middleToken) && LlamaService.getMiddleToken() != -1) {
                middleToken = TokenDecoder.decodeToken(true, LlamaService.getMiddleToken());
            }
            if (StringUtils.endsWithIgnoreCase(prompt, middleToken.trim())) {
                middleToken = StringUtils.EMPTY;
            }

            return generateParams.isSpmFill() ?
                    buffer.insert(0, suffixToken).append(prefixToken).append(middleToken).toString() :
                    buffer.insert(0, prefixToken).append(suffixToken).append(middleToken).toString();
        }


        private int getLogitsIndex() {
            return status.getGenerateTokens().isEmpty() ? promptTokens - 1 : 0;
        }

        /**
         * Batch decoding prompt text.
         */
        private void batchDecode() {
            //batch decode input tokens
            int decodeStatus = LlamaService.batchDecode(status.getId(), status.getInputIds(), status.getInputLength(), status.getPastTokenSize());
            if (decodeStatus != 0) {
                throw new DecodeException(MessageFormat.format("Failed to decode, return code: {0}.", decodeStatus));
            }
            int size = status.getInputLength() - status.getPastTokenSize();
            status.addPastTokensSize(size);
            log.debug("Batch decoding prompt completed, sequence id: {}, decode token size: {}.", status.getId(), size);
        }

        /**
         * Check that inference continue or should be break.
         *
         * @param token  Next new token.
         * @param logits Logits.
         * @return boolean
         */
        private boolean breakOrContinue(Token token, float[] logits) {
            if (LlamaService.isEndOfGeneration(token.getId())) {
                token.updateFinishReason(FinishReason.FINISHED);
                return true;
            }
            if (!generateParams.getStoppingCriteriaList().isEmpty()) {
                boolean matched = generateParams.getStoppingCriteriaList().criteria(status.getInputIds(), logits, status.getGenerateTokens());
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
         * Append multibyte token buffer
         *
         * @param buffer Byte buffer
         * @param length Byte buffer length
         * @return String, Token text.
         */
        private String appendMultiByteTokenBuffer(byte[] buffer, int length) {
            System.arraycopy(buffer, 0, multiByteTokenBuffer, multiByteTokenIndex, length);
            multiByteTokenIndex += length;
            if (multiByteTokenIndex >= multiByteTokenLength) {
                String text = new String(multiByteTokenBuffer, 0, multiByteTokenLength, StandardCharsets.UTF_8);
                multiByteTokenIndex = 0;
                multiByteTokenLength = 0;
                Arrays.fill(multiByteTokenBuffer, (byte) 0);
                return text;
            }
            return StringUtils.EMPTY;
        }


        /**
         * Converts the specified token id to text.
         *
         * @param token Token id.
         * @return String
         */
        private String tokenToText(int token) {
            byte[] buffer = new byte[64];
            int length = LlamaService.tokenToPiece(token, buffer, buffer.length, generateParams.isSpecial());
            if (length == 0) {
                return StringUtils.EMPTY;
            }

            if (multiByteTokenLength > 0) {
                return appendMultiByteTokenBuffer(buffer, length);
            }

            int byteLength = TokenDecoder.getByteLength(buffer, length);
            if (byteLength != length) {
                multiByteTokenIndex = 0;
                multiByteTokenLength = byteLength;
                return appendMultiByteTokenBuffer(buffer, length);
            } else {
                return new String(buffer, 0, length, StandardCharsets.UTF_8);
            }
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
            float[] logits = LlamaService.getLogits(getLogitsIndex());
            //execute logits processor
            if (!generateParams.getLogitsProcessorList().isEmpty()) {
                logits = generateParams.getLogitsProcessorList().processor(status.getInputIds(), logits);
            }
            //do sampling
            int[] lastTokens = null;
            if (generateParams.getLastTokensSize() != 0) {
                int startIndex = Math.max(0, status.getInputLength() - generateParams.getLastTokensSize());
                lastTokens = status.subInputIds(startIndex);
            }
            int tokenId = LlamaService.sampling(generateParams, logits, lastTokens, status.getId(), status.getPastTokenSize());
            Token token = new Token(tokenId, LlamaService.getLlamaTokenAttr(tokenId), tokenToText(tokenId));
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
