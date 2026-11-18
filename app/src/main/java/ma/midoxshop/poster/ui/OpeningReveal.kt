package ma.midoxshop.poster.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.window.SplashScreenView
import androidx.annotation.RequiresApi
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow

/**
 * The opening animation, when Poster is opened from its icon (Android 12 and up), built like Midox's.
 *
 * The splash screen is Poster's ring on the page colour (res/drawable/splash_logo.xml). Instead of
 * letting Android fade it away, Poster takes that very splash and flies through the ring: it pulls back
 * a little, then rushes forward; the middle of the ring clears into a window, the app is seen through
 * it with the ring as its frame, and the ring keeps growing until the window is bigger than the screen.
 * The app behind settles from 94% as it comes through (LaunchSettle). One soft buzz as it starts forward.
 */
@RequiresApi(31)
fun playOpening(splash: SplashScreenView, onForward: () -> Unit) {
    val logo = splash.iconView
    if (logo == null || logo.width == 0) {
        onForward()
        splash.animate().alpha(0f).setDuration(300).withEndAction { splash.remove() }.start()
        return
    }
    // The ring's middle is the middle of the picture.
    logo.pivotX = logo.width / 2f
    logo.pivotY = logo.height / 2f
    val logoAt = IntArray(2).also { logo.getLocationInWindow(it) }
    val splashAt = IntArray(2).also { splash.getLocationInWindow(it) }
    val cx = logoAt[0] - splashAt[0] + logo.width / 2f
    val cy = logoAt[1] - splashAt[1] + logo.height / 2f
    // The window is the inside of the ring. It has grown enough when that passes the furthest corner.
    val face = logo.width * RING_INSIDE
    val through = 1.05f * hypot(max(cx, splash.width - cx), max(cy, splash.height - cy)) / face

    val window = WindowHole(splash.context, cx, cy)
    splash.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    splash.addView(window, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

    fun forward() {
        buzz(splash)
        onForward()
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = OPEN
            interpolator = LinearInterpolator()
            addUpdateListener {
                val t = it.animatedValue as Float
                // Each moment so many times bigger than the last: the feel of flying toward the ring and through it.
                val s = PULL * (through / PULL).pow(t.pow(1.3f))
                logo.scaleX = s
                logo.scaleY = s
                // A little turn as it comes, like the ring is spinning up.
                logo.rotation = -40f * (1f - (1f - t).pow(3))
                // The middle clears in the first fifth (the arrow goes with it), then the window simply is the ring.
                val clear = (t / 0.2f).coerceAtMost(1f).let { x -> x * x * (3f - 2f * x) }
                window.radius = face * s * clear
                // Let the ring go before it gets big enough to look soft; the page stays, so the window keeps its edge.
                logo.alpha = 1f - ((s - 4f) / 5f).coerceIn(0f, 1f)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = splash.remove()
            })
        }.start()
    }

    whenCalm {
        logo.animate().scaleX(PULL).scaleY(PULL).rotation(8f)
            .setDuration(PULL_MS).setInterpolator(PathInterpolator(0.23f, 1f, 0.32f, 1f))
            .withEndAction { forward() }.start()
    }
}

/** A round hole in the splash: it erases what the splash has drawn under it, the app shows through. */
private class WindowHole(context: Context, private val cx: Float, private val cy: Float) : View(context) {
    private val erase = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    var radius = 0f
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        if (radius > 0f) canvas.drawCircle(cx, cy, radius, erase)
    }
}

/** One soft rising push on phones that have it (a Pixel), a tick on others. */
private fun buzz(view: View) {
    if (Build.VERSION.SDK_INT >= 33) {
        val played = runCatching {
            val v = view.context.getSystemService(Vibrator::class.java)
            val rise = VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
            if (v == null || !v.areAllPrimitivesSupported(rise)) false
            else {
                v.vibrate(VibrationEffect.startComposition().addPrimitive(rise, 0.5f).compose(), VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
                true
            }
        }.getOrDefault(false)
        if (played) return
    }
    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

/** Waits for two smooth frames in a row (or half a second): starting while the app is still busy would stutter. */
private fun whenCalm(then: () -> Unit) {
    val frames = Choreographer.getInstance()
    var first = 0L
    var last = 0L
    var calm = 0
    frames.postFrameCallback(object : Choreographer.FrameCallback {
        override fun doFrame(now: Long) {
            if (first == 0L) first = now
            if (last != 0L) calm = if (now - last < 34_000_000L) calm + 1 else 0
            last = now
            if (calm >= 2 || now - first > 500_000_000L) then() else frames.postFrameCallback(this)
        }
    })
}

// splash_logo.xml: a 108 square, the ring's inside edge 20.5 from the middle (radius 24, 7 wide).
private const val RING_INSIDE = 20.5f / 108f
private const val PULL = 0.86f
private const val PULL_MS = 200L
private const val OPEN = 820L
