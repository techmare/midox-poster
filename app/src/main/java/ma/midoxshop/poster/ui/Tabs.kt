package ma.midoxshop.poster.ui

import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.midoxshop.poster.AlarmActivity
import ma.midoxshop.poster.Days
import ma.midoxshop.poster.Item
import ma.midoxshop.poster.Midox
import ma.midoxshop.poster.Nag
import ma.midoxshop.poster.Posts
import ma.midoxshop.poster.Store
import ma.midoxshop.poster.Widget

/** The next 7 days, planned by themselves: for good days, when you feel like doing more. */
@Composable
fun QueueScreen(data: Data, onPost: (Item) -> Unit) {
    val ctx = LocalContext.current
    val plan = remember(data) {
        data.items?.let { Posts.plan(it, data.posted, data.sold, data.today, data.goal, 7, data.skips, data.interest) }.orEmpty()
    }
    var sel by rememberSaveable { mutableIntStateOf(0) }
    val haptics = rememberHaptics()
    Column(Modifier.fillMaxSize()) {
        Title("Queue", sub = "Feeling focused? Prep ahead.")
        if (plan.isNotEmpty()) Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            plan.forEachIndexed { i, (d, list) ->
                val on = i == sel
                val bg by animateColorAsState(if (on) C.chipOn else C.card, tween(Motion.QUICK), label = "dayBg")
                val fg by animateColorAsState(if (on) C.chipOnText else C.ink, tween(Motion.QUICK), label = "dayFg")
                val lift by animateFloatAsState(if (on) 1.06f else 1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "dayLift")
                Column(
                    Modifier.weight(1f).enter(i).graphicsLayer { scaleX = lift; scaleY = lift }.tap { if (sel != i) haptics.tick(); sel = i }
                        .clip(Corners.Small).background(bg).heightIn(min = 72.dp).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(if (i == 0) "Today" else Days.weekday(d), fontSize = 11.sp, color = if (on) fg.copy(alpha = 0.75f) else C.muted, maxLines = 1)
                    Text("${Days.dayOfMonth(d)}", fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = fg)
                    // A dot per post fits up to 5; more than that, "3/10".
                    if (list.size > 5) Text("${list.count { data.posted[it.id] == d }}/${list.size}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (on) fg else C.muted)
                    else Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        list.forEach { p ->
                            val c = when { data.posted[p.id] == d -> C.glow; p.pics.isEmpty() -> C.line; else -> C.good }
                            Box(Modifier.size(5.dp).clip(CircleShape).background(c))
                        }
                    }
                }
            }
        }
        // The chosen day's list slides in from the side of the day you tapped.
        AnimatedContent(
            targetState = sel, modifier = Modifier.weight(1f), label = "day",
            transitionSpec = {
                val right = targetState > initialState
                (slideInHorizontally(tween(Motion.SCREEN, easing = Motion.EaseDrawer)) { if (right) it / 3 else -it / 3 } + fadeIn(tween(Motion.SCREEN))) togetherWith
                    (slideOutHorizontally(tween(Motion.SCREEN, easing = Motion.EaseDrawer)) { if (right) -it / 3 else it / 3 } + fadeOut(tween(Motion.QUICK)))
            }
        ) { s ->
            val (day, list) = plan.getOrNull(s) ?: (data.today to emptyList())
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    val ready = list.count { it.pics.isNotEmpty() }
                    Section((if (s == 0) "Today" else Days.weekday(day) + " " + Days.dayOfMonth(day)) + if (list.isEmpty()) "" else " · $ready of ${list.size} ready")
                }
                if (data.items == null) item { SetupCard(data) }
                else if (list.isEmpty()) item {
                    Text(
                        "Nothing to post that day: every product with a price was posted in the last ${Posts.REST_DAYS} days.",
                        fontSize = 15.sp, color = C.muted, modifier = Modifier.padding(horizontal = 24.dp).enter()
                    )
                }
                itemsIndexed(list, key = { _, p -> day + p.id }) { i, p ->
                    val done = data.posted[p.id] == day
                    Box(Modifier.padding(horizontal = 24.dp, vertical = 5.dp).enter(i, key = day + p.id).animateItem()) {
                        ItemRow(
                            p,
                            when {
                                done -> "Posted"
                                p.pics.isEmpty() -> "No photo on the site yet"
                                data.posted[p.id] == null -> "New post"
                                else -> "Repost · " + Days.ago(data.posted[p.id]!!, data.today)
                            },
                            subColor = if (p.pics.isEmpty() && !done) C.accentInk else C.muted,
                            onClick = { onPost(p) }
                        ) {
                            if (done) Pill("Done", C.good, C.goodSoft)
                            else Text(
                                "Swap", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.ink,
                                modifier = Modifier.tap { haptics.tick(); Store.skip(ctx, day, p.id) }.clip(Corners.Small).background(C.ground)
                                    .padding(horizontal = 14.dp, vertical = 13.dp)
                            )
                        }
                    }
                }
                if (list.isNotEmpty()) item {
                    Text(
                        "Swap to put another product in. Tap one to post it now.",
                        fontSize = 13.sp, color = C.muted, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp).enter(list.size)
                    )
                }
            }
        }
    }
}

