package ma.midoxshop.poster

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/** Everything Poster keeps, on the phone only. Small, so plain settings with JSON in a few of them. */
object Store {
    /** Bumped on every change, so the screens that read it update. */
    var changes by mutableIntStateOf(0)
        private set

    private fun p(ctx: Context): SharedPreferences = ctx.getSharedPreferences("poster", Context.MODE_PRIVATE)
    private fun bump() { changes++ }

    private fun map(ctx: Context, key: String): Map<String, String> = runCatching {
        val o = JSONObject(p(ctx).getString(key, "{}") ?: "{}")
        o.keys().asSequence().associateWith { o.getString(it) }
    }.getOrDefault(emptyMap())

    private fun putMap(ctx: Context, key: String, m: Map<String, String>) {
        val o = JSONObject(); m.forEach { (k, v) -> o.put(k, v) }
        p(ctx).edit().putString(key, o.toString()).apply(); bump()
    }

    // ---- What you posted ----

    /** Each product's last day posted. */
    fun posted(ctx: Context): Map<String, String> = map(ctx, "posted")

    /** How many posts each day (for the ring and the streak). */
    fun counts(ctx: Context): Map<String, Int> = map(ctx, "counts").mapValues { it.value.toIntOrNull() ?: 0 }

    fun postedToday(ctx: Context): Int = counts(ctx)[Days.today()] ?: 0

    fun markPosted(ctx: Context, pid: String, day: String = Days.today()) {
        p(ctx).edit().putLong("last_post_at", System.currentTimeMillis()).apply()
        val m = posted(ctx).toMutableMap()
        val again = m[pid] == day
        m[pid] = day
        // Forget posts older than four months: the list stays small.
        val cut = Days.shift(day, -120)
        putMap(ctx, "posted", m.filterValues { it >= cut })
        if (!again) {
            val c = counts(ctx).toMutableMap()
            c[day] = (c[day] ?: 0) + 1
            putMap(ctx, "counts", c.filterKeys { it >= Days.shift(day, -400) }.mapValues { it.value.toString() })
        }
    }

    /** When you last marked a post, for the 10 minutes between posts. */
    fun lastPostAt(ctx: Context): Long = p(ctx).getLong("last_post_at", 0L)
    fun waitLeft(ctx: Context): Long = Posts.waitLeft(lastPostAt(ctx), System.currentTimeMillis())

    /** Undo today's "I posted it". */
    fun unmarkPosted(ctx: Context, pid: String, day: String = Days.today()) {
        val m = posted(ctx).toMutableMap()
        if (m[pid] != day) return
        m.remove(pid); putMap(ctx, "posted", m)
        p(ctx).edit().putLong("last_post_at", 0L).apply()   // no 10-minute wait for a post that didn't happen
        val c = counts(ctx).toMutableMap()
        c[day] = ((c[day] ?: 1) - 1).coerceAtLeast(0)
        putMap(ctx, "counts", c.mapValues { it.value.toString() })
    }

    /** Sold on Marketplace: no longer suggested. */
    /** Products marked sold here, and the day. [Posts.stillSold] says which still hide a product. */
    fun sold(ctx: Context): Map<String, String> = map(ctx, "sold")
    fun setSold(ctx: Context, pid: String, on: Boolean) {
        val m = map(ctx, "sold").toMutableMap()
        if (on) m[pid] = Days.today() else m.remove(pid)
        putMap(ctx, "sold", m)
    }

    /** How many times buyers messaged about each product ("Got messages" in Posted). */
    fun interest(ctx: Context): Map<String, Int> = map(ctx, "interest").mapValues { it.value.toIntOrNull() ?: 0 }
    /** Counted once a day per product, so a double tap doesn't tilt the picks. */
    fun interestNotedToday(ctx: Context, pid: String): Boolean = map(ctx, "interest_day")[pid] == Days.today()
    fun addInterest(ctx: Context, pid: String) {
        if (interestNotedToday(ctx, pid)) return
        val m = interest(ctx).toMutableMap()
        m[pid] = (m[pid] ?: 0) + 1
        putMap(ctx, "interest", m.mapValues { it.value.toString() })
        putMap(ctx, "interest_day", map(ctx, "interest_day").filterValues { it == Days.today() } + (pid to Days.today()))
    }
    fun undoInterest(ctx: Context, pid: String) {
        if (!interestNotedToday(ctx, pid)) return
        val m = interest(ctx).toMutableMap()
        m[pid] = ((m[pid] ?: 1) - 1).coerceAtLeast(0)
        putMap(ctx, "interest", m.filterValues { it > 0 }.mapValues { it.value.toString() })
        putMap(ctx, "interest_day", map(ctx, "interest_day") - pid)
    }

