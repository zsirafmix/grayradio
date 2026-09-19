package com.grayradio.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.grayradio.app.R
import com.grayradio.app.data.entity.Station
import com.grayradio.app.ui.theme.Gray200
import com.grayradio.app.ui.theme.Gray400
import com.grayradio.app.ui.theme.TileShades

private val grayscaleMatrix = ColorMatrix(
    floatArrayOf(
        0.33f, 0.33f, 0.33f, 0f, 0f,
        0.33f, 0.33f, 0.33f, 0f, 0f,
        0.33f, 0.33f, 0.33f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationTile(
    station: Station,
    isPlaying: Boolean,
    isCurrent: Boolean,
    index: Int,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shade = TileShades[index % TileShades.size]
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(shade)
            .then(
                if (isCurrent) {
                    Modifier.border(2.dp, Gray200, shape)
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                onClick = onPlay,
                onLongClick = { menuOpen = true },
            ),
    ) {
        if (!station.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(station.logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(grayscaleMatrix),
                modifier = Modifier.fillMaxSize(),
                alpha = 0.22f,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    tint = Gray400,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterStart),
                )
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.edit),
                            tint = Gray200,
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                color = Gray200,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Icon(
                imageVector = if (isPlaying && isCurrent) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying && isCurrent) R.string.pause else R.string.play,
                ),
                tint = Gray200,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}
