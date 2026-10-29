package uk.gov.hmcts.reform.dev.dto;

import java.time.Instant;

import uk.gov.hmcts.reform.dev.models.Task;

public record TaskResponse(
    Long id,
    Long caseId,
    Long createdBy,
    String title,
    String description,
    String status,
    Instant dueAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getAssociatedCase().getId(),
            task.getCreatedBy().getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus().getCode(),
            task.getDueAt(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
