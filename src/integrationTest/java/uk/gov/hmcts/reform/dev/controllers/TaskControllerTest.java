package uk.gov.hmcts.reform.dev.controllers;

import java.time.Instant;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.dev.models.Caseworker;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.repositories.CaseworkerRepository;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Caseworker testCaseworker;

    @BeforeEach
    void setUp() {
        testCaseworker = caseworkerRepository.save(
            new Caseworker("Test Caseworker")
        );
    }

    private Task createTestTask(
        Caseworker caseworker,
        String title,
        String dueAt
    ) {
        return taskRepository.saveAndFlush(
            new Task(caseworker, title, null, Instant.parse(dueAt))
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

    @DisplayName("Should retrieve all tasks ordered by due date with their creator IDs")
    @Test
    void shouldRetrieveAllTasks() throws Exception {
        Caseworker otherCaseworker = caseworkerRepository.save(
            new Caseworker("Other Caseworker")
        );

        Task firstTask = createTestTask(
            testCaseworker,
            "Review documents",
            "2026-11-05T12:00:00Z"
        );

        Task secondTask = createTestTask(
            otherCaseworker,
            "Prepare hearing",
            "2026-11-06T12:00:00Z"
        );

        mockMvc.perform(get("/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(firstTask.getId()))
            .andExpect(jsonPath("$[0].createdBy").value(testCaseworker.getId()))
            .andExpect(jsonPath("$[0].title").value("Review documents"))
            .andExpect(jsonPath("$[1].id").value(secondTask.getId()))
            .andExpect(jsonPath("$[1].createdBy").value(otherCaseworker.getId()))
            .andExpect(jsonPath("$[1].title").value("Prepare hearing"))
            .andExpect(jsonPath("$.length()").value(2));
    }

    @DisplayName("Should filter tasks by creator")
    @Test
    void shouldFilterTasksByCreator() throws Exception {
        Caseworker otherCaseworker = caseworkerRepository.save(
            new Caseworker("Other Caseworker")
        );

        Task ownTask = createTestTask(
            testCaseworker,
            "My task",
            "2026-11-05T12:00:00Z"
        );

        createTestTask(
            otherCaseworker,
            "Colleague's task",
            "2026-11-06T12:00:00Z"
        );

        mockMvc.perform(get("/tasks")
                .param("createdBy", testCaseworker.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(ownTask.getId()))
            .andExpect(jsonPath("$[0].createdBy").value(testCaseworker.getId()));
    }

    @DisplayName("Should accept case-insensitive sort directions")
    @ParameterizedTest
    @ValueSource(strings = {"asc", "ASC", "desc", "DESC"})
    void shouldAcceptCaseInsensitiveSortDirection(String direction) throws Exception {
        createTestTask(
            testCaseworker,
            "Test task",
            "2026-11-05T12:00:00Z"
        );

        mockMvc.perform(get("/tasks")
                .param("sortDirection", direction))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @DisplayName("Should filter tasks by status")
    @Test
    void shouldFilterTasksByStatus() throws Exception {
        createTestTask(
            testCaseworker,
            "Review documents",
            "2026-11-05T12:00:00Z"
        );

        Task doneTask = createTestTask(
            testCaseworker,
            "Prepare hearing",
            "2026-11-06T12:00:00Z"
        );

        // Set up a DONE task directly in the test database.
        jdbcTemplate.update(
            "UPDATE tasks SET status = ? WHERE id = ?",
            "DONE",
            doneTask.getId()
        );

        entityManager.clear();

        mockMvc.perform(get("/tasks")
                .param("status", "DONE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(doneTask.getId()))
            .andExpect(jsonPath("$[0].status").value("DONE"));
    }

    @DisplayName("Should filter tasks due before a specified date")
    @Test
    void shouldFilterTasksByDueDate() throws Exception {
        Task earlierTask = createTestTask(
            testCaseworker,
            "Earlier task",
            "2026-11-04T12:00:00Z"
        );

        createTestTask(
            testCaseworker,
            "Task at cutoff",
            "2026-11-05T12:00:00Z"
        );

        createTestTask(
            testCaseworker,
            "Later task",
            "2026-11-06T12:00:00Z"
        );

        mockMvc.perform(get("/tasks")
                .param("dueBefore", "2026-11-05T12:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(earlierTask.getId()))
            .andExpect(jsonPath("$[0].title").value("Earlier task"));
    }

    @DisplayName("Should combine status, creator and due-date filters")
    @Test
    void shouldCombineTaskFilters() throws Exception {
        Caseworker otherCaseworker = caseworkerRepository.save(
            new Caseworker("Other Caseworker")
        );

        // Matches all three filters.
        Task matchingTask = createTestTask(
            testCaseworker,
            "Matching task",
            "2026-11-04T12:00:00Z"
        );

        // Wrong status.
        createTestTask(
            testCaseworker,
            "Wrong status",
            "2026-11-04T12:00:00Z"
        );

        // Wrong creator.
        Task otherCreatorTask = createTestTask(
            otherCaseworker,
            "Wrong creator",
            "2026-11-04T12:00:00Z"
        );

        // Wrong due date.
        Task laterTask = createTestTask(
            testCaseworker,
            "Wrong due date",
            "2026-11-06T12:00:00Z"
        );

        // Mark three tasks as DONE, leaving "Wrong status" as TODO.
        jdbcTemplate.update(
            "UPDATE tasks SET status = ? WHERE id IN (?, ?, ?)",
            "DONE",
            matchingTask.getId(),
            otherCreatorTask.getId(),
            laterTask.getId()
        );

        entityManager.clear();

        mockMvc.perform(get("/tasks")
                .param("status", "DONE")
                .param("createdBy", testCaseworker.getId().toString())
                .param("dueBefore", "2026-11-05T12:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(matchingTask.getId()))
            .andExpect(jsonPath("$[0].title").value("Matching task"))
            .andExpect(jsonPath("$[0].status").value("DONE"))
            .andExpect(jsonPath("$[0].createdBy").value(testCaseworker.getId()));
    }

    @DisplayName("Should sort tasks by due date in either direction")
    @ParameterizedTest
    @ValueSource(strings = {"asc", "desc"})
    void shouldSortTasksByDueDate(String direction) throws Exception {
        Task middleTask = createTestTask(
            testCaseworker,
            "Middle task",
            "2026-11-05T12:00:00Z"
        );

        Task latestTask = createTestTask(
            testCaseworker,
            "Latest task",
            "2026-11-06T12:00:00Z"
        );

        Task earliestTask = createTestTask(
            testCaseworker,
            "Earliest task",
            "2026-11-04T12:00:00Z"
        );

        Long[] expectedOrder = direction.equals("asc")
            ? new Long[] {
                earliestTask.getId(),
                middleTask.getId(),
                latestTask.getId()
            }
            : new Long[] {
                latestTask.getId(),
                middleTask.getId(),
                earliestTask.getId()
            };

        mockMvc.perform(get("/tasks")
                .param("sortBy", "dueAt")
                .param("sortDirection", direction))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(expectedOrder[0]))
            .andExpect(jsonPath("$[1].id").value(expectedOrder[1]))
            .andExpect(jsonPath("$[2].id").value(expectedOrder[2]));
    }

    @DisplayName("Should sort tasks alphabetically by title")
    @Test
    void shouldSortTasksByTitle() throws Exception {
        Task zebraTask = createTestTask(
            testCaseworker,
            "Zebra task",
            "2026-11-04T12:00:00Z"
        );

        Task appleTask = createTestTask(
            testCaseworker,
            "Apple task",
            "2026-11-06T12:00:00Z"
        );

        Task middleTask = createTestTask(
            testCaseworker,
            "Middle task",
            "2026-11-05T12:00:00Z"
        );

        mockMvc.perform(get("/tasks")
                .param("sortBy", "title")
                .param("sortDirection", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(appleTask.getId()))
            .andExpect(jsonPath("$[1].id").value(middleTask.getId()))
            .andExpect(jsonPath("$[2].id").value(zebraTask.getId()));
    }

    @DisplayName("Should reject invalid task filter and sort parameters")
    @ParameterizedTest
    @CsvSource({
        "status, INVALID",
        "sortBy, unknownField",
        "dueBefore, not-a-date"
    })
    void shouldRejectInvalidQueryParameters(
        String parameter,
        String value
    ) throws Exception {
        mockMvc.perform(get("/tasks")
                .param(parameter, value))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title")
                .value("Invalid query parameter"))
            .andExpect(jsonPath("$.errors." + parameter).exists());
    }

    @DisplayName("Should return an empty list when no tasks match the filters")
    @Test
    void shouldReturnEmptyListWhenNoTasksMatch() throws Exception {
        createTestTask(
            testCaseworker,
            "Review documents",
            "2026-11-05T12:00:00Z"
        );

        mockMvc.perform(get("/tasks")
                .param("dueBefore", "2026-11-01T12:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @DisplayName("Should retrieve a task by ID")
    @Test
    void shouldRetrieveTaskById() throws Exception {
        Task task = createTestTask(
            testCaseworker,
            "Review documents",
            "2026-11-05T12:00:00Z"
        );

        mockMvc.perform(get("/tasks/{id}", task.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(task.getId()))
            .andExpect(jsonPath("$.createdBy").value(testCaseworker.getId()))
            .andExpect(jsonPath("$.title").value("Review documents"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.dueAt").value("2026-11-05T12:00:00Z"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());
    }

    @DisplayName("Should return 404 when task does not exist")
    @Test
    void shouldReturnNotFoundForUnknownTask() throws Exception {
        mockMvc.perform(get("/tasks/{id}", -1))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource not found"))
            .andExpect(jsonPath("$.detail").value("Task not found: -1"));
    }

}
