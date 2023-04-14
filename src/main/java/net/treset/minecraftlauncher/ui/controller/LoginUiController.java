package net.treset.minecraftlauncher.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import net.treset.minecraftlauncher.LauncherApplication;
import net.treset.minecraftlauncher.config.Config;
import net.treset.minecraftlauncher.ui.UiLoader;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class LoginUiController {
    private static final Logger LOGGER = Logger.getLogger(LoginUiController.class.toString());

    @FXML
    private Button loginButton;
    @FXML
    private Label statusLabel;

    @FXML
    private void onLoginButtonClicked() {
        loginButton.setDisable(true);
        statusLabel.setText(LauncherApplication.stringLocalizer.get("login.label.authenticating"));
        LauncherApplication.userAuth.authenticate(Config.AUTH_FILE, this::onLoginDone);
    }

    private void onLoginDone(Boolean success) {
        if(success) {
            statusLabel.setText(LauncherApplication.stringLocalizer.get("login.label.success", LauncherApplication.userAuth.getMinecraftUser().name()));
            LOGGER.log(Level.INFO, "Login success");
        } else {
            loginButton.setDisable(false);
            statusLabel.setText(LauncherApplication.stringLocalizer.get("login.label.failure"));
            LOGGER.log(Level.INFO, "Login failed");
        }

    }

    public static LoginUiController showOnStage(Stage stage) throws IOException {
        LoginUiController controller = UiLoader.loadFxmlOnStage("LoginUi", stage, "login.title").getController();
        stage.show();
        return controller;
    }
}
