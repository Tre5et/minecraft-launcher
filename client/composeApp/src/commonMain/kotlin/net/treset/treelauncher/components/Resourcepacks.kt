package net.treset.treelauncher.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.DragData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.treset.mc_version_loader.launcher.LauncherManifest
import net.treset.mc_version_loader.resoucepacks.Resourcepack
import net.treset.treelauncher.AppContext
import net.treset.treelauncher.backend.config.appConfig
import net.treset.treelauncher.backend.config.appSettings
import net.treset.treelauncher.backend.creation.ResourcepackCreator
import net.treset.treelauncher.backend.util.exception.FileLoadException
import net.treset.treelauncher.backend.util.file.LauncherFile
import net.treset.treelauncher.creation.CreationMode
import net.treset.treelauncher.generic.IconButton
import net.treset.treelauncher.localization.strings
import net.treset.treelauncher.style.icons
import java.io.IOException
import java.net.URI

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Resourcepacks() {
    var components by remember { mutableStateOf(AppContext.files.resourcepackComponents.sortedBy { it.name }) }

    var resourcepacks: List<Pair<Resourcepack, LauncherFile>> by remember { mutableStateOf(emptyList()) }

    var selected: LauncherManifest? by remember { mutableStateOf(null) }

    var showAdd by remember(selected) { mutableStateOf(false) }
    var filesToAdd by remember(selected) { mutableStateOf(emptyList<LauncherFile>()) }

    val reloadResourcepacks = {
        selected?.let { current ->
            resourcepacks = LauncherFile.of(current.directory).listFiles()
                .mapNotNull { file ->
                    try {
                        Resourcepack.from(file)
                    } catch (e: Exception) {
                        if(!file.name.startsWith(appConfig().includedFilesDirName) && !file.name.startsWith(appConfig().manifestFileName)) {
                            Resourcepack(file.name, null, null)
                        } else {
                            null
                        }
                    }?.let { it to file }
                }
        }
    }

    LaunchedEffect(showAdd) {
        if(!showAdd) {
            filesToAdd = emptyList()
        }
    }

    Components(
        strings().selector.resourcepacks.title(),
        components = components,
        componentManifest = AppContext.files.resourcepackManifest,
        checkHasComponent = { details, component -> details.resourcepacksComponent == component.id },
        getCreator = { state ->
            when(state.mode) {
                CreationMode.NEW -> state.name?.let {
                    ResourcepackCreator(
                        state.name,
                        AppContext.files.launcherDetails.typeConversion,
                        AppContext.files.resourcepackManifest
                    )
                }
                CreationMode.INHERIT -> state.name?.let{ state.existing?.let {
                    ResourcepackCreator(
                        state.name,
                        state.existing,
                        AppContext.files.resourcepackManifest
                    )
                }}
                CreationMode.USE -> null
            }
        },
        reload = {
            try {
                AppContext.files.reloadResourcepackManifest()
                AppContext.files.reloadResourcepackComponents()
                components = AppContext.files.resourcepackComponents.sortedBy { it.name }
            } catch (e: FileLoadException) {
                AppContext.severeError(e)
            }
        },
        detailsContent = { current, _, _ ->
            DisposableEffect(current) {
                selected = current
                reloadResourcepacks()

                onDispose {
                    selected = null
                }
            }

            if(showAdd) {
                FileImport(
                    current,
                    AppContext.files.resourcepackComponents,
                    {
                        try {
                            Resourcepack.from(this)
                        } catch (e: IOException) {
                            null
                        }
                    },
                    {
                        this.name
                    },
                    icons().resourcePacks,
                    strings().manager.resourcepacks.import,
                    fileExtensions = listOf("zip"),
                    allowDirectoryPicker = true,
                    filesToAdd = filesToAdd,
                    clearFilesToAdd = { filesToAdd = emptyList() }
                ) {
                    showAdd = false
                    reloadResourcepacks()
                }
            } else {
                if (resourcepacks.isNotEmpty()) {
                    resourcepacks.forEach {
                        ResourcepackButton(
                            it.first
                        ) {
                            try {
                                it.second.remove()
                                reloadResourcepacks()
                            } catch (e: IOException) {
                                AppContext.error(e)
                            }
                        }
                    }
                }
            }
        },
        actionBarSpecial = { _, settingsShown, _, _ ->
            if(!settingsShown || !showAdd) {
                IconButton(
                    onClick = {
                        showAdd = true
                    },
                    icon = icons().add,
                    size = 32.dp,
                    tooltip = strings().manager.saves.tooltipAdd()
                )
            }
        },
        actionBarBoxContent = { _, _, _, _ ->
            if(showAdd) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            showAdd = false
                        },
                        icon = icons().back,
                        size = 32.dp,
                        tooltip = strings().manager.component.import.back(),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            }
        },
        detailsOnDrop = {
            if(it is DragData.FilesList) {
                filesToAdd = it.readFiles().map { LauncherFile.of(URI(it).path) }
                showAdd = true
            }
        },
        detailsScrollable = true,
        sortContext = SortContext(
            getSortType = { appSettings().resourcepacksComponentSortType },
            setSortType = { appSettings().resourcepacksComponentSortType = it },
            getReverse = { appSettings().isResourcepacksComponentSortReverse },
            setReverse = { appSettings().isResourcepacksComponentSortReverse = it }
        )
    )
}