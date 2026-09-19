package com.grayradio.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.grayradio.app.data.entity.Station
import com.grayradio.app.data.remote.RemoteStation
import com.grayradio.app.data.repository.StationRepository
import com.grayradio.app.player.PlaybackUiState
import com.grayradio.app.player.RadioPlayer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<RemoteStation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed class UiEvent {
    data class Message(val key: String) : UiEvent()
}

class MainViewModel(
    private val repository: StationRepository,
    private val player: RadioPlayer,
) : ViewModel() {

    val stations: StateFlow<List<Station>> = repository.observeStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playback: StateFlow<PlaybackUiState> = player.state

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
        viewModelScope.launch {
            player.state.collect { state ->
                if (state.errorMessage != null) {
                    _events.emit(UiEvent.Message("stream_error"))
                    player.clearError()
                }
            }
        }
    }

    fun play(station: Station) {
        player.play(station)
    }

    fun togglePlayPause() {
        player.togglePlayPause()
    }

    fun pause() {
        player.pause()
    }

    fun addManual(name: String, url: String) {
        viewModelScope.launch {
            repository.addManual(name, url).fold(
                onSuccess = { _events.emit(UiEvent.Message("station_added")) },
                onFailure = { e ->
                    val key = when (e.message) {
                        "name" -> "name_required"
                        "url" -> "url_required"
                        "invalid_url" -> "invalid_url"
                        "already_added" -> "already_added"
                        else -> "stream_error"
                    }
                    _events.emit(UiEvent.Message(key))
                },
            )
        }
    }

    fun updateStation(station: Station) {
        viewModelScope.launch {
            repository.update(station).fold(
                onSuccess = { _events.emit(UiEvent.Message("station_updated")) },
                onFailure = { e ->
                    val key = when (e.message) {
                        "name" -> "name_required"
                        "invalid_url" -> "invalid_url"
                        else -> "stream_error"
                    }
                    _events.emit(UiEvent.Message(key))
                },
            )
        }
    }

    fun deleteStation(station: Station) {
        viewModelScope.launch {
            val playing = player.state.value.station
            if (playing?.id == station.id) {
                player.stop()
            }
            repository.delete(station)
            _events.emit(UiEvent.Message("station_deleted"))
        }
    }

    fun setSearchQuery(query: String) {
        _search.update { it.copy(query = query) }
    }

    fun searchByName() {
        val q = _search.value.query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _search.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.searchByName(q) }
                .onSuccess { list ->
                    _search.update { it.copy(results = list, isLoading = false) }
                }
                .onFailure {
                    _search.update { it.copy(isLoading = false, error = "search_error", results = emptyList()) }
                    _events.emit(UiEvent.Message("search_error"))
                }
        }
    }

    fun searchHungarian() {
        viewModelScope.launch {
            _search.update { it.copy(isLoading = true, error = null, query = "") }
            runCatching { repository.searchHungarian() }
                .onSuccess { list ->
                    _search.update { it.copy(results = list, isLoading = false) }
                }
                .onFailure {
                    _search.update { it.copy(isLoading = false, error = "search_error", results = emptyList()) }
                    _events.emit(UiEvent.Message("search_error"))
                }
        }
    }

    fun addRemote(remote: RemoteStation) {
        viewModelScope.launch {
            repository.addRemote(remote).fold(
                onSuccess = { _events.emit(UiEvent.Message("station_added")) },
                onFailure = { e ->
                    val key = if (e.message == "already_added") "already_added" else "stream_error"
                    _events.emit(UiEvent.Message(key))
                },
            )
        }
    }

    fun clearSearch() {
        _search.value = SearchUiState()
    }

    override fun onCleared() {
        // Player lives in Application — do not release here.
        super.onCleared()
    }

    class Factory(
        private val repository: StationRepository,
        private val player: RadioPlayer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository, player) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
