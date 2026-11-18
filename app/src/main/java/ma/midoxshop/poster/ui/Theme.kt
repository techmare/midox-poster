package ma.midoxshop.poster.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ma.midoxshop.poster.Item
import ma.midoxshop.poster.Midox
import ma.midoxshop.poster.R

// Poster wears Midox's look (Midox's DESIGN.md is the source): a warm cream page (warm charcoal at
// night), Source Serif for titles and numbers, DM Sans for everything you read or tap, white cards with
// a warm hairline, and one coral action per screen.

val Serif = FontFamily(
    Font(R.font.sourceserif_regular, FontWeight.Normal),
    Font(R.font.sourceserif_semibold, FontWeight.SemiBold)
)
val DmSans = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_semibold, FontWeight.SemiBold),
    Font(R.font.dmsans_bold, FontWeight.Bold)
)

@Immutable
data class PosterColors(
    val ground: Color,      // the page
    val card: Color,
    val line: Color,        // card hairline
    val divider: Color,
    val tonal: Color,       // info cards, segment track, empty photos
    val ink: Color,
    val muted: Color,
    val accent: Color,      // the one coral action
    val onAccent: Color,
    val accentSoft: Color,  // the progress pill behind the current tab
    val good: Color,        // posted, ready, all is well
    val goodSoft: Color,
    val warn: Color,        // missing photo, sinking post
    val chipOn: Color,      // a selected chip, a card's own action
    val chipOnText: Color,
    val bar: Color,         // bottom bar
    val viewer: Color,      // the alarm: near black, day and night
    val onViewer: Color
)

private val Light = PosterColors(
    ground = Color(0xFFFAF9F5), card = Color(0xFFFFFFFF), line = Color(0xFFE6DFD8), divider = Color(0xFFEBE6DF),
    tonal = Color(0xFFEFE9DE), ink = Color(0xFF141413), muted = Color(0xFF6C6A64),
    accent = Color(0xFFB85A3A), onAccent = Color(0xFFFFFFFF), accentSoft = Color(0xFFF6E4DB),
    good = Color(0xFF1F7A4D), goodSoft = Color(0xFFE4EFE6), warn = Color(0xFFB3412B),
    chipOn = Color(0xFF141413), chipOnText = Color(0xFFFAF9F5), bar = Color(0xFFF2EEE6),
    viewer = Color(0xFF0E0E0D), onViewer = Color(0xFFFAF9F5)
)

private val Dark = PosterColors(
    ground = Color(0xFF1A1917), card = Color(0xFF262522), line = Color(0xFF34322E), divider = Color(0xFF2E2D2A),
    tonal = Color(0xFF2F2D29), ink = Color(0xFFFAF9F5), muted = Color(0xFFA09D96),
    accent = Color(0xFFD97757), onAccent = Color(0xFF141413), accentSoft = Color(0xFF5A2D1E),
    good = Color(0xFF7BC98F), goodSoft = Color(0xFF1E3326), warn = Color(0xFFE8846B),
    chipOn = Color(0xFF45423C), chipOnText = Color(0xFFFAF9F5), bar = Color(0xFF262522),
    viewer = Color(0xFF0E0E0D), onViewer = Color(0xFFFAF9F5)
)

val LocalPoster = staticCompositionLocalOf { Light }

/** Short access to the colours: C.ink, C.card… */
object C {
    val ground: Color @Composable get() = LocalPoster.current.ground
    val card: Color @Composable get() = LocalPoster.current.card
    val line: Color @Composable get() = LocalPoster.current.line
    val divider: Color @Composable get() = LocalPoster.current.divider
    val tonal: Color @Composable get() = LocalPoster.current.tonal
    val ink: Color @Composable get() = LocalPoster.current.ink
    val muted: Color @Composable get() = LocalPoster.current.muted
    val accent: Color @Composable get() = LocalPoster.current.accent
    val onAccent: Color @Composable get() = LocalPoster.current.onAccent
    val accentSoft: Color @Composable get() = LocalPoster.current.accentSoft
    val good: Color @Composable get() = LocalPoster.current.good
    val goodSoft: Color @Composable get() = LocalPoster.current.goodSoft
    val warn: Color @Composable get() = LocalPoster.current.warn
    val chipOn: Color @Composable get() = LocalPoster.current.chipOn
    val chipOnText: Color @Composable get() = LocalPoster.current.chipOnText
    val bar: Color @Composable get() = LocalPoster.current.bar
    val viewer: Color @Composable get() = LocalPoster.current.viewer
    val onViewer: Color @Composable get() = LocalPoster.current.onViewer
}

