package chat.octet.app.core.markdown;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vladsch.flexmark.ast.Reference;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.collection.iteration.ReversiblePeekingIterator;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MarkdownRender {
    private final static List<Extension> EXTENSIONS = Arrays.asList(
            TablesExtension.create(),
            AttributesExtension.create(),
            StrikethroughExtension.create(),
            TaskListExtension.create(),
            EmojiExtension.create()
    );
    private final static MutableDataSet OPTIONS = new MutableDataSet();
    private static volatile MarkdownRender instance;

    static {
        OPTIONS.set(Parser.EXTENSIONS, EXTENSIONS);
        OPTIONS.set(AttributesExtension.FENCED_CODE_INFO_ATTRIBUTES, true);
    }

    private Parser parser;

    private MarkdownRender() {
    }

    public static MarkdownRender get() {
        if (instance == null) {
            synchronized (MarkdownRender.class) {
                if (instance == null) {
                    instance = new MarkdownRender();
                }
            }
        }
        return instance;
    }

    public Parser getParser() {
        if (parser == null) {
            parser = Parser.builder(OPTIONS).build();
        }
        return parser;
    }

    public Pane render(String text) {
        Parser parser = getParser();
        Document document = parser.parse(text);
        return new MainRender().render(document);
    }

    @Slf4j
    public static class MainRender implements NodeRendererContext {
        private final MarkdownNode markdown;
        private final Map<String, NodeRendererHandler<?>> handlers = Maps.newLinkedHashMap();
        private Set<Reference> references;
        private List<Node> markdownNodes;

        public MainRender() {
            this.markdown = MarkdownNode.build();

            NodeRenderer nodeRender = new NodeRenderer();
            nodeRender.getHandlers().forEach(handler -> handlers.put(handler.getNodeType().getSimpleName(), handler));
        }

        public Pane render(Node parent) {
            if (parent instanceof Document doc) {
                parseMarkdownElements(doc);

                for (Node node : markdownNodes) {
                    String nodeTypeName = node.getClass().getSimpleName();
                    NodeRendererHandler<?> handler = handlers.get(nodeTypeName);
                    if (handler == null) {
                        log.warn("Can't find node renderer for node type: {}", nodeTypeName);
                        continue;
                    }
                    handler.render(node, this, markdown);
                }
            } else {
                String nodeTypeName = parent.getClass().getSimpleName();
                NodeRendererHandler<?> handler = handlers.get(nodeTypeName);
                if (handler != null) {
                    handler.render(parent, this, markdown);
                }
            }
            return markdown.getMarkdownNode();
        }

        private void parseMarkdownElements(Document parent) {
            if (markdownNodes != null) {
                markdownNodes.clear();
            }
            ReversiblePeekingIterator<Node> iterator = parent.getChildIterator();
            markdownNodes = Lists.newArrayList(iterator);
            if (references != null) {
                references.clear();
            }
            references = markdownNodes.stream().filter(node -> node instanceof Reference)
                    .map(node -> (Reference) node).collect(Collectors.toSet());
        }

        @Override
        public void renderChildren(com.vladsch.flexmark.util.ast.Node parent) {
            com.vladsch.flexmark.util.ast.Node node = parent.getFirstChild();
            while (node != null) {
                com.vladsch.flexmark.util.ast.Node next = node.getNext();
                render(node);
                node = next;
            }
        }

        @Override
        public Reference getReferenceNode(String id) {
            if (references == null) {
                return null;
            }
            return references.stream().filter(ref -> ref.getReference().unescape().equals(id)).findFirst().orElse(null);
        }
    }
}
