package chat.octet.app.core.markdown;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.emoji.Emoji;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem;
import com.vladsch.flexmark.ext.tables.*;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.sequence.Escaping;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Getter
public class NodeRenderer {
    private int listItemOrder = 0;

    private final Set<NodeRendererHandler<?>> handlers = new HashSet<>(Arrays.asList(
            new NodeRendererHandler<>(BlockQuote.class, this::render),
            new NodeRendererHandler<>(BulletList.class, this::render),
            new NodeRendererHandler<>(Document.class, this::render),
            new NodeRendererHandler<>(FencedCodeBlock.class, this::render),
            new NodeRendererHandler<>(Heading.class, this::render),
            new NodeRendererHandler<>(HtmlBlock.class, this::render),
            new NodeRendererHandler<>(HtmlCommentBlock.class, this::render),
            new NodeRendererHandler<>(IndentedCodeBlock.class, this::render),
            new NodeRendererHandler<>(BulletListItem.class, this::render),
            new NodeRendererHandler<>(OrderedListItem.class, this::render),
            new NodeRendererHandler<>(OrderedList.class, this::render),
            new NodeRendererHandler<>(Paragraph.class, this::render),
            new NodeRendererHandler<>(Reference.class, this::render),
            new NodeRendererHandler<>(ThematicBreak.class, this::render),
            new NodeRendererHandler<>(HtmlInnerBlock.class, this::render),
            new NodeRendererHandler<>(HtmlInnerBlockComment.class, this::render),
            new NodeRendererHandler<>(HtmlEntity.class, this::render),
            new NodeRendererHandler<>(HtmlInline.class, this::render),
            new NodeRendererHandler<>(HtmlInlineComment.class, this::render),
            new NodeRendererHandler<>(AutoLink.class, this::render),
            new NodeRendererHandler<>(Code.class, this::render),
            new NodeRendererHandler<>(CodeBlock.class, this::render),
            new NodeRendererHandler<>(Emphasis.class, this::render),
            new NodeRendererHandler<>(HardLineBreak.class, this::render),
            new NodeRendererHandler<>(Image.class, this::render),
            new NodeRendererHandler<>(ImageRef.class, this::render),
            new NodeRendererHandler<>(Link.class, this::render),
            new NodeRendererHandler<>(LinkRef.class, this::render),
            new NodeRendererHandler<>(MailLink.class, this::render),
            new NodeRendererHandler<>(SoftLineBreak.class, this::render),
            new NodeRendererHandler<>(StrongEmphasis.class, this::render),
            new NodeRendererHandler<>(Text.class, this::render),
            new NodeRendererHandler<>(TextBase.class, this::render),
            new NodeRendererHandler<>(Strikethrough.class, this::render),
            new NodeRendererHandler<>(TaskListItem.class, this::render),
            new NodeRendererHandler<>(TableBlock.class, this::render),
            new NodeRendererHandler<>(TableHead.class, this::render),
            new NodeRendererHandler<>(TableBody.class, this::render),
            new NodeRendererHandler<>(TableRow.class, this::render),
            new NodeRendererHandler<>(TableCell.class, this::render),
            new NodeRendererHandler<>(TableSeparator.class, this::render),
            new NodeRendererHandler<>(Emoji.class, this::render)
    ));

    public NodeRenderer() {
    }

    //Text node render

    public void render(Document node, NodeRendererContext context, MarkdownNode markdown) {
        context.renderChildren(node);
    }

    public void render(Heading node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addContentLine();
        markdown.addHeadingClassAttribute(node.getLevel());
        context.renderChildren(node);
    }

    public void render(Code node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addClassAttribute(MarkdownNode.TextClassType.CODE);
        context.renderChildren(node);
    }

    public void render(Emphasis node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addClassAttribute(MarkdownNode.TextClassType.EMPHASIS);
        context.renderChildren(node);
    }

    public void render(StrongEmphasis node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addClassAttribute(MarkdownNode.TextClassType.STRONG_EMPHASIS);
        context.renderChildren(node);
    }

    public void render(Strikethrough node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addClassAttribute(MarkdownNode.TextClassType.STRIKETHROUGH);
        context.renderChildren(node);
    }

