package chat.octet.model.functions;


/**
 * Function calls API.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public interface Function {

    /**
     * Execute function.
     *
     * @param params function input parameters.
     * @return function output.
     */
    FunctionOutput execute(FunctionInput params);

    /**
     * Return function descriptor, including name, description, parameters, etc.
     *
     * @return function descriptor.
     */
    FunctionDescriptor getDesc();
}