/** Midox's five corner sizes, and no others. */
object Corners {
    val Tag = RoundedCornerShape(6.dp)
    val Small = RoundedCornerShape(10.dp)
    val Card = RoundedCornerShape(14.dp)
    val Sheet = RoundedCornerShape(20.dp)
    val Round = RoundedCornerShape(percent = 50)
}

private fun typography(): Typography {
    val b = Typography()
    fun TextStyle.dm() = copy(fontFamily = DmSans, fontFeatureSettings = "tnum")
    fun TextStyle.serif() = copy(fontFamily = Serif, fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum")
    return b.copy(
        displayLarge = b.displayLarge.serif(), displayMedium = b.displayMedium.serif(), displaySmall = b.displaySmall.serif(),
        headlineLarge = b.headlineLarge.serif(), headlineMedium = b.headlineMedium.serif(), headlineSmall = b.headlineSmall.serif(),
        titleLarge = b.titleLarge.serif().copy(fontSize = 22.sp, letterSpacing = (-0.3).sp),
        titleMedium = b.titleMedium.dm().copy(fontWeight = FontWeight.Medium, fontSize = 15.sp),
        titleSmall = b.titleSmall.dm().copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
        bodyLarge = b.bodyLarge.dm().copy(fontSize = 15.sp), bodyMedium = b.bodyMedium.dm().copy(fontSize = 15.sp), bodySmall = b.bodySmall.dm(),
        labelLarge = b.labelLarge.dm().copy(fontWeight = FontWeight.Medium, fontSize = 15.sp),
        labelMedium = b.labelMedium.dm().copy(fontWeight = FontWeight.SemiBold), labelSmall = b.labelSmall.dm().copy(fontWeight = FontWeight.Medium)
    )
}
private val Type = typography()

@Composable
fun PosterTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) Dark else Light
    val scheme = if (dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentSoft, onPrimaryContainer = Color(0xFFF6D3C4),
        background = c.ground, onBackground = c.ink, surface = c.ground, onSurface = c.ink, surfaceVariant = c.line,
        onSurfaceVariant = c.muted, surfaceContainer = c.bar, surfaceContainerHigh = Color(0xFF2E2D2A),
        outline = Color(0xFF7E7A72), outlineVariant = Color(0xFF3B3934)
    ) else lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentSoft, onPrimaryContainer = Color(0xFF3A1206),
        background = c.ground, onBackground = c.ink, surface = c.ground, onSurface = c.ink, surfaceVariant = c.tonal,
        onSurfaceVariant = c.muted, surfaceContainer = c.bar, surfaceContainerHigh = c.tonal,
        outline = Color(0xFFA9A398), outlineVariant = Color(0xFFE0D8CE)
    )
    CompositionLocalProvider(LocalPoster provides c) {
        MaterialTheme(
            colorScheme = scheme, typography = Type,
            shapes = Shapes(extraSmall = Corners.Small, small = Corners.Small, medium = Corners.Card, large = Corners.Sheet, extraLarge = Corners.Sheet)
        ) {
            // DM Sans for every Text that doesn't say otherwise.
            androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = c.ink), content)
        }
    }
}

// ---- Motion and haptics, Midox's tokens ----

object Motion {
    const val PRESS = 120
    const val QUICK = 180
    const val SCREEN = 280
    val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val EaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
    val EaseDrawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
}

/** Shrinks a little while pressed and settles back, with no bounce. */
fun Modifier.pressScale(source: InteractionSource, to: Float = 0.97f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) to else 1f,
        if (pressed) tween(Motion.PRESS, easing = Motion.EaseOut) else spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "press"
    )
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Haptics as punctuation: a tick on − / + and tabs, on / off on a switch, "done" when a post is marked. */
class Haptics(private val view: View) {
    fun tick() = view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK)
    fun done() = view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
    fun toggle(on: Boolean) = view.performHapticFeedback(
        if (Build.VERSION.SDK_INT < 34) HapticFeedbackConstants.CLOCK_TICK else if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
    )
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}

