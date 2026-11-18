package ma.midoxshop.poster.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ma.midoxshop.poster.Item
import ma.midoxshop.poster.Midox
import ma.midoxshop.poster.R

// Poster's own look, from its first design: a warm off-white page, near-black ink, one orange for the
// thing to do now, teal for done, a dark card for today's ring. Space Grotesk for headings and numbers,
// Inter for everything you read or tap. At night: a near-black page with the same orange.

val Display = FontFamily(
    Font(R.font.spacegrotesk_500, FontWeight.Medium),
    Font(R.font.spacegrotesk_700, FontWeight.Bold)
)
val Body = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold)
)

@Immutable
data class PosterColors(
    val ground: Color, val card: Color, val line: Color, val divider: Color, val tonal: Color,
    val ink: Color, val muted: Color,
    val accent: Color, val onAccent: Color, val accentText: Color, val accentSoft: Color, val accentInk: Color, val glow: Color,
    val good: Color, val goodSoft: Color, val warn: Color,
    val chipOn: Color, val chipOnText: Color, val bar: Color,
    // The dark card (today's ring) and the Done screen: dark by day, a step lighter than the page at night.
    val hero: Color, val hero2: Color, val heroTrack: Color, val onHero: Color, val heroMuted: Color,
    val viewer: Color
)

private val Light = PosterColors(
    ground = Color(0xFFF4F2EC), card = Color(0xFFFFFFFF), line = Color(0xFFE7E4DC), divider = Color(0xFFEFECE5), tonal = Color(0xFFE7E4DC),
    ink = Color(0xFF15171C), muted = Color(0xFF5B5F68),
    accent = Color(0xFFEA580C), onAccent = Color(0xFFFFFFFF), accentText = Color(0xFFC2410C), accentSoft = Color(0xFFFFEDD5), accentInk = Color(0xFF9A3412), glow = Color(0xFFFB923C),
    good = Color(0xFF0F766E), goodSoft = Color(0xFFCCFBF1), warn = Color(0xFF9A3412),
    chipOn = Color(0xFF15171C), chipOnText = Color(0xFFFFFFFF), bar = Color(0xFFFFFFFF),
    hero = Color(0xFF15171C), hero2 = Color(0xFF22252B), heroTrack = Color(0xFF2C2F36), onHero = Color(0xFFFFFFFF), heroMuted = Color(0xFFC9CBD1),
    viewer = Color(0xFF0E0F12)
)

private val Dark = PosterColors(
    ground = Color(0xFF0F1013), card = Color(0xFF1A1C21), line = Color(0xFF2A2D34), divider = Color(0xFF23262C), tonal = Color(0xFF23262C),
    ink = Color(0xFFF4F2EC), muted = Color(0xFF9EA2AB),
    accent = Color(0xFFEA580C), onAccent = Color(0xFFFFFFFF), accentText = Color(0xFFFB923C), accentSoft = Color(0xFF3A2314), accentInk = Color(0xFFFDBA74), glow = Color(0xFFFB923C),
    good = Color(0xFF2DD4BF), goodSoft = Color(0xFF123B37), warn = Color(0xFFFDBA74),
    chipOn = Color(0xFFF4F2EC), chipOnText = Color(0xFF15171C), bar = Color(0xFF15171A),
    hero = Color(0xFF1C1F25), hero2 = Color(0xFF262A31), heroTrack = Color(0xFF30343C), onHero = Color(0xFFFFFFFF), heroMuted = Color(0xFFB5B9C2),
    viewer = Color(0xFF0B0C0E)
)

val LocalPoster = staticCompositionLocalOf { Light }

/** Short access to the colours: C.ink, C.card… */
object C {
    private val c: PosterColors @Composable get() = LocalPoster.current
    val ground: Color @Composable get() = c.ground
    val card: Color @Composable get() = c.card
    val line: Color @Composable get() = c.line
    val divider: Color @Composable get() = c.divider
    val tonal: Color @Composable get() = c.tonal
    val ink: Color @Composable get() = c.ink
    val muted: Color @Composable get() = c.muted
    val accent: Color @Composable get() = c.accent
    val onAccent: Color @Composable get() = c.onAccent
    val accentText: Color @Composable get() = c.accentText
    val accentSoft: Color @Composable get() = c.accentSoft
    val accentInk: Color @Composable get() = c.accentInk
    val glow: Color @Composable get() = c.glow
    val good: Color @Composable get() = c.good
    val goodSoft: Color @Composable get() = c.goodSoft
    val warn: Color @Composable get() = c.warn
    val chipOn: Color @Composable get() = c.chipOn
    val chipOnText: Color @Composable get() = c.chipOnText
    val bar: Color @Composable get() = c.bar
    val hero: Color @Composable get() = c.hero
    val hero2: Color @Composable get() = c.hero2
    val heroTrack: Color @Composable get() = c.heroTrack
    val onHero: Color @Composable get() = c.onHero
    val heroMuted: Color @Composable get() = c.heroMuted
    val viewer: Color @Composable get() = c.viewer
}

