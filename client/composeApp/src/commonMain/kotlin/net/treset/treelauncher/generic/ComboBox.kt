package net.treset.treelauncher.generic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import net.treset.treelauncher.localization.strings
import net.treset.treelauncher.style.disabledContainer
import net.treset.treelauncher.style.disabledContent
import net.treset.treelauncher.style.hovered
import net.treset.treelauncher.style.icons

@Composable
fun <T> ComboBox(
    items: List<T>,
    onSelected: (T?) -> Unit = {},
    placeholder: String = "",
    loading: Boolean = false,
    loadingPlaceholder: String = strings().creator.version.loading(),
    defaultSelected: T? = null,
    allowUnselect: Boolean,
    toDisplayString: T.() -> String = { toString() },
    decorated: Boolean = true,
    enabled: Boolean = true
) {
    var expanded by remember(enabled) { mutableStateOf(false) }
    var selectedItem: T? by remember { mutableStateOf(defaultSelected) }
    val displayString = if(loading) loadingPlaceholder else selectedItem?.toDisplayString() ?: placeholder

    LaunchedEffect(defaultSelected) {
        selectedItem = defaultSelected
        onSelected(defaultSelected)
    }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopStart)
            .pointerHoverIcon(PointerIcon.Hand),
    ) {
        val borderColor by animateColorAsState(
            if(enabled && !loading) {
                if (expanded) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            } else {
                MaterialTheme.colorScheme.outline.disabledContainer()
            }
        )

        val textColor by animateColorAsState(
            if(enabled)
                LocalContentColor.current
            else
                LocalContentColor.current.disabledContent()
        )

        val borderWidth by animateDpAsState(
            if(expanded && enabled) 2.dp else 1.dp
        )

        val rowModifier = if(loading || !enabled) {
            Modifier
        } else {
            Modifier.clickable(onClick = { expanded = true })
        }.let {
            if(decorated) {
                it
                    .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
                    .padding(start = 12.dp, bottom = 9.dp, top = 6.dp, end = 6.dp)
            } else {
                it
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = rowModifier
        ) {
            Text(
                displayString,
                color = textColor
            )
            if(decorated) {
                Icon(
                    icons().comboBox,
                    "Open",
                    modifier = Modifier.offset(y = 2.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            //TODO: make lazy
            if(allowUnselect) {
                ComboBoxItem(
                    text = placeholder,
                    onClick = {
                        selectedItem = null
                        onSelected(null)
                        expanded = false
                    }
                )
            }
            items.forEach { i ->
                ComboBoxItem(
                    text = i.toDisplayString(),
                    onClick = {
                        selectedItem = i
                        onSelected(i)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun <T> ComboBox(
    items: List<T>,
    onSelected: (T) -> Unit = {},
    placeholder: String = "",
    loading: Boolean = false,
    loadingPlaceholder: String = strings().creator.version.loading(),
    defaultSelected: T? = null,
    toDisplayString: T.() -> String = { toString() },
    decorated: Boolean = true,
    enabled: Boolean = true
) = ComboBox(
    items,
    { it?.let { e -> onSelected(e) } },
    placeholder,
    loading,
    loadingPlaceholder,
    allowUnselect = false,
    defaultSelected = defaultSelected,
    toDisplayString = toDisplayString,
    decorated = decorated,
    enabled = enabled
)

@Composable
fun <T> TitledComboBox(
    title: String,
    items: List<T>,
    onSelected: (T?) -> Unit = {},
    loading: Boolean = false,
    loadingPlaceholder: String = strings().creator.version.loading(),
    placeholder: String = "",
    defaultSelected: T? = null,
    allowUnselect: Boolean,
    toDisplayString: T.() -> String = { toString() },
    decorated: Boolean = true,
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium
        )

        ComboBox(
            items,
            onSelected,
            placeholder,
            loading,
            loadingPlaceholder,
            defaultSelected,
            allowUnselect,
            toDisplayString,
            decorated,
            enabled
        )
    }
}

@Composable
fun <T> TitledComboBox(
    title: String,
    items: List<T>,
    onSelected: (T) -> Unit = {},
    loading: Boolean = false,
    loadingPlaceholder: String = strings().creator.version.loading(),
    placeholder: String = "",
    defaultSelected: T? = null,
    toDisplayString: T.() -> String = { toString() },
    decorated: Boolean = true,
    enabled: Boolean = true
) = TitledComboBox(
    title,
    items,
    {  it?.let { e -> onSelected(e) } },
    loading,
    loadingPlaceholder,
    placeholder,
    defaultSelected,
    false,
    toDisplayString,
    decorated,
    enabled
)

@Composable
fun ComboBoxItem(
    text: String,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    modifier: Modifier = Modifier
) {
    val hovered by interactionSource.collectIsHoveredAsState()

    val background = if(hovered)
            MaterialTheme.colorScheme.primary.hovered()
        else
            Color.Transparent

    val textColor = if(hovered)
            MaterialTheme.colorScheme.onPrimary
        else
            LocalContentColor.current

    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clip(RoundedCornerShape(4.dp))
            .background(background),
        colors = MenuDefaults.itemColors(
            textColor = textColor
        )
    )
}