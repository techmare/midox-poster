package ma.midoxshop.poster

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * The home-screen widget: today's posts as "1 / 3" with a bar, your streak, and one button that goes
 * straight into post mode. Seen every time you unlock the phone, so it nudges before any nag has to.
 * Plain RemoteViews, like Midox's Today widget.
 */
object Widget {
    /** Redraws every Poster widget. Call after a post, a settings change, and from the nags (a new day). */
    fun update(ctx: Context) {
        val m = AppWidgetManager.getInstance(ctx)
        val ids = m.getAppWidgetIds(ComponentName(ctx, WidgetProvider::class.java))
        if (ids.isNotEmpty()) m.updateAppWidget(ids, views(ctx))
    }

    fun views(ctx: Context): RemoteViews {
        val done = Store.postedToday(ctx)
        val goal = Store.goal(ctx)
        val min = Store.min(ctx)
        val streak = Store.streak(ctx).days
        val paused = Store.pausedToday(ctx)

        val v = RemoteViews(ctx.packageName, R.layout.widget_poster)
        v.setTextViewText(R.id.w_count, "$done")
        v.setTextViewText(R.id.w_goal, " / $goal")
        v.setProgressBar(R.id.w_bar, goal, done.coerceAtMost(goal), false)
        v.setTextViewText(
            R.id.w_detail,
            when {
                paused != null -> "Paused today"
                streak == 0 -> "Start a streak today"
                streak == 1 -> "1 day streak"
                else -> "$streak days streak"
            }
        )
        val finished = done >= goal || paused != null
        val saved = done >= min
        v.setTextViewText(
            R.id.w_post,
            when {
                finished -> "Done for today"
                saved -> "Post one more"
                else -> "Post 1 now"
            }
        )
        // Coral until the streak is safe; then a quiet button, so the widget stops shouting.
        v.setInt(R.id.w_post, "setBackgroundResource", if (saved || paused != null) R.drawable.widget_button_quiet else R.drawable.widget_button)
        v.setTextColor(R.id.w_post, ctx.getColor(if (saved || paused != null) R.color.w_ink else R.color.w_on_accent))

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val post = Intent(ctx, MainActivity::class.java).putExtra("go", "post").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        v.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(ctx, 51, open, flags))
        v.setOnClickPendingIntent(R.id.w_post, PendingIntent.getActivity(ctx, if (finished) 51 else 52, if (finished) open else post, flags))
        return v
    }
}

/** Android calls this when the widget is added, every 30 minutes (so a new day starts at 0), and on reboot. */
class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        manager.updateAppWidget(ids, Widget.views(context))
    }
}
