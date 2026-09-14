package io.github.redsghost.slovopotok;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public final class WordWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            WidgetRenderer.updateOne(context, manager, id);
        }
        Scheduler.scheduleWidget(context);
    }

    @Override
    public void onEnabled(Context context) {
        WidgetRenderer.updateAll(context, false);
        Scheduler.scheduleWidget(context);
    }

    @Override
    public void onDisabled(Context context) {
        Scheduler.cancelWidget(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Scheduler.ACTION_WIDGET_NEXT.equals(intent.getAction())) {
            WidgetRenderer.updateAll(context, true);
            Scheduler.scheduleWidget(context);
            return;
        }
        super.onReceive(context, intent);
    }
}
