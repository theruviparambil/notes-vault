package com.bluestaq.notesvault.notes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/notes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notes", description = "Create, read, and delete notes")
public class NoteController {

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new note")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Note created"),
        @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody NoteRequest request) {
        NoteResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List all notes (newest first)")
    @ApiResponse(responseCode = "200", description = "Notes returned")
    public List<NoteResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a note by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Note returned"),
        @ApiResponse(responseCode = "404", description = "Note not found")
    })
    public NoteResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a note by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Note deleted"),
        @ApiResponse(responseCode = "404", description = "Note not found")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
