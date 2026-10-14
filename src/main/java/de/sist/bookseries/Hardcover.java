package de.sist.bookseries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.sist.bookseries.model.Book;
import de.sist.bookseries.model.BookSeries;
import de.sist.bookseries.model.Publication;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads series data from the Hardcover GraphQL API (https://docs.hardcover.app/api/getting-started/).
 * Requires an API token in the HARDCOVER_API_TOKEN environment variable.
 */
public class Hardcover {

    private static final String API_URL = "https://api.hardcover.app/v1/graphql";
    private static final String SERIES_URL_PREFIX = "https://hardcover.app/series/";
    private static final String BOOK_URL_PREFIX = "https://hardcover.app/books/";
    private static final MediaType JSON = MediaType.get("application/json");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(40))
            .readTimeout(Duration.ofSeconds(35))
            .build();

    // Filters follow https://docs.hardcover.app/api/guides/gettingbooksinseries/ to get one book per position without duplicates
    private static final String SERIES_QUERY = """
            query SeriesBooks($slug: String!) {
              series(where: {slug: {_eq: $slug}}, limit: 1) {
                slug
                name
                author { name }
                book_series(
                  distinct_on: position
                  order_by: [{position: asc}, {book: {users_count: desc}}]
                  where: {
                    book: {canonical_id: {_is_null: true}, is_partial_book: {_eq: false}},
                    compilation: {_eq: false}
                  }
                ) {
                  position
                  book { slug title release_date release_year rating ratings_count }
                }
              }
            }
            """;

    private static final String SEARCH_QUERY = """
            query FindSeries($query: String!) {
              search(query: $query, query_type: "Series", per_page: 10) {
                results
              }
            }
            """;

    private Hardcover() {
    }

    public static boolean isSeriesUrl(String url) {
        return url != null && url.contains("hardcover.app/series/");
    }

    public static String seriesUrl(String slug) {
        return SERIES_URL_PREFIX + slug;
    }

    static String slugFromUrl(String url) {
        String slug = url.substring(url.indexOf("hardcover.app/series/") + "hardcover.app/series/".length());
        slug = slug.split("[/?#]")[0];
        return slug;
    }

    /**
     * Creates a series (without books) from a Hardcover series URL.
     */
    public static BookSeries createSeries(String url) throws Exception {
        final JsonNode series = loadSeries(slugFromUrl(url));
        final BookSeries bookSeries = new BookSeries(series.path("name").asText(), series.path("author").path("name").asText(null), null, new ArrayList<>());
        bookSeries.setHardcoverSlug(series.path("slug").asText());
        System.out.println(bookSeries.getAuthor() + " - " + bookSeries.getTitle());
        return bookSeries;
    }

    public static void updateBooksForSeries(BookSeries series) throws Exception {
        if (series.getHardcoverSlug() == null) {
            final String slug = findSeriesSlug(series.getTitle(), series.getAuthor());
            if (slug == null) {
                System.out.printf("Unable to find series %s by %s on Hardcover. Remove it and add it again using its Hardcover URL.%n", series.getTitle(), series.getAuthor());
                return;
            }
            System.out.println("Found series on Hardcover: " + seriesUrl(slug));
            series.setHardcoverSlug(slug);
        }

        final JsonNode seriesNode = loadSeries(series.getHardcoverSlug());
        final List<Book> books = new ArrayList<>();
        for (JsonNode bookSeries : seriesNode.path("book_series")) {
            final JsonNode position = bookSeries.path("position");
            // Only main entries with an integer position, like the Goodreads source does
            if (!position.isNumber() || position.asDouble() != Math.floor(position.asDouble())) {
                continue;
            }
            final JsonNode bookNode = bookSeries.path("book");
            final Book book = new Book(position.asInt(), bookNode.path("title").asText(), BOOK_URL_PREFIX + bookNode.path("slug").asText(),
                    bookNode.path("rating").asDouble(0), bookNode.path("ratings_count").asInt(0));
            book.setPublication(parsePublication(bookNode));
            books.add(book);
        }
        series.getBooks().clear();
        series.getBooks().addAll(books);
        System.out.printf("Loaded %d books%n", books.size());
    }

    static Publication parsePublication(JsonNode bookNode) {
        final LocalDate today = LocalDate.now();
        final String releaseDate = bookNode.path("release_date").asText(null);
        if (releaseDate != null) {
            final LocalDate date = LocalDate.parse(releaseDate);
            return date.isAfter(today) ? Publication.expected(date) : Publication.published(date);
        }
        if (bookNode.path("release_year").isInt()) {
            final Year year = Year.of(bookNode.path("release_year").asInt());
            return year.getValue() > today.getYear() ? Publication.expected(year) : Publication.published(year);
        }
        return null;
    }

    private static JsonNode loadSeries(String slug) throws Exception {
        final JsonNode seriesList = query(SERIES_QUERY, Map.of("slug", slug)).path("series");
        if (seriesList.isEmpty()) {
            throw new IllegalArgumentException("No series found on Hardcover with slug " + slug);
        }
        return seriesList.get(0);
    }

    /**
     * Searches by series title and, if that yields nothing, by author, then picks the series by the same author whose name best matches.
     */
    static String findSeriesSlug(String title, String author) throws Exception {
        for (String searchTerm : new String[]{title, author}) {
            if (searchTerm == null) {
                continue;
            }
            final JsonNode hits = query(SEARCH_QUERY, Map.of("query", searchTerm)).path("search").path("results").path("hits");
            String containedMatch = null;
            for (JsonNode hit : hits) {
                final JsonNode document = hit.path("document");
                if (author != null && !normalize(author).equals(normalize(document.path("author_name").asText()))) {
                    continue;
                }
                final String name = normalize(document.path("name").asText());
                if (name.equals(normalize(title))) {
                    return document.path("slug").asText();
                }
                if (containedMatch == null && !name.isEmpty() && (name.contains(normalize(title)) || normalize(title).contains(name))) {
                    containedMatch = document.path("slug").asText();
                }
            }
            if (containedMatch != null) {
                return containedMatch;
            }
        }
        return null;
    }

    private static String normalize(String text) {
        return text.toLowerCase().replaceAll("\\b(saga|series|trilogy|cycle|chronicles|universe)\\b", "").replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static JsonNode query(String query, Map<String, Object> variables) throws Exception {
        final String token = System.getenv("HARDCOVER_API_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Set the HARDCOVER_API_TOKEN environment variable to your Hardcover API token (https://hardcover.app/account/api)");
        }
        final String body = OBJECT_MAPPER.writeValueAsString(Map.of("query", query, "variables", variables));
        final Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .header("User-Agent", "BookSeries - tracks new releases of book series")
                .post(RequestBody.create(body, JSON))
                .build();

        for (int attempt = 1; ; attempt++) {
            try (Response response = CLIENT.newCall(request).execute()) {
                final String responseBody = response.body() == null ? "" : response.body().string();
                if ((response.code() == 429 || response.code() == 503) && attempt < 4) {
                    final String retryAfter = response.header("Retry-After");
                    final long waitSeconds = retryAfter != null && retryAfter.matches("\\d+") ? Long.parseLong(retryAfter) : 10;
                    System.out.printf("Hardcover API returned %d, retrying in %d seconds%n", response.code(), waitSeconds);
                    Thread.sleep(waitSeconds * 1000);
                    continue;
                }
                if (!response.isSuccessful()) {
                    throw new IllegalStateException("Hardcover API returned " + response.code() + ": " + responseBody);
                }
                final JsonNode json = OBJECT_MAPPER.readTree(responseBody);
                if (json.has("errors")) {
                    throw new IllegalStateException("Hardcover API returned errors: " + json.get("errors"));
                }
                return json.path("data");
            }
        }
    }
}
