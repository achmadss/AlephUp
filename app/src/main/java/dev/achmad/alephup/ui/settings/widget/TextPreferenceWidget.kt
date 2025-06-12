package dev.achmad.alephup.ui.settings.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import dev.achmad.alephup.util.extension.secondaryItemAlpha

@Composable
fun TextPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: CharSequence? = null,
    titleColor: Color = Color.Unspecified,
    subtitleColor: Color = Color.Unspecified,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    widget: @Composable (() -> Unit)? = null,
    onPreferenceClick: (() -> Unit)? = null,
) {
    BasePreferenceWidget(
        modifier = modifier,
        title = title,
        titleColor = titleColor,
        subcomponent = if (!subtitle.isNullOrBlank()) {
            {
                if (subtitle is AnnotatedString) {
                    Text(
                        text = subtitle,
                        modifier = Modifier
                            .padding(horizontal = PrefsHorizontalPadding)
                            .secondaryItemAlpha(),
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 10,
                    )
                } else {
                    Text(
                        text = subtitle.toString(),
                        modifier = Modifier
                            .padding(horizontal = PrefsHorizontalPadding)
                            .secondaryItemAlpha(),
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 10,
                    )
                }
            }
        } else {
            null
        },
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    tint = iconTint,
                    contentDescription = null,
                )
            }
        } else {
            null
        },
        onClick = onPreferenceClick,
        widget = widget,
    )
}