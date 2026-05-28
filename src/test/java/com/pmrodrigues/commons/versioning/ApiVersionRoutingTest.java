package com.pmrodrigues.commons.versioning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that two controllers declared for the same URL but different API versions
 * are correctly selected based on the {@code X-API-Version} request header.
 *
 * <p>Uses {@link MockMvcBuilders#standaloneSetup} with a custom handler mapping so no
 * Spring Boot context or security configuration is required.
 */
class ApiVersionRoutingTest {

    private MockMvc mockMvc;

    @ApiVersion("1")
    @RestController
    @RequestMapping("/versioned")
    static class VersionOneController {
        @GetMapping
        ResponseEntity<String> get() {
            return ResponseEntity.ok("v1");
        }
    }

    @ApiVersion("2")
    @RestController
    @RequestMapping("/versioned")
    static class VersionTwoController {
        @GetMapping
        ResponseEntity<String> get() {
            return ResponseEntity.ok("v2");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new VersionOneController(), new VersionTwoController())
                .setCustomHandlerMapping(ApiVersionHandlerMapping::new)
                .build();
    }

    @Test
    void withVersionHeader1_routesToVersionOneController() throws Exception {
        mockMvc.perform(get("/versioned").header(ApiVersionRequestCondition.API_VERSION_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("v1"));
    }

    @Test
    void withVersionHeader2_routesToVersionTwoController() throws Exception {
        mockMvc.perform(get("/versioned").header(ApiVersionRequestCondition.API_VERSION_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("v2"));
    }

    @Test
    void withoutVersionHeader_routesToLatestVersion() throws Exception {
        mockMvc.perform(get("/versioned"))
                .andExpect(status().isOk())
                .andExpect(content().string("v2"));
    }

    @Test
    void withUnsupportedVersion_returns404() throws Exception {
        mockMvc.perform(get("/versioned").header(ApiVersionRequestCondition.API_VERSION_HEADER, "99"))
                .andExpect(status().isNotFound());
    }
}
