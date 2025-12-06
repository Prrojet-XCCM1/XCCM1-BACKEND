package com.ihm.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "enseignants")
public class Enseignant extends Utilisateur {
    // Attributs spécifiques, ex. : departement
    private String departement;
}