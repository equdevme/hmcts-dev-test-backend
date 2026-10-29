
package uk.gov.hmcts.reform.dev.controllers;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.models.Caseworker;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;
import uk.gov.hmcts.reform.dev.repositories.CaseworkerRepository;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;
import uk.gov.hmcts.reform.dev.repositories.TaskStatusRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private CaseworkerRepository caseworkerRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    private Caseworker testCaseworker;
    private Case testCase;

    @BeforeEach
    void setUp() {
        testCaseworker = caseworkerRepository.save(
            new Caseworker("Test Caseworker")
        );

        testCase = caseRepository.save(
            new Case(
                testCaseworker,
                "Test Case",
                "A case for integration testing"
            )
        );
    }

    @DisplayName("Should retrieve all cases")
    @Test
    void shouldRetrieveAllCases() throws Exception {
        var secondCase = caseRepository.save(
            new Case(testCaseworker, "Second Case", null)
        );

        mockMvc.perform(get("/cases"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + testCase.getId() + ")]").exists())
            .andExpect(jsonPath("$[?(@.id == " + secondCase.getId() + ")]").exists());
    }

    @DisplayName("Should filter cases by owner")
    @Test
    void shouldFilterCasesByOwner() throws Exception {
        var otherCaseworker = caseworkerRepository.save(
            new Caseworker("Other Caseworker")
        );

        caseRepository.save(
            new Case(otherCaseworker, "Other Case", null)
        );

        mockMvc.perform(get("/cases")
                .param("ownerId", testCaseworker.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(testCase.getId()))
            .andExpect(jsonPath("$[0].ownerId").value(testCaseworker.getId()));
    }

    @DisplayName("Should retrieve a case by ID")
    @Test
    void shouldRetrieveCaseById() throws Exception {
        mockMvc.perform(get("/cases/{id}", testCase.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testCase.getId()))
            .andExpect(jsonPath("$.ownerId").value(testCaseworker.getId()))
            .andExpect(jsonPath("$.title").value("Test Case"))
            .andExpect(jsonPath("$.description")
                .value("A case for integration testing"));
    }

    @DisplayName("Should return 404 when case does not exist")
    @Test
    void shouldReturnNotFoundForUnknownCase() throws Exception {
        mockMvc.perform(get("/cases/{id}", -1))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Case not found: -1"));
    }

    @DisplayName("Should retrieve only tasks associated with a case")
    @Test
    void shouldRetrieveTasksForCase() throws Exception {
        var otherCase = caseRepository.save(
            new Case(testCaseworker, "Other Case", null)
        );

        var defaultStatus = taskStatusRepository.findByIsDefaultTrue()
            .orElseThrow();

        var firstTaskToSave = new Task(
            testCase,
            testCaseworker,
            "Review documents",
            null,
            Instant.parse("2026-11-05T12:00:00Z")
        );
        firstTaskToSave.setStatus(defaultStatus);

        var firstTask = taskRepository.saveAndFlush(firstTaskToSave);

        var secondTaskToSave = new Task(
            testCase,
            testCaseworker,
            "Prepare hearing",
            null,
            Instant.parse("2026-11-06T12:00:00Z")
        );
        secondTaskToSave.setStatus(defaultStatus);

        var secondTask = taskRepository.saveAndFlush(secondTaskToSave);

        var unrelatedTask = new Task(
            otherCase,
            testCaseworker,
            "Unrelated task",
            null,
            Instant.parse("2026-11-04T12:00:00Z")
        );
        unrelatedTask.setStatus(defaultStatus);

        taskRepository.saveAndFlush(unrelatedTask);

        mockMvc.perform(get("/cases/{id}/tasks", testCase.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(firstTask.getId()))
            .andExpect(jsonPath("$[0].caseId").value(testCase.getId()))
            .andExpect(jsonPath("$[0].title").value("Review documents"))
            .andExpect(jsonPath("$[1].id").value(secondTask.getId()))
            .andExpect(jsonPath("$[1].caseId").value(testCase.getId()));
    }

    @DisplayName("Should return an empty list when case has no tasks")
    @Test
    void shouldReturnEmptyTaskList() throws Exception {
        mockMvc.perform(get("/cases/{id}/tasks", testCase.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @DisplayName("Should return 404 when retrieving tasks for an unknown case")
    @Test
    void shouldReturnNotFoundForUnknownCaseTasks() throws Exception {
        mockMvc.perform(get("/cases/{id}/tasks", -1))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Case not found: -1"));
    }
}