/** The first design's corners: pills, 14 for small photos, 20 for cards and buttons, 28 for the big card. */
object Corners {
    val Pill = RoundedCornerShape(percent = 50)
    val Small = RoundedCornerShape(14.dp)
    val Card = RoundedCornerShape(20.dp)
    val Big = RoundedCornerShape(28.dp)
}

private fun typography(): Typography {
    val b = Typography()
    fun TextStyle.body() = copy(fontFamily = Body, fontFeatureSettings = "tnum")
    fun TextStyle.display() = copy(fontFamily = Display, fontWeight = FontWeight.Bold)
    return b.copy(
        displayLarge = b.displayLarge.display(), displayMedium = b.displayMedium.display(), displaySmall = b.displaySmall.display(),
        headlineLarge = b.headlineLarge.display(), headlineMedium = b.headlineMedium.display(), headlineSmall = b.headlineSmall.display(),
        titleLarge = b.titleLarge.display(), titleMedium = b.titleMedium.body(), titleSmall = b.titleSmall.body(),
        bodyLarge = b.bodyLarge.body(), bodyMedium = b.bodyMedium.body().copy(fontSize = 15.sp), bodySmall = b.bodySmall.body(),
        labelLarge = b.labelLarge.body(), labelMedium = b.labelMedium.body(), labelSmall = b.labelSmall.body()
    )
}
private val Type = typography()

@Composable
fun PosterTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) Dark else Light
    val scheme = if (dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentSoft, background = c.ground, onBackground = c.ink,
        surface = c.card, onSurface = c.ink, surfaceVariant = c.tonal, onSurfaceVariant = c.muted, outline = c.muted, outlineVariant = c.line
    ) else lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentSoft, background = c.ground, onBackground = c.ink,
        surface = c.card, onSurface = c.ink, surfaceVariant = c.tonal, onSurfaceVariant = c.muted, outline = c.muted, outlineVariant = c.line
    )
    CompositionLocalProvider(LocalPoster provides c) {
        MaterialTheme(colorScheme = scheme, typography = Type) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = c.ink), content)
        }
    }
}

// ---- Motion ----

object Motion {
    const val PRESS = 120
    const val QUICK = 220
    const val SCREEN = 340
    const val STAGGER = 40L
    val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val EaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
    val EaseDrawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
}

/**
 * Whether the opening animation is over. Things that rise in when a screen appears wait for it, so they
 * play where you can see them, not under the splash.
 */
object Launch {
    var ready by mutableStateOf(true)
}

/**
 * Rises and fades in when it first appears, [index] steps after the first one: lists come in one row
 * after the other instead of all at once.
 */
fun Modifier.enter(index: Int = 0, rise: Dp = 18.dp): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val y = remember { Animatable(1f) }
    val px = with(LocalDensity.current) { rise.toPx() }
    LaunchedEffect(Unit) {
        snapshotFlow { Launch.ready }.first { it }
        delay(Motion.STAGGER * index.coerceAtMost(10))
        launch { alpha.animateTo(1f, tween(Motion.QUICK + 80, easing = Motion.EaseOut)) }
        y.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
    }
    graphicsLayer { this.alpha = alpha.value; translationY = y.value * px }
}

/** Grows from 92% with a soft spring when it first appears (cards, the big ring). */
fun Modifier.pop(delayMs: Long = 0): Modifier = composed {
    val s = remember { Animatable(0.92f) }
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { Launch.ready }.first { it }
        delay(delayMs)
        launch { a.animateTo(1f, tween(Motion.QUICK, easing = Motion.EaseOut)) }
        s.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
    }
    graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = a.value }
}

/** Shrinks a little while pressed, and springs back. */
fun Modifier.pressScale(source: InteractionSource, to: Float = 0.96f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) to else 1f,
        if (pressed) tween(Motion.PRESS, easing = Motion.EaseOut) else spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Tappable, with the press animation and the ripple. */
@Composable
fun Modifier.tap(role: Role = Role.Button, onClick: () -> Unit): Modifier {
    val press = remember { MutableInteractionSource() }
    return this.pressScale(press).clickable(interactionSource = press, indication = LocalIndication.current, role = role, onClick = onClick)
}

fun Modifier.clickableRole(onClick: () -> Unit): Modifier = this.clickable(role = Role.Button, onClick = onClick)

