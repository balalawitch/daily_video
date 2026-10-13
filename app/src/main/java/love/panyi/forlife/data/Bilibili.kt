package love.panyi.forlife.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// B 站视频号：BV 开头再跟 10 位字母数字
private val bvidRegex = Regex("""BV[0-9A-Za-z]{10}""")

/** B 站分享出来的是网页链接（b23.tv 短链或 bilibili.com/video/BV...），播放器不能直接放 */
fun isBilibiliUrl(url: String): Boolean {
    val host = url.substringAfter("://").substringBefore('/').substringBefore('?').lowercase()
    return host == "b23.tv" || host == "bilibili.com" || host.endsWith(".bilibili.com")
}

fun extractBvid(url: String): String? = bvidRegex.find(url)?.value

/** 播放 B 站视频流要带的请求头，不带有时会被拒 */
const val BILIBILI_REFERER = "https://www.bilibili.com/"
const val BROWSER_USER_AGENT = "Mozilla/5.0"

data class BilibiliStream(val title: String, val url: String)

/**
 * 把 B 站网页链接换成可以直接播放的 mp4 地址（360P，不用登录）。
 * 会联网，要在后台线程调用；失败时抛异常。地址几个小时后会过期，所以每次播放都重新取。
 */
fun resolveBilibili(url: String): BilibiliStream {
    val view = videoInfo(url)
    val bvid = view.getString("bvid")
    val cid = view.getLong("cid")
    val play = getJson("https://api.bilibili.com/x/player/playurl?bvid=$bvid&cid=$cid&qn=16&platform=html5")
    return BilibiliStream(
        title = view.getString("title"),
        url = play.getJSONArray("durl").getJSONObject(0).getString("url")
    )
}

/** 只取标题，给视频列表用。会联网，要在后台线程调用；失败时抛异常 */
fun fetchBilibiliTitle(url: String): String = videoInfo(url).getString("title")

// 视频信息：bvid、cid、title 等
private fun videoInfo(url: String): JSONObject {
    val bvid = extractBvid(url) ?: extractBvid(followRedirect(url))
        ?: error("链接里没有 BV 号：$url")
    return getJson("https://api.bilibili.com/x/web-interface/view?bvid=$bvid")
}

// b23.tv 短链接会 302 跳到带 BV 号的网页，只要跳转地址，不下载网页
private fun followRedirect(url: String): String {
    val conn = open(url).apply { instanceFollowRedirects = false }
    try {
        return conn.getHeaderField("Location") ?: error("短链接没有跳转：$url")
    } finally {
        conn.disconnect()
    }
}

// B 站接口返回 {code, message, data}，code 不为 0 就是出错了，成功时返回 data
private fun getJson(url: String): JSONObject {
    val conn = open(url)
    try {
        val body = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        check(body.getInt("code") == 0) { "B 站接口出错：${body.optString("message")}" }
        return body.getJSONObject("data")
    } finally {
        conn.disconnect()
    }
}

private fun open(url: String): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 10_000
        setRequestProperty("User-Agent", BROWSER_USER_AGENT)
        setRequestProperty("Referer", BILIBILI_REFERER)
    }
