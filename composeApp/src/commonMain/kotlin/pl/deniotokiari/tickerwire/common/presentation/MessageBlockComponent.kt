package pl.deniotokiari.tickerwire.common.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NearbyError
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import pl.deniotokiari.tickerwire.theme.AppTheme
import pl.deniotokiari.tickerwire.theme.Spacing

@Composable
fun MessageBlockComponent(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                vertical = Spacing.lg,
                horizontal = Spacing.md,
            )
            .shadow(
                elevation = Spacing.xxxs,
                shape = RoundedCornerShape(Spacing.sm),
            )
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(vertical = Spacing.md, horizontal = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .padding(Spacing.sm)
                .size(Spacing.xl),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            modifier = Modifier.padding(Spacing.xs),
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
        )

        Text(
            modifier = Modifier.padding(bottom = Spacing.sm),
            text = description,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}

@Preview
@Composable
private fun MessageBlockComponentLightPreview() = AppTheme {
    MessageBlockComponent(
        icon = Icons.Default.NearbyError,
        title = "Title",
        description = "Description",
    )
}

@Preview
@Composable
private fun MessageBlockComponentDarkPreview() = AppTheme(darkTheme = true) {
    MessageBlockComponent(
        icon = Icons.Default.NearbyError,
        title = "Title",
        description = "Description",
    )
}