/** What's up on Marketplace: live, due for a repost (it's sinking), or sold. */
@Composable
fun PostedScreen(data: Data, onPost: (Item) -> Unit) {
    val ctx = LocalContext.current
    val haptics = rememberHaptics()
    val byId = data.items?.associateBy { it.id }.orEmpty()
    val rows = data.posted.entries.sortedByDescending { it.value }.mapNotNull { (id, d) -> byId[id]?.let { it to d } }
    val live = rows.filter { it.first.id !in data.sold && Days.between(it.second, data.today) < Posts.REST_DAYS }
    val due = rows.filter { it.first.id !in data.sold && Days.between(it.second, data.today) >= Posts.REST_DAYS && it.first.available }
    // Sold: every mark, newest first, with the day it was sold.
    val sold = data.soldMarks.entries.sortedByDescending { it.value }.mapNotNull { (id, d) -> byId[id]?.let { it to d } }
    // Opens on the list that needs you: what's due, else what's live, else sold.
    var seg by rememberSaveable { mutableIntStateOf(if (due.isNotEmpty()) 1 else if (live.isNotEmpty()) 0 else if (sold.isNotEmpty()) 2 else 1) }

    Column(Modifier.fillMaxSize()) {
        Title("Posted")
        Box(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp).enter(1)) {
            Segments(listOf("Live · ${live.size}", "Repost due · ${due.size}", "Sold · ${sold.size}"), seg) { seg = it }
        }
        AnimatedContent(
            targetState = seg, modifier = Modifier.weight(1f), label = "seg",
            transitionSpec = {
                val right = targetState > initialState
                (slideInHorizontally(tween(Motion.SCREEN, easing = Motion.EaseDrawer)) { if (right) it / 3 else -it / 3 } + fadeIn(tween(Motion.SCREEN))) togetherWith
                    (slideOutHorizontally(tween(Motion.SCREEN, easing = Motion.EaseDrawer)) { if (right) -it / 3 else it / 3 } + fadeOut(tween(Motion.QUICK)))
            }
        ) { s ->
            val shown = listOf(live, due, sold)[s]
            LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
                if (s == 2 && sold.isNotEmpty()) item {
                    Text(
                        "Your commission is still logged in Midox › Log › My online.",
                        fontSize = 13.sp, color = C.muted, modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp).enter()
                    )
                }
                if (shown.isEmpty()) item {
                    Text(
                        when (s) {
                            0 -> "Nothing posted in the last ${Posts.REST_DAYS} days yet."
                            1 -> "Nothing to repost. Posts come back here after ${Posts.REST_DAYS} days, when they sink."
                            else -> "Nothing marked sold."
                        },
                        fontSize = 15.sp, color = C.muted, modifier = Modifier.padding(24.dp).enter()
                    )
                }
                itemsIndexed(shown, key = { _, r -> "$s" + r.first.id }) { i, (p, d) ->
                    Column(
                        Modifier.padding(horizontal = 24.dp, vertical = 5.dp).enter(i, key = "$s" + p.id).animateItem().fillMaxWidth()
                            .clip(Corners.Card).background(C.card).padding(bottom = 14.dp)
                    ) {
                        val asked = data.interest[p.id] ?: 0
                        ItemRow(
                            p,
                            when (s) {
                                2 -> "Sold " + Days.ago(d, data.today) + " · " + when {
                                    p.cond == "Used" -> "one of a kind, gone"
                                    p.id in data.sold -> "back in picks in ${Posts.REST_DAYS - Days.between(d, data.today)} days"
                                    else -> "back in the picks"
                                }
                                else -> "Posted " + Days.ago(d, data.today) + (if (s == 1) " · getting buried" else "") +
                                    if (asked > 0) " · $asked message" + (if (asked > 1) "s" else "") else ""
                            },
                            subColor = if (s == 1) C.accentInk else C.muted
                        )
                        Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (s) {
                                2 -> {
                                    SmallAction("Not sold", Modifier.weight(1f)) { Store.setSold(ctx, p.id, false) }
                                    SmallAction("Log it in Midox", Modifier.weight(1f), main = true) { Midox.open(ctx) }
                                }
                                1 -> {
                                    SmallAction("Repost now", Modifier.weight(1f), main = true) { onPost(p) }
                                    SmallAction("Mark sold", Modifier.weight(1f)) { Store.setSold(ctx, p.id, true) }
                                }
                                // A buyer wrote about it: one tap, and it comes back first when it's due.
                                else -> {
                                    // Counted once a day; tap again to undo a mistake.
                                    val noted = Store.interestNotedToday(ctx, p.id)
                                    SmallAction(if (noted) "Noted ✓ · undo" else "Got messages", Modifier.weight(1f), good = noted) {
                                        haptics.tick()
                                        if (noted) Store.undoInterest(ctx, p.id) else Store.addInterest(ctx, p.id)
                                    }
                                    SmallAction("Mark sold", Modifier.weight(1f)) { Store.setSold(ctx, p.id, true) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallAction(text: String, modifier: Modifier, main: Boolean = false, good: Boolean = false, onClick: () -> Unit) {
    val bg by animateColorAsState(if (main) C.accent else if (good) C.goodSoft else C.ground, tween(Motion.QUICK), label = "act")
    Box(
        modifier.heightIn(min = 46.dp).tap(onClick = onClick).clip(Corners.Small).background(bg),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(text, label = "actText", transitionSpec = { fadeIn(tween(Motion.QUICK)) togetherWith fadeOut(tween(Motion.QUICK / 2)) }) {
            FitText(it, if (main) C.onAccent else if (good) C.good else C.ink, 14f, family = Body)
        }
    }
}

@Composable
fun SettingsScreen(data: Data) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val changes = Store.changes
    var footer by remember { mutableStateOf(Store.footer(ctx)) }
    var message by remember { mutableStateOf(Store.message(ctx)) }
    // Saved 0.6 s after you stop typing (and when you leave), not on every letter.
    LaunchedEffect(message) { kotlinx.coroutines.delay(600); if (message.trim() != Store.message(ctx)) Store.setMessage(ctx, message) }
    LaunchedEffect(footer) { kotlinx.coroutines.delay(600); if (footer.trim() != Store.footer(ctx)) Store.setFooter(ctx, footer) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            if (message.trim() != Store.message(ctx)) Store.setMessage(ctx, message)
            if (footer.trim() != Store.footer(ctx)) Store.setFooter(ctx, footer)
        }
    }
    val reschedule = { Nag.schedule(ctx); Widget.update(ctx) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Title("Settings")
        SetupCard(data)

        Label("Daily goal")
        SettingsCard(1) {
            SettingRow("Minimum", "keeps the streak", divider = true) { Stepper(Store.min(ctx)) { Store.setMin(ctx, it); reschedule() } }
            SettingRow("Good day", "the full ring · up to ${Posts.MAX_A_DAY}, 10 min apart", divider = true) { Stepper(Store.goal(ctx)) { Store.setGoal(ctx, it); reschedule() } }
            SettingRow("Streak freezes", "missed days a week that don't break it") { Stepper(Store.freezes(ctx)) { Store.setFreezes(ctx, it) } }
        }

        Label("Nudges")
        SettingsCard(2) {
            SettingRow("Remind me", null, divider = true) { Toggle(Store.nagsOn(ctx)) { Store.setNagsOn(ctx, it); reschedule() } }
            SettingRow("First reminder", "a quiet notification", divider = true) {
                Text(
                    hm(Store.remindAt(ctx)), fontFamily = Display, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C.ink,
                    modifier = Modifier.tap {
                        val m = Store.remindAt(ctx)
                        TimePickerDialog(ctx, { _, h, min -> Store.setRemindAt(ctx, h * 60 + min); reschedule() }, m / 60, m % 60, true).show()
                    }.clip(Corners.Small).background(C.ground).padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            SettingRow("Full-screen alarm", "covers the screen until you post, snooze or pause", divider = true) {
                Toggle(Store.loud(ctx)) { Store.setLoud(ctx, it); reschedule() }
            }
            // The alarm's own settings open and close under it.
            AnimatedVisibility(Store.loud(ctx), enter = expandVertically(tween(Motion.SCREEN, easing = Motion.EaseOut)) + fadeIn(), exit = shrinkVertically(tween(Motion.QUICK)) + fadeOut()) {
                Column {
                    SettingRow("Start soft", "quiet at first, louder over 30 seconds", divider = true) { Toggle(Store.soft(ctx)) { Store.setSoft(ctx, it) } }
                    ChipRow("Alarm starts after the reminder", listOf(30, 60, 90, 120).map { (if (it < 60) "$it min" else if (it % 60 == 0) "${it / 60} h" else "${it / 60} h ${it % 60}") to it }, Store.alarmAfter(ctx)) { Store.setAlarmAfter(ctx, it); reschedule() }
                }
            }
            SettingRow("Morning nudge", "a quiet one at a moment you're up anyway, like arriving at the shop", divider = Store.morningAt(ctx) != null) {
                Toggle(Store.morningAt(ctx) != null) { Store.setMorningAt(ctx, if (it) 11 * 60 else null); reschedule() }
            }
            AnimatedVisibility(Store.morningAt(ctx) != null, enter = expandVertically(tween(Motion.SCREEN, easing = Motion.EaseOut)) + fadeIn(), exit = shrinkVertically(tween(Motion.QUICK)) + fadeOut()) {
                SettingRow("Morning nudge at", null, divider = true) {
                    val m = Store.morningAt(ctx) ?: (11 * 60)
                    Text(
                        hm(m), fontFamily = Display, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C.ink,
                        modifier = Modifier.tap {
                            TimePickerDialog(ctx, { _, h, min -> Store.setMorningAt(ctx, (h * 60 + min).coerceAtMost(Store.remindAt(ctx) - 1)); reschedule() }, m / 60, m % 60, true).show()
                        }.clip(Corners.Small).background(C.ground).padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
            ChipRow("Stop nagging at", listOf(22 * 60, 23 * 60, 23 * 60 + 59).map { hm(it) to it }, Store.endAt(ctx)) { Store.setEndAt(ctx, it); reschedule() }
        }
        Box(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp).enter(3)) {
            SoftButton("Try the alarm now") {
                ctx.startActivity(Intent(ctx, AlarmActivity::class.java).putExtra("test", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        Label("Post text (French)")
        SettingsCard(4) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    message, { message = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your own words (empty: 10 endings that rotate)") }, minLines = 2
                )
                OutlinedTextField(
                    footer, { footer = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Last line of every post (optional)") }
                )
                Text("Price on the listing: always ${Posts.LISTING_PRICE} DH, never in the text. No shop name.", fontSize = 13.sp, color = C.muted)
            }
        }
    }
}

@Composable
private fun SettingsCard(index: Int, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp).enter(index).fillMaxWidth().clip(Corners.Card).background(C.card).padding(horizontal = 16.dp, vertical = 4.dp)) {
        content()
    }
}

@Composable
private fun SettingRow(title: String, sub: String?, divider: Boolean = false, right: @Composable () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.ink)
                if (sub != null) Text(sub, fontSize = 12.sp, color = C.muted)
            }
            right()
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(C.divider))
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(title: String, options: List<Pair<String, Int>>, value: Int, onPick: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.ink)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (l, v) -> Chip(l, v == value) { onPick(v) } }
        }
    }
}
