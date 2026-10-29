package uk.gov.hmcts.reform.dev.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.hmcts.reform.dev.models.TaskStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TaskStatusRepositoryTest {

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @DisplayName("Should have exactly one default task status")
    @Test
    void shouldHaveExactlyOneDefaultTaskStatus() {
        var defaultStatuses = taskStatusRepository.findAll().stream()
            .filter(status -> status.isDefault())
            .toList();

        assertThat(defaultStatuses).hasSize(1);
    }

    @DisplayName("Should retrieve the configured default task status")
    @Test
    void shouldRetrieveDefaultTaskStatus() {
        var defaultStatus = taskStatusRepository.findByIsDefaultTrue();

        assertThat(defaultStatus).isPresent();
        assertThat(defaultStatus.orElseThrow().isDefault()).isTrue();
    }

    @DisplayName("Should retrieve task statuses in display order")
    @Test
    void shouldRetrieveTaskStatusesInDisplayOrder() {
        var statuses = taskStatusRepository.findAllByOrderByDisplayOrderAsc();

        assertThat(statuses).isNotEmpty();

        assertThat(statuses)
            .extracting(TaskStatus::getDisplayOrder)
            .isSorted();
    }
}
