package com.ihm.backend.lti;

import com.ihm.backend.enums.UserRole;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LtiRoleMapperTest {

    @Test
    void learnerRoleMapsToStudent() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("sub1")
                .claim("https://purl.imsglobal.org/spec/lti/claim/roles",
                        List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner"))
                .build();
        assertEquals(UserRole.STUDENT, LtiRoleMapper.resolveRole(claims));
    }

    @Test
    void instructorMapsToTeacher() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("sub2")
                .claim("https://purl.imsglobal.org/spec/lti/claim/roles",
                        List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor"))
                .build();
        assertEquals(UserRole.TEACHER, LtiRoleMapper.resolveRole(claims));
    }

    @Test
    void missingRolesDefaultToTeacher() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("sub3").build();
        assertEquals(UserRole.TEACHER, LtiRoleMapper.resolveRole(claims));
    }
}
