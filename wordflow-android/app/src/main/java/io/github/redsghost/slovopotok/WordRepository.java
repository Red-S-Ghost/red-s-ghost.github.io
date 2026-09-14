package io.github.redsghost.slovopotok;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class WordRepository {
    private static final Object LOCK = new Object();
    private static final Random RANDOM = new Random();
    private static volatile List<Word> cache;

    private WordRepository() {
    }

    static int count(Context context) {
        return words(context).size();
    }

    static Word current(Context context, String channel) {
        List<Word> all = words(context);
        synchronized (LOCK) {
            int index = Prefs.currentIndex(context, channel);
            if (index < 0 || index >= all.size()) {
                index = RANDOM.nextInt(all.size());
                Prefs.setCurrentIndex(context, channel, index);
            }
            return all.get(index);
        }
    }

    static Word next(Context context, String channel) {
        List<Word> all = words(context);
        synchronized (LOCK) {
            int previous = Prefs.currentIndex(context, channel);
            int index = RANDOM.nextInt(all.size());
            if (all.size() > 1 && index == previous) {
                index = (index + 1) % all.size();
            }
            Prefs.setCurrentIndex(context, channel, index);
            return all.get(index);
        }
    }

    private static List<Word> words(Context context) {
        List<Word> local = cache;
        if (local != null) {
            return local;
        }
        synchronized (LOCK) {
            if (cache != null) {
                return cache;
            }
            List<Word> loaded = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    context.getAssets().open("words.csv"), StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    List<String> fields = parseCsv(line);
                    if (fields.size() >= 2) {
                        String russian = fields.get(0).trim();
                        String english = fields.get(1).trim();
                        if (!russian.isEmpty() && !english.isEmpty()) {
                            loaded.add(new Word(russian, english));
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            if (loaded.isEmpty()) {
                loaded.add(new Word("слово", "word"));
                loaded.add(new Word("дружба", "friendship"));
                loaded.add(new Word("свобода", "freedom"));
                loaded.add(new Word("время", "time"));
                loaded.add(new Word("свет", "light"));
            }
            cache = Collections.unmodifiableList(loaded);
            return cache;
        }
    }

    private static List<String> parseCsv(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char value = line.charAt(i);
            if (value == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (value == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(value);
            }
        }
        values.add(current.toString());
        return values;
    }
}
