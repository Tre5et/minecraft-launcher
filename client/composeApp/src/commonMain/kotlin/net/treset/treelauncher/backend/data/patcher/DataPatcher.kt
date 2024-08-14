package net.treset.treelauncher.backend.data.patcher

import io.github.oshai.kotlinlogging.KotlinLogging
import net.treset.treelauncher.backend.config.appConfig
import net.treset.treelauncher.backend.config.appSettings
import net.treset.treelauncher.backend.data.LauncherFiles
import net.treset.treelauncher.backend.data.Pre2_5LauncherFiles
import net.treset.treelauncher.backend.data.manifest.ComponentManifest
import net.treset.treelauncher.backend.util.Version
import net.treset.treelauncher.backend.util.file.LauncherFile
import net.treset.treelauncher.backend.util.string.PatternString
import java.io.IOException

class DataPatcher(
    currentVersion: String,
    previousVersion: String
) {
    private val currVer = Version.fromString(currentVersion)
    private val prevVer = Version.fromString(previousVersion)

    enum class UpgradeStep {
        REMOVE_BACKUP_EXCLUDED_FILES,
        UPGRADE_SETTINGS,
        UPGRADE_GAME_DATA_COMPONENTS,
        UPGRADE_INCLUDED_FILES,
        REMOVE_RESOURCEPACKS_ARGUMENT,
        ADD_NEW_INCLUDED_FILES
    }

    private class UpgradeFunction(
        val function: (onStep: (UpgradeStep) -> Unit) -> Unit,
        val condition: () -> Boolean
    ) {
        fun execute(onStep: (UpgradeStep) -> Unit) {
            if(condition()) {
                function(onStep)
            }
        }
    }

    private val upgradeMap: Array<UpgradeFunction> = arrayOf(
        UpgradeFunction(this::moveGameDataComponents) { currVer >= Version(2, 5, 0) && prevVer < Version(2, 5, 0) },
        UpgradeFunction(this::upgradeIncludedFiles) { currVer >= Version(2, 5, 0) && prevVer < Version(2, 5, 0) },
        UpgradeFunction(this::addNewIncludedFilesToManifest) { currVer >= Version(2, 5, 0) && prevVer < Version(2, 5, 0) },
        UpgradeFunction(this::removeResourcepacksDirGameArguments) { currVer >= Version(2, 5, 0) && prevVer < Version(2, 5, 0) },
        UpgradeFunction(this::removeBackupExcludedFiles) { currVer >= Version(2, 5, 0) && prevVer < Version(2, 5, 0) },
        UpgradeFunction(this::upgradeSettings) { currVer >= Version(2, 5, 0) && prevVer < Version(2, 5, 0) }
    )

    @Throws(IOException::class)
    fun performUpgrade(onStep: (UpgradeStep) -> Unit) {
        if(currVer <= prevVer) {
            return
        }

        LOGGER.info { "Performing data upgrade: v${prevVer} -> v${currVer} " }
        for(upgrade in upgradeMap) {
            upgrade.execute(onStep)
        }
        LOGGER.info { "Data upgrade complete" }
    }

    @Throws(IOException::class)
    fun moveGameDataComponents(onStep: (UpgradeStep) -> Unit) {
        LOGGER.info { "Moving game data components..."}
        onStep(UpgradeStep.UPGRADE_GAME_DATA_COMPONENTS)

        val files = Pre2_5LauncherFiles()
        files.reloadAll()
        val gameDataDir = LauncherFile.ofData(files.launcherDetails.gamedataDir)

        LOGGER.info { "Moving mods components..." }
        val modsDir = LauncherFile.ofData(files.launcherDetails.modsDir)
        LauncherFile.of(files.modsManifest.directory, files.gameDetailsManifest.components[0]).moveTo(
            LauncherFile.of(modsDir, appConfig().manifestFileName)
        )
        for(file in gameDataDir.listFiles()) {
            if(file.isDirectory && file.name.startsWith(files.modsManifest.prefix)) {
                file.moveTo(LauncherFile.of(modsDir, file.name))
            }
        }
        LOGGER.info { "Moved mods components" }

        LOGGER.info { "Moving saves components..." }
        val savesDir = LauncherFile.ofData(files.launcherDetails.savesDir)
        LauncherFile.of(files.savesManifest.directory, files.gameDetailsManifest.components[1]).moveTo(
            LauncherFile.of(savesDir, appConfig().manifestFileName)
        )
        for(file in gameDataDir.listFiles()) {
            if(file.isDirectory && file.name.startsWith(files.savesManifest.prefix)) {
                file.moveTo(LauncherFile.of(savesDir, file.name))
            }
        }
        LOGGER.info { "Moved saves components" }

        LOGGER.info { "Removing old manifest..." }
        LauncherFile.ofData(files.launcherDetails.gamedataDir, appConfig().manifestFileName).remove()
        LOGGER.info { "Removed old manifest" }

        LOGGER.info { "Moved game data components" }
    }

    @Throws(IOException::class)
    fun upgradeIncludedFiles(onStep: (UpgradeStep) -> Unit) {
        LOGGER.info { "Upgrading included files..." }
        onStep(UpgradeStep.UPGRADE_INCLUDED_FILES)

        val files = LauncherFiles()
        files.reloadAll()

        for(instance in files.instanceComponents) {
            upgradeIncludedFiles(instance.first)
        }

        for(mods in files.modsComponents) {
            moveRootFilesToDirectory(mods.first, "mods")
            upgradeIncludedFiles(mods.first)
        }

        for(saves in files.savesComponents) {
            moveRootFilesToDirectory(saves, "saves")
            upgradeIncludedFiles(saves)
        }

        for(resourcepacks in files.resourcepackComponents) {
            moveRootFilesToDirectory(resourcepacks, "resourcepacks")
            upgradeIncludedFiles(resourcepacks)
        }

        for(options in files.optionsComponents) {
            upgradeIncludedFiles(options)
        }

        LOGGER.info { "Upgraded included files" }
    }

    @Throws(IOException::class)
    fun moveRootFilesToDirectory(component: ComponentManifest, dirName: String) {
        LOGGER.info { "Moving root files to directory for ${component.type}: ${component.id}..." }

        val dir = LauncherFile.of(component.directory)
        val files = dir.listFiles()
        for(file in files) {
            if(file.name != dirName
                && file.name != appConfig().manifestFileName
                && file.name != component.details
                && file.name != appConfig().syncFileName
                && file.name != ".included_files_old"
                && file.name != ".included_files"
            ) {
                file.moveTo(LauncherFile.of(dir, dirName, file.name))
            }
        }

        LOGGER.info { "Moved root files to directory for ${component.type}: ${component.id}" }
    }

    @Throws(IOException::class)
    fun upgradeIncludedFiles(component: ComponentManifest) {
        LOGGER.info { "Upgrading included files for ${component.type}: ${component.id}..." }

        val dir = LauncherFile.of(component.directory, ".included_files")
        val files = dir.listFiles()
        for(file in files) {
            file.moveTo(LauncherFile.of(component.directory, file.name))
        }
        dir.remove()

        LauncherFile.of(component.directory, ".included_files_old").existsOrNull()
            ?.moveTo(LauncherFile.of(component.directory, appConfig().includedFilesBackupDir))
        LOGGER.info { "Upgraded included files for ${component.type}: ${component.id}" }
    }

    @Throws(IOException::class)
    fun removeResourcepacksDirGameArguments(onStep: (UpgradeStep) -> Unit) {
        LOGGER.info { "Removing resourcepacks directory game arguments..." }
        onStep(UpgradeStep.REMOVE_RESOURCEPACKS_ARGUMENT)
        val files = LauncherFiles()
        files.reloadAll()
        files.versionComponents.forEach {
            it.second.gameArguments = it.second.gameArguments.filter { arg ->
                arg.argument != "--resourcePackDir" && arg.argument != "\${resourcepack_directory}"
            }
            LauncherFile.of(it.first.directory, it.first.details).write(it.second)
        }
        LOGGER.info { "Removed resourcepacks directory game arguments" }
    }

    @Throws(IOException::class)
    fun addNewIncludedFilesToManifest(onStep: (UpgradeStep) -> Unit) {
        LOGGER.info { "Adding new included files..." }
        onStep(UpgradeStep.ADD_NEW_INCLUDED_FILES)
        val files = LauncherFiles()
        files.reloadAll()

        for(saves in files.savesComponents) {
            saves.includedFiles = saves.includedFiles.plus("saves/")
            LauncherFile.of(saves.directory, appConfig().manifestFileName).write(saves)
        }

        for(resourcepacks in files.resourcepackComponents) {
            resourcepacks.includedFiles = resourcepacks.includedFiles.plus("resourcepacks/")
            LauncherFile.of(resourcepacks.directory, appConfig().manifestFileName).write(resourcepacks)
        }

        for(mods in files.modsComponents) {
            mods.first.includedFiles = mods.first.includedFiles.plus("mods/")
            LauncherFile.of(mods.first.directory, appConfig().manifestFileName).write(mods.first)
        }
    }

    @Throws(IOException::class)
    fun removeBackupExcludedFiles(onStep: (UpgradeStep) -> Unit) {
        LOGGER.info { "Removing backup excluded files from instances..." }
        onStep(UpgradeStep.REMOVE_BACKUP_EXCLUDED_FILES)

        val files = LauncherFiles()
        files.reloadAll()
        files.instanceComponents.forEach {
            it.second.ignoredFiles = it.second.ignoredFiles.filter { file ->
                !PatternString(file, true).matches("backups/")
            }
            LauncherFile.of(it.first.directory, it.first.details).write(it.second)
        }
        LOGGER.info { "Removed backup excluded files from instances" }
    }

    @Throws(IOException::class)
    fun upgradeSettings(onStep: (UpgradeStep) -> Unit) {
        LOGGER.info { "Upgrading settings..." }
        onStep(UpgradeStep.UPGRADE_SETTINGS)
        appSettings().version = currVer.toString()
        appSettings().save()
        LOGGER.info { "Upgraded settings" }
    }

    companion object {
        val LOGGER = KotlinLogging.logger {  }
    }
}