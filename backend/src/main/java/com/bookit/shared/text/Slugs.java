package com.bookit.shared.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Slugs {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private Slugs() {
    }

    public static String toSlug(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutMarks = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        String lowercase = withoutMarks.replace('ł', 'l').replace('Ł', 'L').toLowerCase(Locale.ROOT);
        String dashed = NON_ALPHANUMERIC.matcher(lowercase).replaceAll("-");
        int start = dashed.startsWith("-") ? 1 : 0;
        int end = dashed.endsWith("-") ? dashed.length() - 1 : dashed.length();
        return start >= end ? "" : dashed.substring(start, end);
    }
}
