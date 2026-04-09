package com.ihm.backend.repository.jpa;

import com.ihm.backend.entity.NotebookChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotebookChatMessageRepository extends JpaRepository<NotebookChatMessage, UUID> {
    List<NotebookChatMessage> findByNotebook_IdOrderByCreatedAtAsc(UUID notebookId);
}
