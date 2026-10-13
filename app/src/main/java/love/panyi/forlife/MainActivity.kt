package love.panyi.forlife

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import love.panyi.forlife.data.SharedPrefsSharedVideoStore
import love.panyi.forlife.data.SharedPrefsWatchTimeStore
import love.panyi.forlife.data.VideoRepository
import love.panyi.forlife.data.WatchTimeRepository
import love.panyi.forlife.data.extractVideoUrl
import love.panyi.forlife.ui.WatchTimeEffect
import love.panyi.forlife.ui.greeting.GreetingRoute
import love.panyi.forlife.ui.player.VideoPlayerRoute
import love.panyi.forlife.ui.theme.DailyVideoTheme
import love.panyi.forlife.ui.videolist.VideoListRoute

@Serializable
data object GreetingKey : NavKey

@Serializable
data object VideoListKey : NavKey

@Serializable
data class VideoPlayerKey(val videoId: Int) : NavKey

class MainActivity : ComponentActivity() {
    private val watchTime by lazy { WatchTimeRepository(SharedPrefsWatchTimeStore(applicationContext)) }
    private val videoRepository by lazy { VideoRepository(SharedPrefsSharedVideoStore(applicationContext)) }

    // 每收到一次可用的分享就 +1，界面据此跳到视频列表页
    private var showListRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // savedInstanceState 不为空说明是旋转屏幕等重建，intent 是旧的，不能重复处理
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            DailyVideoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val backStack = rememberNavBackStack(GreetingKey)
                    // 系统返回键和页面上的返回按钮共用：弹出栈顶
                    val goBack: () -> Unit = { backStack.removeLastOrNull() }

                    // 收到分享且还有额度：回到主页再进视频列表页，让主人看到新添加的视频
                    LaunchedEffect(showListRequest) {
                        if (showListRequest > 0) {
                            while (backStack.size > 1) backStack.removeLastOrNull()
                            backStack.add(VideoListKey)
                        }
                    }

                    // 只要不在主页（列表页、播放页），就在计今日观看时间；额度用完退回主页
                    WatchTimeEffect(
                        active = backStack.lastOrNull() != GreetingKey,
                        repository = watchTime,
                        onExhausted = { while (backStack.size > 1) backStack.removeLastOrNull() }
                    )

                    NavDisplay(
                        backStack = backStack,
                        onBack = goBack,
                        modifier = Modifier.padding(innerPadding),
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator()
                        ),
                        entryProvider = entryProvider {
                            entry<GreetingKey> {
                                GreetingRoute(onOpenVideoList = { backStack.add(VideoListKey) })
                            }
                            entry<VideoListKey> {
                                VideoListRoute(
                                    onVideoClick = { backStack.add(VideoPlayerKey(it)) },
                                    onBack = goBack
                                )
                            }
                            entry<VideoPlayerKey> { key ->
                                VideoPlayerRoute(
                                    videoId = key.videoId,
                                    onBack = goBack
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // singleTop：App 已经开着时再收到分享，走这里而不是新开一个页面
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) return
        val url = intent.getStringExtra(Intent.EXTRA_TEXT)?.let(::extractVideoUrl)
        if (url == null) {
            Toast.makeText(this, "没有找到视频链接", Toast.LENGTH_SHORT).show()
            return
        }
        videoRepository.addSharedUrl(url) // 重复的链接不会重复添加，但仍然带主人去列表页
        if (watchTime.remainingMillis() > 0) {
            showListRequest++
        } else {
            Toast.makeText(this, "已保存，今日额度已用完", Toast.LENGTH_SHORT).show()
        }
    }
}
