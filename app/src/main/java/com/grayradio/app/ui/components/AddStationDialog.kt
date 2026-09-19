package com.grayradio.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.grayradio.app.R
import com.grayradio.app.data.entity.Station
import com.grayradio.app.data.remote.RemoteStation
import com.grayradio.app.ui.theme.Gray200
import com.grayradio.app.ui.theme.Gray400
import com.grayradio.app.ui.viewmodel.SearchUiState

enum class StationDialogMode { Add, Edit }

@Composable
fun StationEditorDialog(
    mode: StationDialogMode,
    initial: Station? = null,
    searchState: SearchUiState,
    onDismiss: () -> Unit,
    onSaveManual: (name: String, url: String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchByName: () -> Unit,
    onSearchHungarian: () -> Unit,
    onAddRemote: (RemoteStation) -> Unit,
    onClearSearch: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var url by remember { mutableStateOf(initial?.streamUrl.orEmpty()) }
    var showSearch by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            onClearSearch()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        title = {
            Text(
                text = if (mode == StationDialogMode.Edit) {
                    stringResource(R.string.edit)
                } else {
                    stringResource(R.string.add_station)
                },
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.manual_add),
                    style = MaterialTheme.typography.labelLarge,
                    color = Gray400,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.station_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.stream_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (mode == StationDialogMode.Add) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.search_radio_browser),
                        style = MaterialTheme.typography.labelLarge,
                        color = Gray400,
                    )
                    OutlinedButton(
                        onClick = {
                            showSearch = true
                            onSearchHungarian()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.search_hu))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = searchState.query,
                            onValueChange = onSearchQueryChange,
                            label = { Text(stringResource(R.string.search_hint)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchByName() }),
                        )
                        IconButton(onClick = onSearchByName) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search),
                                tint = Gray200,
                            )
                        }
                    }

                    if (searchState.isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                color = Gray200,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    if (searchState.results.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                        ) {
                            items(searchState.results, key = { it.streamUrl }) { remote ->
                                RemoteStationRow(
                                    remote = remote,
                                    onAdd = { onAddRemote(remote) },
                                )
                            }
                        }
                    } else if (!searchState.isLoading && showSearch) {
                        Text(
                            text = stringResource(R.string.no_results),
                            color = Gray400,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveManual(name, url)
                    onClearSearch()
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onClearSearch()
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun RemoteStationRow(
    remote: RemoteStation,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = remote.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Gray200,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = buildString {
                remote.country?.let { append(it) }
                if (remote.bitrate > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("${remote.bitrate} kbps")
                }
            }
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onAdd) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add),
                tint = Gray200,
            )
        }
    }
}
