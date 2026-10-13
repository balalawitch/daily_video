package love.panyi.forlife.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BilibiliTest {
    @Test
    fun recognizesShortLinkAndVideoPage() {
        assertTrue(isBilibiliUrl("https://b23.tv/3ZPW1J2"))
        assertTrue(isBilibiliUrl("https://www.bilibili.com/video/BV1WXLy6zEAv?p=1"))
        assertTrue(isBilibiliUrl("https://m.bilibili.com/video/BV1WXLy6zEAv"))
    }

    @Test
    fun ignoresOtherSites() {
        assertFalse(isBilibiliUrl("https://a.com/v.mp4"))
        assertFalse(isBilibiliUrl("https://notbilibili.com/v.mp4"))
        assertFalse(isBilibiliUrl("https://a.com/?from=b23.tv"))
    }

    @Test
    fun extractsBvidFromVideoPage() {
        assertEquals(
            "BV1WXLy6zEAv",
            extractBvid("https://www.bilibili.com/video/BV1WXLy6zEAv?buvid=XU19&p=1")
        )
    }

    @Test
    fun shortLinkHasNoBvid() {
        assertNull(extractBvid("https://b23.tv/3ZPW1J2"))
    }
}
