package com.grayradio.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grayradio.app.R
import com.grayradio.app.data.entity.Station
import com.grayradio.app.data.remote.RemoteCountry
import com.grayradio.app.data.remote.RemoteStation
import com.grayradio.app.ui.components.NowPlayingBar
import com.grayradio.app.ui.components.StationDialogMode
import com.grayradio.app.ui.components.StationEditorDialog
import com.grayradio.app.ui.components.StationTile
import com.grayradio.app.ui.theme.Gray200
import com.grayradio.app.ui.theme.Gray400
import com.grayradio.app.ui.theme.Gray600
import com.grayradio.app.ui.theme.Gray700
import com.grayradio.app.ui.theme.Gray850
import com.grayradio.app.ui.theme.Gray900
import com.grayradio.app.ui.viewmodel.BrowseUiState
import com.grayradio.app.ui.viewmodel.HomeTab
import com.grayradio.app.ui.viewmodel.MainViewModel
import com.grayradio.app.ui.viewmodel.UiEvent
import com.grayradio.app.ui.viewmodel.toEphemeralStation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val browse by viewModel.browse.collectAsStateWithLifecycle()
    val homeTab by viewModel.homeTab.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingStation by remember { mutableStateOf<Station?>(null) }
    var pendingDelete by remember { mutableStateOf<Station?>(null) }
    var showCountrySheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> {
                    val msg = when (event.key) {
                        "stream_error" -> context.getString(R.string.stream_error)
                        "station_added" -> context.getString(R.string.station_added)
                        "station_deleted" -> context.getString(R.string.station_deleted)
                        "station_updated" -> context.getString(R.string.station_updated)
                        "name_required" -> context.getString(R.string.name_required)
                        "url_required" -> context.getString(R.string.url_required)
                        "invalid_url" -> context.getString(R.string.invalid_url)
                        "already_added" -> context.getString(R.string.already_added)
                        "search_error" -> context.getString(R.string.search_error)
                        "browse_error" -> context.getString(R.string.browse_error)
                        "favorite_added" -> context.getString(R.string.favorite_added)
                        "favorite_removed" -> context.getString(R.string.favorite_removed)
                        else -> event.key
                    }
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    Scaffold(
        containerColor = Gray900,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    if (homeTab == HomeTab.Browse) {
                        IconButton(onClick = { viewModel.refreshBrowse() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh),
                                tint = Gray200,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Gray850,
                    titleContentColor = Gray200,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Gray200,
                contentColor = Gray900,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_station),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (playback.station != null) {
                NowPlayingBar(
                    playback = playback,
                    onToggle = { viewModel.togglePlayPause() },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            HomeTabs(
                selected = homeTab,
                onSelect = viewModel::setHomeTab,
            )

            when (homeTab) {
                HomeTab.Saved -> {
                    StationGrid(
                        stations = stations,
                        playbackStation = playback.station,
                        isPlaying = playback.isPlaying,
                        emptyText = stringResource(R.string.empty_stations),
                        onPlay = { station ->
                            val isCurrent = playback.station?.streamUrl == station.streamUrl
                            if (isCurrent && playback.isPlaying) {
                                viewModel.pause()
                            } else {
                                viewModel.play(station)
                            }
                        },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onEdit = { editingStation = it },
                        onDelete = { pendingDelete = it },
                        showMenu = true,
                    )
                }

                HomeTab.Favorites -> {
                    StationGrid(
                        stations = favorites,
                        playbackStation = playback.station,
                        isPlaying = playback.isPlaying,
                        emptyText = stringResource(R.string.empty_favorites),
                        onPlay = { station ->
                            val isCurrent = playback.station?.streamUrl == station.streamUrl
                            if (isCurrent && playback.isPlaying) {
                                viewModel.pause()
                            } else {
                                viewModel.play(station)
                            }
                        },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onEdit = { editingStation = it },
                        onDelete = { pendingDelete = it },
                        showMenu = true,
                    )
                }

                HomeTab.Browse -> {
                    BrowsePanel(
                        browse = browse,
                        genres = viewModel.genres,
                        savedStations = stations,
                        playbackStation = playback.station,
                        isPlaying = playback.isPlaying,
                        onSelectCountryChip = { showCountrySheet = true },
                        onSelectTag = viewModel::selectTag,
                        onPlayRemote = { remote ->
                            val isCurrent = playback.station?.streamUrl == remote.streamUrl
                            if (isCurrent && playback.isPlaying) {
                                viewModel.pause()
                            } else {
                                viewModel.playRemote(remote)
                            }
                        },
                        onToggleFavoriteRemote = viewModel::toggleFavoriteRemote,
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        StationEditorDialog(
            mode = StationDialogMode.Add,
            searchState = search,
            onDismiss = { showAddDialog = false },
            onSaveManual = { name, url -> viewModel.addManual(name, url) },
            onSearchQueryChange = viewModel::setSearchQuery,
            onSearchByName = viewModel::searchByName,
            onSearchHungarian = viewModel::searchHungarian,
            onAddRemote = viewModel::addRemote,
            onFavoriteRemote = viewModel::addRemoteAsFavorite,
            onClearSearch = viewModel::clearSearch,
            favoriteUrls = stations.filter { it.isFavorite }.map { it.streamUrl }.toSet(),
        )
    }

    editingStation?.let { station ->
        StationEditorDialog(
            mode = StationDialogMode.Edit,
            initial = station,
            searchState = search,
            onDismiss = { editingStation = null },
            onSaveManual = { name, url ->
                viewModel.updateStation(station.copy(name = name, streamUrl = url))
            },
            onSearchQueryChange = {},
            onSearchByName = {},
            onSearchHungarian = {},
            onAddRemote = {},
            onFavoriteRemote = {},
            onClearSearch = {},
        )
    }

    pendingDelete?.let { station ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.confirm_delete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStation(station)
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showCountrySheet) {
        CountryPickerSheet(
            countries = browse.countries,
            selectedCode = browse.selectedCountryCode,
            isLoading = browse.isLoadingCountries,
            onSelect = {
                viewModel.selectCountry(it)
                showCountrySheet = false
            },
            onDismiss = { showCountrySheet = false },
        )
    }
}

@Composable
private fun HomeTabs(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
) {
    val tabs = listOf(
        HomeTab.Saved to R.string.tab_saved,
        HomeTab.Favorites to R.string.tab_favorites,
        HomeTab.Browse to R.string.tab_browse,
    )
    val index = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    TabRow(
        selectedTabIndex = index,
        containerColor = Gray850,
        contentColor = Gray200,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                color = Gray200,
            )
        },
        divider = {},
    ) {
        tabs.forEachIndexed { i, (tab, labelRes) ->
            Tab(
                selected = i == index,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        text = stringResource(labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                selectedContentColor = Gray200,
                unselectedContentColor = Gray400,
            )
        }
    }
}

@Composable
private fun StationGrid(
    stations: List<Station>,
    playbackStation: Station?,
    isPlaying: Boolean,
    emptyText: String,
    onPlay: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    onEdit: ((Station) -> Unit)? = null,
    onDelete: ((Station) -> Unit)? = null,
    showMenu: Boolean = true,
) {
    if (stations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emptyText,
                color = Gray400,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 12.dp,
            bottom = if (playbackStation != null) 12.dp else 88.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(stations, key = { _, s -> s.id }) { index, station ->
            val isCurrent = playbackStation?.streamUrl == station.streamUrl
            StationTile(
                station = station,
                isPlaying = isPlaying,
                isCurrent = isCurrent,
                index = index,
                onPlay = { onPlay(station) },
                onToggleFavorite = { onToggleFavorite(station) },
                onEdit = onEdit?.let { { it(station) } },
                onDelete = onDelete?.let { { it(station) } },
                showMenu = showMenu,
            )
        }
    }
}

@Composable
private fun BrowsePanel(
    browse: BrowseUiState,
    genres: List<com.grayradio.app.data.remote.GenreOption>,
    savedStations: List<Station>,
    playbackStation: Station?,
    isPlaying: Boolean,
    onSelectCountryChip: () -> Unit,
    onSelectTag: (String?) -> Unit,
    onPlayRemote: (RemoteStation) -> Unit,
    onToggleFavoriteRemote: (RemoteStation) -> Unit,
) {
    val favoriteUrls = remember(savedStations) {
        savedStations.filter { it.isFavorite }.map { it.streamUrl }.toSet()
    }
    val selectedCountryName = browse.countries
        .firstOrNull { it.code == browse.selectedCountryCode }
        ?.name
        ?: browse.selectedCountryCode

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.country),
                style = MaterialTheme.typography.labelLarge,
                color = Gray400,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = true,
                    onClick = onSelectCountryChip,
                    label = {
                        Text(
                            text = "$selectedCountryName (${browse.selectedCountryCode})",
                            maxLines = 1,
                        )
                    },
                    colors = grayChipColors(),
                )
            }

            Text(
                text = stringResource(R.string.style),
                style = MaterialTheme.typography.labelLarge,
                color = Gray400,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = browse.selectedTag == null,
                    onClick = { onSelectTag(null) },
                    label = { Text(stringResource(R.string.all_styles)) },
                    colors = grayChipColors(),
                )
                genres.forEach { genre ->
                    FilterChip(
                        selected = browse.selectedTag == genre.tag,
                        onClick = { onSelectTag(genre.tag) },
                        label = { Text(genre.labelHu) },
                        colors = grayChipColors(),
                    )
                }
            }
        }

        when {
            browse.isLoadingStations || browse.isLoadingCountries -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Gray200,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            browse.stations.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.empty_browse),
                        color = Gray400,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            else -> {
                val displayStations = browse.stations.map { remote ->
                    remote.toEphemeralStation().copy(isFavorite = remote.streamUrl in favoriteUrls)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = if (playbackStation != null) 12.dp else 88.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(
                        items = browse.stations,
                        key = { _, s -> s.streamUrl },
                    ) { index, remote ->
                        val station = displayStations[index]
                        val isCurrent = playbackStation?.streamUrl == remote.streamUrl
                        StationTile(
                            station = station,
                            isPlaying = isPlaying,
                            isCurrent = isCurrent,
                            index = index,
                            onPlay = { onPlayRemote(remote) },
                            onToggleFavorite = { onToggleFavoriteRemote(remote) },
                            showMenu = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun grayChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Gray700,
    labelColor = Gray400,
    selectedContainerColor = Gray600,
    selectedLabelColor = Gray200,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPickerSheet(
    countries: List<RemoteCountry>,
    selectedCode: String,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Gray850,
        contentColor = Gray200,
    ) {
        Text(
            text = stringResource(R.string.select_country),
            style = MaterialTheme.typography.titleMedium,
            color = Gray200,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (isLoading && countries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Gray200, strokeWidth = 2.dp)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(countries.size) { i ->
                    val country = countries[i]
                    TextButton(
                        onClick = { onSelect(country.code) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${country.name} (${country.code})",
                                color = if (country.code == selectedCode) Gray200 else Gray400,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.stations_count, country.stationCount),
                                color = Gray400,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
