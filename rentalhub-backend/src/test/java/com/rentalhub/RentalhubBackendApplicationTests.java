package com.rentalhub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║           RentalhubApplicationTests — PHASE 2 TESTS             ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║                                                                  ║
 * ║  ANNOTATION: @SpringBootTest                                    ║
 * ║  Loads the FULL Spring application context for the test.        ║
 * ║  This means all beans are created, database is connected,       ║
 * ║  and the app starts in test mode. It's an integration test.     ║
 * ║                                                                  ║
 * ║  ANNOTATION: @AutoConfigureMockMvc                              ║
 * ║  Configures MockMvc — a test utility that lets you make HTTP    ║
 * ║  requests to your controllers WITHOUT starting a real server.   ║
 * ║  Requests go directly to the DispatcherServlet in memory.       ║
 * ║  Much faster than starting a real server for every test.        ║
 * ║                                                                  ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * NOTE FOR PHASE 2: These tests require a running MySQL database.
 * If you don't have MySQL running, the @SpringBootTest context will
 * fail to load. In later phases, we'll add test configurations
 * that use an in-memory H2 database for tests.
 *
 * To run: mvn test (or click the test runner icon in IntelliJ)
 */
@SpringBootTest
@AutoConfigureMockMvc
class RentalhubApplicationTests {

    /**
     * @Autowired — Spring injects the MockMvc bean created by
     * @AutoConfigureMockMvc into this field.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Tests that the Spring ApplicationContext loads without errors.
     * If any bean fails to configure, any @Autowired fails, or any
     * configuration is wrong, this test fails with a clear error.
     *
     * This is the most basic smoke test — if this fails, nothing works.
     */
    @Test
    void contextLoads() {
        // If the context fails to load, this test method never runs
        // and Spring throws a context initialization exception instead
    }

    /**
     * Tests the /health endpoint returns 200 OK with expected body.
     *
     * MockMvc FLUENT API:
     * mockMvc.perform(...)  → Execute a request
     *   .andExpect(...)     → Assert something about the response
     *   .andExpect(...)     → Chain multiple assertions
     *
     * status().isOk()       → Assert HTTP 200
     * jsonPath("$.status")  → Read a JSON field using JSONPath syntax
     *   $.status             → Root ($) → field "status"
     *   $.message            → Root ($) → field "message"
     *
     * .value("UP")          → Assert the field equals "UP"
     */
    @Test
    void healthEndpointReturns200() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("RentalHub API"));
    }

    /**
     * Tests the /health/ping endpoint returns 200 OK with "pong".
     */
    @Test
    void pingEndpointReturnsPong() throws Exception {
        mockMvc.perform(get("/health/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("pong"));
    }
}