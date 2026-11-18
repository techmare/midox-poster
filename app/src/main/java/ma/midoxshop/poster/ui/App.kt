package ma.midoxshop.poster.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ma.midoxshop.poster.Days
import ma.midoxshop.poster.Item
import ma.midoxshop.poster.Midox
import ma.midoxshop.poster.Posts
import ma.midoxshop.poster.Store
import ma.midoxshop.poster.Widget

/** What the screens need from Midox and from Poster's own notes, read again on every change. */
class Data(val items: List<Item>?, ctx: Context) {
    val today = Days.today()
    val posted = Store.posted(ctx)
    val sold = Store.sold(ctx)
    val skips = Store.skips(ctx)
    val goal = Store.goal(ctx)
    val min = Store.min(ctx)
    val picks: List<Item> = items?.let { Posts.picks(it, posted, sold, today, goal, skips[today].orEmpty()) }.orEmpty()
    val doneToday = Store.postedToday(ctx)
    fun byId(id: String) = items?.firstOrNull { it.id == id }
    /** The next of today's picks not posted yet. */
    fun nextPick(): Item? = picks.firstOrNull { posted[it.id] != today }
}

/** Where you are: a tab, post mode on a product, or the "Posted" screen after one. */
private sealed interface Place { val depth: Int }
private data class TabPlace(val tab: Int) : Place { override val depth = 0 }
private data class PostPlace(val id: String) : Place { override val depth = 1 }
private data class DonePlace(val id: String) : Place { override val depth = 2 }

/**
 * The app's side of the opening animation: while the splash is still up the app waits at 94%, and
 * settles to full size as the window opens onto it.
 */
@Composable
fun LaunchSettle(content: @Composable () -> Unit) {
    val s by animateFloatAsState(if (Launch.ready) 1f else 0.94f, tween(700, easing = Motion.EaseOut), label = "settle")
    Box(Modifier.fillMaxSize().graphicsLayer { scaleX = s; scaleY = s }) { content() }
}

@Composable
fun App(request: MutableState<String?>, resumed: Int) {
    val ctx = LocalContext.current
    val changes = Store.changes
    val items by produceState<List<Item>?>(initialValue = null, resumed) {
        value = withContext(Dispatchers.IO) { Midox.items(ctx) }
        // The first time Midox lets us in: take over its Marketplace history and words.
        if (value != null && !Store.imported(ctx)) withContext(Dispatchers.IO) { Midox.market(ctx) }?.let { Store.importFromMidox(ctx, it) }
    }
    val data = remember(items, changes) { Data(items, ctx) }
    LaunchedEffect(data) { Widget.update(ctx) }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var posting by rememberSaveable { mutableStateOf<String?>(null) }
    var done by rememberSaveable { mutableStateOf<String?>(null) }
    val haptics = rememberHaptics()

    // A nag or the widget ("post") or Midox's Post button ("pid:<id>") brings you here.
    LaunchedEffect(request.value, items) {
        val r = request.value ?: return@LaunchedEffect
        if (items == null) return@LaunchedEffect
        when {
            r == "post" -> { done = null; posting = data.nextPick()?.id; if (posting == null) tab = 0 }
            r.startsWith("pid:") -> { done = null; posting = r.removePrefix("pid:") }
        }
        request.value = null
    }

    val startPost = { it: Item? -> done = null; posting = it?.id; if (it == null) tab = 1 }

    val place: Place = when {
        done != null -> DonePlace(done!!)
        posting != null && data.byId(posting!!) != null -> PostPlace(posting!!)
        else -> TabPlace(tab)
    }

    Column(Modifier.fillMaxSize().background(C.ground)) {
        AnimatedContent(
            targetState = place,
            modifier = Modifier.weight(1f),
            transitionSpec = { move(initialState, targetState) },
            label = "place"
        ) { p ->
            when (p) {
                is DonePlace -> {
                    BackHandler { done = null }
                    DoneScreen(data, onNext = { startPost(data.nextPick()) }, onStop = { done = null; tab = 0 })
                }
                is PostPlace -> {
                    val item = data.byId(p.id)
                    BackHandler { posting = null }
                    if (item != null) PostScreen(data, item, onClose = { posting = null }, onPosted = { posting = null; done = item.id }, onSkip = {
                        Store.skip(ctx, data.today, item.id)
                        posting = Data(items, ctx).nextPick()?.id
                    })
                }
                is TabPlace -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    BackHandler(enabled = p.tab != 0) { tab = 0 }
                    when (p.tab) {
                        1 -> QueueScreen(data, onPost = { startPost(it) })
                        2 -> PostedScreen(data, onPost = { startPost(it) })
                        3 -> SettingsScreen(data)
                        else -> TodayScreen(data, onStart = { startPost(data.nextPick()) }, onPost = { startPost(it) }, onQueue = { tab = 1 })
                    }
                }
            }
        }
        // The bar slides away in post mode and on the Done screen, and back after.
        AnimatedVisibility(
            visible = place is TabPlace,
            enter = slideInVertically(tween(Motion.SCREEN, easing = Motion.EaseOut)) { it } + fadeIn(),
            exit = slideOutVertically(tween(Motion.SCREEN, easing = Motion.EaseOut)) { it } + fadeOut()
        ) { BottomBar(tab) { if (it != tab) { haptics.tick(); tab = it } } }
    }
}

