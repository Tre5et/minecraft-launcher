package dev.treset.treelauncher.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.DragData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.treset.treelauncher.AppContext
import dev.treset.treelauncher.backend.data.manifest.Component
import dev.treset.treelauncher.backend.data.manifest.InstanceComponent
import dev.treset.treelauncher.backend.data.manifest.ParentManifest
import dev.treset.treelauncher.backend.util.file.LauncherFile
import dev.treset.treelauncher.backend.util.sort.ComponentSortType
import dev.treset.treelauncher.backend.util.sort.sorted
import dev.treset.treelauncher.backend.util.toggle
import dev.treset.treelauncher.generic.*
import dev.treset.treelauncher.localization.Strings
import dev.treset.treelauncher.style.icons
import java.io.IOException

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T: Component> Components(
    title: String,
    components: MutableList<T>,
    componentManifest: ParentManifest,
    checkHasComponent: (InstanceComponent, T) -> Boolean,
    isEnabled: T.() -> Boolean = {true},
    reload: () -> Unit,
    createContent: @Composable ColumnScope.(onDone: () -> Unit) -> Unit,
    actionBarSpecial: @Composable RowScope.(
        selected: T,
        settingsOpen: Boolean,
        reload: () -> Unit
    ) -> Unit = {_,_,_->},
    actionBarBoxContent: @Composable BoxScope.(
        selected: T,
        settingsOpen: Boolean,
        reload: () -> Unit
    ) -> Unit = {_,_,_->},
    detailsContent: @Composable ColumnScope.(
        selected: T,
        reload: () -> Unit
    ) -> Unit = {_,_->},
    detailsOnDrop: ((DragData) -> Unit)? = null,
    detailsScrollable: Boolean = true,
    settingsDefault: Boolean = false
) {
    @Suppress("NAME_SHADOWING")
    val components = components.toList()

    var selected: T? by remember(components) { mutableStateOf(null) }

    var creatorSelected by remember { mutableStateOf(false) }

    val actualComponents: List<T> = remember(components, componentManifest.sort.type.value, componentManifest.sort.reverse.value, AppContext.runningInstance) {
        components.sorted(componentManifest.sort)
    }

    DisposableEffect(Unit) {
        reload()

        onDispose {
            try {
                componentManifest.write()
            } catch(e: IOException) {
                AppContext.error(e)
            }
        }
    }


    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {

        TitledColumn(
            headerContent = {
                Text(title)

                SortBox(
                    sorts = ComponentSortType.entries.map { it.comparator },
                    reversed = componentManifest.sort.reverse.value,
                    selected = componentManifest.sort.type.value,
                    onReversed = {
                        componentManifest.sort.reverse.toggle()
                    },
                    onSelected = { new ->
                        componentManifest.sort.type.value = new
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            },
            modifier = Modifier.padding(12.dp),
            parentModifier = Modifier.fillMaxWidth(1 / 2f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            scrollable = false,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f, false)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(actualComponents) { component ->
                        ComponentButton(
                            component = component,
                            selected = component == selected,
                            enabled = component.isEnabled(),
                            onClick = {
                                creatorSelected = false
                                selected = if(component == selected) {
                                    null
                                } else {
                                    component
                                }
                            }
                        )
                    }
                }
            }

            SelectorButton(
                title = Strings.components.create(),
                icon = icons().add,
                selected = creatorSelected,
                onClick = {
                    selected = null
                    creatorSelected = !creatorSelected
                }
            )
        }

        selected?.let {
            var showSettings by remember(it) { mutableStateOf(settingsDefault) }

            var showRename by remember { mutableStateOf(false) }

            var showDelete by remember { mutableStateOf(false) }

            TitledColumn(
                headerContent = {
                    if(showSettings && !settingsDefault) {
                        Box(
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            IconButton(
                                onClick = { showSettings = false },
                                icon = icons().back,
                                size = 32.dp,
                                tooltip = Strings.manager.component.back(),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        actionBarSpecial(
                            it,
                            showSettings,
                            reload
                        )

                        Text(it.name.value)

                        IconButton(
                            onClick = {
                                showRename = true
                            },
                            icon = icons().edit,
                            size = 32.dp,
                            tooltip = Strings.selector.component.rename.title()
                        )
                        IconButton(
                            onClick = {
                                LauncherFile.of(it.directory).open()
                            },
                            icon = icons().folder,
                            size = 32.dp,
                            tooltip = Strings.selector.component.openFolder()
                        )
                        IconButton(
                            onClick = {
                                showDelete = true
                            },
                            icon = icons().delete,
                            size = 32.dp,
                            interactionTint = MaterialTheme.colorScheme.error,
                            tooltip = Strings.selector.component.delete.tooltip()
                        )
                    }

                    actionBarBoxContent(
                        it,
                        showSettings,
                        reload
                    )
                },
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(12.dp),
                scrollable = false
            ) {
                val columnContent: @Composable () -> Unit = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f, false)
                                .let {mod ->
                                    if(detailsScrollable) {
                                        mod.verticalScroll(rememberScrollState())
                                    } else {
                                        mod
                                    }
                                },
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            detailsContent(
                                it,
                                reload
                            )
                        }

                        SelectorButton(
                            title = Strings.manager.component.settings(),
                            icon = icons().settings,
                            selected = showSettings,
                            onClick = { showSettings = true }
                        )
                    }
                }

                if(showSettings) {
                    ComponentSettings(it)
                } else {
                    detailsOnDrop?.let {
                        DroppableArea(
                            onDrop = it,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            columnContent()
                        }
                    } ?: columnContent()
                }
            }


            if (showRename) {
                RenamePopup(
                    manifest = it,
                    editValid = { name -> name.isNotBlank() && name != it.name.value },
                    onDone = { name ->
                        showRename = false
                        name?.let { newName ->
                            it.name.value = newName
                            try {
                                it.write()
                            } catch (e: IOException) {
                                AppContext.severeError(e)
                            }
                        }
                    }
                )
            }

            if (showDelete) {
                DeletePopup(
                    component = it,
                    checkHasComponent = { details -> checkHasComponent(details, it) },
                    onClose = { showDelete = false },
                    onConfirm = {
                        try {
                            it.delete(componentManifest)
                        } catch(e: IOException) {
                            AppContext.severeError(e)
                        }
                        reload()
                        showDelete = false
                    }
                )
            }

        }

        if(creatorSelected) {
            TitledColumn(
                title = Strings.components.create(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                createContent {
                    reload()
                    creatorSelected = false
                }
            }
        }
    }
}

