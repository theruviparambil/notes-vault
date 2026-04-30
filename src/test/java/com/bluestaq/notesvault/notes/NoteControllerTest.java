package com.bluestaq.notesvault.notes;

import com.bluestaq.notesvault.api.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoteController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("NoteController web layer")
class NoteControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean NoteService service;

    @Test
    @DisplayName("POST /notes returns 201 Created with Location header on valid input")
    void create_returns201_whenValid() throws Exception {
        UUID id = UUID.randomUUID();
        NoteResponse saved = new NoteResponse(id, "Hello", Instant.parse("2026-04-29T10:00:00Z"));
        given(service.create(any())).willReturn(saved);

        mvc.perform(post("/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new NoteRequest("Hello"))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/notes/" + id))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.content").value("Hello"));
    }

    @Test
    @DisplayName("POST /notes returns 400 ProblemDetail when content is blank")
    void create_returns400_whenContentBlank() throws Exception {
        mvc.perform(post("/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.errors[0].field").value("content"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /notes returns 400 when content exceeds maximum length")
    void create_returns400_whenContentTooLong() throws Exception {
        String tooLong = "x".repeat(10_001);
        mvc.perform(post("/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new NoteRequest(tooLong))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("content"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET /notes returns the list of notes")
    void list_returnsNotes() throws Exception {
        given(service.list()).willReturn(List.of(
            new NoteResponse(UUID.randomUUID(), "A", Instant.now()),
            new NoteResponse(UUID.randomUUID(), "B", Instant.now())
        ));

        mvc.perform(get("/notes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].content").value("A"));
    }

    @Test
    @DisplayName("GET /notes/{id} returns 404 ProblemDetail when missing")
    void get_returns404_whenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        given(service.findById(id)).willThrow(new NoteNotFoundException(id));

        mvc.perform(get("/notes/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Note not found"))
            .andExpect(jsonPath("$.noteId").value(id.toString()));
    }

    @Test
    @DisplayName("GET /notes/{id} returns 400 when id is not a valid UUID")
    void get_returns400_whenIdMalformed() throws Exception {
        mvc.perform(get("/notes/not-a-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /notes/{id} returns 204 on success")
    void delete_returns204_whenSuccess() throws Exception {
        UUID id = UUID.randomUUID();

        mvc.perform(delete("/notes/{id}", id))
            .andExpect(status().isNoContent());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /notes/{id} returns 404 ProblemDetail when missing")
    void delete_returns404_whenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        willThrow(new NoteNotFoundException(id)).given(service).delete(id);

        mvc.perform(delete("/notes/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Note not found"));
    }
}
