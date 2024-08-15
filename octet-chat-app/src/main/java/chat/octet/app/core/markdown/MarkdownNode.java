package chat.octet.app.core.markdown;


import chat.octet.app.core.constants.AppConstants;
import chat.octet.app.service.AppService;
import chat.octet.app.utils.MessageI18N;
import com.google.common.collect.Lists;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HtmlBlockBase;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Escaping;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Stack;

@Slf4j
public class MarkdownNode {
    private final List<TextClassType> classAttributes;
    private final StringBuffer buffer;
    private final VBox root;
    private final Stack<Pane> childrenNodes;
    @Getter
    private TextFlow content;

    private MarkdownNode() {
        this.classAttributes = Lists.newArrayList();
        this.buffer = new StringBuffer();

        this.root = new VBox();
        this.root.getStyleClass().add("md-content");
        this.root.setFillWidth(true);

        this.childrenNodes = new Stack<>();
        this.childrenNodes.push(root);

        newContent();
    }

    public static MarkdownNode build() {
        return new MarkdownNode();
    }

    public static VBox createPaneNode(String... classNames) {
        VBox pane = new VBox();
        pane.getStyleClass().addAll(classNames);
        return pane;
    }

    public Pane getMarkdownNode() {
        return root;
    }

    public void addClassAttribute(TextClassType classType) {
        classAttributes.add(classType);
    }

    public MarkdownNode clearClassAttributes() {
        classAttributes.clear();
        return this;
    }

    private void updateClassAttributes(Node node, String... classNames) {
        if (classNames != null && classNames.length > 0) {
            node.getStyleClass().addAll(classNames);
        }
        if (!classAttributes.isEmpty()) {
            for (TextClassType c : classAttributes) {
                node.getStyleClass().add(c.getClassName());
            }
            classAttributes.clear();
        }
    }

    public void newContent() {
        this.content = new TextFlow();
        this.content.getStyleClass().add("md-text-line");
    }

    public void appendContent(Node node) {
        content.getChildren().add(node);
    }

    public void appendContent(String text) {
        Text node = new Text(text);
        updateClassAttributes(node, TextClassType.NORMAL.getClassName());
        appendContent(node);
    }

    public void appendContent(BasedSequence sequence) {
        appendContent(Escaping.normalizeEOL(sequence.unescape()));
    }

    public void addContentLine() {
        newContent();
        addChildrenNode(content);
    }

    public void addChildrenNode(Node node) {
        childrenNodes.lastElement().getChildren().add(node);
    }

    public MarkdownNode addBlock(Pane parent) {
        childrenNodes.push(parent);
        return this;
    }

    public MarkdownNode closeBlock() {
        Pane parent = childrenNodes.pop();
        addChildrenNode(parent);
        return this;
    }

    public void addHeadingClassAttribute(int level) {
        content.getStyleClass().addAll("md-heading", "md-heading-" + level);
    }

