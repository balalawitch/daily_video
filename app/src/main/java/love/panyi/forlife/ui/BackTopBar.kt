package love.panyi.forlife.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** 带返回箭头的顶部栏。状态栏的间距已由 MainActivity 的 Scaffold 留好，这里不用再处理。 */
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            // 用文字箭头代替图标，省得为一个箭头引入 material-icons 依赖
            Text("←", style = MaterialTheme.typography.titleLarge)
        }
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}
