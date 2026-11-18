package ma.midoxshop.poster.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import ma.midoxshop.poster.Days
import ma.midoxshop.poster.Item
import ma.midoxshop.poster.Midox
import ma.midoxshop.poster.Nag
import ma.midoxshop.poster.Store

/** Today's progress as a ring: [done] of [goal]. It fills in 250 ms (Midox's progress curve). */
@Composable
fun Ring(done: Int, goal: Int, size: Dp, stroke: Dp, track: Color = C.line, fill: Color = C.accent) {
    val target = done.coerceAtMost(goal).toFloat() / goal.coerceAtLeast(1)
    val p by animateFloatAsState(target, tween(250, easing = Motion.EaseInOut), label = "ring")
    Canvas(Modifier.size(size)) {
        val w = stroke.toPx()
        val box = Size(this.size.width - w, this.size.height - w)
        val tl = Offset(w / 2, w / 2)
        drawArc(track, 0f, 360f, false, tl, box, style = Stroke(w))
        if (p > 0f) drawArc(fill, -90f, 360f * p, false, tl, box, style = Stroke(w, cap = StrokeCap.Round))
    }
}

@Composable
fun TodayScreen(data: Data, onStart: () -> Unit, onPost: (Item) -> Unit, onQueue: () -> Unit) {
    val ctx = LocalContext.current
    val streak = Store.streak(ctx)
    val paused = Store.pausedToday(ctx)
    val next = data.nextPick()
    val allDone = data.picks.isNotEmpty() && next == null

    Column(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.weight(1f)) {
            item {
                Title("Today", sub = Days.weekday(data.today) + " " + Days.dayOfMonth(data.today), big = true) {
                    Row(
                        Modifier.padding(bottom = 10.dp).clip(Corners.Round).background(C.card).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Flame, C.accent, 16.dp)
                        Text(if (streak.days == 1) "1 day" else "${streak.days} days", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.ink)
                    }
                }
            }
            // The info card: tonal, serif number, the ring.
            item {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().clip(Corners.Card).background(C.tonal).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("TODAY'S POSTS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = C.muted)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${data.doneToday}", fontFamily = Serif, fontSize = 40.sp, letterSpacing = (-1).sp, color = C.ink)
                            Text(" / ${data.goal}", fontFamily = Serif, fontSize = 24.sp, color = C.muted, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text(
                            when {
                                paused != null -> "Paused today: ${paused.lowercase()}. Your streak is safe."
                                data.doneToday >= data.goal -> "Great day. You're done."
                                data.doneToday >= data.min -> "Streak saved. The rest is a bonus."
                                data.min == 1 -> "1 post keeps your streak. ${data.goal} is a great day."
                                else -> "${data.min} posts keep your streak."
                            },
                            fontSize = 13.sp, color = C.muted
                        )
                    }
                    Ring(data.doneToday, data.goal, 72.dp, 7.dp)
                }
            }
            if (paused != null) item {
                Text(
                    "Resume today", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.accent,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp).clip(Corners.Small)
                        .clickableRole { Store.pause(ctx, null); Nag.schedule(ctx) }.padding(12.dp)
                )
            }
            item { SetupCard(data) }
            if (data.picks.isNotEmpty()) {
                item { Section("Picked for you", trailing = "${data.picks.count { data.posted[it.id] == data.today }} of ${data.picks.size} done") }
                item {
                    Group(
                        Modifier.padding(horizontal = 16.dp),
                        data.picks.map { p ->
                            @Composable {
                                val done = data.posted[p.id] == data.today
                                val last = data.posted[p.id]
                                ItemRow(
                                    p,
                                    when {
                                        done -> "Posted today"
                                        last == null -> "Never posted"
                                        else -> "Repost · last posted " + Days.ago(last, data.today)
                                    },
                                    onClick = { onPost(p) }
                                ) {
                                    when {
                                        done -> Tag("Done", C.good, C.goodSoft)
                                        p.pics.isEmpty() -> Tag("No photo", C.warn, C.tonal)
                                        else -> Tag("Ready", C.good, C.goodSoft)
                                    }
                                }
                            }
                        }
                    )
                }
                item {
                    Text(
                        "See the week", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.ink,
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp).clip(Corners.Small).clickableRole(onQueue).padding(12.dp)
                    )
                }
            }
            item {
                if (Store.nagsOn(ctx)) Text(
                    "Reminder at " + hm(Store.remindAt(ctx)) + if (Store.loud(ctx)) ", then the alarm until you post." else " if you haven't posted.",
                    fontSize = 13.sp, color = C.muted, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp)
                )
            }
        }
        // The one coral action, in thumb reach.
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            when {
                data.items == null -> BigButton("Open Midox", onClick = { Midox.open(ctx) })
                allDone -> SoftButton("All of today's posts are up · see the week", onClick = onQueue)
                next == null -> SoftButton("Nothing to post today · see the week", onClick = onQueue)
                else -> BigButton("Start posting · about 2 min", onClick = onStart)
            }
        }
    }
}

fun hm(m: Int) = "%02d:%02d".format((m / 60) % 24, m % 60)

/** What Poster still needs before it can help: Midox, notifications, the alarm. */
@Composable
fun SetupCard(data: Data) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val changes = Store.changes
    val missing = buildList {
        if (data.items == null) add(Triple(
            if (Midox.installed(ctx)) "Update Midox" else "Install Midox",
            "Poster reads your products and photos from Midox."
        ) { Midox.open(ctx) })
        if (!Nag.canNotify(ctx)) add(Triple("Allow notifications", "For the reminders.") { openAppSettings(ctx) })
        if (!Nag.canOverlay(ctx)) add(Triple("Allow \"Display over other apps\"", "So the alarm can cover the screen over any app.") {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName)))
        })
        if (!Nag.canFullScreen(ctx)) add(Triple("Allow full-screen alarms", "So the alarm shows on the lock screen.") {
            ctx.startActivity(Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT", Uri.parse("package:" + ctx.packageName)))
        })
        if (!Nag.canExact(ctx)) add(Triple("Allow alarms & reminders", "So the nags come on time, not late.") {
            ctx.startActivity(Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM", Uri.parse("package:" + ctx.packageName)))
        })
    }
    if (missing.isEmpty()) return
    Column {
        Section("Needs you")
        Group(Modifier.padding(horizontal = 16.dp), rows = missing.map { (title, why, go) ->
            @Composable {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.ink)
                        Text(why, fontSize = 13.sp, color = C.muted)
                    }
                    Spacer(Modifier.size(10.dp))
                    InkButton("Fix", onClick = go)
                }
            }
        })
    }
}

fun openAppSettings(ctx: Context) {
    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
}
