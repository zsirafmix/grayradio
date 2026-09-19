package com.grayradio.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.grayradio.app.data.entity.Station
import com.grayradio.app.data.remote.GenreOption
import com.grayradio.app.data.remote.RadioBrowserApi
import com.grayradio.app.data.remote.RemoteCountry
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
import kotlin.math.absoluteValue

enum class HomeTab {
    Saved,
    Favorites,
    Browse,
}

data class SearchUiState(
    val query: String = "",
    val results: List<RemoteStation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class BrowseUiState(
    val countries: List<RemoteCountry> = emptyList(),
    val selectedCountryCode: String = "HU",
    val selectedTag: String? = null,
    val stations: List<RemoteStation> = emptyList(),
    val isLoadingCountries: Boolean = false,
    val isLoadingStations: Boolean = false,
    val error: String? = null,
    val loadedOnce: Boolean = false,
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

    val favorites: StateFlow<List<Station>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playback: StateFlow<PlaybackUiState> = player.state

    private val _homeTab = MutableStateFlow(HomeTab.Saved)
    val homeTab: StateFlow<HomeTab> = _homeTab.asStateFlow()

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    private val _browse = MutableStateFlow(BrowseUiState())
    val browse: StateFlow<BrowseUiState> = _browse.asStateFlow()

    val genres: List<GenreOption> = RadioBrowserApi.CURATED_GENRES

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

    fun setHomeTab(tab: HomeTab) {
        _homeTab.value = tab
        if (tab == HomeTab.Browse && !_browse.value.loadedOnce) {
            loadBrowse(forceCountries = true)
        }
    }

    fun play(station: Station) {
        player.play(station)
    }

    fun playRemote(remote: RemoteStation) {
        viewModelScope.launch {
            val existing = repository.findByUrl(remote.streamUrl)
            val station = existing ?: remote.toEphemeralStation()
            player.play(station)
        }
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
                    _events.emit(UiEvent.Message(messageKey(e)))
                },
            )
        }
    }

    fun updateStation(station: Station) {
        viewModelScope.launch {
            repository.update(station).fold(
                onSuccess = { _events.emit(UiEvent.Message("station_updated")) },
                onFailure = { e ->
                    _events.emit(UiEvent.Message(messageKey(e)))
                },
            )
        }
    }

    fun deleteStation(station: Station) {
        viewModelScope.launch {
            val playing = player.state.value.station
            if (playing?.id == station.id || playing?.streamUrl == station.streamUrl) {
                player.stop()
            }
            repository.delete(station)
            _events.emit(UiEvent.Message("station_deleted"))
        }
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            val updated = repository.toggleFavorite(station)
            val key = if (updated.isFavorite) "favorite_added" else "favorite_removed"
            _events.emit(UiEvent.Message(key))
        }
    }

    fun toggleFavoriteRemote(remote: RemoteStation) {
        viewModelScope.launch {
            val existing = repository.findByUrl(remote.streamUrl)
            val wantFavorite = existing?.isFavorite != true
            repository.setFavoriteByUrl(remote, wantFavorite).fold(
                onSuccess = {
                    val key = if (wantFavorite) "favorite_added" else "favorite_removed"
                    _events.emit(UiEvent.Message(key))
                },
                onFailure = { e ->
                    _events.emit(UiEvent.Message(messageKey(e)))
                },
            )
        }
    }

    fun isFavoriteUrl(url: String): Boolean =
        stations.value.any { it.streamUrl == url && it.isFavorite }

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

    fun addRemoteAsFavorite(remote: RemoteStation) {
        viewModelScope.launch {
            repository.addRemote(remote, favorite = true).fold(
                onSuccess = { _events.emit(UiEvent.Message("favorite_added")) },
                onFailure = { e ->
                    if (e.message == "already_added") {
                        repository.setFavoriteByUrl(remote, true)
                        _events.emit(UiEvent.Message("favorite_added"))
                    } else {
                        _events.emit(UiEvent.Message("stream_error"))
                    }
                },
            )
        }
    }

    fun clearSearch() {
        _search.value = SearchUiState()
    }

    fun selectCountry(code: String) {
        if (_browse.value.selectedCountryCode == code) return
        _browse.update { it.copy(selectedCountryCode = code) }
        loadBrowseStations()
    }

    fun selectTag(tag: String?) {
        if (_browse.value.selectedTag == tag) return
        _browse.update { it.copy(selectedTag = tag) }
        loadBrowseStations()
    }

    fun refreshBrowse() {
        loadBrowse(forceCountries = _browse.value.countries.isEmpty())
    }

    private fun loadBrowse(forceCountries: Boolean) {
        if (forceCountries) {
            viewModelScope.launch {
                _browse.update { it.copy(isLoadingCountries = true, error = null) }
                runCatching { repository.fetchCountries() }
                    .onSuccess { list ->
                        val current = _browse.value.selectedCountryCode
                        val code = if (list.any { it.code == current }) current else "HU"
                        _browse.update {
                            it.copy(
                                countries = list,
                                selectedCountryCode = code,
                                isLoadingCountries = false,
                            )
                        }
                        loadBrowseStations()
                    }
                    .onFailure {
                        _browse.update {
                            it.copy(isLoadingCountries = false, error = "browse_error")
                        }
                        _events.emit(UiEvent.Message("browse_error"))
                        loadBrowseStations()
                    }
            }
        } else {
            loadBrowseStations()
        }
    }

    private fun loadBrowseStations() {
        val country = _browse.value.selectedCountryCode
        val tag = _browse.value.selectedTag
        viewModelScope.launch {
            _browse.update { it.copy(isLoadingStations = true, error = null) }
            runCatching { repository.browseStations(country, tag) }
                .onSuccess { list ->
                    _browse.update {
                        it.copy(
                            stations = list,
                            isLoadingStations = false,
                            loadedOnce = true,
                        )
                    }
                }
                .onFailure {
                    _browse.update {
                        it.copy(
                            isLoadingStations = false,
                            error = "browse_error",
                            stations = emptyList(),
                            loadedOnce = true,
                        )
                    }
                    _events.emit(UiEvent.Message("browse_error"))
                }
        }
    }

    private fun messageKey(e: Throwable): String = when (e.message) {
        "name" -> "name_required"
        "url" -> "url_required"
        "invalid_url" -> "invalid_url"
        "already_added" -> "already_added"
        else -> "stream_error"
    }

    override fun onCleared() {
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

fun RemoteStation.toEphemeralStation(): Station {
    val ephemeralId = -(streamUrl.hashCode().toLong().absoluteValue.takeIf { it != 0L } ?: 1L)
    return Station(
        id = ephemeralId,
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        countryCode = countryCode,
        tags = tags,
    )
}
