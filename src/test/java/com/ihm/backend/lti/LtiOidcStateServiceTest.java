package com.ihm.backend.lti;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LtiOidcStateServiceTest {

    private LtiOidcStateService service;

    @BeforeEach
    void setUp() {
        service = new LtiOidcStateService();
    }

    @Test
    void registerThenConsumeSucceeds() {
        String state = service.registerState();
        assertNotNull(state);
        assertDoesNotThrow(() -> service.validateAndConsume(state));
    }

    @Test
    void consumeTwiceFails() {
        String state = service.registerState();
        service.validateAndConsume(state);
        assertThrows(IllegalArgumentException.class, () -> service.validateAndConsume(state));
    }

    @Test
    void blankStateFails() {
        assertThrows(IllegalArgumentException.class, () -> service.validateAndConsume("  "));
    }
}
