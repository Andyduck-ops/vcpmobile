package com.naigebao.sessions.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naigebao.storage.database.entity.SessionEntity
import com.naigebao.storage.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class SessionListState(
    val sessions: List<SessionItemUi> = emptyList(),
    val groupedSessions: Map<String, List<SessionItemUi>> = emptyMap(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val searchQuery: String = ""
)

sealed class DateGroup(val displayName: String) {
    object Today : DateGroup("Today")
    object Yesterday : DateGroup("Yesterday")
    object ThisWeek : DateGroup("This Week")
    object Earlier : DateGroup("Earlier")
    data class Custom(val date: String) : DateGroup(date)

    companion object {
        fun fromTimestamp(timestamp: Long): DateGroup {
            val now = System.currentTimeMillis()
            val todayStart = now - (now % (24 * 60 * 60 * 1000))
            val yesterdayStart = todayStart - (24 * 60 * 60 * 1000)
            val weekStart = todayStart - (7 * 24 * 60 * 60 * 1000)

            return when {
                timestamp >= todayStart -> Today
                timestamp >= yesterdayStart -> Yesterday
                timestamp >= weekStart -> ThisWeek
                else -> Earlier
            }
        }

        fun order(group: DateGroup): Int = when (group) {
            is Today -> 0
            is Yesterday -> 1
            is ThisWeek -> 2
            is Earlier -> 3
            is Custom -> 4
        }
    }
}

data class SessionItemUi(
    val id: String,
    val title: String,
    val preview: String,
    val unreadCount: Int,
    val updatedAt: Long,
    val dateGroup: DateGroup = DateGroup.fromTimestamp(updatedAt),
    val isFork: Boolean = false,
    val forkSourceSessionId: String? = null,
    val forkSourceMessageId: String? = null,
    val isPinned: Boolean = false,
    val highlightedTitle: String? = null,
    val highlightedPreview: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SessionListViewModel(
    private val sessionRepository: SessionRepository,
    private val refreshAction: suspend () -> Unit = {}
) : ViewModel() {
    private val _state = MutableStateFlow(SessionListState())
    val state: StateFlow<SessionListState> = _state.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private var currentPage = 0
    private val pageSize = 20

    init {
        viewModelScope.launch {
            searchQueryFlow.flatMapLatest { query ->
                if (query.isBlank()) {
                    sessionRepository.observeSessions()
                } else {
                    sessionRepository.searchSessions(query)
                }
            }.collect { sessions ->
                val query = searchQueryFlow.value
                val sortedSessions = sessions
                    .sortedWith(compareByDescending<SessionEntity> { it.isPinned }.thenByDescending { it.updatedAt })
                    .map { it.toUi(query) }

                val hasMore = sortedSessions.size > pageSize
                val visibleSessions = if (currentPage == 0) {
                    sortedSessions.take(pageSize)
                } else {
                    sortedSessions.take((currentPage + 1) * pageSize)
                }

                _state.value = _state.value.copy(
                    sessions = visibleSessions,
                    groupedSessions = groupSessionsByDate(visibleSessions),
                    canLoadMore = hasMore && visibleSessions.size < sortedSessions.size
                )
            }
        }
    }

    private fun groupSessionsByDate(sessions: List<SessionItemUi>): Map<String, List<SessionItemUi>> {
        return sessions.groupBy { it.dateGroup.displayName }
            .toSortedMap(compareBy { key ->
                val group = sessions.find { it.dateGroup.displayName == key }?.dateGroup
                DateGroup.order(group ?: DateGroup.Earlier)
            })
    }

    fun updateSearchQuery(query: String) {
        currentPage = 0
        _state.value = _state.value.copy(searchQuery = query, canLoadMore = false)
        searchQueryFlow.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            currentPage = 0
            _state.value = _state.value.copy(isRefreshing = true)
            refreshAction()
            _state.value = _state.value.copy(isRefreshing = false)
        }
    }

    fun loadMore() {
        if (_state.value.isLoadingMore || !_state.value.canLoadMore) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)
            currentPage++

            val query = searchQueryFlow.value
            val allSessions = if (query.isBlank()) {
                sessionRepository.getAllSessionsSync()
            } else {
                sessionRepository.searchSessionsSync(query)
            }

            val sortedSessions = allSessions
                .sortedWith(compareByDescending<SessionEntity> { it.isPinned }.thenByDescending { it.updatedAt })
                .map { it.toUi(query) }

            val visibleSessions = sortedSessions.take((currentPage + 1) * pageSize)

            _state.value = _state.value.copy(
                sessions = visibleSessions,
                groupedSessions = groupSessionsByDate(visibleSessions),
                isLoadingMore = false,
                canLoadMore = visibleSessions.size < sortedSessions.size
            )
        }
    }

    fun togglePinStatus(sessionId: String) {
        viewModelScope.launch {
            val session = sessionRepository.getSessionById(sessionId) ?: return@launch
            sessionRepository.updatePinStatus(sessionId, !session.isPinned)
        }
    }

    private fun SessionEntity.toUi(query: String): SessionItemUi {
        val title = title
        val preview = lastMessagePreview ?: "No messages yet"

        return if (query.isNotBlank()) {
            SessionItemUi(
                id = id,
                title = title,
                preview = preview,
                unreadCount = unreadCount,
                updatedAt = updatedAt,
                isFork = type == "fork",
                forkSourceSessionId = forkSourceSessionId,
                forkSourceMessageId = forkSourceMessageId,
                isPinned = isPinned,
                highlightedTitle = highlightMatch(title, query),
                highlightedPreview = highlightMatch(preview, query)
            )
        } else {
            SessionItemUi(
                id = id,
                title = title,
                preview = preview,
                unreadCount = unreadCount,
                updatedAt = updatedAt,
                isFork = type == "fork",
                forkSourceSessionId = forkSourceSessionId,
                forkSourceMessageId = forkSourceMessageId,
                isPinned = isPinned
            )
        }
    }

    private fun highlightMatch(text: String, query: String): String {
        if (query.isBlank()) return text
        return text.replace(Regex("($query)", RegexOption.IGNORE_CASE), "⟨$1⟩")
    }
}