/** A tappable thing that shrinks a touch while pressed. */
@Composable
fun Modifier.tap(role: Role = Role.Button, onClick: () -> Unit): Modifier {
    val press = remember { MutableInteractionSource() }
    return this.pressScale(press).clickable(interactionSource = press, indication = LocalIndication.current, role = role, onClick = onClick)
}

fun Modifier.clickableRole(onClick: () -> Unit): Modifier = this.clickable(role = Role.Button, onClick = onClick)

// ---- Line icons, drawn like Midox's ----

object Icons {
    private fun icon(name: String, vararg paths: String, stroke: Float = 1.9f): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { d ->
            b.addPath(
                pathData = PathParser().parsePathString(d).toNodes(), stroke = SolidColor(Color.Black),
                strokeLineWidth = stroke, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            )
        }
        return b.build()
    }
    private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r},${cy}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0z"
    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r},${y}h${w - 2 * r}a$r,$r 0 0 1 $r,$r" + "v${h - 2 * r}a$r,$r 0 0 1 ${-r},$r" +
            "h${-(w - 2 * r)}a$r,$r 0 0 1 ${-r},${-r}" + "v${-(h - 2 * r)}a$r,$r 0 0 1 $r,${-r}z"

    val Sun by lazy { icon("sun", circle(12f, 12f, 4f), "M12 3v1.5M12 19.5V21M3 12h1.5M19.5 12H21M5.6 5.6l1.1 1.1M17.3 17.3l1.1 1.1M5.6 18.4l1.1-1.1M17.3 6.7l1.1-1.1") }
    val Calendar by lazy { icon("calendar", rect(4f, 5f, 16f, 15f, 2.5f), "M4 10h16M8.5 3v4M15.5 3v4") }
    val Check by lazy { icon("check", "M5 12.5l4.2 4.2L19 7", stroke = 2.4f) }
    val List by lazy { icon("list", "M9 6.5h11M9 12h11M9 17.5h11", "M4.5 6.5h.5M4.5 12h.5M4.5 17.5h.5", stroke = 2f) }
    val Gear by lazy { icon("gear", circle(12f, 12f, 3f), "M12 3.5v2M12 18.5v2M3.5 12h2M18.5 12h2M6 6l1.4 1.4M16.6 16.6L18 18M6 18l1.4-1.4M16.6 7.4L18 6") }
    val Close by lazy { icon("close", "M6 6l12 12M18 6L6 18", stroke = 2.2f) }
    val Flame by lazy { icon("flame", "M12 3c1 4 5 6 5 11a5 5 0 0 1-10 0c0-3 2-4 2-7 2 1 3 3 3 5") }
    val Copy by lazy { icon("copy", rect(8f, 8f, 12f, 12f, 2f), "M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2") }
    val Arrow by lazy { icon("arrow", "M5 12h14M13 6l6 6-6 6", stroke = 2.2f) }
    val Camera by lazy { icon("camera", "M4 8h3l2-3h6l2 3h3v11H4z", circle(12f, 13f, 3.5f)) }
}

@Composable
fun Icon(v: ImageVector, tint: Color, size: Dp = 22.dp) =
    androidx.compose.material3.Icon(v, contentDescription = null, tint = tint, modifier = Modifier.size(size))

// ---- Parts ----

/** The screen's one coral action, full width at the bottom. */
@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, color: Color = C.accent, textColor: Color = C.onAccent, height: Dp = 58.dp, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().height(height).tap(onClick = onClick).clip(Corners.Card).background(color).padding(horizontal = 16.dp)
    ) { Text(text, color = textColor, fontFamily = DmSans, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1) }
}

