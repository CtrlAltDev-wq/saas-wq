package com.wq.server.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * End to end quota enforcement: the fourth request of a three request window is rejected.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "wq.rate-limit.requests-per-minute=3")
class RateLimitIntegrationTest extends AbstractIntegrationTest {

    @Test
    void rejectsRequestsAboveTheTenantQuota() throws Exception {
        String token = login("acme", "acme.admin");
        for (int attempt = 0; attempt < 3; attempt++) {
            call(token).andExpect(status().isOk());
        }

        ResultActions rejected = call(token);

        rejected.andExpect(status().isTooManyRequests());
        MvcResult result = rejected.andReturn();
        assertNotNull(result.getResponse().getHeader(HttpHeaders.RETRY_AFTER));
        assertEquals("3", result.getResponse().getHeader("X-RateLimit-Limit"));
        assertEquals("0", result.getResponse().getHeader("X-RateLimit-Remaining"));
        assertEquals(2007, json(result).path("code").asInt());
    }

    private ResultActions call(String token) throws Exception {
        return mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
