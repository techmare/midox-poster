package ma.midoxshop.poster.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.midoxshop.poster.Item
import ma.midoxshop.poster.Midox
import ma.midoxshop.poster.Nag
import ma.midoxshop.poster.Store

/** Today's progress as a ring: [done] of [goal]. */
@Composable
fun Ring(done: Int, goal: Int, size: Dp, stroke: Dp, track: Color = C.dark3) {
    Canvas(Modifier.size(size)) {
        val w = stroke.toPx()
        val box = Size(this.size.width - w, this.size.height - w)
        val tl = Offset(w / 2, w / 2)
        drawArc(track, 0f, 360f, false, tl, box, style = Stroke(w))
        val sweep = 360f * (done.coerceAtMost(goal).toFloat() / goal.coerceAtLeast(1))
        if (sweep > 0) drawArc(C.glow, -90f, sweep, false, tl, box, style = Stroke(w, cap = StrokeCap.Round))
    }
}

@Composable
fun TodayScreen(data: Data, onStart: () -> Unit, onPost: (Item) -> Unit, onQueue: () -> Unit) {
    val ctx = LocalContext.current
    val streak = Store.streak(ctx)
    val paused = Store.pausedToday(ctx)
    val next = data.nextPick()
    val allDone = data.picks.isNotEmpty() && next == null

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth().padding(end = 24.dp), verticalAlignment = Alignment.Bottom) {
                Box(Modifier.weight(1f)) { Title("Today") }
                Pill(
                    if (streak.days == 1) "1 day streak" else "${streak.days} day streak",
                    fg = C.accentInk, bg = C.accentSoft
                )
            }
        }
        item {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp)).background(C.dark).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Today's posts", fontSize = 13.sp, color = C.onDark)
                        Text("${data.doneToday} / ${data.goal}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            when {
                                paused != null -> "Paused today: $paused. Your streak is safe."
                                data.doneToday >= data.goal -> "Great day. You're done."
                                data.doneToday >= data.min -> "Streak saved. The rest is a bonus."
                                data.min == 1 -> "1 post keeps your streak. ${data.goal} is a great day."
                                else -> "${data.min} posts keep your streak."
                            },
                            fontSize = 13.sp, color = C.onDark
                        )
                    }
                    Ring(data.doneToday, data.goal, 76.dp, 8.dp)
                }
                when {
                    data.items == null -> BigButton("Open Midox first", onClick = { Midox.open(ctx) })
                    allDone -> BigButton("All of today's posts are up", color = C.good, onClick = onQueue)
                    next == null -> BigButton("Nothing to post: see the queue", color = C.dark3, onClick = onQueue)
                    else -> BigButton("Start posting", onClick = onStart)
                }
                if (next != null && data.items != null) Text(
                    "About 2 min a post · the text and photos are ready",
                    fontSize = 13.sp, color = C.onDark, modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                if (paused != null) Text(
                    "Resume today", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.glow,
                    modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(10.dp))
                        .clickableRole { Store.pause(ctx, null); Nag.schedule(ctx) }.padding(12.dp)
                )
            }
        }
        item { SetupCard(data) }
        if (data.picks.isNotEmpty()) {
            item {
                Row(Modifier.padding(start = 24.dp, end = 16.dp, top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Picked for you", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C.ink, modifier = Modifier.weight(1f))
                    Text("See queue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.accentText,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickableRole(onQueue).padding(10.dp))
                }
            }
            data.picks.forEach { p ->
                item(key = p.id) {
                    val done = data.posted[p.id] == data.today
                    val last = data.posted[p.id]
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                        ItemRow(
                            p,
                            when {
                                done -> "Posted today"
                                last == null -> "Never posted"
                                else -> "Repost · last posted " + ma.midoxshop.poster.Days.ago(last, data.today)
                            },
                            onClick = { onPost(p) }
                        ) {
                            if (done) Pill("Done") else if (p.pics.isEmpty()) Pill("No photo", C.accentInk, C.accentSoft) else Pill("Ready")
                        }
                    }
                }
            }
        }
        item {
            if (Store.nagsOn(ctx)) Text(
                "Reminder at " + hm(Store.remindAt(ctx)) + if (Store.loud(ctx)) ", then the alarm until you post" else " if you haven't posted",
                fontSize = 13.sp, color = C.muted, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp)
            )
        }
    }
}

fun hm(m: Int) = "%02d:%02d".format((m / 60) % 24, m % 60)

fun Modifier.clickableRole(onClick: () -> Unit): Modifier = this.clickable(role = Role.Button, onClick = onClick)

/** What Poster still needs before it can help: Midox, notifications, the alarm. */
@Composable
fun SetupCard(data: Data) {
    val ctx = LocalContext.current
    val missing = buildList {
        if (data.items == null) add(Triple(
            if (Midox.installed(ctx)) "Update Midox" else "Install Midox",
            "Poster reads your products and photos from Midox (version 3.0.28 or newer)."
        ) { Midox.open(ctx) })
        if (!Nag.canNotify(ctx)) add(Triple("Allow notifications", "For the reminders.") { openAppSettings(ctx) })
        if (!Nag.canOverlay(ctx)) add(Triple("Allow \"Display over other apps\"", "So the alarm can cover the screen over any app.") {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName)))
        })
        if (!Nag.canFullScreen(ctx)) add(Triple("Allow full-screen alarms", "So the alarm can show on the lock screen.") {
            ctx.startActivity(Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT", Uri.parse("package:" + ctx.packageName)))
        })
        if (!Nag.canExact(ctx)) add(Triple("Allow alarms & reminders", "So the nags come on time, not late.") {
            ctx.startActivity(Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM", Uri.parse("package:" + ctx.packageName)))
        })
    }
    if (missing.isEmpty()) return
    Column(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(C.accentSoft).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("To help you post, Poster needs:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.accentInk)
        missing.forEach { (title, why, go) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.ink)
                    Text(why, fontSize = 13.sp, color = C.muted)
                }
                Spacer(Modifier.size(8.dp))
                Text("Fix", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(C.accentText).clickableRole(go).padding(horizontal = 16.dp, vertical = 12.dp))
            }
        }
    }
}

fun openAppSettings(ctx: android.content.Context) {
    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
}
