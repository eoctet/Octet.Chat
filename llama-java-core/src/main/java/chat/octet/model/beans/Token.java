package chat.octet.model.beans;

import chat.octet.model.enums.FinishReason;
import chat.octet.model.enums.LlamaTokenAttr;
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
    private final String text;
    private final LlamaTokenAttr tokenAttr;
    private FinishReason finishReason;

    public Token(int id, LlamaTokenAttr tokenAttr, String text) {
        this.id = id;
        this.text = text;
        this.tokenAttr = tokenAttr;
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