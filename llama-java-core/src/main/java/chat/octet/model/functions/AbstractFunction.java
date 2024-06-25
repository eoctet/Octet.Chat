package chat.octet.model.functions;

/**
 * Abstract function.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public abstract class AbstractFunction implements Function {

    private final FunctionDescriptor desc;

    public AbstractFunction(FunctionDescriptor desc) {
        this.desc = desc;
    }

    @Override
    public FunctionDescriptor getDesc() {
        return desc;
    }
}
