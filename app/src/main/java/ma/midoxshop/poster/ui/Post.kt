package ma.midoxshop.poster.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

fun copy(ctx: Context, label: String, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    if (Build.VERSION.SDK_INT < 33) Toast.makeText(ctx, "$label copied", Toast.LENGTH_SHORT).show()
}

/** Opens Facebook's "Create new listing" (the Facebook app opens it when installed). */
fun openMarketplace(ctx: Context) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/marketplace/create/item")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(ctx, "No app found to open Marketplace", Toast.LENGTH_SHORT).show()
    }
}

/** Marks [item] posted today, clears its photos from the gallery and calms the nags. */
fun posted(ctx: Context, item: Item) {
    Store.markPosted(ctx, item.id)
    Album.clear(ctx)
    Nag.calmDown(ctx)
}

/**
 * Post mode: one product, nothing else on screen. The photos go to the gallery by themselves, the
 * text is written; you open Marketplace and paste with the copy buttons, in order.
 */
@Composable
fun PostScreen(data: Data, item: Item, onClose: () -> Unit, onPosted: () -> Unit, onSkip: () -> Unit) {
    val ctx = LocalContext.current
    val changes = Store.changes
    val title = Posts.title(item)
    val text = remember(item.id, changes) { Posts.description(item, Store.messageFor(ctx, item, data.today), Store.footer(ctx)) }
    val index = data.picks.indexOfFirst { it.id == item.id }
    var inGallery by remember(item.id) { mutableIntStateOf(-1) }   // -1: still putting them there

    LaunchedEffect(item.id) {
        Store.startPost(ctx)
        inGallery = if (item.pics.isEmpty() || !Album.works()) 0 else withContext(Dispatchers.IO) { Album.put(ctx, item) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 16.dp, end = 20.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(C.card).clickableRole(onClose),
                contentAlignment = Alignment.Center
            ) { Text("✕", fontSize = 17.sp, color = C.ink) }
            Spacer(Modifier.width(12.dp))
            if (index >= 0) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    data.picks.forEachIndexed { i, p ->
                        val on = i == index || data.posted[p.id] == data.today
                        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (on) C.accent else C.line))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text("${index + 1} of ${data.picks.size}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.muted)
            } else Text("Any product", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.muted, modifier = Modifier.weight(1f))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (item.pics.isEmpty()) {
                Box(
                    Modifier.padding(20.dp).fillMaxWidth().height(140.dp).clip(RoundedCornerShape(24.dp)).background(C.accentSoft),
                    contentAlignment = Alignment.Center
                ) { Text("No photo in Midox yet.\nTake one with the camera in Facebook.", color = C.accentInk, fontSize = 15.sp, textAlign = TextAlign.Center) }
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(item.pics.size) { i ->
                        AsyncImage(
                            Midox.photo(item.pics[i]), null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(if (item.pics.size == 1) 340.dp else 200.dp, 200.dp).clip(RoundedCornerShape(20.dp)).background(C.photo)
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.ink)
                Text(text, fontSize = 15.sp, color = C.muted, lineHeight = 21.sp)
            }
            Column(
                Modifier.padding(20.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.card).padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Step(
                    when {
                        item.pics.isEmpty() -> "No photos to put in the gallery"
                        !Album.works() -> "Photos: pick them from Midox in Facebook"
                        inGallery < 0 -> "Putting the photos in your gallery…"
                        else -> "$inGallery photo" + (if (inGallery == 1) "" else "s") + " first in your gallery"
                    },
                    done = inGallery >= 0
                )
                Step("Title, price (1 dh) and text written", done = true)
                Step("Open Marketplace, then copy and paste each one", done = false, number = 3)
            }
            // Copy one, paste it in Facebook, come back for the next.
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CopyChip("Titre", Modifier.weight(1f)) { copy(ctx, "Titre", title) }
                CopyChip("Prix", Modifier.weight(1f)) { copy(ctx, "Prix", Posts.LISTING_PRICE) }
                CopyChip("Texte", Modifier.weight(1f)) { copy(ctx, "Texte", text) }
            }
            Spacer(Modifier.height(12.dp))
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Open Marketplace") { openMarketplace(ctx) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("Skip this one", Modifier.weight(1f)) { Album.clear(ctx); onSkip() }
                if (Store.message(ctx).isBlank()) SoftButton("New wording", Modifier.weight(1f)) { Store.nextWording(ctx, item.id) }
            }
            BigButton("I posted it", color = C.good, height = 52.dp) { posted(ctx, item); onPosted() }
        }
    }
}

@Composable
private fun Step(text: String, done: Boolean, number: Int = 0) {
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(26.dp).clip(CircleShape)
                .background(if (done) C.good else Color.Transparent)
                .then(if (done) Modifier else Modifier.border(2.dp, C.accent, CircleShape)),
            contentAlignment = Alignment.Center
        ) { Text(if (done) "✓" else "$number", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (done) Color.White else C.accentText) }
        Text(text, fontSize = 15.sp, color = if (done) C.muted else C.ink, fontWeight = if (done) FontWeight.Normal else FontWeight.SemiBold)
    }
}

@Composable
private fun CopyChip(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(C.card).clickableRole(onClick),
        contentAlignment = Alignment.Center
    ) { Text("Copy $label", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.ink) }
}

/** After a post: say it counted, and make stopping as fine as going on. */
@Composable
fun DoneScreen(data: Data, onNext: () -> Unit, onStop: () -> Unit) {
    val ctx = LocalContext.current
    val streak = Store.streak(ctx)
    val secs = remember { Store.postSeconds(ctx) }
    val next = data.nextPick()
    val left = data.goal - data.doneToday
    Column(
        Modifier.fillMaxSize().background(C.dark).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Box(contentAlignment = Alignment.Center) {
            Ring(data.doneToday, data.goal, 168.dp, 14.dp)
            Text("✓", fontSize = 64.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            if (data.doneToday >= data.min) "Posted. Streak saved." else "Posted. ${data.min - data.doneToday} more for your streak.",
            fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                left <= 0 -> "That's your whole day. Go rest."
                data.doneToday >= data.min -> "Today already counts. $left more ready if you have energy. No pressure."
                else -> "One more and today counts."
            },
            fontSize = 17.sp, color = C.onDark, textAlign = TextAlign.Center, lineHeight = 24.sp
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${streak.days}", "day streak", Modifier.weight(1f))
            Stat(secs?.let { if (it < 60) "$it s" else "${it / 60} min ${it % 60} s" } ?: "${data.doneToday}", if (secs != null) "this post took" else "posted today", Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        if (next != null && left > 0) {
            BigButton("Keep going · next post", onClick = onNext)
            Spacer(Modifier.height(10.dp))
        }
        BigButton("I'm done for today", color = C.dark2, height = 56.dp, onClick = onStop)
    }
}

@Composable
private fun Stat(big: String, small: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(C.dark2).padding(14.dp)) {
        Text(big, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(small, fontSize = 13.sp, color = C.onDark)
    }
}
