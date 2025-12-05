package pl.deniotokiari.tickerwire.common.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.jetbrains.compose.ui.tooling.preview.Preview
import pl.deniotokiari.tickerwire.theme.AppTheme
import pl.deniotokiari.tickerwire.theme.Spacing
import pl.deniotokiari.tickerwire.theme.ValueNegative

@Composable
fun SnackMessageComponent(
    modifier: Modifier = Modifier,
    message: String,
    onCloseClick: () -> Unit,
    action: String? = null,
    onActionClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.ime)
            .padding(vertical = Spacing.lg, horizontal = Spacing.md)
            .background(color = ValueNegative, shape = RoundedCornerShape(Spacing.sm))
            .padding(start = Spacing.xs)
            .shadow(
                elevation = Spacing.xxxs,
                shape = RoundedCornerShape(Spacing.sm),
            )
            .background(color = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = Spacing.md, horizontal = Spacing.md)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onError,
            )

            action?.let { action ->
                Text(
                    modifier = Modifier
                        .padding(start = Spacing.xs)
                        .clickable { onActionClick() },
                    style = MaterialTheme.typography.bodySmall
                        .copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                        ),
                    text = action,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(
                modifier = Modifier.weight(0.9F),
            )

            Icon(
                modifier = Modifier.clickable { onCloseClick() },
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError,
            )
        }
    }
}

@Preview
@Composable
private fun SnackMessageComponentLightPreview() = AppTheme {
    Column(modifier = Modifier.fillMaxSize()) {
        SnackMessageComponent(
            message = "Message.",
            action = "Action.",
            onActionClick = {},
            onCloseClick = {},
        )

        SnackMessageComponent(
            message = "Message.",
            onCloseClick = {},
        )
    }
}

@Preview
@Composable
private fun SnackMessageComponentDarkPreview() = AppTheme(darkTheme = true) {
    Column(modifier = Modifier.fillMaxSize()) {
        SnackMessageComponent(
            message = "Message.",
            action = "Action.",
            onActionClick = {},
            onCloseClick = {},
        )

        SnackMessageComponent(
            message = "Message.",
            onCloseClick = {},
        )
    }
}