/*@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Components(
    title: String,
    componentManifest: ParentManifest,
    components: List<ComponentManifest>,
    checkHasComponent: (LauncherInstanceDetails, ComponentManifest) -> Boolean,
    isEnabled: ComponentManifest.() -> Boolean = {true},
    getCreator: (CreationState<ComponentManifest>) -> GenericComponentCreator?,
    reload: () -> Unit,
    actionBarSpecial: @Composable RowScope.(
        ComponentManifest,
        Boolean,
        () -> Unit,
        () -> Unit
    ) -> Unit = {_,_,_,_->},
    actionBarBoxContent: @Composable BoxScope.(
        selected: ComponentManifest,
        settingsOpen: Boolean,
        redraw: () -> Unit,
        reload: () -> Unit
    ) -> Unit = {_,_,_,_->},
    detailsContent: @Composable ColumnScope.(
        selected: ComponentManifest,
        redraw: () -> Unit,
        reload: () -> Unit
    ) -> Unit = {_,_,_->},
    detailsOnDrop: ((DragData) -> Unit)? = null,
    detailsScrollable: Boolean = false,
    settingsDefault: Boolean = false,
    sortContext: SortContext? = null
) = Components(
    title,
    components,
    componentManifest,
    checkHasComponent,
    { this },
    isEnabled,
    getCreator,
    reload,
    { onCreate ->
        ComponentCreator(
            existing = components.toList(),
            toDisplayString = { name },
            onCreate = onCreate,
            allowUse = false
        )
    },
    actionBarSpecial,
    actionBarBoxContent,
    detailsContent,
    detailsOnDrop,
    detailsScrollable,
    settingsDefault,
    sortContext
)*/