package ma.midoxshop.poster

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

/**
 * The nags: a quiet notification at your reminder time, a buzzing pop-up later, then the full-screen
 * alarm again and again until today's minimum is posted, you pause the day, or it's late.
 */
object Nag {
    private const val CH_QUIET = "nudge"
    private const val CH_LOUD = "nudge_loud"
    private const val CH_ALARM = "alarm"
    const val NOTE_ID = 7

    fun channels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_QUIET, "Reminder", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_LOUD, "Pop-up nag", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true); vibrationPattern = longArrayOf(0, 300, 150, 300)
        })
        nm.createNotificationChannel(NotificationChannel(CH_ALARM, "Full-screen alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false)   // the alarm screen plays its own sound
        })
    }

    private fun doneToday(ctx: Context) = Store.postedToday(ctx) >= Store.min(ctx) || Store.pausedToday(ctx) != null

    private fun slots(ctx: Context) =
        NagPlan.slots(Store.remindAt(ctx), Store.loud(ctx), Store.alarmAfter(ctx), Store.every(ctx), Store.endAt(ctx), Store.morningAt(ctx))

    private fun nowMinute(): Int = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

    private fun pending(ctx: Context, level: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, 1, Intent(ctx, NagReceiver::class.java).setAction("ma.midoxshop.poster.NAG").putExtra("level", level),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Sets the next nag: later today if today isn't done, else tomorrow's first. Call after every change. */
    fun schedule(ctx: Context) {
        Widget.update(ctx)   // a nag, a boot or a new day: the widget follows
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pending(ctx, 0))
        if (!Store.nagsOn(ctx)) return
        val all = slots(ctx)
        val today = if (doneToday(ctx)) null else NagPlan.next(all, nowMinute(), Store.snoozedUntil(ctx))
        val cal = Calendar.getInstance().apply { set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val slot = today ?: all.first().also { cal.add(Calendar.DAY_OF_YEAR, 1) }
        cal.set(Calendar.HOUR_OF_DAY, slot.minute / 60); cal.set(Calendar.MINUTE, slot.minute % 60)
        val pi = pending(ctx, slot.level)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }

    /** Posted or paused: take any nag off the screen and set tomorrow's. */
    fun calmDown(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancel(NOTE_ID)
        NotificationManagerCompat.from(ctx).cancel(READY_ID)   // "Next post is ready" is answered
        schedule(ctx)
        scheduleReady(ctx)
    }

    // ---- "Next post is ready": after a post, once the 10 minutes between posts are up ----

    private const val READY_ID = 9

    private fun readyIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, 4, Intent(ctx, NagReceiver::class.java).setAction("ma.midoxshop.poster.READY"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Rings softly when the next post is allowed, while today's goal isn't reached (and it's not late). */
    fun scheduleReady(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(readyIntent(ctx))
        if (!Store.nagsOn(ctx) || Store.postedToday(ctx) >= Store.goal(ctx) || Store.pausedToday(ctx) != null) return
        val at = Store.lastPostAt(ctx) + Posts.GAP_MINUTES * 60_000L
        if (at <= System.currentTimeMillis() || nowMinute() + Posts.GAP_MINUTES > Store.endAt(ctx)) return
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, readyIntent(ctx))
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, readyIntent(ctx))
    }

    fun ready(ctx: Context) {
        val done = Store.postedToday(ctx)
        val goal = Store.goal(ctx)
        if (done >= goal || Store.pausedToday(ctx) != null) return
        val left = goal - done
        notify(ctx, CH_QUIET, "Next post is ready", "$done of $goal today · $left to go, if you feel like it.", id = READY_ID)
    }

    /** On a call (or ringing), the alarm waits 10 minutes rather than covering the call. */
    private fun busy(ctx: Context): Boolean {
        val am = ctx.getSystemService(AudioManager::class.java)
        return am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION || am.mode == AudioManager.MODE_RINGTONE
    }

    fun fire(ctx: Context, level: Int) {
        if (!Store.nagsOn(ctx) || doneToday(ctx)) { schedule(ctx); return }
        val n = Store.nagNumber(ctx)
        val line = NagPlan.line(Days.today(), n)
        val left = Store.min(ctx) - Store.postedToday(ctx)
        val title = if (left <= 1) "Post 1 on Marketplace" else "Post $left on Marketplace"
        if (level >= 2 && busy(ctx)) {
            Store.snooze(ctx, nowMinute() + 10, counts = false)
            schedule(ctx); return
        }
        if (level >= 2) {
            val alarm = Intent(ctx, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            // With "Display over other apps" the alarm opens over whatever is on screen.
            if (Settings.canDrawOverlays(ctx)) runCatching { ctx.startActivity(alarm) }
            // Also as a full-screen notification: the way Android shows it on the lock screen.
            notify(ctx, CH_ALARM, title, line, fullScreen = PendingIntent.getActivity(ctx, 3, alarm, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } else {
            notify(ctx, if (level == 1) CH_LOUD else CH_QUIET, title, line)
        }
        schedule(ctx)
    }

    private fun notify(ctx: Context, channel: String, title: String, text: String, fullScreen: PendingIntent? = null, id: Int = NOTE_ID) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        channels(ctx)
        val open = PendingIntent.getActivity(
            ctx, 2, Intent(ctx, MainActivity::class.java).putExtra("go", "post").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .addAction(0, "Post now", open)
            .setAutoCancel(true)
            .setCategory(if (fullScreen != null) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (channel == CH_QUIET) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
        if (fullScreen != null) b.setFullScreenIntent(fullScreen, true).setOngoing(true)
        NotificationManagerCompat.from(ctx).notify(id, b.build())
    }

    // ---- What's missing for the nags to work ----

    fun canNotify(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    fun canExact(ctx: Context) = Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun canFullScreen(ctx: Context) = Build.VERSION.SDK_INT < 34 || ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    fun canOverlay(ctx: Context) = Settings.canDrawOverlays(ctx)
}

class NagReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == "ma.midoxshop.poster.NAG") Nag.fire(ctx, intent.getIntExtra("level", 0))
        else if (intent.action == "ma.midoxshop.poster.READY") Nag.ready(ctx)
        else Nag.schedule(ctx)   // phone restarted, app updated, clock changed
    }
}
