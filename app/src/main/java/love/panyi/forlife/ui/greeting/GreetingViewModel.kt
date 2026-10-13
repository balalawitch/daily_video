package love.panyi.forlife.ui.greeting

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import love.panyi.forlife.data.WatchTimeRepository

data class GreetingUiState(val remainingMillis: Long = 0L)

class GreetingViewModel(
    private val watchTime: WatchTimeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GreetingUiState())
    val uiState: StateFlow<GreetingUiState> = _uiState.asStateFlow()

    init {
        refreshRemaining()
    }

    /** 回到主页时调用：看完视频回来、或隔天打开 App，剩余时间都会变 */
    fun refreshRemaining() {
        _uiState.value = GreetingUiState(remainingMillis = watchTime.remainingMillis())
    }
}
