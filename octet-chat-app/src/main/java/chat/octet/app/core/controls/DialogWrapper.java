package chat.octet.app.core.controls;


import chat.octet.app.AppResourcesLoader;
import chat.octet.app.core.enums.ThemeType;
import chat.octet.app.utils.MessageI18N;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import io.github.palexdev.materialfx.enums.ScrimPriority;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Window;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public final class DialogWrapper {

    private final MFXGenericDialog dialogContent;
    private final MFXStageDialog dialog;
    private DialogType dialogType;

    private DialogWrapper() {
        this.dialogContent = MFXGenericDialogBuilder.build()
                .makeScrollable(true)
                .setShowMinimize(false)
                .setShowAlwaysOnTop(false)
                .get();
        this.dialogContent.setMinSize(300, 150);
        this.dialogContent.setMaxSize(400, 300);

        this.dialog = MFXGenericDialogBuilder.build(dialogContent)
                .toStageDialogBuilder()
                .initModality(Modality.APPLICATION_MODAL)
                .setDraggable(true)
                .setScrimPriority(ScrimPriority.WINDOW)
                .setScrimOwner(true)
                .get();

        this.dialogType = DialogType.INFO;
    }

    public static DialogWrapper build() {
        return new DialogWrapper();
    }

    public DialogWrapper setOwnerWindow(Window owner) {
        this.dialog.initOwner(owner);
        return this;
    }

    public DialogWrapper setOwnerNode(Pane ownerNode) {
        this.dialog.setOwnerNode(ownerNode);
        return this;
    }

    public DialogWrapper setTitle(String title) {
        this.dialogContent.setHeaderText(title);
        return this;
    }

    public DialogWrapper setContent(String content) {
        this.dialogContent.setContentText(content);
        return this;
    }

    public DialogWrapper setTheme(ThemeType theme) {
        this.dialogContent.getStylesheets().setAll(AppResourcesLoader.load("css/" + theme.getValue().toLowerCase() + ".css"));
        return this;
    }

    public DialogWrapper setType(DialogType dialogType) {
        this.dialogType = dialogType;
        return this;
    }

    public boolean showConfirmDialog() {
        MFXFontIcon infoIcon = new MFXFontIcon(dialogType.getIconName(), 16);
        dialogContent.setHeaderIcon(infoIcon);
        dialogContent.getStyleClass().add(dialogType.getClassName());


        AtomicBoolean confirm = new AtomicBoolean(false);

        MFXButton confirmBtn = new MFXButton(MessageI18N.get("ui.main.dialog.confirm"));
        confirmBtn.getStyleClass().add("primary-button");

        MFXButton cancelBtn = new MFXButton(MessageI18N.get("ui.main.dialog.cancel"));
        cancelBtn.getStyleClass().add("second-button");

        dialogContent.addActions(
                Map.entry(cancelBtn, event -> dialog.close()),
                Map.entry(confirmBtn, event -> {
                    confirm.set(true);
                    dialog.close();
                })
        );
        dialog.showAndWait();
        return confirm.get();
    }

    public void showDialog() {
        MFXFontIcon infoIcon = new MFXFontIcon(dialogType.getIconName(), 16);
        this.dialogContent.setHeaderIcon(infoIcon);
        this.dialogContent.getStyleClass().add(dialogType.getClassName());

        MFXButton defaultBtn = new MFXButton(MessageI18N.get("ui.main.dialog.ok"));
        defaultBtn.getStyleClass().add("primary-button");

        if (DialogType.ERROR == dialogType) {
            MFXButton copyBtn = new MFXButton(MessageI18N.get("ui.main.dialog.copy"));
            copyBtn.getStyleClass().add("second-button");

            this.dialogContent.addActions(Map.entry(copyBtn, event -> {
                try {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(this.dialogContent.getContentText());
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    clipboard.setContent(content);
                } catch (Exception e) {
                    log.error("Failed to copy message", e);
                } finally {
                    dialog.close();
                }
            }));
        }
        this.dialogContent.addActions(Map.entry(defaultBtn, event -> dialog.close()));
        this.dialog.showAndWait();
    }

    @Getter
    public enum DialogType {
        INFO("fas-circle-info", "mfx-info-dialog"),
        ERROR("fas-circle-xmark", "mfx-error-dialog"),
        WARN("fas-circle-exclamation", "mfx-warn-dialog");

        private final String iconName;
        private final String className;

        DialogType(String iconName, String className) {
            this.iconName = iconName;
            this.className = className;
        }
    }

}
