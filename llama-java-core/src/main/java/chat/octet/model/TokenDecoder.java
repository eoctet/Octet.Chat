package chat.octet.model;


import chat.octet.model.beans.Token;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Token decoder
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public class TokenDecoder {

    private TokenDecoder() {
    }

    public static String decodeToken(int... tokens) {
        byte[] buffer = new byte[tokens.length * 64];
        int length = 0;
        for (int token : tokens) {
            byte[] bytes = new byte[64];
            int size = LlamaService.tokenToPiece(token, bytes, bytes.length);
            System.arraycopy(bytes, 0, buffer, length, size);
            length += size;
        }
        return new String(buffer, 0, length, StandardCharsets.UTF_8);
    }

    public static int getByteLength(byte[] buffer, int length) {
        int len = 0;
        for (int i = 0; i < length; i++) {
            byte code = buffer[i];
            if (!Character.isValidCodePoint(code)) {
                try {
                    len += getUtf8ByteLength(code);
                } catch (Exception ignored) {
                }
            } else {
                len++;
            }
        }
        return len;
    }

    public static int getUtf8ByteLength(byte bytes) {
        int topBits = bytes & 0xFF;
        if (topBits <= 0x7F) {
            return 1;
        } else if (topBits >= 0xC2 && topBits <= 0xDF) {
            return 2;
        } else if (topBits >= 0xE0 && topBits <= 0xEF) {
            return 3;
        } else if (topBits >= 0xF0 && topBits <= 0xF7) {
            return 4;
        } else {
            throw new IllegalArgumentException("Illegal byte, byte code is " + bytes);
        }
    }

    private static int findTokenIndex(List<Token> sources, int[] target, boolean toEnd) {
        for (int i = 0; i < sources.size(); i++) {
            int toIndex = Math.min(sources.size() - 1, i + target.length);
            int[] temp = sources.subList(i, toIndex).stream().mapToInt(Token::getId).toArray();

            if (Arrays.equals(temp, target)) {
                if (toEnd) {
                    return toIndex;
                } else {
                    return i;
                }
            }
        }
        return -1;
    }

    public static List<Token> subTokensBetween(List<Token> tokens, String startWord) {
        return subTokensBetween(tokens, startWord, null);
    }

    public static List<Token> subTokensBetween(List<Token> tokens, String startWord, String endWord) {
        Preconditions.checkNotNull(tokens, "Tokens cannot be null");

        int startIndex = 0;
        if (StringUtils.isNotBlank(startWord)) {
            int[] startIds = LlamaService.tokenize(startWord, false, true);
            int index = findTokenIndex(tokens, startIds, true);
            startIndex = (index == -1) ? 0 : index;
        }

        int endIndex = tokens.size();
        if (StringUtils.isNotBlank(endWord)) {
            int[] endIds = LlamaService.tokenize(endWord, false, true);
            int index = findTokenIndex(tokens, endIds, false);
            endIndex = (index == -1) ? tokens.size() : index;
        }
        return tokens.subList(startIndex, endIndex);
    }
}
