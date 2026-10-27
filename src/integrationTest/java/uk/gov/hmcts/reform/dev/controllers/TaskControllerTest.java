package uk.gov.hmcts.reform.dev.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.dev.models.Caseworker;
import uk.gov.hmcts.reform.dev.repositories.CaseworkerRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaseworkerRepository caseworkerRepository;

    private Caseworker testCaseworker;

    @BeforeEach
    void setUp() {
        testCaseworker = caseworkerRepository.save(
            new Caseworker("Test Caseworker")
        );
    }

    @DisplayName("Should create a task and return 201")
    @Test
    void shouldCreateTask() throws Exception {

        String requestBody = """
            {
                "createdBy": %d,
                "title": "Review case documents",
                "description": "Check submitted evidence",
                "dueAt": "2026-11-05T12:00:00Z"
            }
            """.formatted(testCaseworker.getId());

        mockMvc.perform(post("/tasks")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Review case documents"))
            .andExpect(jsonPath("$.createdBy").value(testCaseworker.getId()))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.dueAt").value("2026-11-05T12:00:00Z"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());
    }

    @DisplayName("Should reject a blank title with 400")
    @Test
    void shouldRejectBlankTitle() throws Exception {
        String requestBody = """
            {
                "createdBy": %d,
                "title": "   ",
                "dueAt": "2026-11-05T12:00:00Z"
            }
            """.formatted(testCaseworker.getId());

        mockMvc.perform(post("/tasks")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.title").exists());
    }

    @DisplayName("Should reject a missing due date with 400")
    @Test
    void shouldRejectMissingDueDate() throws Exception {
        String requestBody = """
            {
                "createdBy": %d,
                "title": "Review case documents"
            }
            """.formatted(testCaseworker.getId());

        mockMvc.perform(post("/tasks")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.dueAt").exists());
    }

    @DisplayName("Should reject an invalid caseworker ID type with 400")
    @Test
    void shouldRejectInvalidCaseworkerIdType() throws Exception {
        String requestBody = """
            {
                "createdBy": "abcd",
                "title": "Review case documents",
                "dueAt": "2026-11-05T12:00:00Z"
            }
            """;

        mockMvc.perform(post("/tasks")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.createdBy").exists());
    }

    @DisplayName("Should return 404 when caseworker does not exist")
    @Test
    void shouldRejectUnknownCaseworker() throws Exception {
        String requestBody = """
            {
                "createdBy": -1,
                "title": "Review case documents",
                "dueAt": "2026-11-05T12:00:00Z"
            }
            """;

        mockMvc.perform(post("/tasks")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail")
                .value("Caseworker not found: -1"));
    }
}
