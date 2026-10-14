package de.sist.bookseries.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.sist.bookseries.Hardcover;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BookSeries {

    private String title;
    private String author;
    private String goodReadsUrl;
    private String hardcoverSlug;
    private Instant lastUpdate;
    private List<Book> books = new ArrayList<>();

    public BookSeries() {
    }

    public BookSeries(String title, String author, String goodReadsUrl, List<Book> books) {
        this.title = title;
        this.author = author;
        this.goodReadsUrl = goodReadsUrl;
        this.books = books;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getGoodReadsUrl() {
        return goodReadsUrl;
    }

    public String getHardcoverSlug() {
        return hardcoverSlug;
    }

    public void setHardcoverSlug(String hardcoverSlug) {
        this.hardcoverSlug = hardcoverSlug;
    }

    /**
     * The URL to show for this series: Hardcover if the series is known there, Goodreads otherwise.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getUrl() {
        return hardcoverSlug != null ? Hardcover.seriesUrl(hardcoverSlug) : goodReadsUrl;
    }

    public boolean hasUrl(String url) {
        return url.equals(goodReadsUrl) || (hardcoverSlug != null && url.equals(Hardcover.seriesUrl(hardcoverSlug)));
    }

    public List<Book> getBooks() {
        return books;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
