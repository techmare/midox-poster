package ma.midoxshop.poster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private fun item(id: String, cat: String = "c$id", pics: Int = 1, price: Double? = 100.0, available: Boolean = true) =
        Item(id, "Item $id", price, cat, "New", null, available, List(pics) { "$id-$it.jpg" })

    @Test fun picksSkipPostedSoldAndUnpriced() {
        val items = listOf(item("a"), item("b"), item("c", price = null), item("d", available = false), item("e"))
        val picks = Posts.picks(items, mapOf("a" to "2026-11-10"), setOf("e"), "2026-11-18", 3)
        assertEquals(listOf("b"), picks.map { it.id })
    }

    @Test fun picksPreferPhotosAndKeepTodaysPosts() {
        val items = listOf(item("a", pics = 0), item("b"), item("c"))
        val day = "2026-11-18"
        val picks = Posts.picks(items, mapOf("c" to day), emptySet(), day, 2)
        assertEquals(listOf("c", "b"), picks.map { it.id })
    }

    @Test fun planShowsEachProductOnceAWeek() {
        val items = (1..10).map { item("$it") }
        val plan = Posts.plan(items, emptyMap(), emptySet(), "2026-11-18", 3, 3, emptyMap())
        val all = plan.flatMap { it.second }.map { it.id }
        assertEquals(9, all.size)
        assertEquals(all.size, all.toSet().size)
    }

    @Test fun descriptionHasNoPriceAndNoShop() {
        val d = Posts.description(item("a"), "Écrivez-moi", "")
        assertFalse(d.contains("100"))
        assertFalse(d.contains("Midox", ignoreCase = true))
        assertTrue(d.startsWith("Item a, neuf."))
    }

    @Test fun streakCountsDaysAndFreezes() {
        val today = "2026-11-18" // a Wednesday
        val counts = mapOf("2026-11-18" to 1, "2026-11-17" to 2, "2026-11-15" to 1, "2026-11-14" to 1)
        // The 16th (Monday) is missed: a freeze covers it.
        assertEquals(4, Streak.of(counts, emptySet(), today, 1, 2).days)
        // With no freezes it stops at the 16th.
        assertEquals(2, Streak.of(counts, emptySet(), today, 1, 0).days)
    }

    @Test fun emptyTodayDoesNotBreakTheStreakYet() {
        val counts = mapOf("2026-11-17" to 1, "2026-11-16" to 1)
        assertEquals(2, Streak.of(counts, emptySet(), "2026-11-18", 1, 0).days)
    }

    @Test fun pausedDayKeepsTheStreak() {
        val counts = mapOf("2026-11-17" to 1, "2026-11-15" to 1)
        assertEquals(2, Streak.of(counts, setOf("2026-11-16"), "2026-11-18", 1, 0).days)
    }

    @Test fun nagsGetLouder() {
        val s = NagPlan.slots(19 * 60, loud = true, alarmAfter = 90, every = 30, end = 23 * 60)
        assertEquals(NagPlan.Slot(19 * 60, 0), s[0])
        assertEquals(NagPlan.Slot(19 * 60 + 45, 1), s[1])
        assertEquals(NagPlan.Slot(20 * 60 + 30, 2), s[2])
        assertEquals(23 * 60, s.last().minute)
        assertTrue(s.drop(2).all { it.level == 2 })
    }

    @Test fun snoozePushesTheNextNag() {
        val s = NagPlan.slots(19 * 60, true, 90, 30, 23 * 60)
        assertEquals(NagPlan.Slot(20 * 60 + 30, 2), NagPlan.next(s, 20 * 60, null))
        // Snoozed at 20:30 for 15 minutes: the next one is 20:45, still an alarm.
        assertEquals(NagPlan.Slot(20 * 60 + 45, 2), NagPlan.next(s, 20 * 60 + 30, 20 * 60 + 45))
        assertNull(NagPlan.next(s, 23 * 60, null))
    }

    @Test fun postsAreSpacedTenMinutes() {
        assertEquals(0L, Posts.waitLeft(0L, 1_000_000L))
        assertEquals(10 * 60_000L, Posts.waitLeft(1_000_000L, 1_000_000L))
        assertEquals(60_000L, Posts.waitLeft(1_000_000L, 1_000_000L + 9 * 60_000L))
        assertEquals(0L, Posts.waitLeft(1_000_000L, 1_000_000L + 11 * 60_000L))
    }

    @Test fun tenPicksADay() {
        val items = (1..30).map { item("$it") }
        assertEquals(10, Posts.picks(items, emptyMap(), emptySet(), "2026-11-18", Posts.MAX_A_DAY).size)
    }

    @Test fun productsWithMessagesComeFirst() {
        val items = listOf(item("a"), item("b"), item("c"))
        val posted = mapOf("a" to "2026-10-01", "b" to "2026-10-01", "c" to "2026-10-01")
        val picks = Posts.picks(items, posted, emptySet(), "2026-11-18", 1, interest = mapOf("c" to 2))
        assertEquals(listOf("c"), picks.map { it.id })
    }

    @Test fun morningNudgeComesFirst() {
        val s = NagPlan.slots(19 * 60, true, 90, 30, 23 * 60, morning = 11 * 60)
        assertEquals(NagPlan.Slot(11 * 60, 0), s.first())
        assertEquals(NagPlan.Slot(19 * 60, 0), s[1])
    }

    @Test fun dropdownHintsInFrench() {
        assertEquals("Neuf", Posts.condition(item("a")))
        assertEquals("D'occasion – bon état", Posts.condition(item("a").copy(cond = "Used")))
    }
}
