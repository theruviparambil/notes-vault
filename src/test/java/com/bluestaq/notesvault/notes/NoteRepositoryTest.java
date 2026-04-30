package com.bluestaq.notesvault.notes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("NoteRepository data layer")
class NoteRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired NoteRepository repository;

    @Test
    @DisplayName("save assigns id, persists content, and populates created_at via auditing")
    void save_assignsAuditFields() {
        Note note = Note.create("My first note");

        Note saved = repository.saveAndFlush(note);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getContent()).isEqualTo("My first note");
        assertThat(saved.getCreatedAt())
            .isNotNull()
            .isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    @DisplayName("findById returns the saved note")
    void findById_returnsSavedNote() {
        Note saved = repository.saveAndFlush(Note.create("Find me"));

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("Find me");
    }

    @Test
    @DisplayName("delete removes the note from the database")
    void delete_removesNote() {
        Note saved = repository.saveAndFlush(Note.create("Doomed"));

        repository.delete(saved);
        repository.flush();

        assertThat(repository.findById(saved.getId())).isEmpty();
    }
}
