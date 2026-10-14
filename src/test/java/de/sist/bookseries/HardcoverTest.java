package de.sist.bookseries;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.sist.bookseries.model.Publication;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HardcoverTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void shouldParseSlugFromUrl() {
        assertEquals("red-rising", Hardcover.slugFromUrl("https://hardcover.app/series/red-rising"));
        assertEquals("red-rising", Hardcover.slugFromUrl("https://hardcover.app/series/red-rising/"));
        assertEquals("red-rising", Hardcover.slugFromUrl("hardcover.app/series/red-rising?tab=books"));
    }

    @Test
    public void shouldParsePublication() throws Exception {
        Publication published = Hardcover.parsePublication(OBJECT_MAPPER.readTree("{\"release_date\": \"2014-01-28\", \"release_year\": 2014}"));
        assertEquals(LocalDate.of(2014, 1, 28), published.getPublicationDate().toLocalDate().get());

        LocalDate future = LocalDate.now().plusMonths(3);
        Publication expected = Hardcover.parsePublication(OBJECT_MAPPER.readTree("{\"release_date\": \"" + future + "\"}"));
        assertTrue(expected.notYetPublished());
        assertEquals(future, expected.getExpectedPublicationDate().toLocalDate().get());

        Publication yearOnly = Hardcover.parsePublication(OBJECT_MAPPER.readTree("{\"release_date\": null, \"release_year\": 2010}"));
        assertEquals(2010, yearOnly.getPublicationDate().getYear());

        assertNull(Hardcover.parsePublication(OBJECT_MAPPER.readTree("{\"release_date\": null, \"release_year\": null}")));
    }
}
