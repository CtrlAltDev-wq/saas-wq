package com.wq.server.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Authentication and coarse authorization behaviour of the HTTP layer.
 */
class AuthSecurityIntegrationTest extends AbstractIntegrationTest {

    @Test
    void loginReturnsAnAccessToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantCode":"acme","username":"acme.admin","password":"Admin@123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.data.user.tenantId").value(2));
    }

    @Test
    void loginRejectsAWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantCode":"acme","username":"acme.admin","password":"wrong"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void loginRejectsAnUnknownTenant() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantCode":"unknown","username":"acme.admin","password":"Admin@123"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpointRejectsAnonymousCalls() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCannotListUsers() throws Exception {
        String token = login("acme", "acme.member");

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanListCustomers() throws Exception {
        String token = login("acme", "acme.member");

        mockMvc.perform(get("/api/customers").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber());
    }
}
