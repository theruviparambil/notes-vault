package com.bluestaq.notesvault;

import com.bluestaq.notesvault.notes.NoteRequest;
import com.bluestaq.notesvault.notes.NoteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Notes API end-to-end")
class NotesApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired TestRestTemplate http;

    @Test
    @DisplayName("happy path: create, fetch, list, delete")
    void fullLifecycle() {
        ResponseEntity<NoteResponse> createResp = http.postForEntity(
            "/notes", new NoteRequest("Mission brief"), NoteResponse.class);

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getHeaders().getLocation()).isNotNull();
        NoteResponse created = createResp.getBody();
        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.content()).isEqualTo("Mission brief");
        assertThat(created.createdAt()).isNotNull();

        ResponseEntity<NoteResponse> fetchResp = http.getForEntity(
            "/notes/" + created.id(), NoteResponse.class);
        assertThat(fetchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetchResp.getBody()).isEqualTo(created);

        ResponseEntity<NoteResponse[]> listResp = http.getForEntity("/notes", NoteResponse[].class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).isNotNull().contains(created);

        ResponseEntity<Void> deleteResp = http.exchange(
            "/notes/" + created.id(), HttpMethod.DELETE, null, Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<JsonNode> getAfterDelete = http.getForEntity(
            "/notes/" + created.id(), JsonNode.class);
        assertThat(getAfterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getAfterDelete.getBody()).isNotNull();
        assertThat(getAfterDelete.getBody().get("title").asText()).isEqualTo("Note not found");
    }

    @Test
    @DisplayName("rejects blank content with 400 ProblemDetail")
    void rejectsBlankContent() {
        ResponseEntity<JsonNode> resp = http.postForEntity(
            "/notes", new NoteRequest(""), JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().get("title").asText()).isEqualTo("Validation failed");
    }
}