    public void addURLClickEvent(Node node, String url) {
        if (isSupported(url)) {
            node.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                try {
                    if (chat.octet.model.utils.Platform.isLinux()) {
                        Runtime.getRuntime().exec("xdg-open " + url);
                    } else if (chat.octet.model.utils.Platform.isMac()) {
                        Runtime.getRuntime().exec("open " + url);
                    } else if (chat.octet.model.utils.Platform.isWindows()) {
                        Runtime.getRuntime().exec("explorer " + url);
                    } else {
                        log.warn("Unsupported platform: {}", chat.octet.model.utils.Platform.getOSType());
                    }
                } catch (Exception e) {
                    log.error("Error opening link: {}", url, e);
                }
            });
        }
    }

    public void link(String text, String url) {
        Label link = new Label(text);
        updateClassAttributes(link, "md-link");
        addURLClickEvent(link, url);
        appendContent(link);
    }

    public void appendHardLineBreak() {
        appendContent("\n");
    }

    public void appendSoftLineBreak() {
        appendContent(" ");
    }

    public void addThematicBreak() {
        TextFlow line = new TextFlow();
        line.getStyleClass().add("md-thematic-break");
        addChildrenNode(line);
    }

    public TableNode getTableNode() {
        if (childrenNodes.lastElement() instanceof TableNode tb) {
            return tb;
        }
        return null;
    }

    public ImageView createImageView(String url) {
        try {
            Image img = new Image(url, true);
            ImageView view = new ImageView(img);
            view.getStyleClass().add("md-image");
            return view;
        } catch (Exception e) {
            log.warn("Error loading image: {}", url, e);
        }
        return null;
    }

    public boolean isSupported(String url) {
        return StringUtils.startsWithAny(url.toLowerCase(), "http://", "https://");
    }

    public void addQuoteBlock() {
        VBox pane = createPaneNode("md-block-quote");
        addBlock(pane);
    }

    public void addListBlock() {
        VBox pane = createPaneNode("md-list");
        addBlock(pane);
    }

    public void addListItem(ListItemMarker marker, int listItemOrder) {
        ListItemNode item = new ListItemNode();
        item.setListMarker(marker, listItemOrder);
        VBox content = item.newItemContent();

        addBlock(item).addBlock(content);
    }

    public void addTableBlock() {
        TableNode table = new TableNode();
        addBlock(table);
    }

    public void addCodeBlock(Block node) {
        String txt = node.getContentChars().toString();
        if (!buffer.isEmpty()) {
            buffer.append(txt);
            return;
        }

        String infoText = "";
        if (node instanceof FencedCodeBlock fb) {
            if (fb.getInfo().isNotNull() && !fb.getInfo().isBlank()) {
                infoText = fb.getInfo().unescape();
            }
        }
        StackPane block = createCodeBlockNode(txt, infoText);
        addChildrenNode(block);
    }

    public void addHtmlBlock(HtmlBlockBase node) {
        String txt = node.getContentChars().toString();
        buffer.append(txt);

        com.vladsch.flexmark.util.ast.Node next = node.getNext();
        if (!(next instanceof HtmlBlockBase) && !(next instanceof IndentedCodeBlock)) {
            StackPane block = createCodeBlockNode(buffer.toString(), "html");
            addChildrenNode(block);
            buffer.setLength(0);
        }
    }

    private StackPane createCodeBlockNode(String text, String info) {
        MFXFontIcon copyIcon = new MFXFontIcon("fas-copy", 16);
        copyIcon.getStyleClass().add("md-code-copy");
        copyIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            try {
                if (AppConstants.clipboardContent(text)) {
                    AppService.get().showInfoDialog(MessageI18N.get("ui.message.clipboard.copy"));
                }
            } catch (Exception e) {
                log.error("Failed to copy the code block", e);
                AppService.get().showErrorDialog(MessageI18N.get("ui.message.runtime.error", e.getMessage()));
            }
        });

        VBox container = new VBox();
        container.getStyleClass().add("md-code-block");
        container.setFillWidth(true);
        container.getChildren().setAll(new Label(text));

        HBox toolbar = new HBox();
        toolbar.getStyleClass().add("md-code-toolbar");
        if (!info.isEmpty()) {
            Label label = new Label(info.toUpperCase());
            label.getStyleClass().add("md-code-lang");
            toolbar.getChildren().add(label);
        }
        toolbar.getChildren().add(copyIcon);

        StackPane block = new StackPane();
        block.getStyleClass().add("md-code-block-container");
        block.getChildren().addAll(container, toolbar);
        return block;
    }

    @Getter
    public enum TextClassType {
        NORMAL("md-text"),
        EMPHASIS("md-font-italic"),
        STRONG_EMPHASIS("md-font-bold"),
        STRIKETHROUGH("md-font-strikethrough"),
        CODE("md-code-snippet");

        private final String className;

        TextClassType(String className) {
            this.className = className;
        }
    }

    public enum ListItemMarker {
        ORDERED,
        BULLET,
        TASK_CHECKED,
        TASK_UNCHECKED
    }

    public static class ListItemNode extends HBox {

        public ListItemNode() {
            this.getStyleClass().add("md-list-item");
        }

        public void setListMarker(ListItemMarker marker, int listItemOrder) {
            Node markerNode;
            if (ListItemMarker.ORDERED == marker) {
                markerNode = new Label(listItemOrder + ".");
            } else if (ListItemMarker.TASK_CHECKED == marker) {
                MFXFontIcon icon = new MFXFontIcon("fas-square-check", 16);
                markerNode = new VBox(icon);
            } else if (ListItemMarker.TASK_UNCHECKED == marker) {
                MFXFontIcon icon = new MFXFontIcon("fas-square", 16);
                markerNode = new VBox(icon);
            } else {
                markerNode = new Label("•");
                markerNode.getStyleClass().add("md-font-bold");
            }
            markerNode.getStyleClass().addAll("md-text", "md-list-order");
            this.getChildren().add(markerNode);
        }

        public VBox newItemContent() {
            VBox content = createPaneNode("md-list-content");
            content.setFillWidth(true);
            return content;
        }

    }

    public static class TableNode extends GridPane {
        private int rowIndex;
        private int columnIndex;

        public TableNode() {
            this.getStyleClass().add("md-table");
            this.setHgap(-1);
            this.setVgap(-1);
        }

        public void addTableRow() {
            ++rowIndex;
            columnIndex = 0;
        }

        public void updateTableColumnWidth(String separator) {
            String[] columns = separator.split("\\|");

            int totalCount = StringUtils.countMatches(separator, "-");
            for (String line : columns) {
                if (!line.isEmpty()) {
                    int count = StringUtils.countMatches(line, "-");
                    double columnWidth = Math.round(((double) count / totalCount) * 100);

                    ColumnConstraints constraint = new ColumnConstraints();
                    constraint.setPercentWidth(columnWidth);
                    this.getColumnConstraints().add(constraint);
                }
            }
        }

        public void addTableCell(TextFlow content, boolean isHeaderCell, int span, TableCell.Alignment alignment) {
            content.getStyleClass().setAll("md-table-cell");
            switch (alignment) {
                case LEFT:
                    content.setTextAlignment(TextAlignment.LEFT);
                    break;
                case CENTER:
                    content.setTextAlignment(TextAlignment.CENTER);
                    break;
                case RIGHT:
                    content.setTextAlignment(TextAlignment.RIGHT);
                    break;
            }

            if (isHeaderCell) {
                rowIndex = 0;
                content.getStyleClass().add("md-table-header");
            } else {
                if (rowIndex % 2 == 0) {
                    content.getStyleClass().add("md-table-odd");
                } else {
                    content.getStyleClass().add("md-table-even");
                }
            }
            this.add(content, columnIndex, rowIndex, span, span);
            ++columnIndex;
        }

    }

}
