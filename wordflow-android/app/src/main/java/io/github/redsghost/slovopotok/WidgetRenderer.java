package io.github.redsghost.slovopotok;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.RemoteViews;

final class WidgetRenderer {
    private WidgetRenderer() {
    }

    static void updateAll(Context context, boolean advance) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, WordWidgetProvider.class));
        if (ids == null || ids.length == 0) {
            return;
        }

        boolean englishFirst = Prefs.englishFirst(context);
        Word word = advance
                ? WordRepository.next(context, Prefs.CHANNEL_WIDGET, englishFirst)
                : WordRepository.current(context, Prefs.CHANNEL_WIDGET, englishFirst);
        for (int id : ids) {
            manager.updateAppWidget(id, remoteViews(context, word, englishFirst));
        }
    }

    static void updateOne(Context context, AppWidgetManager manager, int id) {
        boolean englishFirst = Prefs.englishFirst(context);
        Word word = WordRepository.current(context, Prefs.CHANNEL_WIDGET, englishFirst);
        manager.updateAppWidget(id, remoteViews(context, word, englishFirst));
    }

    private static RemoteViews remoteViews(
            Context context,
            Word word,
            boolean englishFirst
    ) {
        int baseColor = Prefs.widgetColor(context);
        int opacity = Prefs.widgetOpacity(context);
        int textColor = readableTextColor(baseColor);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_word);
        views.setImageViewBitmap(R.id.widget_background, background(baseColor, opacity));
        views.setTextViewText(R.id.widget_primary, word.source);
        views.setTextViewText(R.id.widget_secondary, word.translation);
        views.setTextViewText(R.id.widget_direction, englishFirst ? "EN → RU" : "RU → EN");
        views.setTextColor(R.id.widget_primary, textColor);
        views.setTextColor(R.id.widget_secondary, textColor);
        views.setTextColor(R.id.widget_direction, withAlpha(textColor, 180));

        PendingIntent nextWord = PendingIntent.getBroadcast(
                context,
                7201,
                new Intent(context, UpdateReceiver.class).setAction(Scheduler.ACTION_WIDGET_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, nextWord);
        views.setContentDescription(
                R.id.widget_root,
                word.source + ". " + word.translation
        );
        return views;
    }

    private static Bitmap background(int color, int opacityPercent) {
        Bitmap bitmap = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int alpha = Math.round(255f * Math.max(20, Math.min(100, opacityPercent)) / 100f);
        paint.setColor(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)));
        canvas.drawRoundRect(0f, 0f, 600f, 300f, 42f, 42f, paint);
        return bitmap;
    }

    private static int readableTextColor(int color) {
        double luminance = 0.299 * Color.red(color)
                + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color);
        return luminance > 170 ? Color.rgb(24, 24, 28) : Color.WHITE;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
