package uk.gov.hmcts.reform.dev.repositories;

import java.time.Instant;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.models.Caseworker;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CaseworkerRepository caseworkerRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveTask() {
        // Create a caseworker
        var caseworker = caseworkerRepository.save(
            new Caseworker("Test Caseworker")
        );

        // Create a case owned by the caseworker
        var associatedCase = caseRepository.save(
            new Case(caseworker, "Test Case", "A case for integration testing")
        );

        // Create a task associated with the case
        var dueAt = Instant.parse("2026-11-05T12:00:00Z");

        var task = new Task(
            associatedCase,
            caseworker,
            "Investigate suspicious activity",
            "Check reports from headquarters",
            dueAt
        );

        // Save to PostgreSQL
        var savedTask = taskRepository.save(task);

        // Execute pending SQL and clear Hibernate's persistence context
        entityManager.flush();
        entityManager.clear();

        // Retrieve it using the generated ID
        var retrievedTask = taskRepository.findById(savedTask.getId())
            .orElseThrow();

        // Verify the persisted values
        assertThat(retrievedTask.getTitle())
            .isEqualTo("Investigate suspicious activity");

        assertThat(retrievedTask.getAssociatedCase().getId())
            .isEqualTo(associatedCase.getId());

        assertThat(retrievedTask.getCreatedBy().getId())
            .isEqualTo(caseworker.getId());

        assertThat(retrievedTask.getStatus())
            .isEqualTo(TaskStatus.TODO);

        assertThat(retrievedTask.getDueAt())
            .isEqualTo(dueAt);

        assertThat(retrievedTask.getCreatedAt()).isNotNull();
        assertThat(retrievedTask.getUpdatedAt()).isNotNull();
    }
}
