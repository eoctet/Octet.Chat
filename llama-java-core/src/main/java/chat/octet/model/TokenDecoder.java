package chat.octet.model;


import java.nio.charset.StandardCharsets;

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
}
