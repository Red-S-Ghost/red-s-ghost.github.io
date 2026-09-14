package io.github.redsghost.slovopotok;

final class Word {
    final String russian;
    final String english;

    Word(String russian, String english) {
        this.russian = russian;
        this.english = english;
    }

    String primary(boolean englishFirst) {
        return englishFirst ? english : russian;
    }

    String secondary(boolean englishFirst) {
        return englishFirst ? russian : english;
    }
}
