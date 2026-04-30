package com.bluestaq.notesvault.notes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    private final NoteRepository repository;

    NoteService(NoteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    NoteResponse create(NoteRequest request) {
        Note saved = repository.save(Note.create(request.content()));
        log.info("note.created id={} contentLength={}", saved.getId(), saved.getContent().length());
        return NoteResponse.from(saved);
    }

    List<NoteResponse> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream()
            .map(NoteResponse::from)
            .toList();
    }

    NoteResponse findById(UUID id) {
        return repository.findById(id)
            .map(NoteResponse::from)
            .orElseThrow(() -> new NoteNotFoundException(id));
    }

    @Transactional
    void delete(UUID id) {
        Note note = repository.findById(id)
            .orElseThrow(() -> new NoteNotFoundException(id));
        repository.delete(note);
        log.info("note.deleted id={}", id);
    }
}
