package ma.midoxshop.poster.ui

import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
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

/** The next 7 days, planned by themselves: for good days, when you feel like doing more. */
@Composable
fun QueueScreen(data: Data, onPost: (Item) -> Unit) {
    val ctx = LocalContext.current
    val plan = remember(data) {
        data.items?.let { Posts.plan(it, data.posted, data.sold, data.today, data.goal, 7, data.skips) }.orEmpty()
    }
    var sel by rememberSaveable { mutableIntStateOf(0) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
        item { Title("Queue", "The next 7 days plan themselves") }
        item {
            Text(
                "Feeling focused? Post ahead from any day: it moves up, and the rest of the week fills in again.",
                fontSize = 15.sp, color = C.muted, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
        if (plan.isNotEmpty()) item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                plan.forEachIndexed { i, (d, list) ->
                    val on = i == sel
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) C.dark else C.card)
                            .clickableRole { sel = i }.heightIn(min = 72.dp).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(if (i == 0) "Today" else Days.weekday(d), fontSize = 11.sp, color = if (on) C.onDark else C.muted)
                        Text("${Days.dayOfMonth(d)}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else C.ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            list.forEach { p ->
                                val c = when { data.posted[p.id] == d -> C.glow; p.pics.isEmpty() -> C.line; else -> C.good }
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
            Text(
                (if (sel == 0) "Today" else Days.weekday(day) + " " + Days.dayOfMonth(day)) + " · $ready of ${list.size} ready",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C.ink, modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
            )
        }
        if (data.items == null) item { SetupCard(data) }
        else if (list.isEmpty()) item {
            Text("Nothing to post that day: every product with a price was posted in the last ${Posts.REST_DAYS} days.",
                fontSize = 15.sp, color = C.muted, modifier = Modifier.padding(horizontal = 24.dp))
        }
        list.forEach { p ->
            item(key = day + p.id) {
                val done = data.posted[p.id] == day
                Box(Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                    ItemRow(
                        p,
                        when {
                            done -> "Posted"
                            p.pics.isEmpty() -> "No photo in Midox yet"
                            data.posted[p.id] == null -> "New post"
                            else -> "Repost · " + Days.ago(data.posted[p.id]!!, data.today)
                        },
                        subColor = if (p.pics.isEmpty() && !done) C.accentInk else C.muted,
                        onClick = { onPost(p) }
                    ) {
                        if (!done) Text(
                            "Swap", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.ink,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(C.ground)
                                .clickableRole { Store.skip(ctx, day, p.id) }.padding(horizontal = 14.dp, vertical = 14.dp)
                        )
                        else Pill("Done")
                    }
                }
            }
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
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp).clip(RoundedCornerShape(14.dp)).background(C.line).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Live · ${live.size}", "Repost due · ${due.size}", "Sold · ${sold.size}").forEachIndexed { i, label ->
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (i == seg) C.card else Color.Transparent)
                            .clickableRole { seg = i }.heightIn(min = 44.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, fontSize = 13.sp, fontWeight = if (i == seg) FontWeight.Bold else FontWeight.Medium, color = if (i == seg) C.ink else C.muted) }
                }
            }
        }
        if (seg == 2 && sold.isNotEmpty()) item {
            Text(
                "Your commission is still logged in Midox › Log › My online.",
                fontSize = 13.sp, color = C.muted, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)
            )
        }
        if (shown.isEmpty()) item {
            Text(
                when (seg) {
                    0 -> "Nothing posted in the last ${Posts.REST_DAYS} days yet."
                    1 -> "Nothing to repost. Posts come back here after ${Posts.REST_DAYS} days, when they sink."
                    else -> "Nothing marked sold."
                },
                fontSize = 15.sp, color = C.muted, modifier = Modifier.padding(24.dp)
            )
        }
        shown.forEach { (p, d) ->
            item(key = "$seg" + p.id) {
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.card).padding(bottom = 12.dp)
                ) {
                    ItemRow(
                        p,
                        if (d.isBlank()) "Sold" else "Posted " + Days.ago(d, data.today) + if (seg == 1) " · sinking" else "",
                        subColor = if (seg == 1) C.accentInk else C.muted
                    )
                    Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (seg) {
                            2 -> {
                                SmallAction("Not sold", Modifier.weight(1f)) { Store.setSold(ctx, p.id, false) }
                                SmallAction("Log it in Midox", Modifier.weight(1f), main = true) { Midox.open(ctx) }
                            }
                            else -> {
                                if (seg == 1) SmallAction("Repost now", Modifier.weight(1f), main = true) { onPost(p) }
                                SmallAction("Mark sold", Modifier.weight(1f)) { Store.setSold(ctx, p.id, true) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallAction(text: String, modifier: Modifier, main: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(if (main) C.accent else C.ground).clickableRole(onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (main) Color.White else C.ink) }
}

@Composable
fun SettingsScreen(data: Data) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val changes = Store.changes
    var footer by remember { mutableStateOf(Store.footer(ctx)) }
    var message by remember { mutableStateOf(Store.message(ctx)) }
    val reschedule = { Nag.schedule(ctx) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Title("Settings")
        SetupCard(data)
        Box(Modifier.padding(horizontal = 20.dp)) {
            Column {
                Section("Daily goal")
                Group {
                    SettingRow("Minimum", "keeps the streak") { Stepper(Store.min(ctx)) { Store.setMin(ctx, it); reschedule() } }
                    SettingRow("Good day", "the full ring") { Stepper(Store.goal(ctx)) { Store.setGoal(ctx, it) } }
                    SettingRow("Streak freezes", "missed days a week that don't break it") { Stepper(Store.freezes(ctx)) { Store.setFreezes(ctx, it) } }
                }

                Section("Nags")
                Group {
                    SettingRow("Remind me", null) { Toggle(Store.nagsOn(ctx)) { Store.setNagsOn(ctx, it); reschedule() } }
                    SettingRow("First reminder", "a quiet notification") {
                        Text(
                            hm(Store.remindAt(ctx)), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C.ink,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(C.ground).clickableRole {
                                val m = Store.remindAt(ctx)
                                TimePickerDialog(ctx, { _, h, min -> Store.setRemindAt(ctx, h * 60 + min); reschedule() }, m / 60, m % 60, true).show()
                            }.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                    SettingRow("Full-screen alarm", "covers the screen and rings until you post, snooze or pause") {
                        Toggle(Store.loud(ctx)) { Store.setLoud(ctx, it); reschedule() }
                    }
                    if (Store.loud(ctx)) {
                        ChipRow("Alarm starts", listOf(30, 60, 90, 120).map { "+${it} min" to it }, Store.alarmAfter(ctx)) { Store.setAlarmAfter(ctx, it); reschedule() }
                    }
                    ChipRow("Stop nagging at", listOf(22 * 60, 23 * 60, 23 * 60 + 59).map { hm(it) to it }, Store.endAt(ctx)) { Store.setEndAt(ctx, it); reschedule() }
                }
                Spacer(Modifier.size(10.dp))
                SoftButton("Try the alarm now") {
                    ctx.startActivity(Intent(ctx, AlarmActivity::class.java).putExtra("test", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }

                Section("Post text (French)")
                Group {
                    Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }
            }
        }
    }
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.card).padding(horizontal = 16.dp, vertical = 4.dp), content = content)
}

@Composable
private fun SettingRow(title: String, sub: String?, right: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.ink)
            if (sub != null) Text(sub, fontSize = 13.sp, color = C.muted)
        }
        right()
    }
}

@Composable
private fun ChipRow(title: String, options: List<Pair<String, Int>>, value: Int, onPick: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.forEach { (l, v) -> Chip(l, v == value) { onPick(v) } } }
    }
}
