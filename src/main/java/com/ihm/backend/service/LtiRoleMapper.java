package com.ihm.backend.service;

import com.ihm.backend.enums.UserRole;
import com.nimbusds.jwt.JWTClaimsSet;

import java.util.List;

final class LtiRoleMapper {

    private static final String ROLES_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/roles";

    private LtiRoleMapper() {
    }

    static UserRole resolveRole(JWTClaimsSet claims) {
        Object raw = claims.getClaim(ROLES_CLAIM);
        if (!(raw instanceof List<?> roles) || roles.isEmpty()) {
            return UserRole.TEACHER;
        }
        boolean learner = false;
        boolean instructorLike = false;
        for (Object o : roles) {
            if (!(o instanceof String uri)) {
                continue;
            }
            String u = uri.toLowerCase();
            if (u.contains("#learner") || u.contains("/learner") || u.endsWith("learner")) {
                learner = true;
            }
            if (u.contains("#instructor")
                    || u.contains("#administrator")
                    || u.contains("#teachingassistant")
                    || u.contains("#contentdeveloper")
                    || u.contains("/instructor")
                    || u.contains("/administrator")) {
                instructorLike = true;
            }
        }
        if (learner && !instructorLike) {
            return UserRole.STUDENT;
        }
        return UserRole.TEACHER;
    }
}
