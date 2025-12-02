package cm.enspy.xccm.domain.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("course_enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEnrollment {
    @Id
    private UUID id;
    private UUID studentId;
    private UUID courseId;
    private EnrollmentStatus status;
    @Builder.Default
    private Boolean isApproved = false;
    private LocalDateTime enrollmentDate;
    private LocalDateTime approvalDate;
    private String approvalMessage;

    public enum EnrollmentStatus {
        EN_ATTENTE,
        APPROUVE,
        REFUSE
    }

    public CourseEnrollment(UUID studentId, UUID courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.status = EnrollmentStatus.EN_ATTENTE;
        this.enrollmentDate = LocalDateTime.now();
    }

    public void approve() {
        this.status = EnrollmentStatus.APPROUVE;
        this.approvalDate = LocalDateTime.now();
        // Envoyer une notification à l'étudiant
    }

    public void refuse(String message) {
        this.status = EnrollmentStatus.REFUSE;
        this.approvalDate = LocalDateTime.now();
        this.approvalMessage = message;
        // Envoyer une notification à l'étudiant
    }
}