/** White with a warm outline: everything that isn't the main action. */
@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().height(48.dp).tap(onClick = onClick).clip(Corners.Card).background(C.card)
            .border(1.5.dp, C.line, Corners.Card).padding(horizontal = 12.dp)
    ) { Text(text, color = C.ink, fontFamily = DmSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

/** A card's own way forward when the coral is taken: ink (a soft fill at night). */
@Composable
fun InkButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.heightIn(min = 48.dp).tap(onClick = onClick).clip(Corners.Card).background(C.chipOn).padding(horizontal = 16.dp)
    ) { Text(text, color = C.chipOnText, fontFamily = DmSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

/** Small status tag (Ready, Done, No photo): 6dp corners, 11sp. */
@Composable
fun Tag(text: String, fg: Color, bg: Color) {
    Text(
        text, color = fg, fontFamily = DmSans, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(Corners.Tag).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun Thumb(item: Item?, size: Dp = 48.dp) {
    Box(Modifier.size(size).clip(Corners.Small).background(C.tonal), contentAlignment = Alignment.Center) {
        val pic = item?.pics?.firstOrNull()
        if (pic != null) AsyncImage(Midox.photo(pic), null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        else Icon(Icons.Camera, C.muted, 20.dp)
    }
}

/** One product row: photo, name, a line under it, something on the right. */
@Composable
fun ItemRow(item: Item, sub: String, subColor: Color = C.muted, onClick: (() -> Unit)? = null, right: @Composable RowScope.() -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Thumb(item)
        Column(Modifier.weight(1f)) {
            Text(item.name, fontFamily = DmSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, fontFamily = DmSans, fontSize = 13.sp, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        right()
    }
}

/** A white card with a warm hairline, no shadow. */
@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().clip(Corners.Card).background(C.card).border(1.dp, C.line, Corners.Card)) { content() }
}

/** Rows grouped in one card, with dividers between them. */
@Composable
fun Group(modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    Column(modifier.fillMaxWidth().clip(Corners.Card).background(C.card).border(1.dp, C.line, Corners.Card)) {
        rows.forEachIndexed { i, r ->
            if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 12.dp).height(1.dp).background(C.divider))
            r()
        }
    }
}

/** NEEDS YOU, DAILY GOAL…: DM Sans 12sp, 1.5sp tracking, muted. */
@Composable
fun Section(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp)) {
        Text(text.uppercase(), fontFamily = DmSans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = C.muted, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, fontFamily = DmSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = C.muted)
    }
}

/** Midox's switch: Material's, with the on / off feel under the finger. */
@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val h = rememberHaptics()
    Switch(checked = on, onCheckedChange = { h.toggle(it); onChange(it) })
}

/** − n + in a pill, each button 48dp, a tick on each change. */
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit) {
    val h = rememberHaptics()
    Row(
        Modifier.clip(Corners.Round).background(C.tonal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("−" to -1, "+" to 1).forEachIndexed { i, (label, d) ->
            if (i == 1) Text("$value", fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = C.ink, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
            Box(Modifier.size(48.dp).clip(Corners.Round).clickable(role = Role.Button) { h.tick(); onChange(value + d) }, contentAlignment = Alignment.Center) {
                Text(label, fontFamily = DmSans, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = C.ink)
            }
        }
    }
}

/** A 34dp pill, white with a line; ink when chosen. */
@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(
            text, fontFamily = DmSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (on) C.chipOnText else C.ink,
            modifier = Modifier.clip(Corners.Round).background(if (on) C.chipOn else C.card)
                .then(if (on) Modifier else Modifier.border(1.dp, C.line, Corners.Round))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/** A tonal track with a white tab on the chosen option (Live · Repost due · Sold). */
@Composable
fun Segments(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Corners.Card).background(C.tonal).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(42.dp).clip(Corners.Small)
                    .then(if (on) Modifier.background(C.card).border(1.dp, C.line, Corners.Small) else Modifier)
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center
            ) { Text(label, fontFamily = DmSans, fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, color = if (on) C.ink else C.muted, maxLines = 1) }
        }
    }
}

/** The serif title on the cream top of every tab (30sp, 40sp for Today). */
@Composable
fun Title(text: String, sub: String? = null, big: Boolean = false, right: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            if (sub != null) Text(sub, fontFamily = DmSans, fontSize = 13.sp, color = C.muted, fontWeight = FontWeight.Medium)
            Text(
                text, fontFamily = Serif, fontWeight = FontWeight.Normal, color = C.ink,
                fontSize = if (big) 40.sp else 30.sp, letterSpacing = if (big) (-1).sp else (-0.6).sp
            )
        }
        right()
    }
}
