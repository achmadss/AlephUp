package dev.achmad.alephup.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.dp
import dev.achmad.alephup.ui.settings.widget.AlertDialogPreferenceWidget
import dev.achmad.alephup.ui.settings.widget.EditTextPreferenceWidget
import dev.achmad.alephup.ui.settings.widget.InfoWidget
import dev.achmad.alephup.ui.settings.widget.ListPreferenceWidget
import dev.achmad.alephup.ui.settings.widget.MultiSelectListPreferenceWidget
import dev.achmad.alephup.ui.settings.widget.PermissionPreferenceWidget
import dev.achmad.alephup.ui.settings.widget.SwitchPreferenceWidget
import dev.achmad.alephup.ui.settings.widget.TextPreferenceWidget
import dev.achmad.alephup.util.extension.collectAsState
import dev.achmad.alephup.util.extension.onClickInput
import kotlinx.coroutines.launch

val LocalPreferenceHighlighted = compositionLocalOf(structuralEqualityPolicy()) { false }
val LocalPreferenceMinHeight = compositionLocalOf(structuralEqualityPolicy()) { 56.dp }

@Composable
fun StatusWrapper(
    item: Preference.PreferenceItem<*>,
    highlightKey: String?,
    content: @Composable () -> Unit,
) {
    val visible = item.visible
    val enabled = item.enabled
    val highlighted = item.title == highlightKey
    AnimatedVisibility(
        modifier = Modifier
            .then(
                if (!enabled) {
                    Modifier.onClickInput(
                        pass = PointerEventPass.Initial,
                        ripple = false,
                        onUp = {
                            // do nothing
                        }
                    )
                } else Modifier
            ),
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        content = {
            CompositionLocalProvider(
                LocalPreferenceHighlighted provides highlighted,
                LocalContentColor provides when {
                    !enabled -> LocalContentColor.current.copy(alpha = .38f)
                    else -> LocalContentColor.current
                },
                content = content,
            )
        },
    )
}

@Composable
internal fun PreferenceItem(
    item: Preference.PreferenceItem<*>,
    highlightKey: String?,
) {
    val scope = rememberCoroutineScope()
    StatusWrapper(
        item = item,
        highlightKey = highlightKey,
    ) {
        when (item) {
            is Preference.PreferenceItem.SwitchPreference -> {
                val value by item.preference.collectAsState()
                SwitchPreferenceWidget(
                    enabled = item.enabled,
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    checked = value,
                    onCheckedChanged = { newValue ->
                        scope.launch {
                            if (item.onValueChanged(newValue)) {
                                item.preference.set(newValue)
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.BasicSwitchPreference -> {
                SwitchPreferenceWidget(
                    enabled = item.enabled,
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    checked = item.value,
                    onCheckedChanged = { newValue ->
                        scope.launch { item.onValueChanged(newValue) }
                    }
                )
            }
            is Preference.PreferenceItem.ListPreference<*> -> {
                val value by item.preference.collectAsState()
                ListPreferenceWidget(
                    value = value,
                    enabled = item.enabled,
                    title = item.title,
                    subtitle = item.internalSubtitleProvider(value, item.entries),
                    icon = item.icon,
                    entries = item.entries,
                    onValueChange = { newValue ->
                        scope.launch {
                            if (item.internalOnValueChanged(newValue!!)) {
                                item.internalSet(newValue)
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.BasicListPreference -> {
                ListPreferenceWidget(
                    value = item.value,
                    enabled = item.enabled,
                    title = item.title,
                    subtitle = item.subtitleProvider(item.value, item.entries),
                    icon = item.icon,
                    entries = item.entries,
                    onValueChange = { scope.launch { item.onValueChanged(it) } },
                )
            }
            is Preference.PreferenceItem.MultiSelectListPreference -> {
                val values by item.preference.collectAsState()
                MultiSelectListPreferenceWidget(
                    preference = item,
                    values = values,
                    enabled = item.enabled,
                    onValuesChange = { newValues ->
                        scope.launch {
                            if (item.onValueChanged(newValues)) {
                                item.preference.set(newValues.toMutableSet())
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.AlertDialogPreference -> {
                AlertDialogPreferenceWidget(
                    enabled = item.enabled,
                    title = item.title,
                    subtitle = item.subtitle,
                    titleColor = item.titleColor,
                    subtitleColor = item.subtitleColor,
                    dialogTitle = item.dialogTitle,
                    dialogText = item.dialogText,
                    onConfirm = item.onConfirm,
                    onCancel = item.onCancel
                )
            }
            is Preference.PreferenceItem.TextPreference -> {
                TextPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    titleColor = item.titleColor,
                    subtitleColor = item.subtitleColor,
                    icon = item.icon,
                    onPreferenceClick = item.onClick,
                )
            }
            is Preference.PreferenceItem.EditTextPreference -> {
                val values by item.preference.collectAsState()
                EditTextPreferenceWidget(
                    enabled = item.enabled,
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    value = values,
                    onConfirm = {
                        val accepted = item.onValueChanged(it)
                        if (accepted) item.preference.set(it)
                        accepted
                    },
                )
            }
            is Preference.PreferenceItem.InfoPreference -> {
                InfoWidget(text = item.title)
            }
            is Preference.PreferenceItem.MultiplePermissionPreference -> {
                PermissionPreferenceWidget(
                    enabled = item.enabled,
                    isGranted = item.permissionState.isAllPermissionsGranted(),
                    title = item.title,
                    subtitle = item.subtitle,
                    onRequestPermission = {
                        item.permissionState.requestPermissions()
                    }
                )
            }
            is Preference.PreferenceItem.PermissionPreference -> {
                PermissionPreferenceWidget(
                    enabled = item.enabled,
                    isGranted = item.permissionState.isGranted.value,
                    title = item.title,
                    subtitle = item.subtitle,
                    onRequestPermission = {
                        item.permissionState.requestPermission()
                    }
                )
            }
            is Preference.PreferenceItem.CustomPreference -> {
                item.content()
            }
        }
    }
}
