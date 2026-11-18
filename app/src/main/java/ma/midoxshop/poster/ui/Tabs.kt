package ma.midoxshop.poster.ui

import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        data.items?.let { Posts.plan(it, data.posted, data.sold, data.today, data.goal, 7, data.skips) }.orEmpty()
    }
    var sel by rememberSaveable { mutableIntStateOf(0) }
    val haptics = rememberHaptics()
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
        item { Title("Queue") }
        item {
            Text(
                "The next 7 days plan themselves. Feeling focused? Post ahead from any day, and the rest of the week fills in again.",
                fontSize = 13.sp, color = C.muted, modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 10.dp)
            )
        }
        if (plan.isNotEmpty()) item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                plan.forEachIndexed { i, (d, list) ->
                    val on = i == sel
                    Column(
                        Modifier.weight(1f).clip(Corners.Card)
                            .background(if (on) C.chipOn else C.card)
                            .then(if (on) Modifier else Modifier.border(1.dp, C.line, Corners.Card))
                            .clickableRole { if (sel != i) haptics.tick(); sel = i }
                            .heightIn(min = 76.dp).padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(if (i == 0) "Today" else Days.weekday(d), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (on) C.chipOnText else C.muted, maxLines = 1)
                        Text("${Days.dayOfMonth(d)}", fontFamily = Serif, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (on) C.chipOnText else C.ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            list.forEach { p ->
                                val c = when { data.posted[p.id] == d -> C.accent; p.pics.isEmpty() -> C.warn; else -> C.good }
                                Box(Modifier.size(5.dp).clip(CircleShape).background(c))
                            }
                        }
                    }
                }
            }
        }
        val (day, list) = plan.getOrNull(sel) ?: (data.today to emptyList())
        item {
            val ready = list.count { it.pics.isNotEmpty() }
            Section(if (sel == 0) "Today" else Days.weekday(day) + " " + Days.dayOfMonth(day), trailing = "$ready of ${list.size} ready")
        }
        if (data.items == null) item { SetupCard(data) }
        else if (list.isEmpty()) item {
            Text(
                "Nothing to post that day: every product with a price was posted in the last ${Posts.REST_DAYS} days.",
                fontSize = 15.sp, color = C.muted, modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
        if (list.isNotEmpty()) item {
            Group(Modifier.padding(horizontal = 16.dp), list.map { p ->
                @Composable {
                    val done = data.posted[p.id] == day
                    ItemRow(
                        p,
                        when {
                            done -> "Posted"
                            p.pics.isEmpty() -> "No photo in Midox yet"
                            data.posted[p.id] == null -> "New post"
                            else -> "Repost · posted " + Days.ago(data.posted[p.id]!!, data.today)
                        },
                        subColor = if (p.pics.isEmpty() && !done) C.warn else C.muted,
                        onClick = { onPost(p) }
                    ) {
                        if (done) Tag("Done", C.good, C.goodSoft)
                        else Text(
                            "Swap", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = C.ink,
                            modifier = Modifier.clip(Corners.Round).border(1.dp, C.line, Corners.Round)
                                .clickableRole { Store.skip(ctx, day, p.id) }.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
            })
        }
    }
}

/** What's up on Marketplace: live, due for a repost (it's sinking), or sold. */
@Composable
fun PostedScreen(data: Data, onPost: (Item) -> Unit) {
    val ctx = LocalContext.current
    var seg by rememberSaveable { mutableIntStateOf(1) }
    val byId = data.items?.associateBy { it.id }.orEmpty()
    val rows = data.posted.entries.sortedByDescending { it.value }.mapNotNull { (id, d) -> byId[id]?.let { it to d } }
    val live = rows.filter { it.first.id !in data.sold && Days.between(it.second, data.today) < Posts.REST_DAYS }
    val due = rows.filter { it.first.id !in data.sold && Days.between(it.second, data.today) >= Posts.REST_DAYS && it.first.available }
    val sold = data.sold.mapNotNull { byId[it] }.map { it to (data.posted[it.id] ?: "") }
    val shown = listOf(live, due, sold)[seg]

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
        item { Title("Posted") }
        item {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
                Segments(listOf("Live · ${live.size}", "Repost · ${due.size}", "Sold · ${sold.size}"), seg) { seg = it }
            }
        }
        if (seg == 2 && sold.isNotEmpty()) item {
            Text(
                "Your commission is still logged in Midox › Log › My online.",
                fontSize = 13.sp, color = C.muted, modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp)
            )
        }
        if (shown.isEmpty()) item {
            Text(
                when (seg) {
                    0 -> "Nothing posted in the last ${Posts.REST_DAYS} days yet."
                    1 -> "Nothing to repost. Posts come back here after ${Posts.REST_DAYS} days, when they sink."
                    else -> "Nothing marked sold."
                },
                fontSize = 15.sp, color = C.muted, modifier = Modifier.padding(18.dp)
            )
        } else item { Spacer(Modifier.height(12.dp)) }
        shown.forEach { (p, d) ->
            item(key = "$seg" + p.id) {
                CardBox(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(bottom = 12.dp)) {
                        ItemRow(
                            p,
                            if (d.isBlank()) "Sold" else "Posted " + Days.ago(d, data.today) + if (seg == 1) " · sinking" else "",
                            subColor = if (seg == 1) C.warn else C.muted
                        )
                        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (seg) {
                                2 -> {
                                    SoftButton("Not sold", Modifier.weight(1f)) { Store.setSold(ctx, p.id, false) }
                                    InkButton("Log it in Midox", Modifier.weight(1f)) { Midox.open(ctx) }
                                }
                                1 -> {
                                    SoftButton("Mark sold", Modifier.weight(1f)) { Store.setSold(ctx, p.id, true) }
                                    InkButton("Repost now", Modifier.weight(1f)) { onPost(p) }
                                }
                                else -> SoftButton("Mark sold", Modifier.weight(1f)) { Store.setSold(ctx, p.id, true) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(data: Data) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val changes = Store.changes
    var footer by remember { mutableStateOf(Store.footer(ctx)) }
    var message by remember { mutableStateOf(Store.message(ctx)) }
    val reschedule = { Nag.schedule(ctx); Widget.update(ctx) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Title("Settings")
        SetupCard(data)

        Section("Daily goal")
        Group(Modifier.padding(horizontal = 16.dp), listOf(
            { SettingRow("Minimum", "keeps the streak") { Stepper(Store.min(ctx)) { Store.setMin(ctx, it); reschedule() } } },
            { SettingRow("Good day", "the full ring") { Stepper(Store.goal(ctx)) { Store.setGoal(ctx, it); reschedule() } } },
            { SettingRow("Streak freezes", "missed days a week that don't break it") { Stepper(Store.freezes(ctx)) { Store.setFreezes(ctx, it) } } }
        ))

        Section("Nags")
        Group(Modifier.padding(horizontal = 16.dp), buildList<@Composable () -> Unit> {
            add { SettingRow("Remind me", null) { Toggle(Store.nagsOn(ctx)) { Store.setNagsOn(ctx, it); reschedule() } } }
            add {
                SettingRow("First reminder", "a quiet notification") {
                    Text(
                        hm(Store.remindAt(ctx)), fontFamily = Serif, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.ink,
                        modifier = Modifier.clip(Corners.Round).background(C.tonal).clickableRole {
                            val m = Store.remindAt(ctx)
                            TimePickerDialog(ctx, { _, h, min -> Store.setRemindAt(ctx, h * 60 + min); reschedule() }, m / 60, m % 60, true).show()
                        }.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            add { SettingRow("Full-screen alarm", "covers the screen until you post, snooze or pause") { Toggle(Store.loud(ctx)) { Store.setLoud(ctx, it); reschedule() } } }
            if (Store.loud(ctx)) {
                add { SettingRow("Start soft", "quiet at first, louder over 30 seconds, buzz after 10") { Toggle(Store.soft(ctx)) { Store.setSoft(ctx, it) } } }
                add { ChipRow("Alarm starts", listOf(30, 60, 90, 120).map { "+$it min" to it }, Store.alarmAfter(ctx)) { Store.setAlarmAfter(ctx, it); reschedule() } }
            }
            add { ChipRow("Stop nagging at", listOf(22 * 60, 23 * 60, 23 * 60 + 59).map { hm(it) to it }, Store.endAt(ctx)) { Store.setEndAt(ctx, it); reschedule() } }
        })
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
            SoftButton("Try the alarm now") {
                ctx.startActivity(Intent(ctx, AlarmActivity::class.java).putExtra("test", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        Section("Post text (French)")
        Group(Modifier.padding(horizontal = 16.dp), listOf {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    message, { message = it; Store.setMessage(ctx, it) }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your own words (empty: 10 endings that rotate)") }, minLines = 2
                )
                OutlinedTextField(
                    footer, { footer = it; Store.setFooter(ctx, it) }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Last line of every post (optional)") }
                )
                Text("Price on the listing: always ${Posts.LISTING_PRICE} dh, never in the text. No shop name.", fontSize = 13.sp, color = C.muted)
            }
        })
    }
}

@Composable
private fun SettingRow(title: String, sub: String?, right: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.ink)
            if (sub != null) Text(sub, fontSize = 13.sp, color = C.muted)
        }
        right()
    }
}

@Composable
private fun ChipRow(title: String, options: List<Pair<String, Int>>, value: Int, onPick: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.forEach { (l, v) -> Chip(l, v == value) { onPick(v) } } }
    }
}
