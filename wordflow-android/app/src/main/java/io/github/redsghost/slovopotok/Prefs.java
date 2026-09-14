package io.github.redsghost.slovopotok;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

final class Prefs {
    static final String STORE = "slovopotok_preferences";
    static final String KEY_NOTIFICATIONS = "notifications_enabled";
    static final String KEY_WIDGET_INTERVAL = "widget_interval_minutes";
    static final String KEY_NOTIFICATION_INTERVAL = "notification_interval_minutes";
    static final String KEY_WIDGET_COLOR = "widget_color";
    static final String KEY_WIDGET_OPACITY = "widget_opacity";
    static final String KEY_ENGLISH_FIRST = "english_first";

    static final String CHANNEL_WIDGET = "widget";
    static final String CHANNEL_NOTIFICATION = "notification";

    private Prefs() {
    }

    static SharedPreferences get(Context context) {
        return context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }

    static boolean notificationsEnabled(Context context) {
        return get(context).getBoolean(KEY_NOTIFICATIONS, false);
    }

    static int widgetInterval(Context context) {
        return get(context).getInt(KEY_WIDGET_INTERVAL, 60);
    }

    static int notificationInterval(Context context) {
        return get(context).getInt(KEY_NOTIFICATION_INTERVAL, 60);
    }

    static int widgetColor(Context context) {
        return get(context).getInt(KEY_WIDGET_COLOR, Color.rgb(38, 38, 45));
    }

    static int widgetOpacity(Context context) {
        return get(context).getInt(KEY_WIDGET_OPACITY, 88);
    }

    static boolean englishFirst(Context context) {
        return get(context).getBoolean(KEY_ENGLISH_FIRST, true);
    }

    static int currentIndex(Context context, String channel) {
        return get(context).getInt("current_index_" + channel, -1);
    }

    static void setCurrentIndex(Context context, String channel, int index) {
        get(context).edit().putInt("current_index_" + channel, index).apply();
    }
}
