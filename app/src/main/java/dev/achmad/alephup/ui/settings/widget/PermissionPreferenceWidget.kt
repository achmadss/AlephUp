package dev.achmad.alephup.ui.settings.widget

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.achmad.alephup.R

@Composable
fun PermissionPreferenceWidget(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    isGranted: Boolean = false,
    title: String? = null,
    subtitle: CharSequence? = null,
    onRequestPermission: () -> Unit,
) {
    val buttonContentColor = when {
        enabled -> ButtonDefaults.textButtonColors().contentColor
        else -> LocalContentColor.current
    }
    TextPreferenceWidget(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        widget = {
            TextButton(
                onClick = onRequestPermission
            ) {
                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = buttonContentColor
                    )
                } else {
                    Text(
                        text = stringResource(R.string.onboarding_button_grant),
                        color = buttonContentColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        onPreferenceClick = onRequestPermission
    )
}