    public void render(Text node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.appendContent(node.getChars());
    }

    public void render(TextBase node, NodeRendererContext context, MarkdownNode markdown) {
        context.renderChildren(node);
    }

    public void render(ThematicBreak node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addThematicBreak();
    }

    public void render(HardLineBreak node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.appendHardLineBreak();
    }

    public void render(SoftLineBreak node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.appendSoftLineBreak();
    }

    public void render(Paragraph node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addContentLine();
        if (node.hasChildren()) {
            context.renderChildren(node);
        }
    }

    //Block node render

    public void render(BlockQuote node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addQuoteBlock();
        context.renderChildren(node);
        markdown.closeBlock();
    }

    public void render(CodeBlock node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addCodeBlock(node);
    }

    public void render(FencedCodeBlock node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addCodeBlock(node);
    }

    public void render(IndentedCodeBlock node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addCodeBlock(node);
    }

    public void render(HtmlBlock node, NodeRendererContext context, MarkdownNode markdown) {
        if (node.hasChildren()) {
            context.renderChildren(node);
        } else {
            markdown.addHtmlBlock(node);
        }
    }

    public void render(HtmlCommentBlock node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addHtmlBlock(node);
    }

    public void render(HtmlInnerBlock node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addHtmlBlock(node);
    }

    public void render(HtmlInnerBlockComment node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addHtmlBlock(node);
    }

    public void render(HtmlEntity node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.appendContent(node.getChars());
    }

    public void render(HtmlInline node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.appendContent(node.getChars());
    }

    public void render(HtmlInlineComment node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.appendContent(node.getChars());
    }

    //List block node render

    private void renderList(ListBlock node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addListBlock();
        context.renderChildren(node);
        markdown.closeBlock();
    }

    private void renderListItem(ListItem node, NodeRendererContext context, MarkdownNode markdown, MarkdownNode.ListItemMarker marker) {
        if (node.isOrderedItem()) {
            ++listItemOrder;
        }
        markdown.addListItem(marker, listItemOrder);
        context.renderChildren(node);
        markdown.closeBlock().closeBlock();
    }

    public void render(BulletList node, NodeRendererContext context, MarkdownNode markdown) {
        renderList(node, context, markdown);
    }

    public void render(BulletListItem node, NodeRendererContext context, MarkdownNode markdown) {
        renderListItem(node, context, markdown, MarkdownNode.ListItemMarker.BULLET);
    }

    public void render(OrderedList node, NodeRendererContext context, MarkdownNode markdown) {
        listItemOrder = 0;
        renderList(node, context, markdown);
    }

    public void render(OrderedListItem node, NodeRendererContext context, MarkdownNode markdown) {
        renderListItem(node, context, markdown, MarkdownNode.ListItemMarker.ORDERED);
    }

    public void render(TaskListItem node, NodeRendererContext context, MarkdownNode markdown) {
        MarkdownNode.ListItemMarker marker = node.isItemDoneMarker() ?
                MarkdownNode.ListItemMarker.TASK_CHECKED : MarkdownNode.ListItemMarker.TASK_UNCHECKED;
        renderListItem(node, context, markdown, marker);
    }

    //Link node render

    public void render(Reference node, NodeRendererContext context, MarkdownNode markdown) {
    }

    public void render(AutoLink node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.link(Escaping.normalizeEOL(node.getText().unescape()), Escaping.percentEncodeUrl(node.getUrl().unescape()));
    }

    public void render(Link node, NodeRendererContext context, MarkdownNode markdown) {
        if (node.getFirstChild() instanceof Image) {
            context.renderChildren(node);
        } else {
            String text;
            if (node.getFirstChild() instanceof Code cn) {
                text = Escaping.normalizeEOL(cn.getText().unescape());
            } else {
                text = Escaping.normalizeEOL(node.getText().unescape());
            }
            markdown.link(text, Escaping.percentEncodeUrl(node.getUrl().unescape()));
        }
    }

