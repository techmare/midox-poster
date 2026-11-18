package ma.midoxshop.poster.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ma.midoxshop.poster.Days
import ma.midoxshop.poster.Item
import ma.midoxshop.poster.Midox
import ma.midoxshop.poster.Posts
import ma.midoxshop.poster.Store

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

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var posting by rememberSaveable { mutableStateOf<String?>(null) }
    var done by rememberSaveable { mutableStateOf<String?>(null) }

    // A nag ("post") or Midox's Post button ("pid:<id>") brings you here.
    LaunchedEffect(request.value, items) {
        val r = request.value ?: return@LaunchedEffect
        if (items == null && r.startsWith("pid:")) return@LaunchedEffect
        when {
            r == "post" -> { done = null; posting = data.nextPick()?.id }
            r.startsWith("pid:") -> { done = null; posting = r.removePrefix("pid:") }
        }
        request.value = null
    }

    val startPost = { it: Item? -> done = null; posting = it?.id; if (it == null) tab = 1 }

    Box(Modifier.fillMaxSize().background(C.ground)) {
        val p = posting?.let { data.byId(it) }
        when {
            done != null -> {
                BackHandler { done = null }
                DoneScreen(data, onNext = { startPost(data.nextPick()) }, onStop = { done = null; tab = 0 })
            }
            posting != null && p != null -> {
                BackHandler { posting = null }
                PostScreen(data, p, onClose = { posting = null }, onPosted = { posting = null; done = p.id }, onSkip = {
                    Store.skip(ctx, data.today, p.id)
                    posting = Data(items, ctx).nextPick()?.id
                })
            }
            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).statusBarsPadding()) {
                    when (tab) {
                        1 -> QueueScreen(data, onPost = { startPost(it) })
                        2 -> PostedScreen(data, onPost = { startPost(it) })
                        3 -> SettingsScreen(data)
                        else -> TodayScreen(data, onStart = { startPost(data.nextPick()) }, onPost = { startPost(it) }, onQueue = { tab = 1 })
                    }
                }
                BottomNav(tab) { tab = it }
            }
        }
    }
}

@Composable
private fun BottomNav(tab: Int, onTab: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(C.card).windowInsetsPadding(WindowInsets.navigationBars).height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Today", "Queue", "Posted", "Settings").forEachIndexed { i, label ->
            Box(
                Modifier.weight(1f).fillMaxSize().clickable(role = Role.Tab) { onTab(i) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.height(4.dp).fillMaxWidth(0.4f).background(if (i == tab) C.accent else C.card))
                    Text(label, fontSize = 13.sp, fontWeight = if (i == tab) FontWeight.Bold else FontWeight.Medium, color = if (i == tab) C.accentText else C.muted)
                }
            }
        }
    }
}

/** Page title at the top of a tab. */
@Composable
fun Title(text: String, sub: String? = null) {
    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
        if (sub != null) Text(sub, fontSize = 13.sp, color = C.muted, fontWeight = FontWeight.Medium)
        Text(text, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = C.ink)
    }
}
