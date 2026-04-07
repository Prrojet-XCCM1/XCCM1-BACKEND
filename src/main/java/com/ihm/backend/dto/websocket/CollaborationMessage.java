package com.ihm.backend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaborationMessage {
    public enum MessageType {
        MOVE, LOCK, UNLOCK, CURSOR, ERROR
    }

    private MessageType type;
    private String content;
    private String senderEmail;
    private String senderName;
    private Long granuleId;
    private Object payload; // For dynamic data like coordinates or content deltas
}
