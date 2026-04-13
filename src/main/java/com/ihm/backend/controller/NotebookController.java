package com.ihm.backend.controller;

import com.ihm.backend.entity.Notebook;
import com.ihm.backend.entity.NotebookSource;
import com.ihm.backend.entity.User;
import com.ihm.backend.repository.jpa.NotebookRepository;
import com.ihm.backend.repository.jpa.NotebookSourceRepository;
import com.ihm.backend.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notebooks")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookRepository notebookRepository;
    private final NotebookSourceRepository notebookSourceRepository;
    private final UserRepository userRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notebook>> getUserNotebooks(@PathVariable UUID userId) {
        return ResponseEntity.ok(notebookRepository.findByUser_Id(userId));
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<Notebook> createNotebook(@PathVariable UUID userId, @RequestBody Map<String, String> request) {
        User user = userRepository.findById(userId).orElseThrow();
        Notebook notebook = Notebook.builder()
                .user(user)
                .title(request.getOrDefault("title", "Nouveau Notebook"))
                .metadata(request.get("metadata"))
                .build();
        return ResponseEntity.ok(notebookRepository.save(notebook));
    }

    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> deleteNotebook(@PathVariable UUID id) {
        notebookSourceRepository.deleteAll(notebookSourceRepository.findByNotebook_Id(id));
        notebookRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{notebookId}/sources")
    public ResponseEntity<List<NotebookSource>> getNotebookSources(@PathVariable UUID notebookId) {
        return ResponseEntity.ok(notebookSourceRepository.findByNotebook_Id(notebookId));
    }

    @PostMapping("/{notebookId}/sources")
    public ResponseEntity<NotebookSource> addSource(@PathVariable UUID notebookId, @RequestBody Map<String, String> request) {
        Notebook notebook = notebookRepository.findById(notebookId).orElseThrow();
        NotebookSource source = NotebookSource.builder()
                .notebook(notebook)
                .name(request.get("name"))
                .type(request.get("type"))
                .storagePath(request.get("storagePath"))
                .contentText(request.get("contentText"))
                .build();
        return ResponseEntity.ok(notebookSourceRepository.save(source));
    }
}
