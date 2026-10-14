package de.sist.bookseries.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDate;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;


public class Publication {

    private PublicationDate publicationDate;
    private PublicationDate expectedPublicationDate;

    public Publication() {
    }

    public Publication(PublicationDate publicationDate, PublicationDate expectedPublicationDate) {
        this.publicationDate = publicationDate;
        this.expectedPublicationDate = expectedPublicationDate;
    }

    public static Publication published(TemporalAccessor accessor) {
        return new Publication(PublicationDate.from(accessor), null);
    }

    public static Publication expected(TemporalAccessor accessor) {
        return new Publication(null, PublicationDate.from(accessor));
    }


    /**
     * Evaluated against today's date, because a book that was expected when the data was loaded may have been released since.
     */
    public boolean notYetPublished() {
        if (publicationDate != null) {
            return false;
        }
        final Optional<LocalDate> expected = expectedPublicationDate == null ? Optional.empty() : expectedPublicationDate.toLocalDate();
        return expected.isEmpty() || expected.get().isAfter(LocalDate.now());
    }

    /**
     * The publication date if known, the expected publication date otherwise.
     */
    public Optional<LocalDate> releaseDate() {
        final PublicationDate date = publicationDate != null ? publicationDate : expectedPublicationDate;
        return date == null ? Optional.empty() : date.toLocalDate();
    }

    public boolean hasConcreteExpectedData() {
        return expectedPublicationDate == null;
    }

    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    public PublicationDate getExpectedPublicationDate() {
        return expectedPublicationDate;
    }

    @Override
    public String toString() {
        if (publicationDate == null && expectedPublicationDate != null && !notYetPublished()) {
            return expectedPublicationDate.toString();
        } else if (notYetPublished() && expectedPublicationDate != null) {
            return "Expected " + expectedPublicationDate;
        } else if (notYetPublished()) {
            return "No publication date known";
        } else if (publicationDate != null) {
            return publicationDate.toString();
        } else {
            return "<Unknown>";
        }
    }
}
