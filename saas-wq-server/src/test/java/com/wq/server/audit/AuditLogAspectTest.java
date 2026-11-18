package com.wq.server.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import com.wq.framework.audit.Audit;
import com.wq.framework.tenant.TenantContext;
import com.wq.server.dto.LoginRequest;
import com.wq.server.entity.SysAuditLog;
import com.wq.server.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Audit aspect contract: outcome, masking and tenant requirement.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private Audit audit;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void recordsSuccessfulInvocationWithMaskedPassword() throws Throwable {
        when(audit.action()).thenReturn("AUTH_LOGIN");
        when(audit.resource()).thenReturn("AUTH");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{
                new LoginRequest("acme", "acme.admin", "Admin@123")});
        when(joinPoint.proceed()).thenReturn("token");
        TenantContext.set(2L);

        Object result = aspect().around(joinPoint, audit);

        assertEquals("token", result);
        SysAuditLog entry = capturedEntry();
        assertEquals(2L, entry.getTenantId());
        assertEquals("AUTH_LOGIN", entry.getAction());
        assertEquals("AUTH", entry.getResource());
        assertEquals("acme.admin", entry.getUsername());
        assertTrue(entry.getSuccess());
        assertNull(entry.getErrorMessage());
        assertFalse(entry.getRequestArgs().contains("Admin@123"));
        assertTrue(entry.getRequestArgs().contains("***"));
    }

    @Test
    void recordsFailureAndRethrowsTheBusinessException() throws Throwable {
        when(audit.action()).thenReturn("CUSTOMER_CREATE");
        when(audit.resource()).thenReturn("CUSTOMER");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenThrow(new BusinessException(ResultCode.INVALID_CREDENTIALS));
        TenantContext.set(2L);

        assertThrows(BusinessException.class, () -> aspect().around(joinPoint, audit));

        SysAuditLog entry = capturedEntry();
        assertFalse(entry.getSuccess());
        assertNull(entry.getUsername());
    }

    @Test
    void skipsPersistenceWhenNoTenantIsBound() throws Throwable {
        when(audit.action()).thenReturn("AUTH_LOGIN");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(null);

        aspect().around(joinPoint, audit);

        verifyNoInteractions(auditLogService);
    }

    @Test
    void neverPropagatesPersistenceFailures() throws Throwable {
        when(audit.action()).thenReturn("AUTH_LOGIN");
        when(audit.resource()).thenReturn("AUTH");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn("token");
        TenantContext.set(2L);
        doThrow(new IllegalStateException("database down")).when(auditLogService).record(any());

        assertEquals("token", aspect().around(joinPoint, audit));
    }

    private AuditLogAspect aspect() {
        return new AuditLogAspect(auditLogService, new ObjectMapper());
    }

    private SysAuditLog capturedEntry() {
        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogService).record(captor.capture());
        return captor.getValue();
    }
}
