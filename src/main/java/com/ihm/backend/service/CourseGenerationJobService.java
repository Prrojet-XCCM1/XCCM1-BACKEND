package com.ihm.backend.service;

import com.ihm.backend.dto.request.CourseAIGenerateRequest;
import com.ihm.backend.dto.response.CourseGenerationJobCreatedResponse;
import com.ihm.backend.dto.response.CourseGenerationJobResponse;
import com.ihm.backend.entity.CourseGenerationJob;
import com.ihm.backend.enums.CourseGenerationJobStatus;
import com.ihm.backend.repository.jpa.CourseGenerationJobRepository;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseGenerationJobService {

    private final CourseGenerationJobRepository jobRepository;
    private final LLMIndexingService llmIndexingService;

    private final ExecutorService jobExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public CourseGenerationJobCreatedResponse startJob(UUID userId, CourseAIGenerateRequest request) {
        CourseAIGenerateRequest normalizedRequest = cloneRequest(request);

        CourseGenerationJob job = CourseGenerationJob.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(CourseGenerationJobStatus.PENDING)
                .requestPayload(toRequestPayload(normalizedRequest))
                .progressEvent("queued")
                .progressMessage("Génération en attente de démarrage.")
                .progressPercent(0)
                .build();

        jobRepository.save(job);
        jobExecutor.submit(() -> executeJob(job.getId(), normalizedRequest));

        return CourseGenerationJobCreatedResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .build();
    }

    public CourseGenerationJobResponse getJob(UUID jobId, UUID userId) {
        CourseGenerationJob job = jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Job de génération introuvable"));
        return toResponse(job);
    }

    public List<CourseGenerationJobResponse> getActiveJobsForUser(UUID userId) {
        return jobRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                        userId,
                        EnumSet.of(CourseGenerationJobStatus.PENDING, CourseGenerationJobStatus.RUNNING)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void executeJob(UUID jobId, CourseAIGenerateRequest request) {
        markRunning(jobId);

        try {
            Map<String, Object> result = llmIndexingService.generateCourseAndCollectResult(
                    request,
                    progress -> updateProgress(jobId, progress)
            );

            CourseGenerationJob job = getJobOrThrow(jobId);
            job.setStatus(CourseGenerationJobStatus.COMPLETED);
            job.setResultPayload(result);
            job.setErrorMessage(null);
            job.setCompletedAt(LocalDateTime.now());
            job.setProgressEvent("done");
            job.setProgressMessage("Cours généré avec succès.");
            job.setProgressPercent(100);
            jobRepository.save(job);
        } catch (Exception ex) {
            log.error("Échec de la génération IA pour le job {}: {}", jobId, ex.getMessage(), ex);

            CourseGenerationJob job = getJobOrThrow(jobId);
            job.setStatus(CourseGenerationJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            job.setProgressEvent("error");
            job.setProgressMessage(ex.getMessage());
            jobRepository.save(job);
        }
    }

    private void markRunning(UUID jobId) {
        CourseGenerationJob job = getJobOrThrow(jobId);
        job.setStatus(CourseGenerationJobStatus.RUNNING);
        job.setProgressEvent("started");
        job.setProgressMessage("Génération lancée.");
        job.setProgressPercent(0);
        jobRepository.save(job);
    }

    private void updateProgress(UUID jobId, LLMIndexingService.CourseGenerationProgress progress) {
        CourseGenerationJob job = getJobOrThrow(jobId);
        job.setProgressEvent(progress.event());
        job.setProgressMessage(progress.message());
        job.setProgressPercent(progress.percent());
        jobRepository.save(job);
    }

    private CourseGenerationJob getJobOrThrow(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job de génération introuvable"));
    }

    private CourseGenerationJobResponse toResponse(CourseGenerationJob job) {
        return CourseGenerationJobResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .progressEvent(job.getProgressEvent())
                .progressMessage(job.getProgressMessage())
                .progressPercent(job.getProgressPercent())
                .result(job.getResultPayload())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    private CourseAIGenerateRequest cloneRequest(CourseAIGenerateRequest request) {
        CourseAIGenerateRequest clone = new CourseAIGenerateRequest();
        clone.setDescription(request.getDescription());
        clone.setDiscipline(request.getDiscipline());
        clone.setLevel(request.getLevel());
        clone.setLanguage(request.getLanguage());
        clone.setExercisesPerChapter(request.getExercisesPerChapter());
        clone.setCourseId(request.getCourseId());
        return clone;
    }

    private Map<String, Object> toRequestPayload(CourseAIGenerateRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("description", request.getDescription());
        payload.put("discipline", request.getDiscipline());
        payload.put("level", request.getLevel());
        payload.put("language", request.getLanguage());
        payload.put("exercisesPerChapter", request.getExercisesPerChapter());
        payload.put("courseId", request.getCourseId());
        return payload;
    }

    @PreDestroy
    void shutdownExecutor() {
        jobExecutor.shutdown();
    }
}
