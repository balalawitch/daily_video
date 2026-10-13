package love.panyi.forlife.data

import android.content.Context

data class Video(val id: Int, val title: String, val duration: String, val url: String)

val BUILT_IN_VIDEOS = listOf(
    Video(1, "Big Buck Bunny", "09:56", "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"),
    Video(2, "水母", "00:10", "https://test-videos.co.uk/vids/jellyfish/mp4/h264/360/Jellyfish_360_10s_1MB.mp4"),
    Video(3, "Sintel 片段", "00:10", "https://test-videos.co.uk/vids/sintel/mp4/h264/360/Sintel_360_10s_1MB.mp4"),
    Video(4, "花朵", "00:05", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"),
    Video(5, "Android 屏幕", "00:10", "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-10s.mp4")
)

/** 保存别的 App 分享来的视频链接，抽成接口是为了单元测试能用内存假实现 */
interface SharedVideoStore {
    var urls: List<String>
    fun getTitle(url: String): String?
    fun setTitle(url: String, title: String)
}

class SharedPrefsSharedVideoStore(context: Context) : SharedVideoStore {
    private val prefs = context.getSharedPreferences("shared_videos", Context.MODE_PRIVATE)

    // 链接里不会有换行，用换行把多个链接连成一个字符串保存，保持添加顺序
    override var urls: List<String>
        get() = prefs.getString(KEY_URLS, "").orEmpty().lines().filter { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_URLS, value.joinToString("\n")).apply()

    // 每个链接的标题单独存一条，键是 "title:" + 链接
    override fun getTitle(url: String): String? = prefs.getString(TITLE_PREFIX + url, null)

    override fun setTitle(url: String, title: String) =
        prefs.edit().putString(TITLE_PREFIX + url, title).apply()

    private companion object {
        const val KEY_URLS = "urls"
        const val TITLE_PREFIX = "title:"
    }
}

class VideoRepository(private val store: SharedVideoStore) {
    // 分享来的视频 id 从 100 起按添加顺序编，不会和内置的 1~5 冲突
    fun getVideos(): List<Video> =
        BUILT_IN_VIDEOS + store.urls.mapIndexed { index, url ->
            Video(SHARED_ID_START + index, store.getTitle(url) ?: titleOf(url), "未知", url)
        }

    fun getVideo(id: Int): Video? = getVideos().find { it.id == id }

    /** 返回 false 表示这个链接已经有了，没有重复添加 */
    fun addSharedUrl(url: String): Boolean {
        val urls = store.urls
        if (url in urls) return false
        store.urls = urls + url
        return true
    }

    /** 还没有保存标题的分享链接，列表里先拿链接的一段顶着 */
    fun sharedUrlsWithoutTitle(): List<String> = store.urls.filter { store.getTitle(it) == null }

    fun setTitle(url: String, title: String) = store.setTitle(url, title)

    // 取链接最后一段当标题（cat.mp4）；链接没有路径时用域名
    private fun titleOf(url: String): String {
        val withoutQuery = url.substringBefore('?').substringBefore('#').trimEnd('/')
        val afterScheme = withoutQuery.substringAfter("://")
        return if ('/' in afterScheme) afterScheme.substringAfterLast('/') else afterScheme
    }

    private companion object {
        const val SHARED_ID_START = 100
    }
}
