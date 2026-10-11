package com.lro.common.security;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Role-Based Access Control (RBAC) and Audit definitions enforcing Prompt 24.
 */
public final class SecurityContext {

    public enum Role {
        VIEWER,
        BANK_OPERATIONS,
        TREASURY_ANALYST,
        APPROVER,
        SIMULATOR_ADMIN,
        AUDITOR
    }

    public record UserPrincipal(
            String userId,
            String username,
            Set<Role> roles
    ) implements Serializable {
        public boolean hasRole(Role role) {
            return roles != null && roles.contains(role);
        }

        public void checkPermission(Role requiredRole) {
            if (!hasRole(requiredRole) && !hasRole(Role.SIMULATOR_ADMIN)) {
                throw new SecurityException("Access Denied: User '" + username + "' lacks required role " + requiredRole);
            }
        }
    }

    public record AuditRecord(
            String auditId,
            String actorUserId,
            String roleAtExecution,
            String actionName,
            String targetEntityType,
            String targetEntityId,
            Instant timestamp,
            String detailsJson,
            String signatureHash
    ) implements Serializable {

        public static AuditRecord create(UserPrincipal principal, String action, String entityType, String entityId, String detailsJson) {
            String id = UUID.randomUUID().toString();
            Instant now = Instant.now();
            String hash = Integer.toHexString(Objects.hash(id, principal.userId(), action, entityId, now.toEpochMilli()));
            return new AuditRecord(
                    id,
                    principal.userId(),
                    principal.roles().toString(),
                    action,
                    entityType,
                    entityId,
                    now,
                    detailsJson != null ? detailsJson : "{}",
                    hash
            );
        }
    }
}
