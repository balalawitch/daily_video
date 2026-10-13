package love.panyi.forlife.ui.greeting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import love.panyi.forlife.data.SharedPrefsWatchTimeStore
import love.panyi.forlife.data.WatchTimeRepository
import love.panyi.forlife.ui.theme.DailyVideoTheme

/**
 * 【Route 层：负责拿数据，是有状态的】
 * - 连接 ViewModel，把数据转换后交给下面的 [Greeting] 显示。
 * - 这样拆开，[Greeting] 就是一个无状态组件，方便预览和测试。
 *
 * @param onOpenVideoList 点击按钮时的回调。界面层只负责告诉外面"按钮被点了"，
 *        跳转到哪一页由 MainActivity 决定。这就是"事件往上传"。
 * @param modifier 惯例：可组合函数都留一个 modifier 参数，让调用方能控制外层的尺寸和间距。
 * ViewModel 在函数里用 viewModel { } 创建：它需要传入参数（WatchTimeRepository），屏幕旋转时不会被销毁。
 */
@Composable // 带这个注解的函数用来描述界面。数据变了，Compose 会自动重新调用它（叫"重组"）
fun GreetingRoute(
    onOpenVideoList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel = viewModel {
        GreetingViewModel(WatchTimeRepository(SharedPrefsWatchTimeStore(appContext)))
    }
    // 每次回到主页（看完视频返回、App 从后台切回）都刷新剩余时间
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshRemaining() }

    // collectAsStateWithLifecycle：把 StateFlow 转成 Compose 能监听的 State，
    //   App 退到后台时会自动暂停收集，比较省电。
    // by 委托：之后可以直接写 uiState.remainingMillis，不用每次写 .value
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 状态往下传：把数据作为参数交给负责显示的组件
    Greeting(
        remainingMillis = uiState.remainingMillis,
        onOpenVideoList = onOpenVideoList,
        modifier = modifier
    )
}

/**
 * 【Screen 层：只负责显示，是无状态的】
 * 只看传进来的参数，不知道 ViewModel 的存在。传一组假数据就能预览。
 */
@Composable
fun Greeting(
    remainingMillis: Long,
    onOpenVideoList: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 布局三件套：Column 从上到下排、Row 从左到右排、Box 叠在一起（类似 FrameLayout）
    Box(
        // 参数里的 modifier 要用在"最外层"的组件上，调用方传进来的修饰才能作用到整个组件
        // ⚠️ Modifier 从左往右依次生效，顺序会影响结果：
        //   .padding().background() → 边距部分不上色
        //   .background().padding() → 边距部分也上色
        modifier = modifier
            .fillMaxSize()                                  // 占满父容器
            .padding(horizontal = 24.dp, vertical = 32.dp), // 左右留 24dp、上下留 32dp
        contentAlignment = Alignment.Center                 // 子元素放在正中间
    ) {
        Column(
            // 宽度最多 480dp：手机上不起作用，平板上内容不会被拉得很宽
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Column 里的子元素水平居中
        ) {
            // ---------- 顶部圆形图标 ----------
            // Surface：Material 风格的"底板"，可以设置形状、颜色、阴影
            Surface(
                shape = CircleShape, // 圆形；配合 size(88.dp) 就是一个 88×88 的圆
                // 颜色从主题里取，不要写死 Color.Blue：深色模式和 Android 12+ 动态取色都能自动适配
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(88.dp)
            ) {
                // 里面再套一个 Box，把 emoji 摆在圆心
                Box(contentAlignment = Alignment.Center) {
                    // typography：主题预设的字号等级，从大到小
                    //   display > headline > title > body > label（每级又分 Large/Medium/Small）
                    Text("🎬", style = MaterialTheme.typography.displaySmall)
                }
            }

            // Spacer：一块空白，专门用来隔开元素，作用类似 margin
            Spacer(Modifier.height(24.dp))

            // ---------- 标题 ----------
            Text(
                text = "Daily Video",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                // 配色规律：xxx 上面的内容用 onXxx 颜色，文字和背景的对比度才够
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))

            // ---------- 副标题 ----------
            Text(
                text = "每天一段视频，记录生活点滴",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, // 偏灰的颜色，表示"次要信息"
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            // ---------- 今日剩余观看时间 ----------
            RemainingTimeCard(remainingMillis)
            Spacer(Modifier.height(32.dp))

            // ---------- 主按钮 ----------
            Button(
                onClick = onOpenVideoList, // 直接用外面传进来的回调：事件往上传
                enabled = remainingMillis > 0, // 额度用完就置灰，点不了
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                // Slot（插槽）：按钮上显示什么由 { } 里的内容决定，也可以放 Row(图标 + 文字)
                Text(
                    text = if (remainingMillis > 0) "进入看视频 ▶️" else "今日额度已用完",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun RemainingTimeCard(remainingMillis: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⏱️ 今日剩余观看时间",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatRemaining(remainingMillis),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** 毫秒 → "MM:SS"，向上取整到秒（还剩 0.5 秒显示 00:01，不会提前显示 00:00） */
private fun formatRemaining(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}

/**
 * @Preview：在 Android Studio 右侧直接显示效果，不用跑模拟器。
 * - 外面要包一层 DailyVideoTheme，否则取不到项目的主题颜色和字体。
 * - 传的是假数据：这就是把 Greeting 做成无状态组件的好处。
 * - 小技巧：再加一个 @Preview(uiMode = Configuration.UI_MODE_NIGHT_YES) 可以同时预览深色模式。
 */
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DailyVideoTheme {
        Greeting(remainingMillis = 12 * 60_000L + 34_000L, onOpenVideoList = {})
    }
}
