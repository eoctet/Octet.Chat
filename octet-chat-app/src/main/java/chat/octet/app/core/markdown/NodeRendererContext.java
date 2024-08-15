package chat.octet.app.core.markdown;


import com.vladsch.flexmark.ast.Reference;
import com.vladsch.flexmark.util.ast.Node;

public interface NodeRendererContext {

    void renderChildren(Node parent);

    Reference getReferenceNode(String id);

}
