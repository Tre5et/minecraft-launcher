package net.treset.minecraftlauncher.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.treset.minecraftlauncher.LauncherApplication;
import net.treset.minecraftlauncher.ui.base.UiController;

import java.io.IOException;
import java.net.URL;

public class UiLoader {
    public static FXMLLoader getFXMLLoader(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader();
        URL xmlUrl = UiLoader.class.getResource("/fxml/"+fxmlPath+".fxml");
        loader.setLocation(xmlUrl);
        loader.setResources(LauncherApplication.stringLocalizer.getStringBundle());
        return loader;
    }
    public static <T> T loadFXML(String fxmlPath) throws IOException {
        return loadFXML(getFXMLLoader(fxmlPath));
    }

    public static <T> T loadFXML(FXMLLoader fxmlLoader) throws IOException {
        return fxmlLoader.load();
    }

    public static <T extends UiController> T loadFxmlOnStage(String fxmlPath, Stage stage, String title, Object... args) throws IOException {
        FXMLLoader loader = getFXMLLoader(fxmlPath);
        Parent root = loadFXML(loader);

        stage.setTitle(LauncherApplication.stringLocalizer.getFormatted(title, args));
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        T controller = loader.getController();
        return controller;
    }
}
