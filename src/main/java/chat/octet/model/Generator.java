package chat.octet.model;

import chat.octet.model.beans.FinishReason;
import chat.octet.model.beans.Token;
import chat.octet.model.parameters.GenerateParameter;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;


@Slf4j
public final class Generator implements Iterator<Token> {

    private final Model model;
    private final GenerateParameter generateParams;
    private final List<Token> generateTokens;
    private boolean finished = false;
    private final UserContext userContext;
    private final AutoDecoder decoder;

    public Generator(Model model, GenerateParameter generateParams, UserContext userContext, String text) {
        this.model = model;
        this.generateParams = generateParams;
        this.userContext = userContext;
        this.decoder = new AutoDecoder(model);

        int[] tokens = StringUtils.isNotBlank(text) ? model.tokenize(text, true) : new int[]{model.getTokenBOS()};
        if (tokens.length >= model.getContextSize()) {
            throw new IllegalArgumentException(MessageFormat.format("Requested tokens ({0}) exceed context window of {1}", tokens.length, model.getContextSize()));
        }
        if (generateParams.isVerbosePrompt()) {
            log.info(MessageFormat.format("Print prompt text:\n{0}", text));
        }
        if (userContext.getInputLength() + tokens.length > model.getContextSize()) {
            userContext.truncate(generateParams.getKeepContextTokensSize());
        }
        userContext.appendInput(tokens);

        int maxNewTokensSize = (generateParams.getMaxNewTokensSize() <= 0) ? model.getContextSize() - tokens.length : generateParams.getMaxNewTokensSize();
        if (maxNewTokensSize + tokens.length > model.getContextSize()) {
            maxNewTokensSize = model.getContextSize() - tokens.length;
        }
        userContext.setMaxNewTokensSize(maxNewTokensSize);

        generateTokens = Lists.newArrayList();

        log.debug(MessageFormat.format("Generate starting, User id: {0}, context buffer size: {1}, input tokens size: {2}.",
                userContext.getId(),
                userContext.getInputLength(),
                tokens.length
        ));
    }

    private boolean breakOrContinue(Token token) {
        if (token.getId() == model.getTokenEOS()) {
            token.updateFinishReason(FinishReason.FINISHED);
            return true;
        }
        if (generateParams.getStoppingCriteriaList() != null) {
            boolean matched = generateParams.getStoppingCriteriaList().criteria(userContext.getInputCopy(), userContext.getScores());
            if (matched) {
                token.updateFinishReason(FinishReason.STOP);
                return true;
            }
        }
        if (generateTokens.size() > userContext.getMaxNewTokensSize()) {
            token.updateFinishReason(FinishReason.LENGTH);
            return true;
        }
        return false;
    }

    @Override
    public boolean hasNext() {
        return !finished;
    }

    @Override
    public Token next() {
        //evaluation tokens
        int evaluateTotalSize = model.evaluate(
                userContext.getInput(),
                userContext.getPastTokensSize(),
                userContext.getInputLength()
        );
        userContext.addPastTokensSize(evaluateTotalSize);
        userContext.saveScores(model.getLogits(), evaluateTotalSize);

        float[] logits = userContext.getScores();
        // execute logits processor
        if (generateParams.getLogitsProcessorList() != null) {
            logits = generateParams.getLogitsProcessorList().processor(userContext.getInputCopy(), logits);
            userContext.updateScores(logits);
        }
        //do sampling
        long timestamp = System.currentTimeMillis();
        int tokenId = model.sampling(generateParams, logits, userContext.getInput(), userContext.getInputLength());
        Token token = new Token(tokenId, timestamp, decoder.decodeToken(tokenId));
        //Save new token to the list
        generateTokens.add(token);
        if (userContext.getInputLength() + 1 > model.getContextSize()) {
            userContext.truncate(generateParams.getKeepContextTokensSize());
        }
        userContext.appendInput(token.getId());
        if (breakOrContinue(token)) {
            finished = true;
            userContext.addPastTokensSize(1);
        }
        return token;
    }

    public String getFullGenerateText() {
        StringBuilder builder = new StringBuilder();
        generateTokens.forEach(token -> builder.append(token.getText()));
        return builder.toString();
    }
}
