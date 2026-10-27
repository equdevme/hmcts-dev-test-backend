package uk.gov.hmcts.reform.dev.dto;

import java.time.Instant;

import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskStatus;

public record TaskResponse(
    Long id,
    Long createdBy,
    String title,
    String description,
    TaskStatus status,
    Instant dueAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getCreatedBy().getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getDueAt(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
