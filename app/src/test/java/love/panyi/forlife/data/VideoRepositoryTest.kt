package love.panyi.forlife.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoRepositoryTest {
    private class FakeStore : SharedVideoStore {
        override var urls: List<String> = emptyList()
        private val titles = mutableMapOf<String, String>()
        override fun getTitle(url: String) = titles[url]
        override fun setTitle(url: String, title: String) {
            titles[url] = title
        }
    }

    private val store = FakeStore()
    private val repo = VideoRepository(store)
    private val builtInCount = BUILT_IN_VIDEOS.size

    @Test
    fun startsWithBuiltInVideosOnly() {
        assertEquals(BUILT_IN_VIDEOS, repo.getVideos())
    }

    @Test
    fun addedVideo_appendsAfterBuiltIn_withFileNameAsTitle() {
        assertTrue(repo.addSharedUrl("https://a.com/path/cat.mp4?token=1"))

        val added = repo.getVideos().last()
        assertEquals(builtInCount + 1, repo.getVideos().size)
        assertEquals(100, added.id)
        assertEquals("cat.mp4", added.title)
        assertEquals("未知", added.duration)
        assertEquals("https://a.com/path/cat.mp4?token=1", added.url)
    }

    @Test
    fun titleFallsBackToHost_whenNoPath() {
        repo.addSharedUrl("https://example.com/")
        assertEquals("example.com", repo.getVideos().last().title)
    }

    @Test
    fun idsFollowAddOrder() {
        repo.addSharedUrl("https://a.com/1.mp4")
        repo.addSharedUrl("https://a.com/2.mp4")
        assertEquals(listOf(100, 101), repo.getVideos().drop(builtInCount).map { it.id })
    }

    @Test
    fun duplicateUrl_isIgnored() {
        assertTrue(repo.addSharedUrl("https://a.com/1.mp4"))
        assertFalse(repo.addSharedUrl("https://a.com/1.mp4"))
        assertEquals(builtInCount + 1, repo.getVideos().size)
    }

    @Test
    fun addedVideo_survivesNewRepositoryWithSameStore() {
        repo.addSharedUrl("https://a.com/1.mp4")
        assertEquals(100, VideoRepository(store).getVideo(100)?.id)
    }

    @Test
    fun savedTitle_replacesTitleFromUrl() {
        repo.addSharedUrl("https://b23.tv/3ZPW1J2")
        repo.setTitle("https://b23.tv/3ZPW1J2", "跟道奇在家一天吧")
        assertEquals("跟道奇在家一天吧", repo.getVideos().last().title)
    }

    @Test
    fun sharedUrlsWithoutTitle_skipsUrlsThatHaveTitle() {
        repo.addSharedUrl("https://b23.tv/a")
        repo.addSharedUrl("https://b23.tv/b")
        repo.setTitle("https://b23.tv/a", "A")
        assertEquals(listOf("https://b23.tv/b"), repo.sharedUrlsWithoutTitle())
    }

    @Test
    fun getVideo_unknownId_returnsNull() {
        assertNull(repo.getVideo(999))
    }
}
