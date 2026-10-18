package com.nuvio.app.features.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.features.home.components.HomeCatalogRowSection
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.HomeSkeletonRow
import com.nuvio.app.features.watched.WatchedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.home_empty_no_rows_message
import nuvio.composeapp.generated.resources.home_empty_no_rows_title
import org.jetbrains.compose.resources.stringResource

/**
 * Movies/Series use existing Nuvio shelf cards and navigation rather than a redesigned home UI.
 * Sections are sourced from enabled addons and server libraries and are independent of the
 * user's Home screen hide/reorder preferences.
 */
@Composable
internal fun SmartBrowseScreen(
    category: SmartBrowseCategory,
    modifier: Modifier = Modifier,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onCatalogClick: ((HomeCatalogSection) -> Unit)? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
) {
    val homeState by HomeRepository.uiState.collectAsStateWithLifecycle()
    val watchedState by WatchedRepository.uiState.collectAsStateWithLifecycle()
    val sections = remember(homeState.browseSections, category) {
        homeState.browseSections.forSmartBrowse(category)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { WatchedRepository.ensureLoaded() }
    ScreenActivityEffect(scrollToTopRequests) { active ->
        if (active) scrollToTopRequests.collect { listState.animateScrollToItem(0) }
    }

    NuvioScreen(
        modifier = modifier.fillMaxSize(),
        horizontalPadding = 0.dp,
        listState = listState,
        autoHidesNativeTabBar = true,
    ) {
        when {
            homeState.isLoading && sections.isEmpty() -> {
                items(3) {
                    HomeSkeletonRow(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            sections.isEmpty() -> {
                item {
                    HomeEmptyStateCard(
                        title = stringResource(Res.string.home_empty_no_rows_title),
                        message = stringResource(Res.string.home_empty_no_rows_message),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            else -> {
                items(sections, key = { it.key }) { section ->
                    HomeCatalogRowSection(
                        section = section,
                        entries = section.items.take(SMART_BROWSE_PREVIEW_LIMIT),
                        modifier = Modifier.padding(bottom = 12.dp),
                        onViewAllClick = if (section.canOpenCatalog(SMART_BROWSE_PREVIEW_LIMIT)) {
                            onCatalogClick?.let { callback -> { callback(section) } }
                        } else {
                            null
                        },
                        watchedKeys = watchedState.watchedKeys,
                        onPosterClick = onPosterClick,
                        onPosterLongClick = onPosterLongClick,
                    )
                }
            }
        }
    }
}

private const val SMART_BROWSE_PREVIEW_LIMIT = 18
