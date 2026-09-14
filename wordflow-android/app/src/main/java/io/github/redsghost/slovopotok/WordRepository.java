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
    private static volatile List<Word> enRuCache;
    private static volatile List<Word> ruEnCache;

    private WordRepository() {
    }

    static int count(Context context, boolean englishFirst) {
        return words(context, englishFirst).size();
    }

    static Word current(Context context, String channel, boolean englishFirst) {
        List<Word> all = words(context, englishFirst);
        synchronized (LOCK) {
            int index = Prefs.currentIndex(context, channel, englishFirst);
            if (index < 0 || index >= all.size()) {
                index = RANDOM.nextInt(all.size());
                Prefs.setCurrentIndex(context, channel, englishFirst, index);
            }
            return all.get(index);
        }
    }

    static Word next(Context context, String channel, boolean englishFirst) {
        List<Word> all = words(context, englishFirst);
        synchronized (LOCK) {
            int previous = Prefs.currentIndex(context, channel, englishFirst);
            int index = RANDOM.nextInt(all.size());
            if (all.size() > 1 && index == previous) {
                index = (index + 1) % all.size();
            }
            Prefs.setCurrentIndex(context, channel, englishFirst, index);
            return all.get(index);
        }
    }

    private static List<Word> words(Context context, boolean englishFirst) {
        List<Word> local = englishFirst ? enRuCache : ruEnCache;
        if (local != null) {
            return local;
        }
        synchronized (LOCK) {
            local = englishFirst ? enRuCache : ruEnCache;
            if (local != null) {
                return local;
            }

            String filename = englishFirst ? "words_en_ru.csv" : "words_ru_en.csv";
            List<Word> loaded = load(context, filename);
            if (loaded.isEmpty()) {
                if (englishFirst) {
                    loaded.add(new Word("word", "слово"));
                    loaded.add(new Word("friendship", "дружба"));
                    loaded.add(new Word("freedom", "свобода"));
                    loaded.add(new Word("time", "время"));
                    loaded.add(new Word("light", "свет"));
                } else {
                    loaded.add(new Word("слово", "word"));
                    loaded.add(new Word("дружба", "friendship"));
                    loaded.add(new Word("свобода", "freedom"));
                    loaded.add(new Word("время", "time"));
                    loaded.add(new Word("свет", "light"));
                }
            }

            local = Collections.unmodifiableList(loaded);
            if (englishFirst) {
                enRuCache = local;
            } else {
                ruEnCache = local;
            }
            return local;
        }
    }

    private static List<Word> load(Context context, String filename) {
        List<Word> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(filename),
                StandardCharsets.UTF_8
        ))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                List<String> fields = parseCsv(line);
                if (fields.size() < 2) {
                    continue;
                }
                String source = fields.get(0).trim();
                String translation = fields.get(1).trim();
                if (!source.isEmpty() && !translation.isEmpty()) {
                    loaded.add(new Word(source, translation));
                }
            }
        } catch (Exception ignored) {
        }
        return loaded;
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