    /** Facebook's category for each of Midox's categories, as you set it in post mode. */
    fun fbCategory(ctx: Context, midoxCat: String): String? = map(ctx, "fb_cat")[midoxCat.lowercase()]
    fun setFbCategory(ctx: Context, midoxCat: String, fb: String) {
        val m = map(ctx, "fb_cat").toMutableMap()
        if (fb.isBlank()) m.remove(midoxCat.lowercase()) else m[midoxCat.lowercase()] = fb.trim()
        putMap(ctx, "fb_cat", m)
    }

    /** Products you swapped away, per day. */
    fun skips(ctx: Context): Map<String, Set<String>> =
        map(ctx, "skips").mapValues { it.value.split(",").filter { s -> s.isNotBlank() }.toSet() }
    fun skip(ctx: Context, day: String, pid: String) {
        val m = skips(ctx).filterKeys { it >= Days.shift(Days.today(), -1) }.toMutableMap()
        m[day] = m[day].orEmpty() + pid
        putMap(ctx, "skips", m.mapValues { it.value.joinToString(",") })
    }

    /** How many times you tapped New wording for a product today. */
    fun wording(ctx: Context, pid: String, day: String = Days.today()): Int = map(ctx, "wording")["$pid|$day"]?.toIntOrNull() ?: 0
    fun nextWording(ctx: Context, pid: String, day: String = Days.today()) {
        val m = map(ctx, "wording").filterKeys { it.endsWith("|$day") }.toMutableMap()
        m["$pid|$day"] = (wording(ctx, pid, day) + 1).toString()
        putMap(ctx, "wording", m)
    }

    // ---- Pause, snooze ----

    fun paused(ctx: Context): Map<String, String> = map(ctx, "paused")
    fun pausedToday(ctx: Context): String? = paused(ctx)[Days.today()]
    fun pause(ctx: Context, reason: String?) {
        val m = paused(ctx).filterKeys { it >= Days.shift(Days.today(), -60) }.toMutableMap()
        if (reason == null) m.remove(Days.today()) else m[Days.today()] = reason
        putMap(ctx, "paused", m)
    }

    const val MAX_SNOOZES = 3
    fun snoozesLeft(ctx: Context): Int =
        if (p(ctx).getString("snooze_day", "") != Days.today()) MAX_SNOOZES else MAX_SNOOZES - p(ctx).getInt("snooze_n", 0)
    fun snoozedUntil(ctx: Context): Int? =
        if (p(ctx).getString("snooze_day", "") == Days.today()) p(ctx).getInt("snooze_until", -1).takeIf { it >= 0 } else null
    /** [counts] false: Poster pushed it back itself (you were on a call), not one of your snoozes. */
    fun snooze(ctx: Context, untilMinute: Int, counts: Boolean = true) {
        val n = MAX_SNOOZES - snoozesLeft(ctx) + (if (counts) 1 else 0)
        p(ctx).edit().putString("snooze_day", Days.today()).putInt("snooze_n", n).putInt("snooze_until", untilMinute).apply(); bump()
    }

    /** How many nags today, so each one gets other words. */
    fun nagNumber(ctx: Context): Int {
        val n = if (p(ctx).getString("nag_day", "") == Days.today()) p(ctx).getInt("nag_n", 0) + 1 else 1
        p(ctx).edit().putString("nag_day", Days.today()).putInt("nag_n", n).apply()
        return n
    }

    // ---- Settings ----

    fun min(ctx: Context) = p(ctx).getInt("min", 1).coerceIn(1, 5)
    fun goal(ctx: Context) = p(ctx).getInt("goal", 3).coerceIn(min(ctx), Posts.MAX_A_DAY)
    fun setMin(ctx: Context, n: Int) { p(ctx).edit().putInt("min", n.coerceIn(1, 5)).apply(); if (goal(ctx) < n) setGoal(ctx, n); bump() }
    fun setGoal(ctx: Context, n: Int) { p(ctx).edit().putInt("goal", n.coerceIn(min(ctx), Posts.MAX_A_DAY)).apply(); bump() }

