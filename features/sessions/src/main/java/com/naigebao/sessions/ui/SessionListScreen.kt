package com.naigebao.sessions.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naigebao.sessions.viewmodel.SessionItemUi
import com.naigebao.sessions.viewmodel.SessionListViewModel
import com.naigebao.ui.components.SessionListSkeleton

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SessionListScreen(
    viewModel: SessionListViewModel,
    onSessionSelected: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val refreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = viewModel::refresh
    )
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && state.canLoadMore && !state.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = state.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(refreshState)
        ) {
            when {
                state.isRefreshing && state.sessions.isEmpty() -> {
                    SessionListSkeleton()
                }
                state.sessions.isEmpty() -> {
                    Text(
                        text = if (state.searchQuery.isNotBlank()) "No matching sessions" else "No sessions yet",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    if (state.searchQuery.isBlank() && state.groupedSessions.isNotEmpty()) {
                        GroupedSessionList(
                            groupedSessions = state.groupedSessions,
                            listState = listState,
                            isLoadingMore = state.isLoadingMore,
                            canLoadMore = state.canLoadMore,
                            onSessionSelected = onSessionSelected,
                            onPinClick = viewModel::togglePinStatus,
                            onLoadMore = viewModel::loadMore
                        )
                    } else {
                        SessionList(
                            sessions = state.sessions,
                            listState = listState,
                            isLoadingMore = state.isLoadingMore,
                            canLoadMore = state.canLoadMore,
                            onSessionSelected = onSessionSelected,
                            onPinClick = viewModel::togglePinStatus,
                            onLoadMore = viewModel::loadMore
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = refreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupedSessionList(
    groupedSessions: Map<String, List<SessionItemUi>>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onSessionSelected: (String) -> Unit,
    onPinClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        groupedSessions.forEach { (groupTitle, sessions) ->
            stickyHeader(key = "header_$groupTitle") {
                GroupHeader(title = groupTitle)
            }

            items(
                items = sessions,
                key = { it.id }
            ) { session ->
                SessionItemCard(
                    session = session,
                    onClick = onSessionSelected,
                    onPinClick = onPinClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (canLoadMore) {
            item(key = "load_more_trigger") {
                Box(modifier = Modifier.height(1.dp))
            }
        }

        item {
            Box(modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun GroupHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SessionList(
    sessions: List<SessionItemUi>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onSessionSelected: (String) -> Unit,
    onPinClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(
            items = sessions,
            key = { it.id }
        ) { session ->
            SessionItemCard(
                session = session,
                onClick = onSessionSelected,
                onPinClick = onPinClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        item {
            Box(modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search sessions...") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        modifier = modifier
    )
}
