package love.panyi.forlife.ui.player

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    // rememberSaveable：全屏时转成横屏会重建 Activity，状态不能丢
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    FullscreenEffect(isFullscreen)
    // 全屏时按返回键先退出全屏，而不是直接离开播放页
    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    Column(modifier = modifier) {
        if (!isFullscreen) BackTopBar(title = "正在播放", onBack = onBack)
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    // 设了监听器，控制栏才会显示全屏按钮
                    setFullscreenButtonClickListener { isFullscreen = it }
                }
            },
            update = { it.setFullscreenButtonState(isFullscreen) },
            onRelease = { it.player = null },
            modifier = Modifier
                .background(Color.Black)
                .then(
                    if (isFullscreen) Modifier.fillMaxSize()
                    else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                )
        )
        if (isFullscreen) return@Column
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

/** 全屏时横屏并隐藏状态栏、导航栏；退出全屏或离开页面时恢复。 */
@Composable
private fun FullscreenEffect(isFullscreen: Boolean) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(isFullscreen) {
        if (!isFullscreen) return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            // 横竖屏切换导致的重建不算退出全屏，新 Activity 会接着保持全屏
            if (activity.isChangingConfigurations) return@onDispose
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
