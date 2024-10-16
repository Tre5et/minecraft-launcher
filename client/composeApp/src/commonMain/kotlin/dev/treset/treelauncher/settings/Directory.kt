package dev.treset.treelauncher.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import dev.treset.treelauncher.AppContext
import dev.treset.treelauncher.backend.config.appConfig
import dev.treset.treelauncher.backend.util.file.LauncherFile
import dev.treset.treelauncher.generic.*
import dev.treset.treelauncher.localization.strings
import dev.treset.treelauncher.style.disabledContainer
import dev.treset.treelauncher.style.disabledContent
import dev.treset.treelauncher.style.icons
import java.io.IOException

@Composable
fun Directory() {
    var popupContent: PopupData? by remember { mutableStateOf(null) }
    val instanceRunning = remember(AppContext.runningInstance) { AppContext.runningInstance != null }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if(instanceRunning) MaterialTheme.colorScheme.secondaryContainer.disabledContainer() else MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Text(
                strings().settings.path.title(),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp),
                color = if (instanceRunning) LocalContentColor.current.disabledContent() else LocalContentColor.current
            )

            var tfValue by remember { mutableStateOf(appConfig().baseDir.absolutePath) }
            var showDirPicker by remember { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextBox(
                    tfValue,
                    {
                        tfValue = it
                    },
                    enabled = !instanceRunning
                )

                IconButton(
                    onClick = {
                        showDirPicker = true
                    },
                    icon = icons().folder,
                    tooltip = strings().settings.path.select(),
                    enabled = !instanceRunning
                )
            }

            DirectoryPicker(
                show = showDirPicker && !instanceRunning,
                initialDirectory = if (LauncherFile.of(tfValue)
                        .isDirectory()
                ) tfValue else appConfig().baseDir.absolutePath,
                onFileSelected = {
                    it?.let { tfValue = it }
                    showDirPicker = false
                },
            )

            var cbState by remember { mutableStateOf(false) }
            TitledCheckBox(
                title = strings().settings.path.remove(),
                checked = cbState,
                onCheckedChange = {
                    cbState = it
                },
                enabled = !instanceRunning
            )

            Button(
                onClick = {
                    val dir = LauncherFile.of(tfValue)

                    if (!dir.isDirectory()) {
                        popupContent = PopupData(
                            type = PopupType.ERROR,
                            titleRow = { Text(strings().settings.path.invalid()) },
                            buttonRow = {
                                Button(
                                    onClick = { popupContent = null }
                                ) {
                                    Text(strings().settings.path.close())
                                }
                            }
                        )
                    } else {
                        popupContent = PopupData(
                            titleRow = { Text(strings().settings.path.changing()) }
                        )

                        Thread {
                            try {
                                appConfig().setBaseDir(dir, false, cbState)

                                popupContent = PopupData(
                                    type = PopupType.SUCCESS,
                                    titleRow = { Text(strings().settings.path.success()) },
                                    buttonRow = {
                                        Button(
                                            onClick = { popupContent = null }
                                        ) {
                                            Text(strings().settings.path.close())
                                        }
                                    }
                                )
                            } catch (e: IOException) {
                                popupContent = PopupData(
                                    type = PopupType.ERROR,
                                    titleRow = { Text(strings().settings.path.errorTitle()) },
                                    content = { Text(strings().settings.path.errorMessage(e)) },
                                    buttonRow = {
                                        Button(
                                            onClick = { popupContent = null }
                                        ) {
                                            Text(strings().settings.path.close())
                                        }
                                    }
                                )
                            }
                        }.start()
                    }
                },
                enabled = !instanceRunning
            ) {
                Text(
                    strings().settings.path.apply()
                )
            }
        }
    }

    popupContent?.let {
        PopupOverlay(it)
    }
}