    public void render(LinkRef node, NodeRendererContext context, MarkdownNode markdown) {
        String referenceId = node.getReference().unescape();
        if (!node.isDefined()) {
            if (context.getReferenceNode(referenceId) != null) {
                node.setDefined(true);
            }
        }

        if (node.isDefined()) {
            Reference reference = context.getReferenceNode(referenceId);
            String text;
            if (StringUtils.isBlank(node.getText().unescape())) {
                text = Escaping.normalizeEOL(node.getReference().unescape());
            } else {
                text = Escaping.normalizeEOL(node.getText().unescape());
            }

            String url = Escaping.percentEncodeUrl(reference.getUrl().unescape());
            markdown.link(text, url);
        } else {
            markdown.appendContent(node.getChars().unescape());
        }
    }

    public void render(MailLink node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.link(Escaping.normalizeEOL(node.getText().unescape()), Escaping.percentEncodeUrl(node.getUrl().unescape()));
    }

    public void render(Image node, NodeRendererContext context, MarkdownNode markdown) {
        String imageUrl = Escaping.percentEncodeUrl(node.getUrl().unescape());
        ImageView view = markdown.createImageView(Escaping.percentEncodeUrl(imageUrl));

        if (view != null) {
            if (node.getParent() instanceof Link link) {
                view.getStyleClass().add("md-link");
                String linkUrl = Escaping.percentEncodeUrl(link.getUrl().unescape());
                markdown.addURLClickEvent(view, linkUrl);
            }
            markdown.addChildrenNode(view);
        } else {
            if (node.getParent() instanceof Link link) {
                markdown.link(Escaping.normalizeEOL(node.getText().unescape()), Escaping.percentEncodeUrl(link.getUrl().unescape()));
            } else {
                markdown.appendContent(node.getText().unescape());
            }
        }
    }

    public void render(ImageRef node, NodeRendererContext context, MarkdownNode markdown) {
        String referenceId = node.getReference().unescape();
        if (!node.isDefined()) {
            if (context.getReferenceNode(referenceId) != null) {
                node.setDefined(true);
            }
        }

        if (node.isDefined()) {
            Reference reference = context.getReferenceNode(referenceId);
            String url = Escaping.percentEncodeUrl(reference.getUrl().unescape());

            ImageView view = markdown.createImageView(url);
            if (view != null) {
                markdown.clearClassAttributes().appendContent(view);
            } else {
                String text;
                if (StringUtils.isBlank(reference.getTitle().unescape())) {
                    text = referenceId;
                } else {
                    text = Escaping.normalizeEOL(reference.getTitle().unescape());
                }
                markdown.clearClassAttributes().appendContent(text);
            }
        } else {
            markdown.appendContent(node.getChars().unescape());
        }
    }

    //Table node render
    public void render(TableBlock node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.addContentLine();
        markdown.addTableBlock();
        context.renderChildren(node);
        markdown.closeBlock();
    }

    public void render(TableHead node, NodeRendererContext context, MarkdownNode markdown) {
        context.renderChildren(node);
    }

    public void render(TableBody node, NodeRendererContext context, MarkdownNode markdown) {
        context.renderChildren(node);
    }

    public void render(TableRow node, NodeRendererContext context, MarkdownNode markdown) {
        MarkdownNode.TableNode table = markdown.getTableNode();
        if (table != null) {
            table.addTableRow();
            context.renderChildren(node);
        }
    }

    public void render(TableCell node, NodeRendererContext context, MarkdownNode markdown) {
        MarkdownNode.TableNode table = markdown.getTableNode();
        if (table != null) {
            if (node.hasChildren()) {
                context.renderChildren(node);
            }
            TextFlow content = markdown.getContent();
            table.addTableCell(content, node.isHeader(), node.getSpan(), Optional.ofNullable(node.getAlignment()).orElse(TableCell.Alignment.LEFT));
            markdown.newContent();
        }
    }

    private void render(TableSeparator node, NodeRendererContext context, MarkdownNode markdown) {
        String separator = node.getChars().unescape();
        MarkdownNode.TableNode table = markdown.getTableNode();
        if (table != null) {
            table.updateTableColumnWidth(separator);
        }
    }

    //Emoji node render

    public void render(Emoji node, NodeRendererContext context, MarkdownNode markdown) {
        markdown.appendContent(node.getChars());
    }

}
