package Model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents an audit log entry for user actions and system events.
 */
public class AuditLogEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDateTime timestamp;
    private String userId;
    private String action;
    private String entityType;
    private String entityId;
    private String details;

    public AuditLogEntry(String userId, String action, String entityType, String entityId, String details) {
        this.timestamp = LocalDateTime.now();
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    public AuditLogEntry(LocalDateTime timestamp, String userId, String action, String entityType, String entityId, String details) {
        this(userId, action, entityType, entityId, details);
        this.timestamp = timestamp;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }
}
