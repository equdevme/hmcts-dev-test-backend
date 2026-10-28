package uk.gov.hmcts.reform.dev.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import uk.gov.hmcts.reform.dev.models.Task;

public interface TaskRepository
    extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
        List<Task> findByAssociatedCaseIdOrderByDueAtAscIdAsc(Long caseId);
}
