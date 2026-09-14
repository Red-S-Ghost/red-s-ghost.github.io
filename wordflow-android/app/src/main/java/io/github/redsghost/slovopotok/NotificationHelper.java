package io.github.redsghost.slovopotok;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

final class NotificationHelper {
    private static final String CHANNEL_ID = "word_stream";
    private static final int NOTIFICATION_ID = 7001;

    private NotificationHelper() {
    }

    static void show(Context context, boolean advance) {
        if (!Prefs.notificationsEnabled(context)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        ensureChannel(manager);

        Word word = advance
                ? WordRepository.next(context, Prefs.CHANNEL_NOTIFICATION)
                : WordRepository.current(context, Prefs.CHANNEL_NOTIFICATION);
        boolean englishFirst = Prefs.englishFirst(context);

        PendingIntent openApp = PendingIntent.getActivity(
                context,
                7101,
                new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent nextWord = PendingIntent.getBroadcast(
                context,
                7102,
                new Intent(context, UpdateReceiver.class)
                        .setAction(Scheduler.ACTION_NOTIFICATION_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Color.rgb(103, 80, 164))
                .setContentTitle(word.primary(englishFirst))
                .setContentText(word.secondary(englishFirst))
                .setStyle(new Notification.BigTextStyle().bigText(word.secondary(englishFirst)))
                .setContentIntent(openApp)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_REMINDER)
                .addAction(new Notification.Action.Builder(
                        R.drawable.ic_notification, "Следующее", nextWord).build())
                .build();

        try {
            manager.notify(NOTIFICATION_ID, notification);
        } catch (SecurityException ignored) {
        }
    }

    static void dismiss(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }

    private static void ensureChannel(NotificationManager manager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Слово в уведомлении",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Постоянная карточка со словом и переводом");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setShowBadge(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }
}
