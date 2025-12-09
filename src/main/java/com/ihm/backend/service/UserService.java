// UserService.java
package com.ihm.backend.service;

import com.ihm.backend.domain.entity.User;
import com.ihm.backend.domain.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    User getCurrentUser(); // basé sur SecurityContext

    Page<User> getAllUsers(Pageable pageable);

    User getUserById(UUID id);

    User updateUser(UUID id, User updatedUser);

    void deactivateUser(UUID id);

    void activateUser(UUID id);
}