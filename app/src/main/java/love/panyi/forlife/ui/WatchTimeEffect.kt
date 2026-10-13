package love.panyi.forlife.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import love.panyi.forlife.data.WatchTimeRepository

/**
 * 【观看计时】active 为 true（人在列表页/播放页）且 App 在前台时，每秒把时间记入今日已看。
 * 不管视频是否在播放：从进入列表页就开始算。
 * 额度用完时调用 [onExhausted]（外面负责退回主页）。
 *
 * 用 SystemClock.elapsedRealtime()（开机后的单调时钟）算时间差，不受用户改系统时间影响。
 */
@Composable
fun WatchTimeEffect(active: Boolean, repository: WatchTimeRepository, onExhausted: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnExhausted by rememberUpdatedState(onExhausted)

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        // 切到后台会取消里面的协程，回到前台再重新开始，所以后台时间不计入
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var mark = SystemClock.elapsedRealtime()
            try {
                while (repository.remainingMillis() > 0) {
                    delay(1000)
                    val now = SystemClock.elapsedRealtime()
                    repository.addWatchedMillis(now - mark)
                    mark = now
                }
                currentOnExhausted()
            } finally {
                // 被取消（退回主页、切后台）时，把最后不足 1 秒的部分也记上
                repository.addWatchedMillis(SystemClock.elapsedRealtime() - mark)
            }
        }
    }
}
