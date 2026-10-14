package de.sist.bookseries;

import de.sist.bookseries.model.Publication;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicationTest {

    @Test
    public void shouldTreatExpectedBookAsPublishedOnceDateHasPassed() {
        LocalDate past = LocalDate.now().minusDays(10);
        Publication publication = Publication.expected(past);
        assertFalse(publication.notYetPublished());
        assertEquals(past, publication.releaseDate().get());
    }

    @Test
    public void shouldTreatExpectedBookAsNotYetPublishedBeforeDate() {
        assertTrue(Publication.expected(LocalDate.now().plusDays(10)).notYetPublished());
    }
}
