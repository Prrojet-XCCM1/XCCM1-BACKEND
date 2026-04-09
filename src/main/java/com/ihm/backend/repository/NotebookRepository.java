package com.ihm.backend.repository;

import com.ihm.backend.entity.Notebook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotebookRepository extends JpaRepository<Notebook, UUID> {
    List<Notebook> findByUser_Id(UUID userId);
}
