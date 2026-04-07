package com.ihm.backend.repository;

import com.ihm.backend.entity.StudentBehaviorEvent;
import com.ihm.backend.enums.BehaviorEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentBehaviorEventRepository extends JpaRepository<StudentBehaviorEvent, Long> {

    List<StudentBehaviorEvent> findByStudentIdOrderByOccurredAtDesc(UUID studentId);

    List<StudentBehaviorEvent> findByStudentIdAndNotion(UUID studentId, String notion);

    @Query("SELECT AVG(e.rawScore) FROM StudentBehaviorEvent e " +
           "WHERE e.student.id = :studentId AND e.notion = :notion " +
           "AND e.eventType = :eventType AND e.rawScore IS NOT NULL")
    Double findAverageScoreByStudentAndNotion(
        @Param("studentId") UUID studentId,
        @Param("notion") String notion,
        @Param("eventType") BehaviorEventType eventType
    );

    @Query("SELECT AVG(e.durationSeconds) FROM StudentBehaviorEvent e " +
           "WHERE e.student.id = :studentId AND e.notion = :notion " +
           "AND e.eventType = 'CONTENT_READ'")
    Double findAverageReadDurationByStudentAndNotion(
        @Param("studentId") UUID studentId,
        @Param("notion") String notion
    );
}
