package com.matijakljajic.freeairradio.playback.metadata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public final class NowPlaying {
    private static final int INVALID_INDEX = -1;

    @Nullable
    private final String artist;
    @Nullable
    private final String title;

    public NowPlaying(@Nullable String artist, @Nullable String title) {
        this.artist = normalizeValue(artist);
        this.title = normalizeValue(title);
    }

    @Nullable
    static NowPlaying fromMetadata(@Nullable String stationName,
                                   @Nullable String artist,
                                   @Nullable String title) {
        String normalizedArtist = normalizeValue(artist);
        ParsedTrack parsedTrack = parseTrackTitle(title);
        if (normalizedArtist == null && parsedTrack != null) {
            normalizedArtist = parsedTrack.artist;
        }
        String normalizedTitle = parsedTrack != null
                ? parsedTrack.title
                : normalizeValue(title);

        NowPlaying nowPlaying = new NowPlaying(normalizedArtist, normalizedTitle);
        if (!nowPlaying.hasTrackInfo() || nowPlaying.matchesStationNameOnly(stationName)) {
            return null;
        }
        return nowPlaying;
    }

    @Nullable
    public String getArtist() {
        return artist;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String buildDisplayText() {
        if (title != null) {
            return artist == null ? title : artist + " - " + title;
        }
        return artist;
    }

    boolean hasTrackInfo() {
        return artist != null || title != null;
    }

    private boolean matchesStationNameOnly(@Nullable String stationName) {
        return artist == null
                && stationName != null
                && stationName.equals(title);
    }

    @Nullable
    static ParsedTrack parseTrackTitle(@Nullable String rawTitle) {
        String normalizedTitle = normalizeValue(rawTitle);
        if (normalizedTitle == null) {
            return null;
        }

        String[] separators = {" - ", " – ", " — "};
        for (String separator : separators) {
            int firstSeparator = normalizedTitle.indexOf(separator);
            if (firstSeparator <= 0) {
                continue;
            }
            int secondSeparator = normalizedTitle.indexOf(separator, firstSeparator + separator.length());
            if (secondSeparator >= 0) {
                continue;
            }

            String left = normalizeValue(normalizedTitle.substring(0, firstSeparator));
            String right = normalizeValue(normalizedTitle.substring(firstSeparator + separator.length()));
            if (left == null || right == null) {
                continue;
            }

            return new ParsedTrack(left, right);
        }

        return new ParsedTrack(null, normalizedTitle);
    }

    @Nullable
    private static String normalizeValue(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }

        String decodedValue = decodeCharacterReferences(trimmedValue).trim();
        return decodedValue.isEmpty() ? null : decodedValue;
    }

    @NonNull
    private static String decodeCharacterReferences(@NonNull String value) {
        StringBuilder builder = null;
        int index = 0;
        while (index < value.length()) {
            int nextAmpersand = value.indexOf('&', index);
            if (nextAmpersand == INVALID_INDEX) {
                break;
            }

            DecodedReference decodedReference = decodeReference(value, nextAmpersand);
            if (decodedReference == null) {
                index = nextAmpersand + 1;
                continue;
            }

            if (builder == null) {
                builder = new StringBuilder(value.length());
            }
            builder.append(value, index, nextAmpersand);
            builder.append(decodedReference.value);
            index = decodedReference.nextIndex;
        }

        if (builder == null) {
            return value;
        }

        builder.append(value, index, value.length());
        return builder.toString();
    }

    @Nullable
    private static DecodedReference decodeReference(@NonNull String value, int ampersandIndex) {
        DecodedReference numericReference = decodeNumericReference(value, ampersandIndex);
        if (numericReference != null) {
            return numericReference;
        }

        return decodeNamedReference(value, ampersandIndex);
    }

    @Nullable
    private static DecodedReference decodeNumericReference(@NonNull String value, int ampersandIndex) {
        int length = value.length();
        if (ampersandIndex + 2 >= length || value.charAt(ampersandIndex + 1) != '#') {
            return null;
        }

        int index = ampersandIndex + 2;
        int radix = 10;
        if (index < length && (value.charAt(index) == 'x' || value.charAt(index) == 'X')) {
            radix = 16;
            index++;
        }

        int digitsStart = index;
        while (index < length && isValidDigit(value.charAt(index), radix)) {
            index++;
        }
        if (digitsStart == index) {
            return null;
        }

        boolean hasSemicolon = index < length && value.charAt(index) == ';';
        if (!hasSemicolon && index < length && Character.isLetterOrDigit(value.charAt(index))) {
            return null;
        }

        int codePoint;
        try {
            codePoint = Integer.parseInt(value.substring(digitsStart, index), radix);
        } catch (NumberFormatException exception) {
            return null;
        }
        if (!Character.isValidCodePoint(codePoint)) {
            return null;
        }

        String decodedValue = new String(Character.toChars(codePoint));
        return new DecodedReference(decodedValue, hasSemicolon ? index + 1 : index);
    }

    private static boolean isValidDigit(char character, int radix) {
        return Character.digit(character, radix) != INVALID_INDEX;
    }

    @SuppressWarnings("GrazieInspectionRunner")
    @Nullable
    private static DecodedReference decodeNamedReference(@NonNull String value, int ampersandIndex) {
        if (value.startsWith("&amp;", ampersandIndex)) {
            return new DecodedReference("&", ampersandIndex + 5);
        }
        if (value.startsWith("&lt;", ampersandIndex)) {
            return new DecodedReference("<", ampersandIndex + 4);
        }
        if (value.startsWith("&gt;", ampersandIndex)) {
            return new DecodedReference(">", ampersandIndex + 4);
        }
        if (value.startsWith("&quot;", ampersandIndex)) {
            return new DecodedReference("\"", ampersandIndex + 6);
        }
        if (value.startsWith("&apos;", ampersandIndex)) {
            return new DecodedReference("'", ampersandIndex + 6);
        }
        if (value.startsWith("&nbsp;", ampersandIndex)) {
            return new DecodedReference(" ", ampersandIndex + 6);
        }
        return null;
    }

    private static final class DecodedReference {
        @NonNull
        private final String value;
        private final int nextIndex;

        private DecodedReference(@NonNull String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }

    static final class ParsedTrack {
        @Nullable
        final String artist;
        @Nullable
        final String title;

        private ParsedTrack(@Nullable String artist, @Nullable String title) {
            this.artist = artist;
            this.title = title;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NowPlaying)) {
            return false;
        }
        NowPlaying that = (NowPlaying) o;
        return Objects.equals(artist, that.artist)
                && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artist, title);
    }

    @NonNull
    @Override
    public String toString() {
        return "NowPlaying{"
                + "artist='" + artist + '\''
                + ", title='" + title + '\''
                + '}';
    }
}
