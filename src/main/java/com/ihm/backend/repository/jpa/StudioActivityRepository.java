package com.ihm.backend.repository.jpa;

import com.ihm.backend.entity.StudioActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudioActivityRepository extends JpaRepository<StudioActivity, UUID> {
    List<StudioActivity> findByNotebook_Id(UUID notebookId);
}
