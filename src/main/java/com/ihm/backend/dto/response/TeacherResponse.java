
package com.ihm.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private String city;
    private String university;
    private String grade;
    private String certification;
    private List<String> subjects;
    private LocalDateTime registrationDate;
    private boolean active;
    private boolean verified;
}