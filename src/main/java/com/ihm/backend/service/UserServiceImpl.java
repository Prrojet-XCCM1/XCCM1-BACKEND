package com.ihm.backend.service;

import com.ihm.backend.dto.response.StudentResponse;
import com.ihm.backend.dto.response.TeacherResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.repository.UserRepository;
import com.ihm.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<User> getAllUsers() {
        // Retourne tous les utilisateurs sans pagination
        return userRepository.findAll();
    }

    @Override
    public List<StudentResponse> getAllStudents() {
        // Récupère tous les étudiants et les mappe
        List<User> students = userRepository.findByRole(UserRole.STUDENT);
        return students.stream()
                .map(this::mapToStudentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TeacherResponse> getAllTeachers() {
        // Récupère tous les enseignants et les mappe
        List<User> teachers = userRepository.findByRole(UserRole.TEACHER);
        return teachers.stream()
                .map(this::mapToTeacherResponse)
                .collect(Collectors.toList());
    }

    // Les autres méthodes restent inchangées...
    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("Aucun utilisateur authentifié");
        }
        
        Object principal = authentication.getPrincipal();
        String username;
        
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé: " + username));
    }

    @Override
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));
    }

    @Override
    @Transactional
    public User updateUser(UUID id, User updatedUser) {
        User existingUser = getUserById(id);
        
        // Mettre à jour seulement les champs autorisés
        if (updatedUser.getFirstName() != null) {
            existingUser.setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getLastName() != null) {
            existingUser.setLastName(updatedUser.getLastName());
        }
        if (updatedUser.getPhotoUrl() != null) {
            existingUser.setPhotoUrl(updatedUser.getPhotoUrl());
        }
        if (updatedUser.getCity() != null) {
            existingUser.setCity(updatedUser.getCity());
        }
        if (updatedUser.getUniversity() != null) {
            existingUser.setUniversity(updatedUser.getUniversity());
        }
        
        // Mise à jour spécifique selon le rôle
        if (existingUser.getRole() == UserRole.STUDENT && updatedUser.getSpecialization() != null) {
            existingUser.setSpecialization(updatedUser.getSpecialization());
        }
        
        if (existingUser.getRole() == UserRole.TEACHER) {
            if (updatedUser.getGrade() != null) {
                existingUser.setGrade(updatedUser.getGrade());
            }
            if (updatedUser.getCertification() != null) {
                existingUser.setCertification(updatedUser.getCertification());
            }
            if (updatedUser.getSubjects() != null) {
                existingUser.setSubjects(updatedUser.getSubjects());
            }
        }
        
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        User user = getUserById(id);
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateUser(UUID id) {
        User user = getUserById(id);
        user.setActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public StudentResponse getStudentById(UUID id) {
        User user = getUserById(id);
        
        if (user.getRole() != UserRole.STUDENT) {
            throw new ResourceNotFoundException("L'utilisateur avec l'ID " + id + " n'est pas un étudiant");
        }
        
        return mapToStudentResponse(user);
    }

    @Override
    public TeacherResponse getTeacherById(UUID id) {
        User user = getUserById(id);
        
        if (user.getRole() != UserRole.TEACHER) {
            throw new ResourceNotFoundException("L'utilisateur avec l'ID " + id + " n'est pas un enseignant");
        }
        
        return mapToTeacherResponse(user);
    }

    private StudentResponse mapToStudentResponse(User user) {
        return StudentResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .photoUrl(user.getPhotoUrl())
                .city(user.getCity())
                .university(user.getUniversity())
                .specialization(user.getSpecialization())
                .registrationDate(user.getRegistrationDate())
                .active(user.isActive())
                .verified(user.isVerified())
                .build();
    }

    private TeacherResponse mapToTeacherResponse(User user) {
        List<String> subjects = null;
        if (user.getSubjects() != null && !user.getSubjects().isEmpty()) {
            subjects = Arrays.asList(user.getSubjects().split(","));
        }
        
        return TeacherResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .photoUrl(user.getPhotoUrl())
                .city(user.getCity())
                .university(user.getUniversity())
                .grade(user.getGrade())
                .certification(user.getCertification())
                .subjects(subjects)
                .registrationDate(user.getRegistrationDate())
                .active(user.isActive())
                .verified(user.isVerified())
                .build();
    }
}