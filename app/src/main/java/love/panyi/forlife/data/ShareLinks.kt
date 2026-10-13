package love.panyi.forlife.data

// 只匹配 ASCII 可见字符：遇到空格、换行、中文就停，"标题https://a.com/v.mp4好看"也能取对
private val urlRegex = Regex("""https?://[\x21-\x7E]+""")

/** 从别的 App 分享来的文字里取第一个 http(s) 链接，没有就返回 null */
fun extractVideoUrl(text: String): String? =
    urlRegex.find(text)?.value?.trimEnd('.', ',', ';', ')', '!', '"', '\'')
