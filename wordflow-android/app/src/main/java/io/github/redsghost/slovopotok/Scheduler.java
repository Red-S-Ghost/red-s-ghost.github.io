package io.github.redsghost.slovopotok;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

final class Scheduler {
    static final String ACTION_NOTIFICATION_TICK =
            "io.github.redsghost.slovopotok.action.NOTIFICATION_TICK";
    static final String ACTION_NOTIFICATION_NEXT =
            "io.github.redsghost.slovopotok.action.NOTIFICATION_NEXT";
    static final String ACTION_WIDGET_TICK =
            "io.github.redsghost.slovopotok.action.WIDGET_TICK";
    static final String ACTION_WIDGET_NEXT =
            "io.github.redsghost.slovopotok.action.WIDGET_NEXT";

    private static final int REQUEST_NOTIFICATION = 401;
    private static final int REQUEST_WIDGET = 402;

    private Scheduler() {
    }

    static void applySettings(Context context) {
        Context app = context.getApplicationContext();

        if (Prefs.notificationsEnabled(app)) {
            NotificationHelper.show(app, false);
            scheduleNotification(app);
        } else {
            cancelNotification(app);
            NotificationHelper.dismiss(app);
        }

        WidgetRenderer.updateAll(app, false);
        if (hasWidgets(app)) {
            scheduleWidget(app);
        } else {
            cancelWidget(app);
        }
    }

    static void restore(Context context) {
        Context app = context.getApplicationContext();
        if (Prefs.notificationsEnabled(app)) {
            NotificationHelper.show(app, false);
            scheduleNotification(app);
        }
        if (hasWidgets(app)) {
            WidgetRenderer.updateAll(app, false);
            scheduleWidget(app);
        }
    }

    static void scheduleNotification(Context context) {
        schedule(context, ACTION_NOTIFICATION_TICK, REQUEST_NOTIFICATION,
                Prefs.notificationInterval(context));
    }

    static void scheduleWidget(Context context) {
        schedule(context, ACTION_WIDGET_TICK, REQUEST_WIDGET,
                Prefs.widgetInterval(context));
    }

    static void cancelNotification(Context context) {
        cancel(context, ACTION_NOTIFICATION_TICK, REQUEST_NOTIFICATION);
    }

    static void cancelWidget(Context context) {
        cancel(context, ACTION_WIDGET_TICK, REQUEST_WIDGET);
    }

    static boolean hasWidgets(Context context) {
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, WordWidgetProvider.class));
        return ids != null && ids.length > 0;
    }

    private static void schedule(Context context, String action, int requestCode, int minutes) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        PendingIntent pendingIntent = pendingIntent(context, action, requestCode);
        manager.cancel(pendingIntent);
        long delay = Math.max(15, minutes) * 60_000L;
        long triggerAt = SystemClock.elapsedRealtime() + delay;
        manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
    }

    private static void cancel(Context context, String action, int requestCode) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) {
            PendingIntent pendingIntent = pendingIntent(context, action, requestCode);
            manager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static PendingIntent pendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, UpdateReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
