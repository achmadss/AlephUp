package dev.achmad.alephup.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.achmad.alephup.util.MultiplePermissionsState
import dev.achmad.alephup.util.PermissionState
import dev.achmad.core.preference.Preference as PreferenceData

sealed class Preference {
    abstract val title: String
    abstract val visible: Boolean
    abstract val enabled: Boolean

    sealed class PreferenceItem<T> : Preference() {
        abstract val subtitle: CharSequence?
        abstract val icon: ImageVector?
        abstract val onValueChanged: suspend (value: T) -> Boolean

        /**
         * A basic [PreferenceItem] that only displays texts.
         */
        data class TextPreference(
            override val title: String,
            override val subtitle: CharSequence? = null,
            override val visible: Boolean = true,
            override val enabled: Boolean = true,
            val titleColor: Color = Color.Unspecified,
            val subtitleColor: Color = Color.Unspecified,
            val onClick: (() -> Unit)? = null,
        ) : PreferenceItem<String>() {
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (value: String) -> Boolean = { true }
        }

        /**
         * A [PreferenceItem] that provides a two-state toggleable option.
         */
        data class SwitchPreference(
            val preference: PreferenceData<Boolean>,
            override val title: String,
            override val subtitle: CharSequence? = null,
            override val visible: Boolean = true,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (value: Boolean) -> Boolean = { true },
        ) : PreferenceItem<Boolean>() {
            override val icon: ImageVector? = null
        }

        data class BasicSwitchPreference(
            val value: Boolean,
            override val title: String,
            override val subtitle: CharSequence? = null,
            override val visible: Boolean = true,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (value: Boolean) -> Boolean = { true },
        ) : PreferenceItem<Boolean>() {
            override val icon: ImageVector? = null
        }

        /**
         * A [PreferenceItem] that displays a list of entries as a dialog.
         */
        @Suppress("UNCHECKED_CAST")
        data class ListPreference<T>(
            val preference: PreferenceData<T>,
            val entries: Map<T, String>,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (value: T, entries: Map<T, String>) -> String? =
                { v, e -> subtitle?.format(e[v]) },
            override val icon: ImageVector? = null,
            override val visible: Boolean = true,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (value: T) -> Boolean = { true },
        ) : PreferenceItem<T>() {
            internal fun internalSet(value: Any) = preference.set(value as T)
            internal suspend fun internalOnValueChanged(value: Any) = onValueChanged(value as T)

            @Composable
            internal fun internalSubtitleProvider(value: Any?, entries: Map<out Any?, String>) =
                subtitleProvider(value as T, entries as Map<T, String>)
        }

        /**
         * [ListPreference] but with no connection to a [PreferenceData]
         */
        data class BasicListPreference(
            val value: String,
            val entries: Map<String, String>,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (value: String, entries: Map<String, String>) -> String? =
                { v, e -> subtitle?.format(e[v]) },
            override val icon: ImageVector? = null,
            override val visible: Boolean = true,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (value: String) -> Boolean = { true },
        ) : PreferenceItem<String>()

        /**
         * A [PreferenceItem] that displays a list of entries as a dialog.
         * Multiple entries can be selected at the same time.
         */
        data class MultiSelectListPreference(
            val preference: PreferenceData<Set<String>>,
            val entries: Map<String, String>,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (value: Set<String>, entries: Map<String, String>) -> String? =
                { v, e ->
                    val combined = remember(v, e) {
                        v.mapNotNull { e[it] }
                            .joinToString()
                            .takeUnless { it.isBlank() }
                    }
                        ?: "None" // TODO copy
                    subtitle?.format(combined)
                },
            override val icon: ImageVector? = null,
            override val visible: Boolean = true,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (value: Set<String>) -> Boolean = { true },
        ) : PreferenceItem<Set<String>>()

        /**
         * A [PreferenceItem] that shows a EditText in the dialog.
         */
        data class EditTextPreference(
            val preference: PreferenceData<String>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val visible: Boolean = true,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (value: String) -> Boolean = { true },
        ) : PreferenceItem<String>() {
            override val icon: ImageVector? = null
        }

        data class InfoPreference(
            override val title: String,
        ) : PreferenceItem<String>() {
            override val visible: Boolean = true
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (value: String) -> Boolean = { true }
        }

        data class MultiplePermissionPreference(
            val permissionState: MultiplePermissionsState,
            override val title: String,
            override val subtitle: String? = null,
            override val visible: Boolean = true,
            override val enabled: Boolean = true,
        ): PreferenceItem<Unit>() {
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (value: Unit) -> Boolean = { true }
        }

        data class PermissionPreference(
            val permissionState: PermissionState,
            override val title: String,
            override val subtitle: String? = null,
            override val visible: Boolean = true,
            override val enabled: Boolean = true,
        ): PreferenceItem<Unit>() {
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (value: Unit) -> Boolean = { true }
        }

        data class CustomPreference(
            val content: @Composable () -> Unit,
        ) : PreferenceItem<Unit>() {
            override val title: String = ""
            override val visible: Boolean = true
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (value: Unit) -> Boolean = { true }
        }
    }

    data class PreferenceGroup(
        override val title: String,
        override val visible: Boolean = true,
        override val enabled: Boolean = true,

        val preferenceItems: List<PreferenceItem<out Any>>,
    ) : Preference()
}