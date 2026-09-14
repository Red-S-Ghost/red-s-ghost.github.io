package io.github.redsghost.slovopotok;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class UpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Scheduler.ACTION_NOTIFICATION_TICK.equals(action)
                || Scheduler.ACTION_NOTIFICATION_NEXT.equals(action)) {
            if (Prefs.notificationsEnabled(context)) {
                NotificationHelper.show(context, true);
                Scheduler.scheduleNotification(context);
            }
            return;
        }

        if (Scheduler.ACTION_WIDGET_TICK.equals(action)
                || Scheduler.ACTION_WIDGET_NEXT.equals(action)) {
            if (Scheduler.hasWidgets(context)) {
                WidgetRenderer.updateAll(context, true);
                Scheduler.scheduleWidget(context);
            }
        }
    }
}
