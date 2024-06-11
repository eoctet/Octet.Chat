package chat.octet.model.enums;

import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Model file type define
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
public enum ModelFileType {
    LLAMA_FTYPE_ALL_F32(0),
    LLAMA_FTYPE_MOSTLY_F16(1),
    LLAMA_FTYPE_MOSTLY_Q4_0(2),
    LLAMA_FTYPE_MOSTLY_Q4_1(3),
    /**
     * tok_embeddings.weight and output.weight are F16
     */
    LLAMA_FTYPE_MOSTLY_Q4_1_SOME_F16(4),
    LLAMA_FTYPE_MOSTLY_Q8_0(7),
    LLAMA_FTYPE_MOSTLY_Q5_0(8),
    LLAMA_FTYPE_MOSTLY_Q5_1(9),
    LLAMA_FTYPE_MOSTLY_Q2_K(10),
    LLAMA_FTYPE_MOSTLY_Q3_K_S(11),
    LLAMA_FTYPE_MOSTLY_Q3_K_M(12),
    LLAMA_FTYPE_MOSTLY_Q3_K_L(13),
    LLAMA_FTYPE_MOSTLY_Q4_K_S(14),
    LLAMA_FTYPE_MOSTLY_Q4_K_M(15),
    LLAMA_FTYPE_MOSTLY_Q5_K_S(16),
    LLAMA_FTYPE_MOSTLY_Q5_K_M(17),
    LLAMA_FTYPE_MOSTLY_Q6_K(18),
    LLAMA_FTYPE_MOSTLY_IQ2_XXS(19),
    LLAMA_FTYPE_MOSTLY_IQ2_XS(20),
    LLAMA_FTYPE_MOSTLY_Q2_K_S(21),
    LLAMA_FTYPE_MOSTLY_Q3_K_XS(22),
    LLAMA_FTYPE_MOSTLY_IQ3_XXS(23),
    LLAMA_FTYPE_MOSTLY_IQ1_S(24),
    LLAMA_FTYPE_MOSTLY_IQ4_NL(25),
    LLAMA_FTYPE_MOSTLY_IQ3_S(26),
    LLAMA_FTYPE_MOSTLY_IQ3_M(27),
    LLAMA_FTYPE_MOSTLY_IQ2_S(28),
    LLAMA_FTYPE_MOSTLY_IQ2_M(29),
    LLAMA_FTYPE_MOSTLY_IQ4_XS(30),
    LLAMA_FTYPE_MOSTLY_IQ1_M(31),
    LLAMA_FTYPE_MOSTLY_BF16(32),
    /**
     * not specified in the model file
     */
    LLAMA_FTYPE_GUESSED(1024);

    private static final Map<Integer, ModelFileType> TYPES;

    static {
        Map<Integer, ModelFileType> map = Maps.newHashMap();

        for (ModelFileType type : values()) {
            if (map.put(type.type, type) != null) {
                throw new IllegalStateException("Duplicated key found: " + type.name());
            }
        }
        TYPES = Collections.unmodifiableMap(map);
    }

    private final int type;

    ModelFileType(int type) {
        this.type = type;
    }

    public static ModelFileType valueOfType(int type) {
        return TYPES.get(type);
    }

    @Override
    public String toString() {
        return this.name();
    }

}
