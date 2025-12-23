package com.ihm.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    STUDENT,
    TEACHER,
    ADMIN;

    @JsonValue
    public String toLowerCase() {
        return this.name().toLowerCase();
    }

    @JsonCreator
    public static UserRole fromString(String value) {
        if (value == null) {
            return null;
        }
        return UserRole.valueOf(value.toUpperCase());
    }
}