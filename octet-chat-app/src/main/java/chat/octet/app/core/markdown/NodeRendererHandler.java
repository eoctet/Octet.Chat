package chat.octet.app.core.markdown;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.visitor.AstAction;
import com.vladsch.flexmark.util.visitor.AstHandler;

public class NodeRendererHandler<N extends Node> extends AstHandler<N, NodeRendererHandler.CustomNodeRenderer<N>> {
    public NodeRendererHandler(Class<N> aClass, CustomNodeRenderer<N> adapter) {
        super(aClass, adapter);
    }

    public void render(Node node, NodeRendererContext context, MarkdownNode markdown) {
        //noinspection unchecked
        getAdapter().render((N) node, context, markdown);
    }

    public interface CustomNodeRenderer<N extends Node> extends AstAction<N> {
        void render(N node, NodeRendererContext context, MarkdownNode markdown);
    }
}