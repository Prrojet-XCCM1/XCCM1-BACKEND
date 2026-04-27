package com.ihm.backend.dto.response;

import com.ihm.backend.enums.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInvitationResponse {
    private Long id;
    private Integer courseId;
    private String courseTitle;
    private String inviterName;
    private String email;
    private String token;
    private InvitationStatus status;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
}
