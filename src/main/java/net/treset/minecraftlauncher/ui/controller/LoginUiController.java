package net.treset.minecraftlauncher.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import net.treset.minecraftlauncher.LauncherApplication;
import net.treset.minecraftlauncher.config.Config;
import net.treset.minecraftlauncher.ui.UiLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;


public class LoginUiController {
    private static final Logger LOGGER = LogManager.getLogger(LoginUiController.class);

    @FXML
    public Button loginButton;
    @FXML
    public Button continueButton;
    @FXML
    public Label statusLabel;

    @FXML
    public void onLoginButtonClicked() {
        loginButton.setDisable(true);
        statusLabel.setText(LauncherApplication.stringLocalizer.get("login.label.authenticating"));
        new Thread(() -> LauncherApplication.userAuth.authenticate(Config.AUTH_FILE, this::onLoginDone)).start();
    }

    private void onLoginDone(Boolean success) {
        Platform.runLater(() -> loginDoneActions(success));
    }

    private void loginDoneActions(boolean success) {
        if(success) {
            statusLabel.setText(LauncherApplication.stringLocalizer.get("login.label.success", LauncherApplication.userAuth.getMinecraftUser().name()));
            LOGGER.debug("Login success, username=" + LauncherApplication.userAuth.getMinecraftUser().name());

            continueButton.setVisible(true);


            /*LauncherFiles files = new LauncherFiles();
            files.reloadAll();
            Pair<LauncherManifest, LauncherInstanceDetails> instance = files.getInstanceComponents().get(1);
            GameLauncher gameLauncher = new GameLauncher(instance, files, LauncherApplication.userAuth.getMinecraftUser(), List.of(this::onGameExit));
            gameLauncher.launch();*/
        } else {
            loginButton.setDisable(false);
            statusLabel.setText(LauncherApplication.stringLocalizer.get("login.label.failure"));
            LOGGER.warn("Login failed");
        }
    }

    private void onGameExit(String error) {
        LOGGER.debug("Game exited: " + error);
    }

    public static LoginUiController showOnStage(Stage stage) throws IOException {
        LoginUiController controller = UiLoader.loadFxmlOnStage("LoginUi", stage, "login.title").getController();
        stage.show();
        if(Config.AUTH_FILE.isFile()) {
            controller.onLoginButtonClicked();
        }
        return controller;
    }
}
