package uk.gov.hmcts.reform.dev.specifications;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskStatus;

public final class TaskSpecifications {

    private TaskSpecifications() {
        // Utility class
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Task> createdBy(Long caseworkerId) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("createdBy").get("id"), caseworkerId);
    }

    public static Specification<Task> dueBefore(Instant dueBefore) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.lessThan(root.get("dueAt"), dueBefore);
    }
}
