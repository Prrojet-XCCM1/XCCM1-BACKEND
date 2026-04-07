package com.ihm.backend.service;

import com.ihm.backend.dto.response.StudentResponse;
import com.ihm.backend.dto.response.TeacherResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserService {

    User getCurrentUser(); // basé sur SecurityContext

    List<User> getAllUsers();

    User getUserById(UUID id);

    User updateUser(UUID id, User updatedUser);

    void deactivateUser(UUID id);

    void activateUser(UUID id);


    // Nouvelles méthodes pour récupérer par rôle
    StudentResponse getStudentById(UUID id);
    
    TeacherResponse getTeacherById(UUID id);
    
    List<StudentResponse> getAllStudents();
    
    List<TeacherResponse> getAllTeachers();
}