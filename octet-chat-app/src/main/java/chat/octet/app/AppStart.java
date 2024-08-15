package chat.octet.app;

import chat.octet.app.controller.AppMainController;
import chat.octet.app.core.constants.AppConstants;
import chat.octet.app.service.AppService;
import chat.octet.app.service.ChatService;
import chat.octet.app.utils.MessageI18N;
import fr.brouillard.oss.cssfx.CSSFX;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class AppStart extends Application {

    public static void main(String[] args) {
        //System.setProperty("prism.lcdtext", "false");
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        //CSSFX.start();

        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();

        FXMLLoader loader = AppService.get().createFXMLLoader(AppConstants.FXML_MAIN);
        loader.setControllerFactory(c -> new AppMainController(stage));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1024, 800);
        scene.setFill(Color.TRANSPARENT);

        stage.setOnCloseRequest(event -> {
            boolean confirm = AppService.get().showConfirmDialog(MessageI18N.get("ui.message.close.app"));
            if (confirm) {
                ChatService.get().close();
                Platform.exit();
            } else {
                event.consume();
            }
        });
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle(AppConstants.APP_NAME);
        stage.toFront();
        stage.setScene(scene);
        stage.show();
    }
}