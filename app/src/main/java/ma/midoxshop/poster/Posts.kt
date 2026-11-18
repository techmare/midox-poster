package ma.midoxshop.poster

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A product as Midox shares it. [qty] is null when Midox doesn't count it. */
data class Item(
    val id: String,
    val name: String,
    val price: Double?,
    val cat: String,
    val cond: String,
    val qty: Int?,
    val available: Boolean,
    val pics: List<String>
)

/** Days as "yyyy-MM-dd", the same as Midox writes them. */
object Days {
    fun today(): String = LocalDate.now().toString()
    fun shift(day: String, n: Int): String = LocalDate.parse(day).plusDays(n.toLong()).toString()
    fun between(from: String, to: String): Int = ChronoUnit.DAYS.between(LocalDate.parse(from), LocalDate.parse(to)).toInt()
    fun weekday(day: String): String = LocalDate.parse(day).dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    fun dayOfMonth(day: String): Int = LocalDate.parse(day).dayOfMonth
    /** Monday of the day's week: streak freezes are counted per week. */
    fun weekOf(day: String): String = LocalDate.parse(day).with(DayOfWeek.MONDAY).toString()
    fun ago(day: String, today: String): String = when (val d = between(day, today)) {
        0 -> "today"; 1 -> "yesterday"; else -> "$d days ago"
    }
}

/**
 * What to post, and the post itself, in French. The picks are Midox's old Sell › Marketplace picks:
 * photos first, the longest since posted first (never posted first of all), one per category where
 * possible, the same all day.
 */
object Posts {
    /** A product posted less than this many days ago is not suggested again; after it, it is "repost due". */
    const val REST_DAYS = 14

    /** The most posts a day you can aim for: more, close together, looks like spam to Facebook. */
    const val MAX_A_DAY = 10

    /** Minutes between two posts, so they're spread out instead of arriving in a burst. */
    const val GAP_MINUTES = 10

    /** Milliseconds until the next post is OK, or 0. */
    fun waitLeft(lastPostAt: Long, now: Long): Long =
        if (lastPostAt <= 0L) 0L else (lastPostAt + GAP_MINUTES * 60_000L - now).coerceAtLeast(0L)

    /** The price typed on Marketplace: always 1 dh (0 shows as "Free"). Buyers ask for the real one. */
    const val LISTING_PRICE = "1"

    /** Can go on Marketplace: a name, a price, not sold out, and not marked sold here. */
    fun postable(items: List<Item>, sold: Set<String>): List<Item> =
        items.filter { it.name.isNotBlank() && it.price != null && it.available && it.id !in sold }

    /**
     * [n] picks for [day]. [posted] is each product's last day posted; [skipped] the ones you swapped
     * away that day. Products already posted on [day] stay in the list (with a tick).
     */
    fun picks(items: List<Item>, posted: Map<String, String>, sold: Set<String>, day: String, n: Int, skipped: Set<String> = emptySet()): List<Item> {
        val all = postable(items, sold)
        val today = all.filter { posted[it.id] == day }
        val rest = Days.shift(day, -REST_DAYS)
        val pool = all
            .filter { it.id !in skipped && (posted[it.id] == null || posted[it.id]!! < rest) }
            .sortedWith(
                compareBy<Item> { it.pics.isEmpty() }
                    .thenBy { posted[it.id] ?: "" }
                    .thenBy { (it.id + day).hashCode() }
            )
        val chosen = today.take(n).toMutableList()
        val cats = chosen.map { it.cat.lowercase() }.toMutableSet()
        for (group in listOf(pool.filter { it.pics.isNotEmpty() }, pool.filter { it.pics.isEmpty() })) {
            for (p in group) {
                if (chosen.size >= n) break
                if (p !in chosen && p.cat.lowercase() !in cats) { chosen += p; cats += p.cat.lowercase() }
            }
            for (p in group) {
                if (chosen.size >= n) break
                if (p !in chosen) chosen += p
            }
        }
        return chosen
    }

    /**
     * The next [days] days, starting with [from]: each day's picks as if you post every one of them,
     * so a product shows only once in the week.
     */
    fun plan(items: List<Item>, posted: Map<String, String>, sold: Set<String>, from: String, n: Int, days: Int, skips: Map<String, Set<String>>): List<Pair<String, List<Item>>> {
        val p = posted.toMutableMap()
        return (0 until days).map { i ->
            val d = Days.shift(from, i)
            val list = picks(items, p, sold, d, n, skips[d].orEmpty())
            list.forEach { if (p[it.id] == null || p[it.id]!! < d) p[it.id] = d }
            d to list
        }
    }

    private fun proName(p: Item): String = ProName.pro(p.name) ?: p.name

    fun title(p: Item): String = proName(p).let { n ->
        when (p.cond) { "Used" -> "$n (occasion)"; "New" -> "$n (neuf)"; else -> n }
    }

