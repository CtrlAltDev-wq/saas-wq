package com.wq.server.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wq.framework.audit.Audit;
import com.wq.framework.security.LoginUser;
import com.wq.framework.tenant.TenantContext;
import com.wq.server.dto.LoginRequest;
import com.wq.server.entity.SysAuditLog;
import com.wq.server.service.AuditLogService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Writes one audit entry per invocation of a method annotated with {@link Audit}.
 *
 * <p>The aspect never changes the outcome of the business call: persistence failures are
 * logged and swallowed, while exceptions are rethrown untouched. Sensitive arguments are
 * masked before they reach the database.</p>
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "secret", "token");

    private static final int MAX_ARGUMENT_LENGTH = 2000;

    private static final int MAX_ERROR_LENGTH = 512;

    private final AuditLogService auditLogService;

    private final ObjectMapper objectMapper;

    /**
     * Records the invocation and always lets the business result through.
     *
     * @param joinPoint audited invocation
     * @param audit     annotation metadata
     * @return whatever the business method returned
     * @throws Throwable whatever the business method threw
     */
    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long startedAt = System.nanoTime();
        boolean success = true;
        String errorMessage = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            success = false;
            errorMessage = truncate(throwable.getMessage(), MAX_ERROR_LENGTH);
            throw throwable;
        } finally {
            persist(joinPoint, audit, success, errorMessage, (System.nanoTime() - startedAt) / 1_000_000L);
        }
    }

    private void persist(ProceedingJoinPoint joinPoint, Audit audit, boolean success, String errorMessage,
                         long durationMs) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.debug("Audit entry {} skipped because no tenant is bound to the request", audit.action());
            return;
        }
        try {
            Object[] arguments = joinPoint.getArgs();
            SysAuditLog entry = new SysAuditLog();
            entry.setTenantId(tenantId);
            entry.setAction(audit.action());
            entry.setResource(audit.resource());
            entry.setSuccess(success);
            entry.setErrorMessage(errorMessage);
            entry.setDurationMs(durationMs);
            entry.setCreatedAt(LocalDateTime.now());
            entry.setRequestArgs(describeArguments(arguments));
            entry.setUsername(resolveUsername(arguments));
            fillActor(entry, arguments);
            fillRequest(entry);
            auditLogService.record(entry);
        } catch (Exception exception) {
            log.error("Unable to persist the audit entry of action {}", audit.action(), exception);
        }
    }

    private void fillActor(SysAuditLog entry, Object[] arguments) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            entry.setUserId(loginUser.userId());
            if (!StringUtils.hasText(entry.getUsername())) {
                entry.setUsername(loginUser.username());
            }
        }
        if (!StringUtils.hasText(entry.getUsername())) {
            entry.setUsername(resolveUsername(arguments));
        }
    }

    private void fillRequest(SysAuditLog entry) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        entry.setHttpMethod(request.getMethod());
        entry.setUri(request.getRequestURI());
        entry.setIpAddress(resolveClientIp(request));
    }

    private String resolveUsername(Object[] arguments) {
        if (arguments == null) {
            return null;
        }
        for (Object argument : arguments) {
            if (argument instanceof LoginRequest loginRequest) {
                return loginRequest.username();
            }
        }
        return null;
    }

    private String describeArguments(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return null;
        }
        List<Object> payload = new ArrayList<>();
        for (Object argument : arguments) {
            if (argument == null || argument instanceof ServletRequest || argument instanceof ServletResponse) {
                continue;
            }
            payload.add(argument);
        }
        if (payload.isEmpty()) {
            return null;
        }
        try {
            Object candidate = payload.size() == 1 ? payload.get(0) : payload;
            JsonNode tree = objectMapper.valueToTree(candidate);
            mask(tree);
            return truncate(objectMapper.writeValueAsString(tree), MAX_ARGUMENT_LENGTH);
        } catch (Exception exception) {
            log.debug("Unable to serialise audited arguments: {}", exception.getMessage());
            return null;
        }
    }

    private void mask(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                if (isSensitive(fieldName)) {
                    objectNode.put(fieldName, "***");
                } else {
                    mask(objectNode.get(fieldName));
                }
            }
            return;
        }
        if (node != null && node.isArray()) {
            node.forEach(this::mask);
        }
    }

    private boolean isSensitive(String fieldName) {
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELDS.stream().anyMatch(normalized::contains);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
