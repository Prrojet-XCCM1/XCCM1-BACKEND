package com.ihm.backend.controller;

import com.ihm.backend.dto.request.AcceptanceRequest;
import com.ihm.backend.dto.request.CourseInvitationRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthorDTO;
import com.ihm.backend.dto.response.CourseInvitationResponse;
import com.ihm.backend.entity.CourseInvitation;
import com.ihm.backend.entity.User;
import com.ihm.backend.mappers.CourseInvitationMapper;
import com.ihm.backend.service.CourseInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class CourseInvitationController {

    private final CourseInvitationService invitationService;
    private final CourseInvitationMapper invitationMapper;

    @PostMapping("/invite")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<CourseInvitationResponse>> inviteEditor(
            @Valid @RequestBody CourseInvitationRequest request,
            @AuthenticationPrincipal User user) {
        CourseInvitation invitation = invitationService.inviteEditor(request.getCourseId(), request.getEmailOrName(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("Invitation envoyée avec succès", invitationMapper.toResponse(invitation)));
    }

    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(
            @Valid @RequestBody AcceptanceRequest request,
            @AuthenticationPrincipal User user) {
        invitationService.acceptInvitation(request.getToken(), user);
        return ResponseEntity.ok(ApiResponse.success("Invitation acceptée. Vous êtes maintenant éditeur.", null));
    }

    @GetMapping("/search-users")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<AuthorDTO>>> searchUsers(@RequestParam String query) {
        List<User> users = invitationService.searchUsers(query);
        List<AuthorDTO> authorDTOs = users.stream()
                .map(AuthorDTO::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Utilisateurs trouvés", authorDTOs));
    }
}
