package com.wq.server.vo;

import com.wq.server.entity.Tenant;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Tenant representation returned to API consumers.
 *
 * @param id           tenant identifier
 * @param code         unique tenant code
 * @param name         display name
 * @param contactEmail administrator contact mailbox
 * @param status       ACTIVE or DISABLED
 * @param createdAt    creation time
 */
@Schema(description = "Tenant profile")
public record TenantVO(Long id, String code, String name, String contactEmail, String status,
                       LocalDateTime createdAt) {

    /**
     * Maps a persistence entity to its API representation.
     *
     * @param tenant entity
     * @return tenant view object
     */
    public static TenantVO from(Tenant tenant) {
        return new TenantVO(tenant.getId(), tenant.getCode(), tenant.getName(), tenant.getContactEmail(),
                tenant.getStatus(), tenant.getCreatedAt());
    }
}
