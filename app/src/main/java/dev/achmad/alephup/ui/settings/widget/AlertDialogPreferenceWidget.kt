package dev.achmad.alephup.ui.settings.widget

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@Composable
fun AlertDialogPreferenceWidget(
    enabled: Boolean,
    title: String,
    subtitle: String?,
    titleColor: Color = Color.Unspecified,
    subtitleColor: Color = Color.Unspecified,
    dialogTitle: String,
    dialogText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle,
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        onPreferenceClick = { if (enabled) isDialogShown = true },
    )

    if (isDialogShown) {
        AlertDialog(
            onDismissRequest = { isDialogShown = false },
            title = { Text(text = dialogTitle) },
            text = {
                Text(text = dialogText)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDialogShown = false
                        onConfirm()
                    }
                ) {
                    Text(text = "Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isDialogShown = false
                        onCancel()
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

}