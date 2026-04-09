package com.ihm.backend.repository.jpa;

import com.ihm.backend.entity.NotebookSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotebookSourceRepository extends JpaRepository<NotebookSource, UUID> {
    List<NotebookSource> findByNotebook_Id(UUID notebookId);
}
