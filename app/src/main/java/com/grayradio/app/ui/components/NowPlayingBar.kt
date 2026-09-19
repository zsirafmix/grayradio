package com.grayradio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grayradio.app.R
import com.grayradio.app.player.PlaybackUiState
import com.grayradio.app.ui.theme.Gray200
import com.grayradio.app.ui.theme.Gray400
import com.grayradio.app.ui.theme.Gray700
import com.grayradio.app.ui.theme.Gray850

@Composable
fun NowPlayingBar(
    playback: PlaybackUiState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val station = playback.station ?: return

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        color = Gray850,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Gray700.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Radio,
                contentDescription = null,
                tint = Gray200,
                modifier = Modifier.size(32.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.labelLarge,
                    color = Gray400,
                )
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Gray200,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (playback.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Gray200,
                    strokeWidth = 2.dp,
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (playback.isPlaying) R.string.pause else R.string.play,
                    ),
                    tint = Gray200,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}
