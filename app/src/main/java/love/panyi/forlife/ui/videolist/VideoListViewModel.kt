package love.panyi.forlife.ui.videolist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import love.panyi.forlife.data.Video
import love.panyi.forlife.data.VideoRepository
import love.panyi.forlife.data.fetchBilibiliTitle
import love.panyi.forlife.data.isBilibiliUrl

data class VideoListUiState(val videos: List<Video> = emptyList())

class VideoListViewModel(
    private val repository: VideoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoListUiState())
    val uiState: StateFlow<VideoListUiState> = _uiState.asStateFlow()

    private var titleJob: Job? = null

    init {
        refresh()
    }

    /** 回到列表页时调用：别的 App 分享了新视频后，列表要跟着更新 */
    fun refresh() {
        _uiState.value = VideoListUiState(videos = repository.getVideos())
        fetchMissingTitles()
    }

    // B 站链接只有短码，后台取真标题存起来，下次直接用；取不到就先显示短码，下次再试
    private fun fetchMissingTitles() {
        if (titleJob?.isActive == true) return
        val urls = repository.sharedUrlsWithoutTitle().filter(::isBilibiliUrl)
        if (urls.isEmpty()) return
        titleJob = viewModelScope.launch {
            for (url in urls) {
                val title = runCatching { withContext(Dispatchers.IO) { fetchBilibiliTitle(url) } }
                    .getOrNull() ?: continue
                repository.setTitle(url, title)
                _uiState.value = VideoListUiState(videos = repository.getVideos())
            }
        }
    }
}
