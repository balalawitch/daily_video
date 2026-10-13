package love.panyi.forlife.ui.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import love.panyi.forlife.data.BILIBILI_REFERER
import love.panyi.forlife.data.BROWSER_USER_AGENT
import love.panyi.forlife.data.SharedPrefsSharedVideoStore
import love.panyi.forlife.data.Video
import love.panyi.forlife.data.VideoRepository
import love.panyi.forlife.data.isBilibiliUrl
import love.panyi.forlife.data.resolveBilibili

class VideoPlayerViewModel(
    appContext: Context,
    videoId: Int,
    private val repository: VideoRepository = VideoRepository(SharedPrefsSharedVideoStore(appContext))
) : ViewModel() {
    val video: Video? = repository.getVideo(videoId)

    // B 站视频取到真标题后会替换掉链接里的短码
    var title by mutableStateOf(video?.title ?: "视频不存在")
        private set

    // 不为空时界面显示报错，不让主人对着黑屏干等
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // 播放器放在 ViewModel 里，旋转屏幕时不会重新加载
    // B 站的视频流要带 Referer 才给放，其他网站带上也没影响
    val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent(BROWSER_USER_AGENT)
                    .setDefaultRequestProperties(mapOf("Referer" to BILIBILI_REFERER))
            )
        )
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    errorMessage = "这个链接无法播放"
                }
            })
        }

    init {
        video?.let { load(it.url) }
    }

    private fun load(url: String) {
        if (!isBilibiliUrl(url)) {
            play(url)
            return
        }
        viewModelScope.launch {
            val stream = runCatching { withContext(Dispatchers.IO) { resolveBilibili(url) } }
                .getOrElse {
                    errorMessage = "B 站视频解析失败，请检查网络后重试"
                    return@launch
                }
            title = stream.title
            repository.setTitle(url, stream.title) // B 站改了标题时，列表也跟着更新
            play(stream.url)
        }
    }

    private fun play(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    override fun onCleared() {
        player.release()
    }
}
