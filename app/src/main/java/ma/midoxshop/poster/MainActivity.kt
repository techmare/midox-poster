package ma.midoxshop.poster

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Choreographer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import ma.midoxshop.poster.ui.App
import ma.midoxshop.poster.ui.Launch
import ma.midoxshop.poster.ui.LaunchSettle
import ma.midoxshop.poster.ui.PosterTheme
import ma.midoxshop.poster.ui.playOpening

class MainActivity : ComponentActivity() {
    /** Where an intent asks to go: a product id from Midox, "post" from a nag or the widget. */
    private val request = mutableStateOf<String?>(null)
    /** Bumped when the app comes back, so the products are read from Midox again. */
    private val resumed = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Nag.channels(this)
        // Screen check (debug builds only): sample products instead of Midox's.
        if (BuildConfig.DEBUG) {
            val prefs = getSharedPreferences("poster", MODE_PRIVATE)
            if (intent?.hasExtra("demo") == true) prefs.edit().putBoolean("demo", intent.getBooleanExtra("demo", false)).apply()
            Midox.demo = prefs.getBoolean("demo", false)
        }
        // The opening animation plays when Poster is opened from its icon: not from a nag, the widget or
        // Midox, and not again after turning the phone (Android 12 and up, where the splash shows the ring).
        val opening = Build.VERSION.SDK_INT >= 31 && savedInstanceState == null && intent?.action == Intent.ACTION_MAIN &&
            !intent.hasExtra("go") && !intent.hasExtra("pid")
        Launch.ready = !opening
        // Safety: if Android never hands the splash over (some launchers, a restart after a crash),
        // the screens would wait for the opening forever. After 2.5 s they come in anyway.
        if (opening) android.os.Handler(mainLooper).postDelayed({ Launch.ready = true }, 2500)
        if (opening && Build.VERSION.SDK_INT >= 31) {
            // On a very slow start Android shows the app before handing the splash over: then skip it.
            var firstFrame = 0L
            Choreographer.getInstance().postFrameCallback { firstFrame = SystemClock.uptimeMillis() }
            splashScreen.setOnExitAnimationListener { splash ->
                val late = firstFrame != 0L && SystemClock.uptimeMillis() - firstFrame > 1500
                if (late) { splash.remove(); Launch.ready = true } else playOpening(splash) { Launch.ready = true }
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && !Nag.canNotify(this)) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        take(intent)
        setContent { PosterTheme { LaunchSettle { App(request, resumed.value) } } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    override fun onResume() {
        super.onResume()
        resumed.value++
        Nag.schedule(this)
    }

    private fun take(i: Intent?) {
        val r = i?.getStringExtra("pid")?.let { "pid:$it" } ?: i?.getStringExtra("go")
        if (r != null) request.value = r
    }
}
