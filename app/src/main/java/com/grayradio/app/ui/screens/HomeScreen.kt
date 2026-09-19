package com.grayradio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grayradio.app.R
import com.grayradio.app.data.entity.Station
import com.grayradio.app.ui.components.NowPlayingBar
import com.grayradio.app.ui.components.StationDialogMode
import com.grayradio.app.ui.components.StationEditorDialog
import com.grayradio.app.ui.components.StationTile
import com.grayradio.app.ui.theme.Gray200
import com.grayradio.app.ui.theme.Gray400
import com.grayradio.app.ui.theme.Gray850
import com.grayradio.app.ui.theme.Gray900
import com.grayradio.app.ui.viewmodel.MainViewModel
import com.grayradio.app.ui.viewmodel.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingStation by remember { mutableStateOf<Station?>(null) }
    var pendingDelete by remember { mutableStateOf<Station?>(null) }

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
        if (stations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.empty_stations),
                    color = Gray400,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = if (playback.station != null) 12.dp else 88.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                itemsIndexed(stations, key = { _, s -> s.id }) { index, station ->
                    val isCurrent = playback.station?.id == station.id
                    StationTile(
                        station = station,
                        isPlaying = playback.isPlaying,
                        isCurrent = isCurrent,
                        index = index,
                        onPlay = {
                            if (isCurrent && playback.isPlaying) {
                                viewModel.pause()
                            } else {
                                viewModel.play(station)
                            }
                        },
                        onEdit = { editingStation = station },
                        onDelete = { pendingDelete = station },
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
            onClearSearch = viewModel::clearSearch,
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
}
