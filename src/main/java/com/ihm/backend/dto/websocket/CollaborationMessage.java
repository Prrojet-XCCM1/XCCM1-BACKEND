package com.ihm.backend.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollaborationMessage {

    public enum MessageType {
        MOVE, LOCK, UNLOCK, CURSOR, BLOCK_UPDATE, ERROR,
        DELETE, RENAME, DUPLICATE
    }

    private MessageType type;
    private String content;
    private String senderEmail;
    private String senderName;
    private String granuleId;
    private Object payload; // For dynamic data like coordinates or content deltas
    private String nodeId; // Pour DELETE, RENAME, DUPLICATE
    private String newTitle; // Pour RENAME
}
