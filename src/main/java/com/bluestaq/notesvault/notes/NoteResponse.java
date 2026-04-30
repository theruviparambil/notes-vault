package com.bluestaq.notesvault.notes;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
    UUID id,
    String content,
    Instant createdAt
) {
    static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getContent(), note.getCreatedAt());
    }
}
