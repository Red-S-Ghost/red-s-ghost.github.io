package io.github.redsghost.slovopotok;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 9001;

    private static final int[] INTERVALS = {15, 30, 60, 180, 360, 720, 1440};
    private static final String[] INTERVAL_LABELS = {
            "15 минут", "30 минут", "1 час", "3 часа", "6 часов", "12 часов", "24 часа"
    };
    private static final int[] PALETTE = {
            Color.rgb(38, 38, 45),
            Color.rgb(245, 245, 247),
            Color.rgb(26, 75, 140),
            Color.rgb(26, 100, 73),
            Color.rgb(94, 53, 135),
            Color.rgb(132, 42, 61)
    };
    private static final String[] PALETTE_LABELS = {
            "Графит", "Светлый", "Синий", "Зелёный", "Фиолетовый", "Бордовый"
    };

    private Spinner directionSpinner;
    private Spinner widgetIntervalSpinner;
    private Spinner notificationIntervalSpinner;
    private Spinner colorSpinner;
    private Switch notificationsSwitch;
    private SeekBar opacitySeekBar;
    private TextView opacityValue;
    private TextView previewPrimary;
    private TextView previewSecondary;
    private TextView previewDirection;
    private LinearLayout previewCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(73, 54, 122));
        buildInterface();
        loadSettings();
        bindPreview();
        renderPreview();
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(24), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text("Словопоток", 32, Color.rgb(31, 29, 36));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title);

        TextView subtitle = text(
                "Офлайн-слова на домашнем экране и в постоянном уведомлении.",
                16,
                Color.rgb(82, 78, 88)
        );
        subtitle.setPadding(0, dp(6), 0, dp(18));
        content.addView(subtitle);

        previewCard = new LinearLayout(this);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setPadding(dp(20), dp(16), dp(20), dp(16));
        content.addView(previewCard, layout(-1, -2, 0, dp(10)));

        previewDirection = text("EN → RU", 12, Color.WHITE);
        previewDirection.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        previewCard.addView(previewDirection);

        previewPrimary = text("freedom", 30, Color.WHITE);
        previewPrimary.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        previewPrimary.setPadding(0, dp(8), 0, 0);
        previewCard.addView(previewPrimary);

        previewSecondary = text("свобода", 18, Color.WHITE);
        previewSecondary.setPadding(0, dp(3), 0, 0);
        previewCard.addView(previewSecondary);

        section(content, "Направление");
        directionSpinner = spinner(new String[]{"English → Русский", "Русский → English"});
        content.addView(directionSpinner, layout(-1, -2, 0, dp(8)));

        section(content, "Виджет");
        widgetIntervalSpinner = spinner(INTERVAL_LABELS);
        content.addView(caption("Менять слово каждые"), layout(-1, -2, 0, dp(2)));
        content.addView(widgetIntervalSpinner);

        colorSpinner = spinner(PALETTE_LABELS);
        content.addView(caption("Цвет фона"), layout(-1, -2, 0, dp(2)));
        content.addView(colorSpinner);

        opacityValue = caption("Прозрачность: 88%");
        content.addView(opacityValue, layout(-1, -2, 0, dp(2)));
        opacitySeekBar = new SeekBar(this);
        opacitySeekBar.setMin(20);
        opacitySeekBar.setMax(100);
        content.addView(opacitySeekBar);

        section(content, "Уведомление");
        notificationsSwitch = new Switch(this);
        notificationsSwitch.setText("Показывать постоянное уведомление");
        notificationsSwitch.setTextSize(16);
        content.addView(notificationsSwitch, layout(-1, -2, 0, dp(8)));

        content.addView(caption("Менять слово каждые"), layout(-1, -2, 0, dp(2)));
        notificationIntervalSpinner = spinner(INTERVAL_LABELS);
        content.addView(notificationIntervalSpinner);

        Button save = button("Применить настройки");
        save.setOnClickListener(view -> saveSettings());
        content.addView(save, layout(-1, dp(52), dp(16), 0));

        Button next = button("Показать другое слово сейчас");
        next.setOnClickListener(view -> {
            WidgetRenderer.updateAll(this, true);
            if (Prefs.notificationsEnabled(this)) {
                NotificationHelper.show(this, true);
            }
            Scheduler.applySettings(this);
            renderPreview();
            Toast.makeText(this, "Слово обновлено", Toast.LENGTH_SHORT).show();
        });
        content.addView(next, layout(-1, dp(50), dp(10), 0));

        Button notificationSettings = button("Настройки уведомления Android");
        notificationSettings.setOnClickListener(view -> openNotificationSettings());
        content.addView(notificationSettings, layout(-1, dp(50), dp(10), 0));

        TextView note = text(
                "Чтобы добавить виджет: удерживайте пустое место на рабочем столе → Виджеты → Словопоток. "
                        + "Во сне Android может немного сдвигать время обновления. Нажатие на виджет сразу меняет слово.",
                14,
                Color.rgb(94, 89, 101)
        );
        note.setPadding(0, dp(18), 0, dp(8));
        content.addView(note);

        TextView database = text(
                "База: " + WordRepository.count(this)
                        + " частотных общеупотребительных пар, полностью офлайн.",
                14,
                Color.rgb(94, 89, 101)
        );
        content.addView(database);

        Button source = button("Исходный код и лицензии");
        source.setOnClickListener(view -> {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/Red-S-Ghost/red-s-ghost.github.io/tree/codex/slovopotok-android/wordflow-android")
            );
            startActivity(intent);
        });
        content.addView(source, layout(-1, dp(48), dp(12), 0));

        setContentView(scroll);
    }

    private void loadSettings() {
        directionSpinner.setSelection(Prefs.englishFirst(this) ? 0 : 1);
        widgetIntervalSpinner.setSelection(indexOf(INTERVALS, Prefs.widgetInterval(this)));
        notificationIntervalSpinner.setSelection(
                indexOf(INTERVALS, Prefs.notificationInterval(this))
        );
        colorSpinner.setSelection(indexOf(PALETTE, Prefs.widgetColor(this)));
        opacitySeekBar.setProgress(Prefs.widgetOpacity(this));
        notificationsSwitch.setChecked(Prefs.notificationsEnabled(this));
        opacityValue.setText("Прозрачность: " + opacitySeekBar.getProgress() + "%");
    }

    private void bindPreview() {
        directionSpinner.setOnItemSelectedListener(new SimpleSelectionListener(this::renderPreview));
        colorSpinner.setOnItemSelectedListener(new SimpleSelectionListener(this::renderPreview));
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                opacityValue.setText("Прозрачность: " + progress + "%");
                renderPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void renderPreview() {
        if (previewCard == null || colorSpinner == null || opacitySeekBar == null) {
            return;
        }
        int position = Math.max(0, colorSpinner.getSelectedItemPosition());
        int color = PALETTE[Math.min(position, PALETTE.length - 1)];
        int opacity = opacitySeekBar.getProgress();
        int alpha = Math.round(255f * opacity / 100f);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)));
        background.setCornerRadius(dp(22));
        previewCard.setBackground(background);

        int textColor = readableTextColor(color);
        previewPrimary.setTextColor(textColor);
        previewSecondary.setTextColor(textColor);
        previewDirection.setTextColor(Color.argb(
                185, Color.red(textColor), Color.green(textColor), Color.blue(textColor)
        ));

        Word word = WordRepository.current(this, Prefs.CHANNEL_WIDGET);
        boolean englishFirst = directionSpinner.getSelectedItemPosition() == 0;
        previewPrimary.setText(word.primary(englishFirst));
        previewSecondary.setText(word.secondary(englishFirst));
        previewDirection.setText(englishFirst ? "EN → RU" : "RU → EN");
    }

    private void saveSettings() {
        int colorPosition = Math.max(0, colorSpinner.getSelectedItemPosition());
        int widgetPosition = Math.max(0, widgetIntervalSpinner.getSelectedItemPosition());
        int notificationPosition = Math.max(
                0, notificationIntervalSpinner.getSelectedItemPosition()
        );

        SharedPreferences.Editor editor = Prefs.get(this).edit();
        editor.putBoolean(Prefs.KEY_ENGLISH_FIRST, directionSpinner.getSelectedItemPosition() == 0);
        editor.putInt(
                Prefs.KEY_WIDGET_INTERVAL,
                INTERVALS[Math.min(widgetPosition, INTERVALS.length - 1)]
        );
        editor.putInt(
                Prefs.KEY_NOTIFICATION_INTERVAL,
                INTERVALS[Math.min(notificationPosition, INTERVALS.length - 1)]
        );
        editor.putInt(
                Prefs.KEY_WIDGET_COLOR,
                PALETTE[Math.min(colorPosition, PALETTE.length - 1)]
        );
        editor.putInt(Prefs.KEY_WIDGET_OPACITY, opacitySeekBar.getProgress());
        editor.putBoolean(Prefs.KEY_NOTIFICATIONS, notificationsSwitch.isChecked());
        editor.apply();

        Scheduler.applySettings(this);
        renderPreview();

        if (notificationsSwitch.isChecked()
                && Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS
            );
        } else {
            Toast.makeText(this, "Настройки применены", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATIONS) {
            return;
        }
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            NotificationHelper.show(this, false);
            Scheduler.scheduleNotification(this);
            Toast.makeText(this, "Уведомление включено", Toast.LENGTH_SHORT).show();
        } else {
            Prefs.get(this).edit().putBoolean(Prefs.KEY_NOTIFICATIONS, false).apply();
            notificationsSwitch.setChecked(false);
            Scheduler.applySettings(this);
            Toast.makeText(
                    this,
                    "Без разрешения Android не покажет карточку на экране блокировки",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
    }

    private void section(LinearLayout parent, String label) {
        TextView title = text(label, 21, Color.rgb(48, 44, 54));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        parent.addView(title, layout(-1, -2, dp(20), dp(8)));
    }

    private TextView caption(String value) {
        return text(value, 14, Color.rgb(92, 87, 99));
    }

    private TextView text(String value, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private Button button(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(15);
        button.setAllCaps(false);
        return button;
    }

    private LinearLayout.LayoutParams layout(
            int width,
            int height,
            int topMargin,
            int bottomMargin
    ) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.topMargin = topMargin;
        params.bottomMargin = bottomMargin;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int indexOf(int[] values, int selected) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == selected) {
                return i;
            }
        }
        return 0;
    }

    private static int readableTextColor(int color) {
        double luminance = 0.299 * Color.red(color)
                + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color);
        return luminance > 170 ? Color.rgb(24, 24, 28) : Color.WHITE;
    }

    private static final class SimpleSelectionListener
            implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable action;

        SimpleSelectionListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void onItemSelected(
                android.widget.AdapterView<?> parent,
                View view,
                int position,
                long id
        ) {
            action.run();
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }
    }
}
