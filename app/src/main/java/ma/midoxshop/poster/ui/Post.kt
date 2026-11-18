package ma.midoxshop.poster.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ma.midoxshop.poster.Album
import ma.midoxshop.poster.Item
import ma.midoxshop.poster.Midox
import ma.midoxshop.poster.Nag
import ma.midoxshop.poster.Posts
import ma.midoxshop.poster.Store
import ma.midoxshop.poster.Widget

fun copy(ctx: Context, label: String, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

/** Opens Facebook's "Create new listing" (the Facebook app opens it when installed). Nothing more. */
fun openMarketplace(ctx: Context) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/marketplace/create/item")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(ctx, "No app found to open Marketplace", Toast.LENGTH_SHORT).show()
    }
}

/** Marks [item] posted today, clears its photos from the gallery, calms the nags, redraws the widget. */
fun posted(ctx: Context, item: Item) {
    Store.markPosted(ctx, item.id)
    Album.clear(ctx)
    Nag.calmDown(ctx)
    Widget.update(ctx)
}

/** A tick that draws itself, stroke by stroke. */
@Composable
fun DrawnCheck(color: Color, size: androidx.compose.ui.unit.Dp, stroke: androidx.compose.ui.unit.Dp, delayMs: Long = 0, durationMs: Int = 420) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs); p.animateTo(1f, tween(durationMs, easing = Motion.EaseOut)) }
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 24f
        val path = Path().apply { moveTo(5f * s, 12.5f * s); lineTo(10f * s, 17.5f * s); lineTo(19f * s, 7f * s) }
        val m = PathMeasure().apply { setPath(path, false) }
        val part = Path()
        m.getSegment(0f, m.length * p.value, part, true)
        drawPath(part, color, style = Stroke(stroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Post mode, one product at a time, nothing else on screen. The photos go to the gallery by themselves
 * and the text is written. The orange button is Open Marketplace, then (once opened) I posted it.
 */
@Composable
fun PostScreen(data: Data, item: Item, onClose: () -> Unit, onPosted: () -> Unit, onSkip: () -> Unit) {
    val ctx = LocalContext.current
    val changes = Store.changes
    val haptics = rememberHaptics()
    val title = Posts.title(item)
    val text = remember(item.id, changes) { Posts.description(item, Store.messageFor(ctx, item, data.today), Store.footer(ctx)) }
    val index = data.picks.indexOfFirst { it.id == item.id }
    var inGallery by remember(item.id) { mutableIntStateOf(-1) }   // -1: still putting them there
    var opened by remember(item.id) { mutableStateOf(false) }
    var copied by remember(item.id) { mutableStateOf(setOf<String>()) }
    val wait = rememberWait()

    LaunchedEffect(item.id) {
        Store.startPost(ctx)
        inGallery = if (item.pics.isEmpty() || !Album.works()) 0 else withContext(Dispatchers.IO) { Album.put(ctx, item) }
    }

    Column(Modifier.fillMaxSize().background(C.ground).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).tap(onClick = onClose).clip(Corners.Small).background(C.card),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Close, C.ink, 20.dp) }
            Spacer(Modifier.width(12.dp))
            if (index >= 0) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    data.picks.forEachIndexed { i, p ->
                        val on = i <= index || data.posted[p.id] == data.today
                        val c by animateColorAsState(if (on) C.accent else C.line, tween(Motion.SCREEN), label = "seg$i")
                        Box(Modifier.weight(1f).height(6.dp).clip(Corners.Pill).background(c))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text("${index + 1} of ${data.picks.size}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.muted)
            } else Text("Any product", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.muted, modifier = Modifier.weight(1f))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (item.pics.isEmpty()) {
                Box(
                    Modifier.padding(20.dp).pop().fillMaxWidth().height(150.dp).clip(Corners.Big).background(C.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Camera, C.accentText, 28.dp)
                        Text("No photo in Midox yet.\nTake one with the camera in Facebook.", color = C.accentInk, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(item.pics) { i, pic ->
                        AsyncImage(
                            Midox.photo(pic), null, contentScale = ContentScale.Crop,
                            modifier = Modifier.enter(i, rise = 30.dp).size(if (item.pics.size == 1) 350.dp else 230.dp, 220.dp).clip(Corners.Big).background(C.tonal)
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 24.dp).enter(1), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.ink, lineHeight = 27.sp)
                AnimatedContent(
                    targetState = when {
                        item.pics.isEmpty() -> "No photos to put in the gallery"
                        !Album.works() -> "Photos: pick them from the Midox album"
                        inGallery < 0 -> "Putting the photos in your gallery…"
                        else -> "$inGallery photo" + (if (inGallery == 1) " is" else "s are") + " first in your gallery"
                    },
                    transitionSpec = { (fadeIn(tween(Motion.QUICK)) + slideInVertically { it / 2 }) togetherWith fadeOut(tween(Motion.QUICK / 2)) },
                    label = "gallery"
                ) { Text(it, fontSize = 14.sp, color = if (inGallery > 0) C.good else C.muted, fontWeight = FontWeight.Medium) }
            }

            Label("Copy, then paste in Facebook")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("Titre", title, title),
                    Triple("Prix", Posts.LISTING_PRICE + " DH  ·  the real price stays private", Posts.LISTING_PRICE),
                    Triple("Description", text, text)
                ).forEachIndexed { i, (label, shown, value) ->
                    val done = label in copied
                    val border by animateColorAsState(if (done) C.good else Color.Transparent, tween(Motion.QUICK), label = "copyBorder")
                    Row(
                        Modifier.enter(i + 2).fillMaxWidth().tap { copy(ctx, label, value); haptics.tick(); copied = copied + label }
                            .clip(Corners.Card).background(C.card).border(2.dp, border, Corners.Card)
                            .heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.muted)
                            Text(shown, fontSize = 15.sp, color = C.ink, maxLines = if (label == "Description") 4 else 2, lineHeight = 20.sp)
                        }
                        AnimatedContent(
                            targetState = done, label = "copied",
                            transitionSpec = { (scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), 0.6f) + fadeIn()) togetherWith (scaleOut(tween(100), 0.6f) + fadeOut(tween(100))) }
                        ) { d ->
                            Row(
                                Modifier.clip(Corners.Pill).background(if (d) C.goodSoft else C.ground).padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (d) DrawnCheck(C.good, 16.dp, 2.4.dp, durationMs = 260) else Icon(Icons.Copy, C.ink, 16.dp)
                                Text(if (d) "Copied" else "Copy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (d) C.good else C.ink)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // The orange button: Open Marketplace, then I posted it. They swap with a slide.
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AnimatedContent(
                targetState = opened, label = "main",
                transitionSpec = {
                    (slideInVertically(tween(Motion.SCREEN, easing = Motion.EaseOut)) { it } + fadeIn(tween(Motion.SCREEN))) togetherWith
                        (slideOutVertically(tween(Motion.SCREEN, easing = Motion.EaseOut)) { -it } + fadeOut(tween(Motion.QUICK)))
                }
            ) { o ->
                // 10 minutes between posts: copy everything meanwhile, Marketplace opens when it's time.
                if (!o && wait > 0) BigButton("Ready in ${mmss(wait)} · copy meanwhile", color = C.tonal, textColor = C.muted) { haptics.tick() }
                else if (!o) BigButton("Open Marketplace", arrow = true) { opened = true; openMarketplace(ctx) }
                else BigButton("I posted it", color = C.good) { haptics.done(); posted(ctx, item); onPosted() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (opened) SoftButton("Open again", Modifier.weight(1f)) { openMarketplace(ctx) }
                else SoftButton("Skip this one", Modifier.weight(1f)) { Album.clear(ctx); onSkip() }
                if (!opened && Store.message(ctx).isBlank()) SoftButton("New wording", Modifier.weight(1f)) { Store.nextWording(ctx, item.id); copied = copied - "Description" }
                if (opened) SoftButton("I posted it", Modifier.weight(1f)) { haptics.done(); posted(ctx, item); onPosted() }
            }
        }
    }
}

/** After a post: the check draws itself, the ring fills, then the words. Stopping is as fine as going on. */
@Composable
fun DoneScreen(data: Data, onNext: () -> Unit, onStop: () -> Unit) {
    val ctx = LocalContext.current
    val streak = Store.streak(ctx)
    val secs = remember { Store.postSeconds(ctx) }
    val next = data.nextPick()
    val left = data.goal - data.doneToday
    val wait = rememberWait()
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }

    Column(
        Modifier.fillMaxSize().background(C.hero).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
            Ring(data.doneToday, data.goal, 168.dp, 14.dp)
            DrawnCheck(C.onHero, 72.dp, 7.dp, delayMs = 150)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            if (data.doneToday >= data.min) "Posted. Streak saved." else "Posted. ${data.min - data.doneToday} more for your streak.",
            fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = C.onHero, textAlign = TextAlign.Center,
            modifier = Modifier.enter(3)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                left <= 0 -> "That's your whole day. Go rest."
                data.doneToday >= data.min -> "Today already counts. $left more ready if you have energy. No pressure."
                else -> "One more and today counts."
            },
            fontSize = 16.sp, color = C.heroMuted, textAlign = TextAlign.Center, lineHeight = 24.sp, modifier = Modifier.enter(4)
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${streak.days}", if (streak.days == 1) "day streak" else "days streak", Modifier.weight(1f).enter(5))
            Stat(
                secs?.let { if (it < 60) "$it s" else "${it / 60} min ${it % 60} s" } ?: "${data.doneToday}",
                if (secs != null) "this post took" else "posted today", Modifier.weight(1f).enter(6)
            )
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.enter(7, rise = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (next != null && left > 0) {
                BigButton(if (wait > 0) "Next post in ${mmss(wait)} · get it ready" else "Keep going · next post", arrow = wait == 0L, onClick = onNext)
                BigButton("I'm done for today", color = C.hero2, height = 56.dp, onClick = onStop)
            } else BigButton("Back to Today", onClick = onStop)
        }
    }
}

@Composable
private fun Stat(big: String, small: String, modifier: Modifier) {
    Column(modifier.clip(Corners.Card).background(C.hero2).padding(16.dp)) {
        Text(big, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = C.onHero, maxLines = 1)
        Text(small, fontSize = 13.sp, color = C.heroMuted)
    }
}
