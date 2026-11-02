package uk.gov.hmcts.reform.dev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateTaskRequest(
    @NotNull Long caseId,
    @NotNull Long createdBy,
    @NotBlank @Size(max = 255) String title,
    String description,
    @NotNull Instant dueAt
) {
}
