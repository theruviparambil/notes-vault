package com.bluestaq.notesvault.notes;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notes")
@EntityListeners(AuditingEntityListener.class)
class Note {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Note() {
    }

    static Note create(String content) {
        Note note = new Note();
        note.id = UuidCreator.getTimeOrderedEpoch();
        note.content = content;
        return note;
    }

    UUID getId() {
        return id;
    }

    String getContent() {
        return content;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Note other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
