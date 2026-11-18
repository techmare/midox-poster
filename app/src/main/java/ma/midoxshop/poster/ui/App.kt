package ma.midoxshop.poster.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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

/** Where you are: the tabs, post mode on a product, or the "Posted" screen after one. */
private sealed interface Place { val depth: Int }
private data object TabsPlace : Place { override val depth = 0 }
private data class PostPlace(val id: String) : Place { override val depth = 1 }
private data class DonePlace(val id: String) : Place { override val depth = 2 }

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
    // Keep the home-screen widget in step with what you see here.
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
        else -> TabsPlace
    }

    AnimatedContent(
        targetState = place,
        modifier = Modifier.fillMaxSize().background(C.ground),
        transitionSpec = { slide(targetState.depth > initialState.depth) },
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
            TabsPlace -> Column(Modifier.fillMaxSize().background(C.ground)) {
                Box(Modifier.weight(1f).statusBarsPadding()) {
                    // Tabs switch instantly: no animation for something you do all day.
                    when (tab) {
                        1 -> QueueScreen(data, onPost = { startPost(it) })
                        2 -> PostedScreen(data, onPost = { startPost(it) })
                        3 -> SettingsScreen(data)
                        else -> TodayScreen(data, onStart = { startPost(data.nextPick()) }, onPost = { startPost(it) }, onQueue = { tab = 1 })
                    }
                }
                BackHandler(enabled = tab != 0) { tab = 0 }
                BottomBar(tab) { if (it != tab) { haptics.tick(); tab = it } }
            }
        }
    }
}

/** Deeper screens slide in from the right; going back slides them away. Midox's curve and timing. */
private fun slide(forward: Boolean): ContentTransform {
    val t = tween<IntOffset>(Motion.SCREEN, easing = Motion.EaseDrawer)
    val f = tween<Float>(Motion.SCREEN, easing = Motion.EaseOut)
    return if (forward) (slideInHorizontally(t) { it } + fadeIn(f)) togetherWith (slideOutHorizontally(t) { -it / 4 } + fadeOut(f))
    else (slideInHorizontally(t) { -it / 4 } + fadeIn(f)) togetherWith (slideOutHorizontally(t) { it } + fadeOut(f))
}

/** Midox's bottom bar: cream with a hairline on top, a soft coral pill on the current tab. */
@Composable
private fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    val line = C.line
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = C.accent, selectedTextColor = C.ink, indicatorColor = C.accentSoft,
        unselectedIconColor = C.muted, unselectedTextColor = C.muted
    )
    NavigationBar(
        containerColor = C.bar, tonalElevation = 0.dp,
        modifier = Modifier.drawBehind { drawLine(line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
    ) {
        listOf("Today" to Icons.Sun, "Queue" to Icons.Calendar, "Posted" to Icons.List, "Settings" to Icons.Gear).forEachIndexed { i, (label, icon) ->
            NavigationBarItem(
                selected = tab == i, onClick = { onTab(i) }, colors = colors,
                icon = { androidx.compose.material3.Icon(icon, null) }, label = { Text(label) }
            )
        }
    }
}
