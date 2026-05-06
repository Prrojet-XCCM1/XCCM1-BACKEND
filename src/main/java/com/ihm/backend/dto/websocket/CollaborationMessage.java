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
        MOVE, LOCK, UNLOCK, CURSOR, BLOCK_UPDATE, ERROR
    }

    private MessageType type;
    private String content;
    private String senderEmail;
    private String senderName;
    private String granuleId;
    private Object payload; // For dynamic data like coordinates or content deltas
}
