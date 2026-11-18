package com.wq.server.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Audit trail persistence and live role permission administration.
 */
class AuditAndRbacIntegrationTest extends AbstractIntegrationTest {

    @Test
    void creatingACustomerWritesAnAuditEntry() throws Exception {
        String token = login("acme", "acme.admin");
        mockMvc.perform(post("/api/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Audit Probe","company":"Audit Co","email":"audit@example.com"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Audit Probe"));

        MvcResult result = mockMvc.perform(get("/api/audit-logs?action=CUSTOMER_CREATE&size=50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode records = json(result).path("data").path("records");
        assertFalse(records.isEmpty(), "The create operation must be audited");
        JsonNode entry = records.get(0);
        assertEquals("CUSTOMER_CREATE", entry.path("action").asText());
        assertEquals("/api/customers", entry.path("uri").asText());
        assertEquals("acme.admin", entry.path("username").asText());
        assertTrue(entry.path("success").asBoolean());
        assertTrue(entry.path("requestArgs").asText().contains("Audit Probe"));
    }

    @Test
    void loginAuditNeverStoresThePassword() throws Exception {
        String token = login("acme", "acme.admin");

        MvcResult result = mockMvc.perform(get("/api/audit-logs?action=AUTH_LOGIN&size=50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode records = json(result).path("data").path("records");
        assertFalse(records.isEmpty(), "Login attempts must be audited");
        boolean foundMaskedEntry = false;
        for (JsonNode record : records) {
            if (!"acme.admin".equals(record.path("username").asText())) {
                continue;
            }
            String args = record.path("requestArgs").asText();
            assertFalse(args.contains("Admin@123"), "The password leaked into the audit trail");
            assertTrue(args.contains("***"));
            foundMaskedEntry = true;
        }
        assertTrue(foundMaskedEntry, "No audit entry for the successful acme login");
    }

    @Test
    void rolePermissionChangesApplyOnTheNextRequest() throws Exception {
        String superAdmin = login("platform", "superadmin");
        String tenantAdmin = login("acme", "acme.admin");
        String member = login("acme", "acme.member");
        mockMvc.perform(get("/api/customers").header(HttpHeaders.AUTHORIZATION, "Bearer " + member))
                .andExpect(status().isOk());

        try {
            mockMvc.perform(put("/api/rbac/roles/MEMBER/permissions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"permissionCodes":["customer:read"]}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.permissions.length()").value(1));

            mockMvc.perform(get("/api/orders")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + member))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/customers").header(HttpHeaders.AUTHORIZATION, "Bearer " + member))
                    .andExpect(status().isOk());
        } finally {
            mockMvc.perform(put("/api/rbac/roles/MEMBER/permissions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"permissionCodes":["customer:read","order:read","subscription:read"]}"""))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + member))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/rbac/roles/MEMBER/permissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissionCodes":["customer:read"]}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolesAndPermissionsAreExposedToAdministrators() throws Exception {
        String superAdmin = login("platform", "superadmin");

        MvcResult roles = mockMvc.perform(get("/api/rbac/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdmin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode roleList = json(roles).path("data");
        assertEquals(3, roleList.size());
        assertNotNull(roleList.get(0).path("code").asText());

        MvcResult permissions = mockMvc.perform(get("/api/rbac/permissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdmin))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(12, json(permissions).path("data").size());
    }

    @Test
    void superAdminPermissionsCannotBeDowngraded() throws Exception {
        String superAdmin = login("platform", "superadmin");

        mockMvc.perform(put("/api/rbac/roles/SUPER_ADMIN/permissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissionCodes":["customer:read"]}"""))
                .andExpect(status().isConflict());
    }
}
