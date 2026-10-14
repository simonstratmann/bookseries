package de.sist.bookseries;

/**
 * Where series and book data is loaded from. Configured via the BOOK_SERIES_SOURCE environment variable.
 */
public enum MetadataSource {
    HARDCOVER,
    GOODREADS;

    public static MetadataSource configured() {
        final String value = System.getenv("BOOK_SERIES_SOURCE");
        if (value == null || value.isBlank()) {
            return HARDCOVER;
        }
        return MetadataSource.valueOf(value.trim().toUpperCase());
    }
}