/** A number that rolls up (or down) when it changes. */
@Composable
fun Rolling(value: String, size: TextUnit, color: Color, family: FontFamily = Display, weight: FontWeight = FontWeight.Bold, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = value, modifier = modifier, label = "roll",
        transitionSpec = {
            val up = (targetState.toIntOrNull() ?: 0) >= (initialState.toIntOrNull() ?: 0)
            (slideInVertically(tween(Motion.QUICK, easing = Motion.EaseOut)) { if (up) it else -it } + fadeIn(tween(Motion.QUICK))) togetherWith
                (slideOutVertically(tween(Motion.QUICK, easing = Motion.EaseOut)) { if (up) -it else it } + fadeOut(tween(Motion.QUICK / 2))) using
                SizeTransform(clip = false)
        }
    ) { Text(it, fontFamily = family, fontWeight = weight, fontSize = size, color = color, maxLines = 1) }
}

// ---- Haptics ----

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

// ---- Icons (stroke line icons, like the design) ----

object Icons {
    private fun icon(name: String, vararg paths: String, stroke: Float = 2f): ImageVector {
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

    val Home by lazy { icon("home", "M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z") }
    val Calendar by lazy { icon("calendar", "M6 4h12a3 3 0 0 1 3 3v11a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3z", "M3 9h18M8 2v4M16 2v4") }
    val List by lazy { icon("list", "M4 6h16M4 12h16M4 18h10") }
    val Gear by lazy { icon("gear", circle(12f, 12f, 3f), "M12 3.5v2M12 18.5v2M3.5 12h2M18.5 12h2M6 6l1.4 1.4M16.6 16.6L18 18M6 18l1.4-1.4M16.6 7.4L18 6") }
    val Check by lazy { icon("check", "M5 12l5 5 9-10", stroke = 2.6f) }
    val Close by lazy { icon("close", "M6 6l12 12M18 6L6 18", stroke = 2.2f) }
    val Flame by lazy { icon("flame", "M12 2c1 4 5 6 5 11a5 5 0 0 1-10 0c0-3 2-4 2-7 2 1 3 3 3 5", stroke = 2.2f) }
    val Copy by lazy { icon("copy", "M10 8h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-8a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z", "M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2") }
    val Arrow by lazy { icon("arrow", "M5 12h14M13 6l6 6-6 6", stroke = 2.4f) }
    val Camera by lazy { icon("camera", "M4 8h3l2-3h6l2 3h3v11H4z", circle(12f, 13f, 3.5f)) }
    val Clock by lazy { icon("clock", circle(12f, 12f, 9f), "M12 7v5l3 2") }
    val Sparkle by lazy { icon("sparkle", "M12 3l2.5 5.5L20 11l-5.5 2.5L12 19l-2.5-5.5L4 11l5.5-2.5z", stroke = 2.2f) }
}

@Composable
fun Icon(v: ImageVector, tint: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) =
    androidx.compose.material3.Icon(v, contentDescription = null, tint = tint, modifier = modifier.size(size))

// ---- Parts ----

/** The orange action: 64dp, Space Grotesk, 20dp corners. */
@Composable
fun BigButton(
    text: String, modifier: Modifier = Modifier, color: Color = C.accent, textColor: Color = C.onAccent,
    height: Dp = 64.dp, arrow: Boolean = false, onClick: () -> Unit
) {
    val bg by animateColorAsState(color, tween(Motion.QUICK), label = "btn")
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().height(height).tap(onClick = onClick).clip(Corners.Card).background(bg).padding(horizontal = 16.dp)
    ) {
        Text(text, color = textColor, fontFamily = Display, fontSize = if (height >= 60.dp) 19.sp else 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
        if (arrow) Icon(Icons.Arrow, textColor, 20.dp)
    }
}

/** White, for everything that isn't the main action. */
@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().height(52.dp).tap(onClick = onClick).clip(Corners.Card).background(C.card).padding(horizontal = 12.dp)
    ) { Text(text, color = C.ink, fontFamily = Body, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

/** A small dark (light at night) button for a card's own action. */
@Composable
fun InkButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.heightIn(min = 44.dp).tap(onClick = onClick).clip(Corners.Small).background(C.chipOn).padding(horizontal = 16.dp)
    ) { Text(text, color = C.chipOnText, fontFamily = Body, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

/** A round status pill (Ready, Done, No photo) that crossfades when it changes. */
@Composable
fun Pill(text: String, fg: Color, bg: Color) {
    AnimatedContent(
        targetState = Triple(text, fg, bg), label = "pill",
        transitionSpec = { (scaleIn(tween(Motion.QUICK), 0.8f) + fadeIn(tween(Motion.QUICK))) togetherWith (scaleOut(tween(Motion.QUICK / 2), 0.8f) + fadeOut(tween(Motion.QUICK / 2))) }
    ) { (t, f, b) ->
        Text(t, color = f, fontFamily = Body, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(Corners.Pill).background(b).padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun Thumb(item: Item?, size: Dp = 56.dp) {
    Box(Modifier.size(size).clip(Corners.Small).background(C.tonal), contentAlignment = Alignment.Center) {
        val pic = item?.pics?.firstOrNull()
        if (pic != null) AsyncImage(Midox.photo(pic), null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        else Icon(Icons.Camera, C.muted, 20.dp)
    }
}

/** A product as a white card: photo, name, a line under it, something on the right. */
@Composable
fun ItemRow(item: Item, sub: String, subColor: Color = C.muted, onClick: (() -> Unit)? = null, right: @Composable RowScope.() -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.tap(onClick = onClick) else Modifier)
            .clip(Corners.Card).background(C.card).heightIn(min = 80.dp).padding(12.dp)
    ) {
        Thumb(item)
        Column(Modifier.weight(1f)) {
            Text(item.name, fontFamily = Body, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, fontFamily = Body, fontSize = 13.sp, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        right()
    }
}

/** DAILY GOAL, NUDGES…: small bold muted capitals. */
@Composable
fun Section(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontFamily = Body, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.ink, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun Label(text: String) {
    Text(
        text.uppercase(), fontFamily = Body, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.muted, letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
    )
}

/** The design's switch: an orange pill with a white knob that slides over on a spring. */
@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val h = rememberHaptics()
    val x by animateDpAsState(if (on) 22.dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "knob")
    val bg by animateColorAsState(if (on) C.accent else C.line, tween(Motion.QUICK), label = "track")
    Box(
        Modifier.size(52.dp, 30.dp).clip(Corners.Pill).background(bg).clickable(role = Role.Switch) { h.toggle(!on); onChange(!on) }.padding(3.dp)
    ) { Box(Modifier.offset(x = x).size(24.dp).clip(Corners.Pill).background(Color.White)) }
}

/** − n + with a rolling number and a tick on each change. */
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit) {
    val h = rememberHaptics()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepButton("−", "Less") { h.tick(); onChange(value - 1) }
        Box(Modifier.width(18.dp), contentAlignment = Alignment.Center) { Rolling("$value", 17.sp, C.ink) }
        StepButton("+", "More") { h.tick(); onChange(value + 1) }
    }
}

@Composable
private fun StepButton(label: String, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).tap(onClick = onClick).clip(RoundedCornerShape(12.dp)).background(C.ground)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center
    ) { Text(label, fontFamily = Body, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.ink) }
}

/** A chip: white, dark when chosen, the colour sliding over. */
@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) C.chipOn else C.card, tween(Motion.QUICK), label = "chipBg")
    val fg by animateColorAsState(if (on) C.chipOnText else C.ink, tween(Motion.QUICK), label = "chipFg")
    Text(
        text, fontFamily = Body, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg,
        modifier = Modifier.tap(onClick = onClick).clip(RoundedCornerShape(12.dp)).background(bg)
            .border(1.dp, if (on) Color.Transparent else C.line, RoundedCornerShape(12.dp))
            .heightIn(min = 44.dp).padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

/** The design's segmented control: a track with a white tab that slides to your choice. */
@Composable
fun Segments(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val h = rememberHaptics()
    BoxWithConstraints(Modifier.fillMaxWidth().clip(Corners.Small).background(C.tonal).padding(4.dp)) {
        val n = options.size
        val w = (maxWidth - 4.dp * (n - 1)) / n
        val x by animateDpAsState((w + 4.dp) * selected, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "seg")
        Box(Modifier.offset(x = x).width(w).height(40.dp).clip(RoundedCornerShape(10.dp)).background(C.card))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                val fg by animateColorAsState(if (on) C.ink else C.muted, tween(Motion.QUICK), label = "segFg")
                Box(
                    Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Tab) { if (!on) { h.tick(); onSelect(i) } },
                    contentAlignment = Alignment.Center
                ) { Text(label, fontFamily = Body, fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, color = fg, maxLines = 1) }
            }
        }
    }
}

/** The page title: Space Grotesk 28, an optional line above, and something on the right. */
@Composable
fun Title(text: String, sub: String? = null, right: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp).enter(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (sub != null) Text(sub, fontFamily = Body, fontSize = 14.sp, color = C.muted, fontWeight = FontWeight.Medium)
            Text(text, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = C.ink)
        }
        right()
    }
}
