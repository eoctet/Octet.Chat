package chat.octet.model.functions.impl;

import chat.octet.model.functions.AbstractFunction;
import chat.octet.model.functions.FunctionDescriptor;
import chat.octet.model.functions.FunctionInput;
import chat.octet.model.functions.FunctionOutput;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provide a function to query the current time,
 * which is an example implementation. You can refer to this function for extension.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
@Slf4j
public class DataTimeFunction extends AbstractFunction {

    private final DateTimeFormatter formatter;

    public DataTimeFunction(FunctionDescriptor desc) {
        super(desc);
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    }

    @Override
    public FunctionOutput execute(FunctionInput params) {
        FunctionOutput result = new FunctionOutput();
        result.put("now", formatter.format(LocalDateTime.now()));
        return result;
    }
}
