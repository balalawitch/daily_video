package love.panyi.forlife.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import love.panyi.forlife.ui.BackTopBar

@Composable
fun VideoPlayerRoute(
    videoId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel = viewModel { VideoPlayerViewModel(appContext, videoId) }

    // 切到后台时暂停
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.player.pause()
    }

    VideoPlayerScreen(
        player = viewModel.player,
        title = viewModel.title,
        errorMessage = viewModel.errorMessage,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun VideoPlayerScreen(
    player: Player,
    title: String,
    errorMessage: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BackTopBar(title = "正在播放", onBack = onBack)
        AndroidView(
            factory = { context -> PlayerView(context).apply { this.player = player } },
            onRelease = { it.player = null },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
    }
}
