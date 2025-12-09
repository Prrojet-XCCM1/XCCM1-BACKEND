package com.ihm.backend.entities;

import java.time.LocalDateTime;

import com.ihm.backend.enums.UserRole;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
@Entity
@Table(name = "\"user\"") 
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    @Enumerated(value = EnumType.STRING)
    private  UserRole role;
    private String photoUrl;
    private Boolean isActive;
    
    private LocalDateTime createdAt;
}
