package chat.octet.model.beans;

import lombok.Data;

import java.text.MessageFormat;

@Data
public class Metrics {

    public final static String METRICS_TEMPLATE = "\n\t   load time =\t{0,number,#.##} ms\n" +
            "\t sample time =\t{1,number,#.##} ms / {2} runs\t( {3,number,#.##} ms per token, {4,number,#.##} tokens per second)\n" +
            "prompt eval time =\t{5,number,#.##} ms / {6} tokens\t( {7,number,#.##} ms per token, {8,number,#.##} tokens per second)\n" +
            "\t   eval time =\t{9,number,#.##} ms / {10} runs\t( {11,number,#.##} ms per token, {12,number,#.##} tokens per second)\n" +
            "\t  total time =\t{13,number,#.##} ms";

    private double startTimeMs;
    private double endTimeMs;
    private double loadTimeMs;
    private double sampleTimeMs;
    private double promptEvalTimeMs;
    private double evalTimeMs;
    private int sampleCount;
    private int promptEvalCount;
    private int evalCount;

    @Override
    public String toString() {
        return MessageFormat.format(METRICS_TEMPLATE,
                loadTimeMs,
                sampleTimeMs, sampleCount, (sampleTimeMs / sampleCount), (1e3 / sampleTimeMs * sampleCount),
                promptEvalTimeMs, promptEvalCount, (promptEvalTimeMs / promptEvalCount), (1e3 / promptEvalTimeMs * promptEvalCount),
                evalTimeMs, evalCount, (evalTimeMs / evalCount), (1e3 / evalTimeMs * evalCount),
                (endTimeMs - startTimeMs)
        );
    }
}
