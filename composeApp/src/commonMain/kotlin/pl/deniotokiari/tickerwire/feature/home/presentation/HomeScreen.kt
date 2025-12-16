package pl.deniotokiari.tickerwire.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.serialization.serializer
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import pl.deniotokiari.tickerwire.common.platform.openUrl
import pl.deniotokiari.tickerwire.common.presentation.MessageBlockComponent
import pl.deniotokiari.tickerwire.common.presentation.SnackMessageComponent
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerData
import pl.deniotokiari.tickerwire.model.TickerNews
import pl.deniotokiari.tickerwire.navigation.CollectData
import pl.deniotokiari.tickerwire.navigation.Route
import pl.deniotokiari.tickerwire.theme.AppTheme
import pl.deniotokiari.tickerwire.theme.LightOnSurface
import pl.deniotokiari.tickerwire.theme.Spacing
import pl.deniotokiari.tickerwire.theme.ValueNegative
import pl.deniotokiari.tickerwire.theme.ValuePositive
import tickerwire.composeapp.generated.resources.Res
import tickerwire.composeapp.generated.resources.app_name
import tickerwire.composeapp.generated.resources.error_failed_to_load_news_action
import tickerwire.composeapp.generated.resources.error_failed_to_load_news_message
import tickerwire.composeapp.generated.resources.my_watchlist
import tickerwire.composeapp.generated.resources.recent_news
import tickerwire.composeapp.generated.resources.watchlist_empty_description
import tickerwire.composeapp.generated.resources.watchlist_empty_title

private const val KEY_HEADER = "KEY_HEADER"
private const val KEY_MY_WATCH_LIST_HEADER = "KEY_MY_WATCH_LIST_HEADER"
private const val KEY_MY_WATCH_LIST_CONTENT = "KEY_MY_WATCH_LIST_CONTENT"
private const val KEY_NEWS_HEADER = "KEY_NEWS_HEADER"
private const val KEY_NEWS_CONTENT = "KEY_NEWS_CONTENT"

