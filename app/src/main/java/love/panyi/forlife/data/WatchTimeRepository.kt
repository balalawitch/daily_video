package love.panyi.forlife.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 每天最多可看 20 分钟，想改额度只改这里 */
const val DAILY_LIMIT_MILLIS = 20 * 60 * 1000L

/** 存"哪一天"和"那天已看了多久"，抽成接口是为了单元测试能用内存假实现 */
interface WatchTimeStore {
    var date: String
    var usedMillis: Long
}

class SharedPrefsWatchTimeStore(context: Context) : WatchTimeStore {
    private val prefs = context.getSharedPreferences("watch_time", Context.MODE_PRIVATE)

    override var date: String
        get() = prefs.getString(KEY_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DATE, value).apply()

    override var usedMillis: Long
        get() = prefs.getLong(KEY_USED, 0L)
        set(value) = prefs.edit().putLong(KEY_USED, value).apply()

    private companion object {
        const val KEY_DATE = "date"
        const val KEY_USED = "used_millis"
    }
}

class WatchTimeRepository(
    private val store: WatchTimeStore,
    // minSdk 24 没有 java.time，用字符串表示"今天"；跨天时存的日期和今天不同，已看时长按 0 算
    private val today: () -> String = {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
) {
    private fun usedToday(): Long = if (store.date == today()) store.usedMillis else 0L

    fun remainingMillis(): Long = (DAILY_LIMIT_MILLIS - usedToday()).coerceAtLeast(0L)

    fun addWatchedMillis(millis: Long) {
        val used = usedToday() + millis
        store.date = today()
        store.usedMillis = used
    }

    /**
     * 给今天加额度：直接从已看里扣（可以扣成负数），跨天时已看归零，所以只对当天有效。
     * 先把已看封顶到额度，免得额度用完时多记的零头吃掉加的时间。
     */
    fun addExtraMillis(millis: Long) {
        val used = usedToday().coerceAtMost(DAILY_LIMIT_MILLIS) - millis
        store.date = today()
        store.usedMillis = used
    }
}
