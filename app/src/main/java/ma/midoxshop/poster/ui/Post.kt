package ma.midoxshop.poster.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
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

/** Midox's progress bar: a thick round bar, ink on the line colour, moving in 250 ms. */
@Composable
fun Progress(value: Float, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(value, tween(250, easing = Motion.EaseInOut), label = "progress")
    val track = C.line; val fill = C.ink
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        val w = size.width * p
        if (w > 0f) drawRoundRect(fill, size = Size(maxOf(w, size.height), size.height), cornerRadius = r)
    }
}

/**
 * Post mode, one product at a time (Midox's "one item at a time"): close, the job's name, "1 / 3" and a
 * thick bar on top; the product large; the one coral action at the bottom. The photos go to the gallery
 * by themselves and the text is written. Coral is first Open Marketplace, then, once opened, I posted it.
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

    LaunchedEffect(item.id) {
        Store.startPost(ctx)
        inGallery = if (item.pics.isEmpty() || !Album.works()) 0 else withContext(Dispatchers.IO) { Album.put(ctx, item) }
    }

    Column(Modifier.fillMaxSize().background(C.ground).statusBarsPadding().navigationBarsPadding()) {
        // The top: close, name, count, bar.
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(44.dp).tap(onClick = onClose).clip(CircleShape).background(C.card).border(1.dp, C.line, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Close, C.ink, 20.dp) }
                Text("Post on Marketplace", fontFamily = Serif, fontSize = 22.sp, letterSpacing = (-0.3).sp, color = C.ink, modifier = Modifier.weight(1f))
                if (index >= 0) Text("${index + 1} / ${data.picks.size}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.muted)
            }
            if (index >= 0) {
                Spacer(Modifier.height(12.dp))
                Progress((index + if (data.posted[item.id] == data.today) 1f else 0.5f) / data.picks.size)
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (item.pics.isEmpty()) {
                Box(
                    Modifier.padding(16.dp).fillMaxWidth().height(140.dp).clip(Corners.Sheet).background(C.tonal),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Camera, C.warn, 28.dp)
                        Text("No photo in Midox yet.\nTake one with the camera in Facebook.", color = C.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(item.pics.size) { i ->
                        AsyncImage(
                            Midox.photo(item.pics[i]), null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(if (item.pics.size == 1) 340.dp else 220.dp, 220.dp).clip(Corners.Sheet).background(C.tonal)
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontFamily = Serif, fontSize = 22.sp, letterSpacing = (-0.3).sp, color = C.ink)
                Text(
                    when {
                        item.pics.isEmpty() -> "No photos to put in the gallery"
                        !Album.works() -> "Photos: pick them from the Midox album"
                        inGallery < 0 -> "Putting the photos in your gallery…"
                        else -> "$inGallery photo" + (if (inGallery == 1) " is" else "s are") + " first in your gallery"
                    },
                    fontSize = 13.sp, color = if (inGallery > 0) C.good else C.muted, fontWeight = FontWeight.Medium
                )
            }

            Section("Copy, then paste in Facebook")
            Group(
                Modifier.padding(horizontal = 16.dp),
                listOf(
                    Triple("Titre", title, title),
                    Triple("Prix", Posts.LISTING_PRICE + " dh (the real price stays private)", Posts.LISTING_PRICE),
                    Triple("Description", text, text)
                ).map { (label, shown, value) ->
                    @Composable {
                        val done = label in copied
                        Row(
                            Modifier.fillMaxWidth().clickableRole { copy(ctx, label, value); haptics.tick(); copied = copied + label }
                                .heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = C.muted)
                                Text(shown, fontSize = 15.sp, color = C.ink, maxLines = if (label == "Description") 4 else 2, lineHeight = 20.sp)
                            }
                            Row(
                                Modifier.clip(Corners.Round).background(if (done) C.goodSoft else C.tonal).padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(if (done) Icons.Check else Icons.Copy, if (done) C.good else C.ink, 16.dp)
                                Text(if (done) "Copied" else "Copy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (done) C.good else C.ink)
                            }
                        }
                    }
                }
            )
            Row(Modifier.padding(start = 8.dp, top = 4.dp)) {
                Text("Skip this one", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.muted,
                    modifier = Modifier.clip(Corners.Small).clickableRole { Album.clear(ctx); onSkip() }.padding(12.dp))
                if (Store.message(ctx).isBlank()) Text("New wording", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.muted,
                    modifier = Modifier.clip(Corners.Small).clickableRole { Store.nextWording(ctx, item.id); copied = copied - "Description" }.padding(12.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        // The one coral action: open Marketplace, then (once opened) "I posted it".
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!opened) {
                BigButton("Open Marketplace") { opened = true; openMarketplace(ctx) }
                SoftButton("I posted it") { haptics.done(); posted(ctx, item); onPosted() }
            } else {
                BigButton("I posted it") { haptics.done(); posted(ctx, item); onPosted() }
                SoftButton("Open Marketplace again") { openMarketplace(ctx) }
            }
        }
    }
}

/** After a post: say it counted, and make stopping as fine as going on. */
@Composable
fun DoneScreen(data: Data, onNext: () -> Unit, onStop: () -> Unit) {
    val ctx = LocalContext.current
    val streak = Store.streak(ctx)
    val secs = remember { Store.postSeconds(ctx) }
    val next = data.nextPick()
    val left = data.goal - data.doneToday
    // The one visible bounce Midox allows: the "job finished" check.
    val pop = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }

    Column(
        Modifier.fillMaxSize().background(C.ground).statusBarsPadding().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
            Ring(data.doneToday, data.goal, 160.dp, 12.dp)
            Icon(Icons.Check, C.ink, 56.dp)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            if (data.doneToday >= data.min) "Posted. Streak saved." else "Posted. ${data.min - data.doneToday} more for your streak.",
            fontFamily = Serif, fontSize = 30.sp, letterSpacing = (-0.6).sp, color = C.ink, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                left <= 0 -> "That's your whole day. Go rest."
                data.doneToday >= data.min -> "Today already counts. $left more ready if you have energy. No pressure."
                else -> "One more and today counts."
            },
            fontSize = 15.sp, color = C.muted, textAlign = TextAlign.Center, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${streak.days}", if (streak.days == 1) "day streak" else "days streak", Modifier.weight(1f))
            Stat(
                secs?.let { if (it < 60) "$it s" else "${it / 60} min ${it % 60} s" } ?: "${data.doneToday}",
                if (secs != null) "this post took" else "posted today", Modifier.weight(1f)
            )
        }
        Spacer(Modifier.weight(1f))
        if (next != null && left > 0) {
            BigButton("Keep going · next post", onClick = onNext)
            Spacer(Modifier.height(8.dp))
            SoftButton("I'm done for today", onClick = onStop)
        } else BigButton("Back to Today", onClick = onStop)
    }
}

@Composable
private fun Stat(big: String, small: String, modifier: Modifier) {
    Column(modifier.clip(Corners.Card).background(C.tonal).padding(14.dp)) {
        Text(big, fontFamily = Serif, fontSize = 24.sp, color = C.ink, maxLines = 1)
        Text(small, fontSize = 13.sp, color = C.muted)
    }
}
