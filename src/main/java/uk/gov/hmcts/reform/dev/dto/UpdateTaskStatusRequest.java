package uk.gov.hmcts.reform.dev.dto;

import jakarta.validation.constraints.NotNull;
import uk.gov.hmcts.reform.dev.models.TaskStatus;

public record UpdateTaskStatusRequest(
    @NotNull TaskStatus status
) {
}
