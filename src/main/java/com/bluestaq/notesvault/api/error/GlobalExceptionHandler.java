package com.bluestaq.notesvault.api.error;

import com.bluestaq.notesvault.notes.NoteNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final URI ERR_NOT_FOUND  = URI.create("https://api.bluestaq.example/errors/note-not-found");
    private static final URI ERR_VALIDATION = URI.create("https://api.bluestaq.example/errors/validation-failed");
    private static final URI ERR_CONFLICT   = URI.create("https://api.bluestaq.example/errors/concurrent-modification");

    @ExceptionHandler(NoteNotFoundException.class)
    public ProblemDetail handleNoteNotFound(NoteNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Note not found");
        pd.setType(ERR_NOT_FOUND);
        pd.setProperty("noteId", ex.getNoteId().toString());
        return pd;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "The resource was modified concurrently. Retry the request."
        );
        pd.setTitle("Concurrent modification");
        pd.setType(ERR_CONFLICT);
        return pd;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of(
                "field", fe.getField(),
                "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()
            ))
            .toList();

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setTitle("Validation failed");
        pd.setType(ERR_VALIDATION);
        pd.setProperty("errors", fieldErrors);
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("unhandled exception", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
        pd.setTitle("Internal Server Error");
        return pd;
    }
}
