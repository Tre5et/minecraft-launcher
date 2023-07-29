package net.treset.minecraftlauncher.ui.manager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import net.treset.mc_version_loader.exception.FileDownloadException;
import net.treset.mc_version_loader.launcher.LauncherManifest;
import net.treset.mc_version_loader.launcher.LauncherMod;
import net.treset.mc_version_loader.launcher.LauncherModDownload;
import net.treset.mc_version_loader.launcher.LauncherModsDetails;
import net.treset.mc_version_loader.mods.ModUtil;
import net.treset.minecraftlauncher.LauncherApplication;
import net.treset.minecraftlauncher.ui.base.UiElement;
import net.treset.minecraftlauncher.ui.generic.lists.ChangeEvent;
import net.treset.minecraftlauncher.ui.generic.lists.ModContentElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AddModElement extends UiElement {
    @FXML private AnchorPane rootPane;
    @FXML private TextField tfName;
    @FXML private TextField tfPath;
    @FXML private Label lbFileError;
    @FXML private TextField tfVersion;
    @FXML private Label lbVersionError;
    @FXML private TextField tfModrinth;
    @FXML private Label lbModrinthError;
    @FXML private TextField tfCurseforge;
    @FXML private Label lbCurseforgeError;

    private Pair<LauncherManifest, LauncherModsDetails> details;
    private ChangeEvent<LauncherMod, ModContentElement> searchChangeCallback;

    public void init(Pair<LauncherManifest, LauncherModsDetails> details, ChangeEvent<LauncherMod, ModContentElement> searchChangeCallback) {
        this.details = details;
        this.searchChangeCallback = searchChangeCallback;
    }

    @FXML
    private void onFileSelector() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Mod files (*.jar)", "*.jar"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File chosen = fileChooser.showOpenDialog(LauncherApplication.primaryStage);
        if(chosen != null && chosen.isFile()) {
            tfPath.setText(chosen.getAbsolutePath());
        }
    }

    @FXML
    private void onCancel() {
        setVisible(false);
    }

    @FXML
    private void onAdd() {
        lbFileError.setVisible(false);
        tfPath.getStyleClass().remove("error");
        if(!isComplete()) {
            displayError();
            return;
        }
        moveModFile();
        setVisible(false);
    }

    private void moveModFile() {
        File modFile = new File(tfPath.getText());
        String fileName = modFile.getName();
        String name = tfName.getText().isEmpty() ? fileName.substring(0, fileName.length() - 4) : tfName.getText();
        List<LauncherModDownload> downloads = getDownloads();
        String url = downloads.isEmpty() ? null : downloads.get(0).getProvider().equals("modrinth") ? "https://modrinth.com/project/" + downloads.get(0).getId() : "https://www.curseforge.com/projects/" + downloads.get(0).getId();
        LauncherMod mod = new LauncherMod(
                null,
                null,
                true,
                url,
                getIconUrl(),
                name,
                fileName,
                tfVersion.getText(),
                getDownloads()
        );
        File newFile = new File(details.getKey().getDirectory(), fileName);

        if(newFile.exists()) {
            LauncherApplication.displayError(new IOException("Unable to add local mod: File already exists: " + fileName));
            return;
        }

        try {
            Files.copy(modFile.toPath(), newFile.toPath());
        } catch (IOException e) {
            LauncherApplication.displayError(e);
            return;
        }

        ArrayList<LauncherMod> mods = new ArrayList<>(details.getValue().getMods());
        mods.add(mod);
        details.getValue().setMods(mods);

        searchChangeCallback.update();
    }

    private boolean modrinthValid() {
        if(!tfModrinth.getText().isEmpty()) {
            try {
                return ModUtil.checkModrinthValid(tfModrinth.getText());
            } catch (FileDownloadException e) {
                return false;
            }
        }
        return true;
    }

    private boolean curseforgeValid() {
        if(!tfCurseforge.getText().isEmpty()) {
            if(!tfCurseforge.getText().matches("[0-9]+")) return false;
            try {
                return ModUtil.checkCurseforgeValid(Integer.parseInt(tfCurseforge.getText()));
            } catch (FileDownloadException e) {
                return false;
            }
        }
        return true;
    }

    private String getIconUrl() {
        if(!tfModrinth.getText().isEmpty()) {
            try {
                return ModUtil.getModrinthMod(tfModrinth.getText()).getIconUrl();
            } catch (FileDownloadException ignored) {}
        }
        if(!tfCurseforge.getText().isEmpty()) {
            try {
                return ModUtil.getCurseforgeMod(Integer.parseInt(tfCurseforge.getText())).getIconUrl();
            } catch (FileDownloadException ignored) {}
        }
        return null;
    }

    private List<LauncherModDownload> getDownloads() {
        ArrayList<LauncherModDownload> downloads = new ArrayList<>();
        if(!tfModrinth.getText().isEmpty()) {
            downloads.add(new LauncherModDownload(
                    "modrinth",
                    tfModrinth.getText()
            ));
        }
        if(!tfCurseforge.getText().isEmpty()) {
            downloads.add(new LauncherModDownload(
                    "curseforge",
                    tfCurseforge.getText()
            ));
        }
        return downloads;
    }

    public void displayError() {
        tfVersion.getStyleClass().remove("error");
        lbVersionError.setVisible(false);
        tfPath.getStyleClass().remove("error");
        lbFileError.setVisible(false);
        tfModrinth.getStyleClass().remove("error");
        lbModrinthError.setVisible(false);
        tfCurseforge.getStyleClass().remove("error");
        lbCurseforgeError.setVisible(false);
        if (tfVersion.getText().isEmpty()) {
            tfVersion.getStyleClass().add("error");
            lbVersionError.setVisible(true);
        }
        if (tfPath.getText().isEmpty() || !tfPath.getText().endsWith(".jar") || !new File(tfPath.getText()).isFile()) {
            tfPath.getStyleClass().add("error");
            lbFileError.setVisible(true);
        }
        if(!modrinthValid()) {
            tfModrinth.getStyleClass().add("error");
            lbModrinthError.setVisible(true);
        }
        if(!curseforgeValid()) {
            tfCurseforge.getStyleClass().add("error");
            lbCurseforgeError.setVisible(true);
        }
    }

    public boolean isComplete() {
        return !tfVersion.getText().isEmpty() && !tfPath.getText().isEmpty() && tfPath.getText().endsWith(".jar") && new File(tfPath.getText()).isFile() && modrinthValid() && curseforgeValid();
    }

    @Override
    public void setRootVisible(boolean visible) {
        rootPane.setVisible(visible);
    }

    @Override
    public void beforeShow(Stage stage) {
        tfName.clear();
        tfPath.clear();
        tfVersion.clear();
        tfModrinth.clear();
        tfCurseforge.clear();
        tfPath.getStyleClass().remove("error");
        lbFileError.setVisible(false);
        tfVersion.getStyleClass().remove("error");
        lbVersionError.setVisible(false);
        tfModrinth.getStyleClass().remove("error");
        lbModrinthError.setVisible(false);
        tfCurseforge.getStyleClass().remove("error");
        lbCurseforgeError.setVisible(false);
    }

    @Override
    public void afterShow(Stage stage) {}
}
