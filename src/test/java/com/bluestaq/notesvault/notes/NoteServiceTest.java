package com.bluestaq.notesvault.notes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteService business logic")
class NoteServiceTest {

    @Mock NoteRepository repository;
    @InjectMocks NoteService service;

    @Test
    @DisplayName("create persists the note and returns response with generated id and timestamp")
    void create_persistsAndReturnsResponse() {
        Instant now = Instant.parse("2026-04-29T10:00:00Z");
        given(repository.save(any(Note.class))).willAnswer(inv -> {
            Note n = inv.getArgument(0);
            ReflectionTestUtils.setField(n, "createdAt", now);
            return n;
        });

        NoteResponse result = service.create(new NoteRequest("Hello, world."));

        assertThat(result.id()).isNotNull();
        assertThat(result.content()).isEqualTo("Hello, world.");
        assertThat(result.createdAt()).isEqualTo(now);

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Hello, world.");
        assertThat(captor.getValue().getId()).isNotNull();
    }

    @Test
    @DisplayName("findById returns mapped response when the note exists")
    void findById_returnsResponse_whenFound() {
        Note saved = noteWithTime("Hello", Instant.parse("2026-04-29T10:00:00Z"));
        given(repository.findById(saved.getId())).willReturn(Optional.of(saved));

        NoteResponse result = service.findById(saved.getId());

        assertThat(result.id()).isEqualTo(saved.getId());
        assertThat(result.content()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("findById throws NoteNotFoundException when the note is missing")
    void findById_throws_whenMissing() {
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
            .isInstanceOf(NoteNotFoundException.class)
            .hasMessageContaining(id.toString());
    }

    @Test
    @DisplayName("list requests notes sorted by createdAt descending")
    void list_requestsDescendingSort() {
        Note older = noteWithTime("Older", Instant.parse("2026-01-01T00:00:00Z"));
        Note newer = noteWithTime("Newer", Instant.parse("2026-04-01T00:00:00Z"));
        given(repository.findAll(any(Sort.class))).willReturn(List.of(newer, older));

        List<NoteResponse> result = service.list();

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findAll(sortCaptor.capture());
        assertThat(sortCaptor.getValue().getOrderFor("createdAt").getDirection())
            .isEqualTo(Sort.Direction.DESC);
        assertThat(result).extracting(NoteResponse::content).containsExactly("Newer", "Older");
    }

    @Test
    @DisplayName("delete removes the note when it exists")
    void delete_removesNote_whenFound() {
        Note saved = noteWithTime("Doomed", Instant.now());
        given(repository.findById(saved.getId())).willReturn(Optional.of(saved));

        service.delete(saved.getId());

        verify(repository).delete(saved);
    }

    @Test
    @DisplayName("delete throws NoteNotFoundException when the note is missing")
    void delete_throws_whenMissing() {
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
            .isInstanceOf(NoteNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    private static Note noteWithTime(String content, Instant createdAt) {
        Note n = Note.create(content);
        ReflectionTestUtils.setField(n, "createdAt", createdAt);
        return n;
    }
}
