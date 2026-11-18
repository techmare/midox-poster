package ma.midoxshop.poster

import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay
import ma.midoxshop.poster.ui.BigButton
import ma.midoxshop.poster.ui.C
import ma.midoxshop.poster.ui.Chip
import ma.midoxshop.poster.ui.PosterTheme
import java.util.Calendar

/**
 * The loud nag: the whole screen, a pulsing glow, the alarm sound and a buzz. The easy way out is
 * to post; snoozing needs a 3-second hold (3 times a day at most), and pausing the day needs a reason.
 * Back does nothing. The colour, words and buzz change every day, so it never becomes background noise.
 */
class AlarmActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() {} })
        val test = intent.getBooleanExtra("test", false)
        startNoise()
        setContent { PosterTheme { AlarmScreen(test) } }
    }

    private fun startNoise() {
        val day = Days.today()
        runCatching {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                if (Build.VERSION.SDK_INT >= 28) isLooping = true
                play()
            }
        }
        // A different buzz each day.
        val patterns = listOf(
            longArrayOf(0, 500, 300, 500, 1200),
            longArrayOf(0, 150, 100, 150, 100, 150, 900),
            longArrayOf(0, 900, 600),
            longArrayOf(0, 250, 120, 600, 1000)
        )
        runCatching {
            vibrator = getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createWaveform(patterns[Math.floorMod(day.hashCode(), patterns.size)], 0))
        }
    }

    private fun stopNoise() {
        runCatching { ringtone?.stop() }
        runCatching { vibrator?.cancel() }
    }

    override fun onDestroy() { stopNoise(); super.onDestroy() }

    private fun finishWith(after: () -> Unit) {
        stopNoise()
        NotificationManagerCompat.from(this).cancel(Nag.NOTE_ID)
        after()
        Nag.schedule(this)
        finish()
    }

    private fun nowMinute() = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

    @Composable
    private fun AlarmScreen(test: Boolean) {
        val day = Days.today()
        val n = remember { if (test) 0 else Store.nagNumber(this) }
        val tints = listOf(Color(0xFFEA580C), Color(0xFFDB2777), Color(0xFF7C3AED), Color(0xFF0891B2), Color(0xFFCA8A04))
        val tint = tints[Math.floorMod(day.hashCode() + n, tints.size)]
        val glow by rememberInfiniteTransition(label = "glow").animateFloat(
            0.35f, 0.95f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse), label = "glow"
        )
        val left = (Store.min(this) - Store.postedToday(this)).coerceAtLeast(1)
        val snoozes = Store.snoozesLeft(this)
        var pausing by remember { mutableStateOf(false) }

        // Two minutes of ringing, then it goes quiet (the next one comes in half an hour).
        LaunchedEffect(Unit) { delay(120_000); stopNoise() }

        Box(
            Modifier.fillMaxSize().background(C.dark)
                .background(Brush.radialGradient(listOf(tint.copy(alpha = glow), C.dark), radius = 1400f))
        ) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(56.dp))
                Text(if (test) "This is how the alarm looks" else "Post time", fontSize = 17.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text(NagPlan.line(day, n), fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                val streak = Store.streak(this@AlarmActivity).days
                Text(
                    if (streak > 0) "Your $streak day streak needs $left post" + (if (left > 1) "s" else "") + " today." else "Start a streak tonight.",
                    fontSize = 17.sp, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center
                )
                Spacer(Modifier.weight(1f))
                if (!pausing) {
                    BigButton("Post 1 now · 2 min", color = Color.White, textColor = C.ink, height = 72.dp) {
                        finishWith {
                            startActivity(Intent(this@AlarmActivity, MainActivity::class.java).putExtra("go", "post").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (test) BigButton("Close", color = Color.White.copy(alpha = 0.15f), height = 56.dp) { stopNoise(); finish() }
                    else if (snoozes > 0) HoldButton("Hold 3 s to snooze 15 min · $snoozes left") {
                        finishWith { Store.snooze(this@AlarmActivity, (nowMinute() + 15).coerceAtMost(24 * 60 - 1)) }
                    }
                    if (!test) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "I can't today", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .pointerInput(Unit) { detectTapGestures { pausing = true } }
                                .semantics { contentDescription = "Pause today" }
                                .padding(16.dp)
                        )
                    }
                } else {
                    Text("Why not today?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    val reasons = listOf("Riding / driving", "At work", "Sick", "Out with people", "No energy")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        reasons.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { r -> Chip(r, false) { finishWith { Store.pause(this@AlarmActivity, r) } } }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Back", fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.pointerInput(Unit) { detectTapGestures { pausing = false } }.padding(16.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    /** A button that only works when held: snoozing should take a moment of choosing. */
    @Composable
    private fun HoldButton(label: String, onDone: () -> Unit) {
        var progress by remember { mutableFloatStateOf(0f) }
        var holding by remember { mutableStateOf(false) }
        LaunchedEffect(holding) {
            if (!holding) { progress = 0f; return@LaunchedEffect }
            val start = System.currentTimeMillis()
            while (holding) {
                progress = ((System.currentTimeMillis() - start) / 3000f).coerceAtMost(1f)
                if (progress >= 1f) { holding = false; onDone(); break }
                delay(16)
            }
        }
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.15f))
                .semantics { contentDescription = label }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        holding = true
                        tryAwaitRelease()
                        holding = false
                    })
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(Color.White.copy(alpha = 0.3f)))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
