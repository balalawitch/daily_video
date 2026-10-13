package love.panyi.forlife.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchTimeRepositoryTest {
    private class FakeStore : WatchTimeStore {
        override var date: String = ""
        override var usedMillis: Long = 0
    }

    private val store = FakeStore()
    private var today = "2026-10-08"
    private val repo = WatchTimeRepository(store, today = { today })

    @Test
    fun freshInstall_hasFullQuota() {
        assertEquals(DAILY_LIMIT_MILLIS, repo.remainingMillis())
    }

    @Test
    fun watching_reducesRemaining() {
        repo.addWatchedMillis(5 * 60_000L)
        assertEquals(DAILY_LIMIT_MILLIS - 5 * 60_000L, repo.remainingMillis())
    }

    @Test
    fun watching_accumulatesAcrossCalls() {
        repo.addWatchedMillis(60_000L)
        repo.addWatchedMillis(30_000L)
        assertEquals(DAILY_LIMIT_MILLIS - 90_000L, repo.remainingMillis())
    }

    @Test
    fun remaining_neverGoesNegative() {
        repo.addWatchedMillis(DAILY_LIMIT_MILLIS + 10_000L)
        assertEquals(0L, repo.remainingMillis())
    }

    @Test
    fun newDay_resetsQuota() {
        repo.addWatchedMillis(10 * 60_000L)
        today = "2026-10-09"
        assertEquals(DAILY_LIMIT_MILLIS, repo.remainingMillis())
    }

    @Test
    fun newDay_addStartsFromZero() {
        repo.addWatchedMillis(10 * 60_000L)
        today = "2026-10-09"
        repo.addWatchedMillis(60_000L)
        assertEquals(DAILY_LIMIT_MILLIS - 60_000L, repo.remainingMillis())
    }
}
