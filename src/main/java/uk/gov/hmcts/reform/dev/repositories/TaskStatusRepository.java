package uk.gov.hmcts.reform.dev.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import uk.gov.hmcts.reform.dev.models.TaskStatus;

public interface TaskStatusRepository extends JpaRepository<TaskStatus, String> {

    Optional<TaskStatus> findByIsDefaultTrue();

    List<TaskStatus> findAllByOrderByDisplayOrderAsc();
}
