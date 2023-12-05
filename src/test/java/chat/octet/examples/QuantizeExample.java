package chat.octet.examples;


import chat.octet.model.LlamaService;
import chat.octet.model.enums.ModelFileType;

public class QuantizeExample {

    public static void main(String[] args) {
        int status = LlamaService.llamaModelQuantize("YOUR_SOURCE_MODEL_FILE.gguf",
                "OUTPUT_MODEL_FILE.gguf",
                ModelFileType.LLAMA_FTYPE_MOSTLY_Q8_0
        );
        System.out.println("Quantize status: " + status);
    }
}
