package com.bluestaq.notesvault.notes;

import java.util.UUID;

public class NoteNotFoundException extends RuntimeException {

    private final UUID noteId;

    public NoteNotFoundException(UUID noteId) {
        super("Note not found: " + noteId);
        this.noteId = noteId;
    }

    public UUID getNoteId() {
        return noteId;
    }
}
