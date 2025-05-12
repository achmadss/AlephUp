package dev.achmad.alephup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.achmad.alephup.R


/**
 * A reusable section component for settings screens.
 *
 * @param title The title of the section
 * @param content The content to be displayed inside the section
 * @param modifier Additional modifiers to apply to the section container
 */
@Composable
fun CardSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(
                        alpha = 0.2f
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.5f
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

/**
 * A reusable permission item that displays a permission with a grant button or check icon.
 *
 * @param text The text describing the permission
 * @param isGranted Whether the permission is granted
 * @param onRequestPermission Callback when the user clicks to grant permission
 */
@Composable
fun CardSectionItem(
    text: String,
    description: String? = null,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            description?.let {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
        }
        TextButton(
            onClick = { onRequestPermission() },
            content = {
                if (isGranted) {
                    Icon(
                        modifier = Modifier
                            .defaultMinSize(
                                minHeight = ButtonDefaults.MinHeight,
                            ),
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = ButtonDefaults.textButtonColors().contentColor
                    )
                } else {
                    Text(
                        text = stringResource(R.string.onboarding_button_grant),
                        color = ButtonDefaults.textButtonColors().contentColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

            }
        )
    }
}

/**
 * A reusable toggle item that displays text with a toggle switch.
 *
 * @param text The text describing the toggle
 * @param isChecked Whether the toggle is checked
 * @param onCheckedChange Callback when the user changes the toggle state
 */
@Composable
fun ToggleItem(
    text: String,
    description: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            description?.let {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * A reusable button for settings actions.
 *
 * @param text The text to display on the button
 * @param onClick Callback when the button is clicked
 * @param enabled Whether the button is enabled
 * @param backgroundColor Optional background color override
 * @param contentColor Optional content color override
 */
@Composable
fun CardSectionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color? = null,
    contentColor: Color? = null
) {
    Button(
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onClick,
        colors = if (backgroundColor != null || contentColor != null) {
            ButtonDefaults.buttonColors(
                containerColor = backgroundColor ?: ButtonDefaults.buttonColors().containerColor,
                contentColor = contentColor ?: ButtonDefaults.buttonColors().contentColor
            )
        } else {
            ButtonDefaults.buttonColors()
        }
    ) {
        Text(text)
    }
}