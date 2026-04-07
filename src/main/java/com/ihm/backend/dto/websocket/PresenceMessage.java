package com.ihm.backend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceMessage {
    public enum PresenceType {
        JOIN, LEAVE
    }

    private PresenceType type;
    private String userEmail;
    private String userName;
}