/**
 * How one place gives way to another: a tab slides in from the side it sits on; deeper screens
 * (post mode) come in from the right and leave to the right; Done grows in.
 */
private fun move(from: Place, to: Place): ContentTransform {
    val t = tween<IntOffset>(Motion.SCREEN, easing = Motion.EaseDrawer)
    val f = tween<Float>(Motion.SCREEN, easing = Motion.EaseOut)
    if (from is TabPlace && to is TabPlace) {
        val right = to.tab > from.tab
        return (slideInHorizontally(t) { w -> if (right) w / 5 else -w / 5 } + fadeIn(f)) togetherWith
            (slideOutHorizontally(t) { w -> if (right) -w / 5 else w / 5 } + fadeOut(tween(Motion.QUICK)))
    }
    if (to is DonePlace) return (scaleIn(tween(Motion.SCREEN, easing = Motion.EaseOut), 0.9f) + fadeIn(f)) togetherWith fadeOut(f)
    val forward = to.depth > from.depth
    return if (forward) (slideInHorizontally(t) { it } + fadeIn(f)) togetherWith (slideOutHorizontally(t) { -it / 4 } + fadeOut(f))
    else (slideInHorizontally(t) { -it / 4 } + fadeIn(f)) togetherWith (slideOutHorizontally(t) { it } + fadeOut(f))
}

/** The design's bottom bar: white, an orange bar over the current tab that slides along, icons that bounce. */
@Composable
private fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    val line = C.line
    val tabs = listOf("Today" to Icons.Home, "Queue" to Icons.Calendar, "Posted" to Icons.List, "Settings" to Icons.Gear)
    Column(
        Modifier.fillMaxWidth().background(C.bar)
            .drawBehind { drawLine(line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(3.dp)) {
            val w = maxWidth / tabs.size
            val x by animateDpAsState(w * tab, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow), label = "bar")
            Box(Modifier.offset(x = x + w * 0.3f).width(w * 0.4f).height(3.dp).clip(Corners.Pill).background(C.accent))
        }
        Row(Modifier.fillMaxWidth().height(64.dp)) {
            tabs.forEachIndexed { i, (label, icon) ->
                val on = i == tab
                val color by animateColorAsState(if (on) C.accentText else C.muted, tween(Motion.QUICK), label = "tabColor")
                val lift by animateFloatAsState(if (on) 1.12f else 1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "lift")
                Column(
                    Modifier.weight(1f).fillMaxSize().clickable(role = Role.Tab) { onTab(i) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    Icon(icon, color, 22.dp, Modifier.graphicsLayer { scaleX = lift; scaleY = lift })
                    Text(label, fontFamily = Body, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = color)
                }
            }
        }
    }
}
