package uk.gov.hmcts.reform.dev.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTaskStatusRequest(
    @NotBlank String status
) {
}
