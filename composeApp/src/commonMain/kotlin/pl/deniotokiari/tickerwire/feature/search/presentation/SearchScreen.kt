package pl.deniotokiari.tickerwire.feature.search.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.serialization.serializer
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import pl.deniotokiari.tickerwire.common.presentation.MessageBlockComponent
import pl.deniotokiari.tickerwire.common.presentation.SnackMessageComponent
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.navigation.Route
import pl.deniotokiari.tickerwire.navigation.popBackStackWithData
import pl.deniotokiari.tickerwire.theme.AppTheme
import pl.deniotokiari.tickerwire.theme.Spacing
import tickerwire.composeapp.generated.resources.Res
import tickerwire.composeapp.generated.resources.error_failed_to_load_search_results_action
import tickerwire.composeapp.generated.resources.error_failed_to_load_search_results_message
import tickerwire.composeapp.generated.resources.search_loading
import tickerwire.composeapp.generated.resources.search_no_results_message
import tickerwire.composeapp.generated.resources.search_no_results_title
import tickerwire.composeapp.generated.resources.search_placeholder

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                SearchUiEvent.NavigateBack -> navController.popBackStack()
                is SearchUiEvent.NavigateBackWithSelectedSearchItem -> {
                    navController.popBackStackWithData(
                        key = Route.KEY_SEARCH_ITEM,
                        serializer = serializer<Ticker>(),
                        data = event.item,
                    )
                }
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    SearchContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun SearchContent(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
) {
    Box {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = Spacing.md, start = Spacing.sm)
                    .padding(top = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier
                        .padding(end = Spacing.sm)
                        .clickable { onAction(SearchUiAction.OnBackClicked) },
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )

                SearchInput(
                    query = uiState.query,
                    onAction = onAction,
                )
            }

            when (uiState) {
                is SearchUiState.Content -> Content(uiState, onAction)
                is SearchUiState.Empty -> Empty(uiState)
                is SearchUiState.Idle, is SearchUiState.Error -> Unit
                is SearchUiState.Loading -> Loading()
            }
        }

        if (uiState is SearchUiState.Error) {
            Error(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.ime),
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun Loading() {
    val infiniteTransition = rememberInfiniteTransition(label = "alpha")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5F,
        targetValue = 1.0F,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha_animation",
    )

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.md,
                vertical = Spacing.lg,
            ),
        textAlign = TextAlign.Center,
        text = stringResource(Res.string.search_loading),
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        ),
    )
}

@Composable
private fun Content(
    uiState: SearchUiState.Content,
    onAction: (SearchUiAction) -> Unit,
) {
    LazyColumn(modifier = Modifier.padding(top = Spacing.md)) {
        itemsIndexed(
            items = uiState.items,
            key = { _, item -> "${item.symbol}_${item.company}" },
        ) { index, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(SearchUiAction.OnSearchItemClicked(item)) }
                    .padding(horizontal = Spacing.md),
            ) {
                Text(
                    modifier = Modifier.padding(top = Spacing.md),
                    text = item.symbol,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )

                Text(
                    modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.md),
                    text = item.company,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )

                if (index < uiState.items.size - 1) {
                    Spacer(
                        modifier = Modifier
                            .height(Spacing.xxxs)
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.1F,
                                ),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Empty(uiState: SearchUiState.Empty) {
    MessageBlockComponent(
        icon = Icons.Default.SearchOff,
        title = stringResource(Res.string.search_no_results_title, uiState.query),
        description = stringResource(Res.string.search_no_results_message),
    )
}

@Composable
private fun Error(
    modifier: Modifier,
    onAction: (SearchUiAction) -> Unit,
) {
    SnackMessageComponent(
        modifier = modifier,
        message = stringResource(Res.string.error_failed_to_load_search_results_message),
        action = stringResource(Res.string.error_failed_to_load_search_results_action),
        onActionClick = { onAction(SearchUiAction.OnErrorMessageActionClicked) },
        onCloseClick = { onAction(SearchUiAction.OnErrorMessageClosed) },
    )
}

@Composable
private fun SearchInput(
    query: String,
    onAction: (SearchUiAction) -> Unit,
) {
    val textStyle = MaterialTheme.typography.bodyMedium
        .copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
        )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = Spacing.xxxs,
                shape = RoundedCornerShape(size = Spacing.sm),
            )
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(vertical = Spacing.xs),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = Spacing.md),
            tint = if (query.isEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Spacing.xxl,
                    end = Spacing.md,
                    top = Spacing.xs + Spacing.sm,
                    bottom = Spacing.xs + Spacing.sm,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(Res.string.search_placeholder),
                    style = textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            BasicTextField(
                value = query,
                onValueChange = { value -> onAction(SearchUiAction.OnQueryChanged(value)) },
                textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
private fun SearchLoadingIdleLightPreview() = AppTheme {
    SearchContent(
        uiState = SearchUiState.Loading("APPL"),
        onAction = {},
    )
}

@Preview
@Composable
private fun SearchContentIdleLightPreview() = AppTheme {
    SearchContent(
        uiState = SearchUiState.Idle(),
        onAction = {},
    )
}

@Preview
@Composable
private fun SearchContentEmptyLightPreview() = AppTheme {
    SearchContent(
        uiState = SearchUiState.Empty(query = "APPL"),
        onAction = {},
    )
}

@Preview
@Composable
private fun SearchContentErrorLightPreview() = AppTheme {
    SearchContent(
        uiState = SearchUiState.Error(query = "APPL"),
        onAction = {},
    )
}

@Preview
@Composable
private fun SearchContentContentLightPreview() = AppTheme {
    SearchContent(
        uiState = SearchUiState.Content(
            query = "APPL",
            items = listOf(
                Ticker(symbol = "APPL", company = "Apple Inc"),
                Ticker(symbol = "PLTR", company = "Palantir Inc"),
                Ticker(symbol = "GOOGL", company = "Alphabet Inc"),
            ),
        ),
        onAction = {},
    )
}

@Preview
@Composable
private fun SearchLoadingIdleDarkPreview() = AppTheme(darkTheme = true) {
    SearchContent(
        uiState = SearchUiState.Loading("APPL"),
        onAction = {},
    )
}

@Preview
@Composable
private fun SearchContentIdleDarkPreview() = AppTheme(darkTheme = true) {
    SearchContent(
        uiState = SearchUiState.Idle(),
        onAction = {},
    )
}

@Preview
@Composable
private fun SearchContentEmptyDarkPreview() = AppTheme(darkTheme = true) {
    SearchContent(
        uiState = SearchUiState.Empty(query = "APPL"),
        onAction = {},
    )
}

@Preview
@Composable
private fun SearchContentErrorDarkPreview() = AppTheme(darkTheme = true) {
    SearchContent(
        uiState = SearchUiState.Error(query = "APPL"),
        onAction = {},
    )
}

@Preview
@Composable
private fun SearchContentContentDarkPreview() = AppTheme(darkTheme = true) {
    SearchContent(
        uiState = SearchUiState.Content(
            query = "APPL",
            items = listOf(
                Ticker(symbol = "APPL", company = "Apple Inc"),
                Ticker(symbol = "PLTR", company = "Palantir Inc"),
                Ticker(symbol = "GOOGL", company = "Alphabet Inc"),
            ),
        ),
        onAction = {},
    )
}
