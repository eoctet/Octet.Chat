package chat.octet.model.beans;

import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;


/**
 * Token
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
@ToString
public final class Token implements Serializable {
    private final int id;
    //NOTE: Token sampling time in milliseconds
    private final long elapsed;
    private final String text;
    private final LlamaTokenType tokenType;
    private FinishReason finishReason;

    public Token(int id, long elapsed, LlamaTokenType tokenType, String text) {
        this.id = id;
        this.elapsed = System.currentTimeMillis() - elapsed;
        this.text = text;
        this.tokenType = tokenType;
        this.finishReason = FinishReason.NONE;
    }

    public void updateFinishReason(FinishReason finishReason) {
        this.finishReason = finishReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token)) return false;
        Token token = (Token) o;
        return id == token.id && Objects.equals(text, token.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text);
    }
}