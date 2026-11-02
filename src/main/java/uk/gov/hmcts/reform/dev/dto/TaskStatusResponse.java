package uk.gov.hmcts.reform.dev.dto;

import uk.gov.hmcts.reform.dev.models.TaskStatus;

public record TaskStatusResponse(
    String code,
    String label,
    Integer displayOrder,
    boolean isDefault
) {
    public static TaskStatusResponse from(TaskStatus status) {
        return new TaskStatusResponse(
            status.getCode(),
            status.getLabel(),
            status.getDisplayOrder(),
            status.isDefault()
        );
    }
}
