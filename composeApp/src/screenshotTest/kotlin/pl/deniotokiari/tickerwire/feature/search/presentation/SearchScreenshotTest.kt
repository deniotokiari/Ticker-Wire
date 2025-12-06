package pl.deniotokiari.tickerwire.feature.search.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview
@Composable
fun SearchScreenshotTest() {
    SearchContent(
        uiState = SearchUiState.Idle(),
        onAction = {},
    )
}
