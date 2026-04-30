package com.bluestaq.notesvault.notes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface NoteRepository extends JpaRepository<Note, UUID> {
}
