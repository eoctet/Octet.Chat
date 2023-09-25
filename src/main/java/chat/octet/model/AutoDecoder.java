package chat.octet.model;


import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class AutoDecoder {

    private final Model model;
    private final byte[] tokenBytesBuffer;
    private int length;
    private int index;

    public AutoDecoder(Model model) {
        this.model = model;
        this.tokenBytesBuffer = new byte[8];
    }

    public String decodeToken(int token) {
        byte[] buffer = new byte[64];
        int size = LlamaService.getTokenToPiece(model.getLlamaContext(), token, buffer, buffer.length);
        byte code = buffer[0];

        if (size == 1 && !Character.isValidCodePoint(code)) {
            if (length == 0) {
                length = getUtf8ByteLength(code);
            }
            tokenBytesBuffer[index] = code;
            index++;
            if (index == length) {
                String text = new String(tokenBytesBuffer, 0, length, StandardCharsets.UTF_8);
                index = 0;
                length = 0;
                Arrays.fill(tokenBytesBuffer, (byte) 0);
                return text;
            }
            return StringUtils.EMPTY;
        }
        return new String(buffer, 0, size, StandardCharsets.UTF_8);
    }

    public String decodeToken(int... tokens) {
        byte[] buffer = new byte[tokens.length * 64];
        int length = 0;
        for (int token : tokens) {
            byte[] bytes = new byte[64];
            int size = LlamaService.getTokenToPiece(model.getLlamaContext(), token, bytes, bytes.length);
            System.arraycopy(bytes, 0, buffer, length, size);
            length += size;
        }
        return new String(buffer, 0, length, StandardCharsets.UTF_8);
    }

    private int getUtf8ByteLength(byte bytes) {
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
            throw new IllegalArgumentException("Illegal token byte, byte code is " + bytes);
        }
    }
}
