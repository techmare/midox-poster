package ma.midoxshop.poster.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.first
import ma.midoxshop.poster.Days
import ma.midoxshop.poster.Item
import ma.midoxshop.poster.Midox
import ma.midoxshop.poster.Nag
import ma.midoxshop.poster.Store
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Today's progress as a ring: [done] of [goal]. It sweeps in from empty when it first shows, then
 * springs to each new value.
 */
@Composable
fun Ring(done: Int, goal: Int, size: Dp, stroke: Dp, track: Color = C.heroTrack, fill: Color = C.glow) {
    val target = done.coerceAtMost(goal).toFloat() / goal.coerceAtLeast(1)
    val p = remember { Animatable(0f) }
    LaunchedEffect(target) {
        snapshotFlow { Launch.ready }.first { it }
        p.animateTo(target, tween(700, easing = Motion.EaseOut))
    }
    Canvas(Modifier.size(size)) {
        val w = stroke.toPx()
        val box = Size(this.size.width - w, this.size.height - w)
        val tl = Offset(w / 2, w / 2)
        drawArc(track, 0f, 360f, false, tl, box, style = Stroke(w))
        // A dot even at 0, so the ring shows where it will fill from.
        drawArc(fill, -90f, (360f * p.value).coerceAtLeast(1.5f), false, tl, box, style = Stroke(w, cap = StrokeCap.Round))
    }
}

fun longDate(day: String): String {
    val d = LocalDate.parse(day)
    return d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " · " + d.dayOfMonth + " " + d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
}

@Composable
fun TodayScreen(data: Data, onStart: () -> Unit, onPost: (Item) -> Unit, onQueue: () -> Unit) {
    val ctx = LocalContext.current
    val streak = Store.streak(ctx)
    val paused = Store.pausedToday(ctx)
    val next = data.nextPick()
    val allDone = data.picks.isNotEmpty() && next == null
    val wait = rememberWait()

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Title("Today", sub = longDate(data.today)) {
                Row(
                    Modifier.pop(120).clip(Corners.Pill).background(C.card).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Flame, C.accentText, 16.dp)
                    Rolling("${streak.days}", 14.sp, C.ink, Body, FontWeight.SemiBold)
                    Text(if (streak.days == 1) "day" else "days", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.ink)
                }
            }
        }
        // The dark card: today's count, the ring, and Start posting.
        item {
            Column(
                Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp).pop(60).fillMaxWidth()
                    .clip(Corners.Big).background(C.hero).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Today's posts", fontSize = 14.sp, color = C.heroMuted)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Rolling("${data.doneToday}", 44.sp, C.onHero)
                            Text(" / ${data.goal}", fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.heroMuted, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        AnimatedContent(
                            targetState = when {
                                paused != null -> "Paused today: ${paused.lowercase()}. Your streak is safe."
                                data.doneToday >= data.goal -> "Great day. You're done."
                                data.doneToday >= data.min -> "Streak saved. The rest is a bonus."
                                data.min == 1 -> "1 post keeps your streak. ${data.goal} is a great day."
                                else -> "${data.min} posts keep your streak."
                            },
                            transitionSpec = { (fadeIn(tween(Motion.QUICK)) + slideInVertically(tween(Motion.QUICK)) { it / 2 }) togetherWith fadeOut(tween(Motion.QUICK / 2)) },
                            label = "msg"
                        ) { Text(it, fontSize = 13.sp, color = C.heroMuted) }
                    }
                    Ring(data.doneToday, data.goal, 76.dp, 8.dp)
                }
                when {
                    data.items == null -> BigButton("Open Midox", arrow = true, onClick = { Midox.open(ctx) })
                    allDone -> BigButton("All of today's posts are up", color = C.good, onClick = onQueue)
                    next == null -> BigButton("Nothing to post · see the queue", color = C.heroTrack, onClick = onQueue)
                    // Posts are spaced 10 minutes apart; you can still open the next one and get it ready.
                    wait > 0 -> BigButton("Next post in ${mmss(wait)}", color = C.heroTrack, onClick = onStart)
                    else -> BigButton("Start posting", arrow = true, onClick = onStart)
                }
                if (next != null && data.items != null) Text(
                    if (wait > 0) "Spacing your posts so Facebook sees no burst" else "About 2 min a post · everything is ready", fontSize = 13.sp, color = C.heroMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 0.dp)
                )
                if (paused != null) Text(
                    "Resume today", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.glow,
                    modifier = Modifier.align(Alignment.CenterHorizontally).clip(Corners.Small)
                        .clickableRole { Store.pause(ctx, null); Nag.schedule(ctx) }.padding(12.dp)
                )
            }
        }
        item { SetupCard(data) }
        if (data.picks.isNotEmpty()) {
            item {
                Section("Picked for you") {
                    Text("See queue", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.accentText,
                        modifier = Modifier.clip(Corners.Small).clickableRole(onQueue).padding(8.dp))
                }
            }
            itemsIndexed(data.picks, key = { _, p -> p.id }) { i, p ->
                val done = data.posted[p.id] == data.today
                val last = data.posted[p.id]
                Box(Modifier.padding(horizontal = 24.dp, vertical = 5.dp).enter(i + 2).animateItem()) {
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
                            done -> Pill("Done", C.good, C.goodSoft)
                            p.pics.isEmpty() -> Pill("No photo", C.accentInk, C.accentSoft)
                            else -> Pill("Ready", C.good, C.goodSoft)
                        }
                    }
                }
            }
        }
        item {
            if (Store.nagsOn(ctx)) Row(
                Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp).enter(6),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Clock, C.muted, 16.dp)
                Text(
                    "Reminder at " + hm(Store.remindAt(ctx)) + if (Store.loud(ctx)) ", then the alarm until you post" else " if you haven't posted",
                    fontSize = 13.sp, color = C.muted
                )
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
    Column(
        Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp).enter(1).fillMaxWidth().clip(Corners.Card)
            .background(C.accentSoft).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("To help you post, Poster needs:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.accentInk)
        missing.forEach { (title, why, go) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.ink)
                    Text(why, fontSize = 13.sp, color = C.muted)
                }
                Spacer(Modifier.size(10.dp))
                Text("Fix", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.tap(onClick = go).clip(Corners.Small).background(C.accentText).padding(horizontal = 16.dp, vertical = 12.dp))
            }
        }
    }
}

fun openAppSettings(ctx: Context) {
    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
}
