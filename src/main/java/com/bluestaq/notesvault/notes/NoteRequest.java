package com.bluestaq.notesvault.notes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
    @NotBlank(message = "content must not be blank")
    @Size(max = 10_000, message = "content must be at most 10000 characters")
    String content
) {
}
