package net.treset.minecraftlauncher.ui.create;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.treset.minecraftlauncher.LauncherApplication;
import net.treset.minecraftlauncher.creation.InstanceCreator;
import net.treset.minecraftlauncher.data.LauncherFiles;
import net.treset.minecraftlauncher.ui.base.UiElement;
import net.treset.minecraftlauncher.ui.generic.PopupElement;
import net.treset.minecraftlauncher.util.CreationStatus;
import net.treset.minecraftlauncher.util.exception.ComponentCreationException;
import net.treset.minecraftlauncher.util.exception.FileLoadException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class InstanceCreatorElement extends UiElement {
    private static final Logger LOGGER = LogManager.getLogger(InstanceCreatorElement.class);

    @FXML private HBox rootPane;
    @FXML private ScrollPane spContainer;
    @FXML private TextField nameInput;
    @FXML private Label lbNameError;
    @FXML private VersionCreatorElement icVersionCreatorController;
    @FXML private SavesCreatorElement icSavesCreatorController;
    @FXML private ResourcepacksCreatorElement icResourcepacksCreatorController;
    @FXML private OptionsCreatorElement icOptionsCreatorController;
    @FXML private ModsCreatorElement icModsCreatorController;
    @FXML private VBox modsContainer;
    @FXML private Button btCreate;
    @FXML private PopupElement popupController;
    private LauncherFiles launcherFiles;
    private boolean modsActive = true;
    private void onCreateStatusChanged(CreationStatus status) {
        StringBuilder message = new StringBuilder(status.getCurrentStep().getMessage());
        if(status.getDownloadStatus() != null) {
            message.append("\n").append(status.getDownloadStatus().getCurrentFile()).append("\n(").append(status.getDownloadStatus().getCurrentAmount()).append("/").append(status.getDownloadStatus().getTotalAmount()).append(")");
        }
        Platform.runLater(()-> popupController.setMessage(message.toString()));
    }

    @FXML
    private void onCreate() {
        icModsCreatorController.setGameVersion(icVersionCreatorController.getGameVersion());
        icModsCreatorController.setModsType(icVersionCreatorController.getVersionType());
        if(checkCreateReady() && setLock(true)) {
            InstanceCreator creator;
            try {
                creator = new InstanceCreator(
                        nameInput.getText(),
                        launcherFiles.getLauncherDetails().getTypeConversion(),
                        launcherFiles.getInstanceManifest(),
                        List.of(),
                        List.of(),
                        List.of(),
                        modsActive ? icModsCreatorController.getCreator() : null,
                        icOptionsCreatorController.getCreator(),
                        icResourcepacksCreatorController.getCreator(),
                        icSavesCreatorController.getCreator(),
                        icVersionCreatorController.getCreator()
                );
                creator.setStatusCallback(this::onCreateStatusChanged);
            } catch (ComponentCreationException e) {
                LauncherApplication.displayError(e);
                return;
            }
            spContainer.getStyleClass().add("popup-background");
            popupController.setType(PopupElement.PopupType.NONE);
            popupController.setContent("creator.instance.popup.label.creating", "");
            popupController.setVisible(true);
            new Thread(() -> {
                try {
                    creator.getId();
                    onInstanceCreationSuccess();
                } catch (Exception e) {
                    onInstanceCreationFailure(e);
                }
            }).start();

        } else {
            showError(true);
            icModsCreatorController.setGameVersion(null);
            icModsCreatorController.setModsType(null);
        }
    }

    private void onInstanceCreationSuccess() {
        LOGGER.info("Created instance");
        Platform.runLater(() -> {
            popupController.setType(PopupElement.PopupType.SUCCESS);
            popupController.setContent("creator.instance.popup.label.success", "");
            popupController.setControlsDisabled(false);
            setLock(false);
        });
    }

    private void onInstanceCreationFailure(Exception e) {
        LOGGER.error("Failed to create instance", e);
        Platform.runLater(() -> {
            popupController.setType(PopupElement.PopupType.ERROR);
            popupController.setTitle("creator.instance.popup.label.failure");
            popupController.setMessage("error.message", e.getMessage());
            popupController.setControlsDisabled(false);
            setLock(false);
        });
    }

    private void onBackButtonClicked(String id) {
        triggerHomeAction();
    }

    @Override
    public void beforeShow(Stage stage) {
        popupController.setContent("creator.instance.popup.label.undefined", "");
        popupController.clearControls();
        popupController.addButtons(new PopupElement.PopupButton(
                PopupElement.ButtonType.POSITIVE,
                "creator.instance.popup.button.back",
                "backButton",
                this::onBackButtonClicked
        ));
        popupController.setControlsDisabled(true);
        popupController.setVisible(false);
        spContainer.getStyleClass().remove("popup-background");
        spContainer.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        spContainer.setVvalue(0);
        try {
            launcherFiles = new LauncherFiles();
            launcherFiles.reloadAll();
        } catch (FileLoadException e) {
            LauncherApplication.displaySevereError(e);
        }
        lbNameError.setVisible(false);
        nameInput.getStyleClass().remove("error");
        nameInput.setText("");
        icVersionCreatorController.setPrerequisites(launcherFiles.getLauncherDetails().getTypeConversion(), launcherFiles.getVersionManifest(), launcherFiles, LauncherApplication.config.BASE_DIR + launcherFiles.getLauncherDetails().getLibrariesDir(), this::onModsChange);
        icSavesCreatorController.setPrerequisites(launcherFiles.getSavesComponents(), launcherFiles.getLauncherDetails().getTypeConversion(), launcherFiles.getSavesManifest(), launcherFiles.getGameDetailsManifest());
        icResourcepacksCreatorController.setPrerequisites(launcherFiles.getResourcepackComponents(), launcherFiles.getLauncherDetails().getTypeConversion(), launcherFiles.getResourcepackManifest());
        icOptionsCreatorController.setPrerequisites(launcherFiles.getOptionsComponents(), launcherFiles.getLauncherDetails().getTypeConversion(), launcherFiles.getOptionsManifest());
        icModsCreatorController.setPrerequisites(launcherFiles.getModsComponents(), launcherFiles.getLauncherDetails().getTypeConversion(), launcherFiles.getModsManifest(), launcherFiles.getGameDetailsManifest());
        icVersionCreatorController.beforeShow(stage);
        icSavesCreatorController.beforeShow(stage);
        icResourcepacksCreatorController.beforeShow(stage);
        icOptionsCreatorController.beforeShow(stage);
        icModsCreatorController.beforeShow(stage);
        onModsChange(false);
    }

    @Override
    public void afterShow(Stage stage) {}

    @Override
    public void setRootVisible(boolean visible) {
        rootPane.setVisible(visible);
    }

    public void showError(boolean show) {
        lbNameError.setVisible(false);
        nameInput.getStyleClass().remove("error");
        if(show && nameInput.getText().isEmpty()) {
            lbNameError.setVisible(true);
            nameInput.getStyleClass().add("error");
        }
        icVersionCreatorController.showError(show);
        icSavesCreatorController.showError(show);
        icResourcepacksCreatorController.showError(show);
        icOptionsCreatorController.showError(show);
        if(modsActive)
            icModsCreatorController.showError(show);
    }

    public boolean checkCreateReady() {
        return !nameInput.getText().isEmpty() && icVersionCreatorController.checkCreateReady() && icSavesCreatorController.checkCreateReady() && icResourcepacksCreatorController.checkCreateReady() && icOptionsCreatorController.checkCreateReady() && (!modsActive || icModsCreatorController.checkCreateReady());
    }

    public void onModsChange(boolean active) {
        if(active == modsActive) {
            return;
        }
        modsActive = active;
        modsContainer.setDisable(!active);
    }
}