@Suppress("ModifierRequired", "EffectKeys")
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel(),
) {
    CollectData<Ticker>(
        key = Route.KEY_SEARCH_ITEM,
        serializer = serializer(),
        navController = navController,
    ) { ticker ->
        viewModel.onAction(HomeUiAction.OnAddTicker(ticker))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                HomeUiEvent.NavigateToSearch -> navController.navigate(Route.Search)
                is HomeUiEvent.OpenNewsUri -> openUrl(event.uri)
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onAction: (HomeUiAction) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = uiState.isRefreshing,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = uiState.isRefreshing,
                state = pullToRefreshState,
                containerColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        onRefresh = { onAction(HomeUiAction.OnRefresh) },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            item(key = KEY_HEADER) {
                Header(
                    isDarkTheme = uiState.isDarkTheme,
                    onAction = onAction,
                )
            }

            item(key = KEY_MY_WATCH_LIST_HEADER) {
                Text(
                    modifier = Modifier
                        .padding(top = Spacing.md)
                        .padding(horizontal = Spacing.md),
                    text = stringResource(Res.string.my_watchlist),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            watchListContent(
                state = uiState,
                onAction = onAction,
            )

            item(key = KEY_NEWS_HEADER) {
                Text(
                    modifier = Modifier
                        .padding(top = Spacing.sm)
                        .padding(horizontal = Spacing.md),
                    text = stringResource(Res.string.recent_news),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            newsContent(
                uiState = uiState,
                visitedNews = uiState.visitedNews,
                onAction = onAction,
            )
        }

        when (uiState.errorUiState) {
            HomeUiState.ErrorUiState.Error -> SnackMessageComponent(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.ime),
                message = stringResource(Res.string.error_failed_to_load_news_message),
                action = stringResource(Res.string.error_failed_to_load_news_action),
                onActionClick = { onAction(HomeUiAction.OnErrorMessageActionClick) },
                onCloseClick = { onAction(HomeUiAction.OnErrorMessageClose) },
            )

            HomeUiState.ErrorUiState.None -> Unit
        }
    }
}

private fun LazyListScope.watchListContent(
    state: HomeUiState,
    onAction: (HomeUiAction) -> Unit,
) {
    val tickers = state.tickers

    if (tickers.isEmpty()) {
        item(key = KEY_MY_WATCH_LIST_CONTENT) {
            MessageBlockComponent(
                modifier = Modifier,
                icon = Icons.Default.VisibilityOff,
                title = stringResource(Res.string.watchlist_empty_title),
                description = stringResource(Res.string.watchlist_empty_description),
            )
        }
    } else {
        val info = state.info

        item(key = KEY_MY_WATCH_LIST_CONTENT) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg, bottom = Spacing.md),
                contentPadding = PaddingValues(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.sm),
            ) {
                items(tickers, key = { item -> item.symbol }) { item ->
                    Box(
                        modifier = Modifier
                            .clickable { onAction(HomeUiAction.OnTickerClick(item)) },
                    ) {
                        Column(
                            modifier = Modifier
                                .width(Spacing.xxl * 3)
                                .shadow(
                                    elevation = Spacing.xxxs,
                                    shape = RoundedCornerShape(Spacing.sm),
                                )
                                .background(
                                    color = if (state.selectedTickers.contains(item)) {
                                        MaterialTheme.colorScheme.surfaceDim
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                                .padding(Spacing.md),
                        ) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = Spacing.xs),
                                text = item.symbol,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )

                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xs),
                                text = item.company,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Light,
                                ),
                            )

                            info[item]?.let { (_, marketValue, delta, percent, currency) ->
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.xs),
                                    text = currency + marketValue,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )

                                Row(
                                    modifier = Modifier
                                        .padding(top = Spacing.xxs)
                                        .fillMaxWidth(),
                                ) {
                                    Text(
                                        modifier = Modifier.padding(end = Spacing.xs * 2),
                                        text = delta,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = delta.tickerValueColor,
                                    )

                                    Text(
                                        text = "($percent)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = percent.tickerValueColor,
                                    )
                                }
                            }
                        }

                        Icon(
                            modifier = Modifier
                                .clickable { onAction(HomeUiAction.OnRemoveTicker(item)) }
                                .padding(Spacing.xs + Spacing.xxs)
                                .align(Alignment.TopEnd),
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.newsContent(
    visitedNews: Set<TickerNews>,
    uiState: HomeUiState,
    onAction: (HomeUiAction) -> Unit
) {
    val newsUiState = uiState.newsUiState

    when (newsUiState) {
        is HomeUiState.NewsUiState.Content -> {
            val news = uiState.filteredNews

            if (news.isEmpty()) {
                item(key = KEY_NEWS_CONTENT) {
                    MessageBlockComponent(
                        modifier = Modifier,
                        icon = Icons.Default.Newspaper,
                        title = stringResource(Res.string.watchlist_empty_title),
                        description = stringResource(Res.string.watchlist_empty_description),
                    )
                }
            } else {
                itemsIndexed(
                    items = news,
                    key = { _, item -> "${item.ticker.symbol}_${item.title.hashCode()}" },
                ) { index, item ->
                    if (index == 0) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }

                    val newsUrl = item.url

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (newsUrl != null) {
                                    Modifier.clickable {
                                        onAction(
                                            HomeUiAction.OnNewsClick(item)
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(top = Spacing.md)
                                .padding(bottom = Spacing.sm)
                                .padding(horizontal = Spacing.md)
                                .fillMaxWidth(),
                            text = "${item.ticker.symbol} | " + listOfNotNull(
                                item.provider,
                                item.dateTimeFormatted,
                            ).joinToString(separator = " - "),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Light,
                            ),
                        )

                        Text(
                            modifier = Modifier
                                .padding(horizontal = Spacing.md)
                                .fillMaxWidth(),
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (visitedNews.contains(item)) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = FontWeight.Normal,
                            ),
                        )

                        Spacer(
                            modifier = Modifier
                                .padding(top = Spacing.md)
                                .height(Spacing.xxxs)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = if (index < news.size - 1) 0.1F else 0F,
                                    )
                                ),
                        )
                    }
                }
            }
        }

        HomeUiState.NewsUiState.Loading -> {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xxl),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    isDarkTheme: Boolean,
    onAction: (HomeUiAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isDarkTheme) {
                Icons.Default.LightMode
            } else {
                Icons.Default.DarkMode
            },
            contentDescription = null,
            modifier = Modifier
                .clickable { onAction(HomeUiAction.OnThemeChangeClick) }
                .padding(Spacing.md),
            tint = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            modifier = Modifier.weight(0.9F),
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier
                .clickable { onAction(HomeUiAction.OnSearchClick) }
                .padding(Spacing.md),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val String.tickerValueColor: Color
    get() = when {
        startsWith("+") -> ValuePositive
        startsWith("-") -> ValueNegative
        else -> LightOnSurface
    }

@Preview
@Composable
private fun HomeContentLightEmptyPreview() = AppTheme {
    HomeContent(
        uiState = HomeUiState(
            tickers = emptyList(),
            newsUiState = HomeUiState.NewsUiState.Loading,
            isRefreshing = true,
            errorUiState = HomeUiState.ErrorUiState.Error,
        ),
        onAction = {},
    )
}

