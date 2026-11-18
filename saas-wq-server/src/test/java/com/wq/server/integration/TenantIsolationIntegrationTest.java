package com.wq.server.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Row level isolation guaranteed by the MyBatis-Plus tenant plugin.
 */
class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void acmeCannotReadAGlobexCustomer() throws Exception {
        String token = login("acme", "acme.admin");

        mockMvc.perform(get("/api/customers/4").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void globexCannotReadAnAcmeCustomer() throws Exception {
        String token = login("globex", "globex.admin");

        mockMvc.perform(get("/api/customers/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void acmeReadsItsOwnCustomer() throws Exception {
        String token = login("acme", "acme.admin");

        mockMvc.perform(get("/api/customers/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alice Johnson"));
    }

    @Test
    void customerListingNeverLeaksOtherTenants() throws Exception {
        String token = login("acme", "acme.admin");

        MvcResult result = mockMvc.perform(get("/api/customers?size=100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode records = json(result).path("data").path("records");
        assertFalse(records.isEmpty(), "The acme tenant should own demo customers");
        for (JsonNode record : records) {
            String company = record.path("company").asText();
            assertFalse("Initech".equals(company) || "Umbrella Group".equals(company),
                    "Globex customer leaked into the acme listing: " + record);
        }
    }
}