    /**
     * Ten endings in French, without a phone number or the shop's name (you post as yourself). Each post
     * gets one, so listings don't all end with the same words (Facebook shows copy-paste posts less).
     */
    val ENDINGS = listOf(
        "Livraison partout au Maroc 🇲🇦\nGarantie magasin et service après-vente.\nÉcrivez-moi ici pour plus d'infos 👋",
        "📦 Livraison dans tout le Maroc\n✅ Garanti, avec service après-vente\nUne question ? Envoyez-moi un message.",
        "Disponible tout de suite, livraison partout au Maroc.\nProduit garanti par notre magasin.\nN'hésitez pas à me contacter ici 😊",
        "🚚 On livre dans toutes les villes du Maroc\n🛠️ Garantie et SAV assurés\nMessage ici pour le prix et la dispo.",
        "Vendu par un magasin photo et vidéo.\nLivraison possible partout au Maroc.\nÉcrivez-moi pour réserver.",
        "Garantie magasin incluse.\nEnvoi rapide dans tout le Maroc 📦\nContactez-moi en message privé pour plus de détails.",
        "En stock, prêt à partir 📸\nLivraison partout au Maroc, garantie et SAV assurés.\nDemandez-moi le prix en message.",
        "Vous pouvez passer le voir au magasin ou vous le faire livrer partout au Maroc.\nProduit garanti.\nÉcrivez-moi, je réponds vite 👍",
        "✔️ Garantie magasin\n✔️ Service après-vente\n✔️ Livraison dans tout le Maroc\nPlus d'infos par message.",
        "Matériel photo avec garantie et SAV.\nLivraison dans tout le Maroc 🇲🇦\nUne question ? Écrivez-moi ici."
    )

    /** The ending for [p] on [day], moved on by [shift] each time you tap New wording. */
    fun ending(p: Item, day: String, shift: Int = 0): String =
        ENDINGS[Math.floorMod((p.id + "|" + day).hashCode() + shift, ENDINGS.size)]

    /** The product and its condition first, then your own words (or an ending), then your footer. No price. */
    fun description(p: Item, message: String, footer: String): String = buildString {
        append(proName(p))
        when (p.cond) { "New" -> append(", neuf"); "Used" -> append(", d'occasion en bon état") }
        append(".")
        if (message.isNotBlank()) append("\n").append(message.trim())
        if (footer.isNotBlank()) append("\n").append(footer.trim())
    }
}

/**
 * The streak: days in a row with at least your minimum. A missed day doesn't break it while the week
 * still has a freeze left ([freezesPerWeek]), and a paused day (riding, sick…) never breaks it.
 * Today only counts once it's done: an empty today doesn't break anything yet.
 */
object Streak {
    data class Result(val days: Int, val freezesLeft: Int)

    fun of(counts: Map<String, Int>, paused: Set<String>, today: String, min: Int, freezesPerWeek: Int): Result {
        val used = mutableMapOf<String, Int>()
        var days = 0
        var d = if ((counts[today] ?: 0) >= min) today else Days.shift(today, -1)
        val first = counts.filterValues { it > 0 }.keys.minOrNull() ?: return Result(0, freezesPerWeek)
        while (d >= first) {
            when {
                (counts[d] ?: 0) >= min -> days++
                d in paused -> {}
                (used[Days.weekOf(d)] ?: 0) < freezesPerWeek -> used[Days.weekOf(d)] = (used[Days.weekOf(d)] ?: 0) + 1
                else -> break
            }
            d = Days.shift(d, -1)
        }
        return Result(days, (freezesPerWeek - (used[Days.weekOf(today)] ?: 0)).coerceAtLeast(0))
    }
}

/**
 * When to nag, in minutes of the day. Level 0 is a quiet notification at your reminder time, 1 a
 * pop-up that buzzes 45 minutes later, 2 the full-screen alarm, again every [every] minutes until
 * [end]. With the loud alarm off, the pop-up comes back instead.
 */
object NagPlan {
    data class Slot(val minute: Int, val level: Int)

    fun slots(remindAt: Int, loud: Boolean, alarmAfter: Int, every: Int, end: Int): List<Slot> {
        val out = mutableListOf(Slot(remindAt, 0))
        if (remindAt + 45 < end) out += Slot(remindAt + 45, 1)
        var t = remindAt + (if (loud) alarmAfter else 90)
        while (t <= end) {
            out += Slot(t, if (loud) 2 else 1)
            t += if (loud) every else 45
        }
        return out.distinctBy { it.minute }.sortedBy { it.minute }
    }

    /**
     * The next nag today after [now], or null: the next is tomorrow's first. A snooze moves it to the
     * snooze's end exactly (as loud as the slot it pushed back), skipping the slots in between.
     */
    fun next(slots: List<Slot>, now: Int, snoozedUntil: Int?): Slot? {
        if (snoozedUntil != null && snoozedUntil > now) {
            if (snoozedUntil > slots.last().minute) return null
            val pushed = slots.lastOrNull { it.minute <= snoozedUntil } ?: slots.first()
            return Slot(snoozedUntil, pushed.level)
        }
        return slots.firstOrNull { it.minute > now }
    }

    /** Words for the nag, a different one each day and each time, so your brain can't learn to skip it. */
    val LINES = listOf(
        "One post. Two minutes. Then you're free.",
        "Everything is ready. You only press paste.",
        "Your streak is waiting for one post.",
        "Do the first one. The second is optional.",
        "Future you will be glad. Post one now.",
        "Tiny task: one post, then back to your evening.",
        "It's already written. Just put it up.",
        "Two minutes now beats thinking about it all night."
    )
    fun line(day: String, n: Int): String = LINES[Math.floorMod(day.hashCode() + n * 3, LINES.size)]
}
