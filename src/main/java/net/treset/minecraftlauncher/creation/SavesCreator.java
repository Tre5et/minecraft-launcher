package net.treset.minecraftlauncher.creation;

import net.treset.mc_version_loader.launcher.LauncherManifest;
import net.treset.mc_version_loader.launcher.LauncherManifestType;
import net.treset.minecraftlauncher.config.Config;

import java.util.Map;

public class SavesCreator extends GenericComponentCreator {
    private LauncherManifest gameManifest;

    public SavesCreator(String name, Map<String, LauncherManifestType> typeConversion, LauncherManifest componentsManifest, LauncherManifest gameManifest) {
        super(LauncherManifestType.SAVES_COMPONENT, null, null, name, typeConversion, Config.SAVES_DEFAULT_INCLUDED_FILES, null, componentsManifest);
        this.gameManifest = gameManifest;
    }

    public SavesCreator(String name, LauncherManifest inheritsFrom, LauncherManifest componentsManifest, LauncherManifest gameManifest) {
        super(LauncherManifestType.SAVES_COMPONENT, null, inheritsFrom, name, null, null, null, componentsManifest);
        this.gameManifest = gameManifest;
    }

    public SavesCreator(LauncherManifest uses) {
        super(LauncherManifestType.SAVES_COMPONENT, uses, null, null, null, null, null, null);
    }

    @Override
    protected String getParentManifestFileName() {
        return gameManifest.getComponents().get(1);
    }
}
