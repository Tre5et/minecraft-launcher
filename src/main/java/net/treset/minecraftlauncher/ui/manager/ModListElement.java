package net.treset.minecraftlauncher.ui.manager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Pair;
import net.treset.mc_version_loader.launcher.LauncherMod;
import net.treset.mc_version_loader.launcher.LauncherModDownload;
import net.treset.mc_version_loader.mods.ModData;
import net.treset.mc_version_loader.mods.ModVersionData;
import net.treset.minecraftlauncher.ui.base.UiElement;
import net.treset.minecraftlauncher.util.UiLoader;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ModListElement extends UiElement {
    @FXML private AnchorPane rootPane;
    @FXML private ImageView logoImage;
    @FXML private Label title;
    @FXML private Label description;
    @FXML private Button installButton;
    @FXML private ComboBox<String> versionSelector;
    @FXML private ImageView modrinthLogo;
    @FXML private ImageView curseforgeLogo;

    private LauncherMod mod;
    private ModData modData;
    private String gameVersion;
    private TriConsumer<ModVersionData, LauncherMod, ModListElement> installCallback;


    @Override
    public void beforeShow(Stage stage) {
        if(mod != null) {
            title.setText(mod.getName());
            description.setText(mod.getDescription());
            versionSelector.getItems().clear();
            versionSelector.getItems().add(mod.getVersion());
            versionSelector.getSelectionModel().select(0);
            modrinthLogo.getStyleClass().remove("current");
            modrinthLogo.getStyleClass().remove("available");
            curseforgeLogo.getStyleClass().remove("current");
            curseforgeLogo.getStyleClass().remove("available");
            installButton.setDisable(true);
            versionSelector.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
                Platform.runLater(this::onVersionSelected);
            });
            for(LauncherModDownload d : mod.getDownloads()) {
                if("modrinth".equals(d.getProvider())) {
                    if("modrinth".equals(mod.getCurrentProvider())) {
                        modrinthLogo.getStyleClass().add("current");
                    } else {
                        modrinthLogo.getStyleClass().add("available");
                    }
                } else if("curseforge".equals(d.getProvider())) {
                    if("curseforge".equals(mod.getCurrentProvider())) {
                        curseforgeLogo.getStyleClass().add("current");
                    } else {
                        curseforgeLogo.getStyleClass().add("available");
                    }
                }
            }
        }
    }

    @Override
    public void afterShow(Stage stage) {
        new Thread(this::populateVersionChoice).start();
        if(logoImage.getImage() == null) {
            new Thread(this::loadImage).start();
        }
    }

    private void populateVersionChoice() {
        if(modData == null && mod != null) {
            modData = mod.getModData();
        }
        if (modData != null) {
            List<ModVersionData> versionData = modData.getVersions(gameVersion, "fabric");
            List<String> selectorList = new ArrayList<>();
            for (ModVersionData v : versionData) {
                if(!mod.getVersion().equals(v.getVersionNumber())) {
                    selectorList.add(v.getVersionNumber());
                }
            }
            Platform.runLater(() -> versionSelector.getItems().addAll(selectorList));
        }
    }

    private void loadImage() {
        Image logo = new Image(mod.getIconUrl());
        Platform.runLater(() -> logoImage.setImage(logo));
    }

    private void onVersionSelected() {
        if(mod != null && mod.getVersion().equals(versionSelector.getSelectionModel().getSelectedItem())) {
            installButton.setDisable(true);
        } else {
            installButton.setDisable(false);
        }
    }

    @FXML
    private void onInstallClicked() {
        installCallback.accept(getSelectedVersion(), mod, this);
    }

    private ModVersionData getSelectedVersion() {
        String selected = versionSelector.getSelectionModel().getSelectedItem();
        if(selected != null && modData != null) {
            for(ModVersionData v : modData.getVersions(gameVersion, "fabric")) {
                if(selected.equals(v.getVersionNumber())) {
                    return v;
                }
            }
        }
        return null;
    }

    @Override
    public void setRootVisible(boolean visible) {
        rootPane.setVisible(visible);
    }

    public void setInstallAvailable(boolean available) {
        installButton.setVisible(available);
    }

    public LauncherMod getMod() {
        return mod;
    }

    public void setMod(LauncherMod mod) {
        this.mod = mod;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(String gameVersion) {
        this.gameVersion = gameVersion;
    }

    public TriConsumer<ModVersionData, LauncherMod, ModListElement> getInstallCallback() {
        return installCallback;
    }

    public void setInstallCallback(TriConsumer<ModVersionData, LauncherMod, ModListElement> installCallback) {
        this.installCallback = installCallback;
    }

    public static Pair<ModListElement, AnchorPane> from(LauncherMod mod, String gameVersion, TriConsumer<ModVersionData, LauncherMod, ModListElement> installCallback) throws IOException {
        Pair<ModListElement, AnchorPane> result = newInstance();
        result.getKey().setMod(mod);
        result.getKey().setGameVersion(gameVersion);
        result.getKey().setInstallCallback(installCallback);
        return result;
    }
    public static Pair<ModListElement, AnchorPane> newInstance() throws IOException {
        FXMLLoader loader = UiLoader.getFXMLLoader("manager/ModListElement");
        AnchorPane element = UiLoader.loadFXML(loader);
        ModListElement listElementController = loader.getController();
        return new Pair<>(listElementController, element);
    }

}