@Preview
@Composable
private fun HomeContentLightPreview() = AppTheme {
    val tickers = listOf(
        Ticker(symbol = "APPL", company = "Apple Inc"),
        Ticker(symbol = "PLTR", company = "Palantir Inc"),
        Ticker(symbol = "GOOGL", company = "Alphabet Inc"),
    )
    val info = mapOf(
        tickers[0] to TickerData(
            ticker = tickers[0],
            marketValueFormatted = "172.28",
            deltaFormatted = "+2.59",
            percentFormatted = "+1.50%",
            currency = "$",
        ),
        tickers[1] to TickerData(
            ticker = tickers[1],
            marketValueFormatted = "139.83",
            deltaFormatted = "-1.05",
            percentFormatted = "-0.75%",
            currency = "$",
        ),
        tickers[2] to TickerData(
            ticker = tickers[2],
            marketValueFormatted = "256.49",
            deltaFormatted = "+5.28",
            percentFormatted = "+2.10%",
            currency = "$",
        ),
    )
    val now = 0L
    val news = listOf(
        TickerNews(
            ticker = tickers[0],
            title = "Apple supplier Foxconn's Q2 profit plunges amid weakening demand.",
            provider = "Bloomberg",
            dateTimeFormatted = "5m ago",
            timestamp = now,
            url = "https://example.com/news/1",
        ),
        TickerNews(
            ticker = tickers[1],
            title = "Google announces major AI advancements for its search engine.",
            dateTimeFormatted = "12m ago",
            provider = null,
            timestamp = now + 1,
            url = null,
        ),
        TickerNews(
            ticker = tickers[2],
            title = "Tesla faces new investigation over Autopilot claims, shares dip.",
            provider = "WSJ",
            dateTimeFormatted = "25m ago",
            timestamp = now + 2,
            url = "https://example.com/news/3",
        ),
    )
    val visitedNews = setOf(news.first())

    HomeContent(
        uiState = HomeUiState(
            tickers = tickers,
            newsUiState = HomeUiState.NewsUiState.Content(news = news),
            info = info,
            visitedNews = visitedNews,
            isRefreshing = true,
            errorUiState = HomeUiState.ErrorUiState.Error,
            selectedTickers = setOf(tickers.first()),
        ),
        onAction = {},
    )
}

@Preview
@Composable
private fun HomeContentDarkEmptyPreview() = AppTheme(darkTheme = true) {
    HomeContent(
        uiState = HomeUiState(
            tickers = emptyList(),
            newsUiState = HomeUiState.NewsUiState.Loading,
            isRefreshing = true,
            errorUiState = HomeUiState.ErrorUiState.Error,
        ),
        onAction = {},
    )
}

@Preview
@Composable
private fun HomeContentDarkPreview() = AppTheme(darkTheme = true) {
    val tickers = listOf(
        Ticker(symbol = "APPL", company = "Apple Inc"),
        Ticker(symbol = "PLTR", company = "Palantir Inc"),
        Ticker(symbol = "GOOGL", company = "Alphabet Inc"),
    )
    val info = mapOf(
        tickers[0] to TickerData(
            ticker = tickers[0],
            marketValueFormatted = "172.28",
            deltaFormatted = "+2.59",
            percentFormatted = "+1.50%",
            currency = "$",
        ),
        tickers[1] to TickerData(
            ticker = tickers[1],
            marketValueFormatted = "139.83",
            deltaFormatted = "-1.05",
            percentFormatted = "-0.75%",
            currency = "$",
        ),
        tickers[2] to TickerData(
            ticker = tickers[2],
            marketValueFormatted = "256.49",
            deltaFormatted = "+5.28",
            percentFormatted = "+2.10%",
            currency = "$",
        ),
    )
    val now = 0L
    val news = listOf(
        TickerNews(
            ticker = tickers[0],
            title = "Apple supplier Foxconn's Q2 profit plunges amid weakening demand.",
            provider = "Bloomberg",
            dateTimeFormatted = "5m ago",
            timestamp = now,
            url = "https://example.com/news/1",
        ),
        TickerNews(
            ticker = tickers[1],
            title = "Google announces major AI advancements for its search engine.",
            dateTimeFormatted = "12m ago",
            provider = null,
            timestamp = now + 1,
            url = null,
        ),
        TickerNews(
            ticker = tickers[2],
            title = "Tesla faces new investigation over Autopilot claims, shares dip.",
            provider = "WSJ",
            dateTimeFormatted = "25m ago",
            timestamp = now + 2,
            url = "https://example.com/news/3",
        ),
    )
    val visitedNews = setOf(news.first())

    HomeContent(
        uiState = HomeUiState(
            tickers = tickers,
            newsUiState = HomeUiState.NewsUiState.Content(news = news),
            info = info,
            visitedNews = visitedNews,
            isRefreshing = true,
            errorUiState = HomeUiState.ErrorUiState.Error,
            selectedTickers = setOf(tickers.first()),
        ),
        onAction = {},
    )
}
