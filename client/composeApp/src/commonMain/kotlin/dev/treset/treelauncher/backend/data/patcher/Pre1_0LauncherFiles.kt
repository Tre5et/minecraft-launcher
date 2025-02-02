package dev.treset.treelauncher.backend.data.patcher

import dev.treset.treelauncher.backend.data.manifest.LauncherManifestType
import dev.treset.treelauncher.backend.data.patcher.Pre2_0LauncherModsDetails
import dev.treset.treelauncher.backend.util.exception.FileLoadException
import dev.treset.treelauncher.backend.util.file.LauncherFile

class Pre1_0LauncherFiles : Pre2_0LauncherFiles() {
    private var _gameDetailsManifest: Pre2_0ParentManifest? = null
    val gameDetailsManifest: Pre2_0ParentManifest
        get() = _gameDetailsManifest!!

    @Throws(FileLoadException::class)
    fun reloadGameDetailsManifest() {
        _gameDetailsManifest = reloadParentManifest(
            LauncherFile.ofData(launcherDetails.gamedataDir),
            LauncherManifestType.GAME
        )
    }

    @Throws(FileLoadException::class)
    override fun reloadModsManifest() {
        reloadGameDetailsManifest()
        _modsManifest = reloadParentManifest(
            LauncherFile.ofData(launcherDetails.gamedataDir),
            gameDetailsManifest.components[0],
            LauncherManifestType.MODS
        )
    }

    @Throws(FileLoadException::class)
    override fun reloadModsComponents() {
        _modsComponents = reloadComponents(
            _modsManifest?: throw FileLoadException("Unable to load mods components: invalid configuration"),
            LauncherFile.ofData(launcherDetails.gamedataDir),
            LauncherManifestType.MODS_COMPONENT,
            Pre2_0LauncherModsDetails::fromJson,
            {
                it.types = types
                it.versions = versions
                it.mods = mods
            },
            LauncherFile.ofData(
                launcherDetails.gamedataDir,
                "mods"
            ),
            _modsComponents?: emptyArray()
        )
    }

    @Throws(FileLoadException::class)
    override fun reloadSavesManifest() {
        _savesManifest = reloadParentManifest(
            LauncherFile.ofData(launcherDetails.gamedataDir),
            _gameDetailsManifest?.components?.get(1)?: throw FileLoadException("Unable to load saves manifest: invalid configuration"),
            LauncherManifestType.SAVES
        )
    }

    @Throws(FileLoadException::class)
    override fun reloadSavesComponents() {
        _savesComponents = reloadComponents(
            _savesManifest?: throw FileLoadException("Unable to load saves components: invalid configuration"),
            LauncherFile.ofData(launcherDetails.gamedataDir),
            LauncherManifestType.SAVES_COMPONENT,
            LauncherFile.ofData(
                launcherDetails.gamedataDir,
                "saves"
            ),
            _savesComponents?: emptyArray()
        )
    }
}