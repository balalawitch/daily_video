package love.panyi.forlife.ui.videolist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import love.panyi.forlife.data.BUILT_IN_VIDEOS
import love.panyi.forlife.data.SharedPrefsSharedVideoStore
import love.panyi.forlife.data.Video
import love.panyi.forlife.data.VideoRepository
import love.panyi.forlife.ui.BackTopBar
import love.panyi.forlife.ui.theme.DailyVideoTheme

@Composable
fun VideoListRoute(
    onVideoClick: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel = viewModel { VideoListViewModel(VideoRepository(SharedPrefsSharedVideoStore(appContext))) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VideoListScreen(
        videos = uiState.videos,
        onVideoClick = onVideoClick,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun VideoListScreen(
    videos: List<Video>,
    onVideoClick: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BackTopBar(title = "视频列表", onBack = onBack)
        LazyColumn {
            items(videos, key = { it.id }) { video ->
                ListItem(
                    headlineContent = { Text(video.title) },
                    supportingContent = { Text(video.duration) },
                    modifier = Modifier.clickable { onVideoClick(video.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VideoListScreenPreview() {
    DailyVideoTheme {
        VideoListScreen(BUILT_IN_VIDEOS, onVideoClick = {}, onBack = {})
    }
}
