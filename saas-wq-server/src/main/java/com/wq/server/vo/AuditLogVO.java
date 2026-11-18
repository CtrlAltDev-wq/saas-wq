package com.wq.server.vo;

import com.wq.server.entity.SysAuditLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Audit trail entry returned to tenant administrators.
 *
 * @param id           audit entry identifier
 * @param tenantId     owning tenant
 * @param userId       actor identifier
 * @param username     actor login name
 * @param action       business action
 * @param resource     affected resource
 * @param httpMethod   HTTP method
 * @param uri          request URI
 * @param requestArgs  sanitised request arguments
 * @param success      whether the operation succeeded
 * @param errorMessage failure message
 * @param durationMs   execution time in milliseconds
 * @param ipAddress    client address
 * @param createdAt    creation time
 */
@Schema(description = "Audit trail entry")
public record AuditLogVO(Long id, Long tenantId, Long userId, String username, String action, String resource,
                         String httpMethod, String uri, String requestArgs, boolean success, String errorMessage,
                         Long durationMs, String ipAddress, LocalDateTime createdAt) {

    /**
     * Maps a persistence entity to the API representation.
     *
     * @param log audit entity
     * @return audit view object
     */
    public static AuditLogVO from(SysAuditLog log) {
        return new AuditLogVO(log.getId(), log.getTenantId(), log.getUserId(), log.getUsername(), log.getAction(),
                log.getResource(), log.getHttpMethod(), log.getUri(), log.getRequestArgs(),
                Boolean.TRUE.equals(log.getSuccess()), log.getErrorMessage(), log.getDurationMs(),
                log.getIpAddress(), log.getCreatedAt());
    }
}
