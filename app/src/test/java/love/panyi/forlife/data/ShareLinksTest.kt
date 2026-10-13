package love.panyi.forlife.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareLinksTest {
    @Test
    fun extractsLinkFromTextWithTitle() {
        assertEquals(
            "https://a.com/v.mp4",
            extractVideoUrl("看这个 https://a.com/v.mp4 不错")
        )
    }

    @Test
    fun stopsAtChineseCharactersWithoutSpace() {
        assertEquals(
            "https://a.com/v.mp4",
            extractVideoUrl("标题https://a.com/v.mp4好看")
        )
    }

    @Test
    fun picksFirstLinkWhenSeveral() {
        assertEquals(
            "http://a.com/1.mp4",
            extractVideoUrl("http://a.com/1.mp4 https://b.com/2.mp4")
        )
    }

    @Test
    fun trimsTrailingPunctuation() {
        assertEquals("https://a.com/v.mp4", extractVideoUrl("(https://a.com/v.mp4),"))
    }

    @Test
    fun returnsNullWhenNoHttpLink() {
        assertNull(extractVideoUrl("只有文字"))
        assertNull(extractVideoUrl("ftp://a.com/v.mp4"))
    }
}
