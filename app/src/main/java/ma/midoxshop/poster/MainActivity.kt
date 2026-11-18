package ma.midoxshop.poster

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import ma.midoxshop.poster.ui.App
import ma.midoxshop.poster.ui.PosterTheme

class MainActivity : ComponentActivity() {
    /** Where an intent asks to go: a product id from Midox, "post" from a nag. */
    private val request = mutableStateOf<String?>(null)
    /** Bumped when the app comes back, so the products are read from Midox again. */
    private val resumed = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Nag.channels(this)
        if (Build.VERSION.SDK_INT >= 33 && !Nag.canNotify(this)) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        take(intent)
        setContent { PosterTheme { App(request, resumed.value) } }
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