    fun remindAt(ctx: Context) = p(ctx).getInt("remind_at", 19 * 60)
    fun setRemindAt(ctx: Context, m: Int) { p(ctx).edit().putInt("remind_at", m).apply(); bump() }
    /** A quiet morning nudge (minutes of the day), or null when off. */
    fun morningAt(ctx: Context): Int? = p(ctx).getInt("morning_at", -1).takeIf { it >= 0 }
    fun setMorningAt(ctx: Context, m: Int?) { p(ctx).edit().putInt("morning_at", m ?: -1).apply(); bump() }
    fun nagsOn(ctx: Context) = p(ctx).getBoolean("nags", true)
    fun setNagsOn(ctx: Context, on: Boolean) { p(ctx).edit().putBoolean("nags", on).apply(); bump() }
    fun loud(ctx: Context) = p(ctx).getBoolean("loud", true)
    fun setLoud(ctx: Context, on: Boolean) { p(ctx).edit().putBoolean("loud", on).apply(); bump() }
    /** The alarm starts quiet and gets louder over 30 seconds, and buzzes only after 10. */
    fun soft(ctx: Context) = p(ctx).getBoolean("soft", true)
    fun setSoft(ctx: Context, on: Boolean) { p(ctx).edit().putBoolean("soft", on).apply(); bump() }
    fun alarmAfter(ctx: Context) = p(ctx).getInt("alarm_after", 90)
    fun setAlarmAfter(ctx: Context, m: Int) { p(ctx).edit().putInt("alarm_after", m).apply(); bump() }
    fun every(ctx: Context) = 30
    fun endAt(ctx: Context) = p(ctx).getInt("end_at", 23 * 60)
    fun setEndAt(ctx: Context, m: Int) { p(ctx).edit().putInt("end_at", m).apply(); bump() }
    fun freezes(ctx: Context) = p(ctx).getInt("freezes", 2)
    fun setFreezes(ctx: Context, n: Int) { p(ctx).edit().putInt("freezes", n.coerceIn(0, 3)).apply(); bump() }

    /** Your own line at the end of every post (phone, address…). */
    fun footer(ctx: Context): String = p(ctx).getString("footer", "") ?: ""
    fun setFooter(ctx: Context, s: String) { p(ctx).edit().putString("footer", s.trim()).apply(); bump() }
    /** Your own words under the product line; empty = one of the ten endings. */
    fun message(ctx: Context): String = p(ctx).getString("message", "") ?: ""
    fun setMessage(ctx: Context, s: String) { p(ctx).edit().putString("message", s.trim()).apply(); bump() }

    fun messageFor(ctx: Context, item: Item, day: String = Days.today()): String =
        message(ctx).ifBlank { Posts.ending(item, day, wording(ctx, item.id, day)) }

    fun streak(ctx: Context): Streak.Result =
        Streak.of(counts(ctx), paused(ctx).keys, Days.today(), min(ctx), freezes(ctx))

    /** When the post you're on started, for "this post took 1 min 48 s". */
    fun startPost(ctx: Context) { p(ctx).edit().putLong("post_start", System.currentTimeMillis()).apply() }
    fun postSeconds(ctx: Context): Long? = p(ctx).getLong("post_start", 0L).takeIf { it > 0 }?.let { (System.currentTimeMillis() - it) / 1000 }
        ?.takeIf { it in 1..3600 }

    // ---- Taking over from Midox ----

    fun imported(ctx: Context) = p(ctx).getBoolean("imported", false)

    /** Midox's old Marketplace settings: what was posted when, how many a day, your words. Once. */
    fun importFromMidox(ctx: Context, rows: Map<String, String>) {
        val e = p(ctx).edit()
        runCatching {
            val o = JSONObject(rows["posted"] ?: "{}")
            val posted = posted(ctx).toMutableMap()
            val counts = counts(ctx).toMutableMap()
            o.keys().forEach { pid ->
                val d = o.getString(pid)
                if (posted[pid] == null || posted[pid]!! < d) posted[pid] = d
                counts[d] = (counts[d] ?: 0) + 1
            }
            val po = JSONObject(); posted.forEach { (k, v) -> po.put(k, v) }
            val co = JSONObject(); counts.forEach { (k, v) -> co.put(k, v.toString()) }
            e.putString("posted", po.toString()).putString("counts", co.toString())
        }
        rows["count"]?.toIntOrNull()?.let { e.putInt("goal", it.coerceIn(1, 5)) }
        rows["footer"]?.takeIf { it.isNotBlank() }?.let { e.putString("footer", it) }
        rows["message"]?.takeIf { it.isNotBlank() }?.let { e.putString("message", it) }
        e.putBoolean("imported", true).apply()
        bump()
    }
}
