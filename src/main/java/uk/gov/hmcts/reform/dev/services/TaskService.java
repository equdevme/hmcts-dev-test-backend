package uk.gov.hmcts.reform.dev.services;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.dev.exceptions.InvalidQueryParameterException;
import uk.gov.hmcts.reform.dev.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskSortField;
import uk.gov.hmcts.reform.dev.models.TaskStatus;
import uk.gov.hmcts.reform.dev.repositories.CaseworkerRepository;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;
import uk.gov.hmcts.reform.dev.specifications.TaskSpecifications;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CaseworkerRepository caseworkerRepository;

    public TaskService(
        TaskRepository taskRepository,
        CaseworkerRepository caseworkerRepository
    ) {
        this.taskRepository = taskRepository;
        this.caseworkerRepository = caseworkerRepository;
    }

    public Task createTask(
        Long createdBy,
        String title,
        String description,
        Instant dueAt
    ) {
        var caseworker = caseworkerRepository.findById(createdBy)
            .orElseThrow(() -> new ResourceNotFoundException("Caseworker", createdBy));

        var task = new Task(caseworker, title, description, dueAt);

        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasks(
        TaskStatus status,
        Long createdBy,
        Instant dueBefore,
        TaskSortField sortBy,
        String sortDirection
    ) {
        Sort.Direction direction;

        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException exception) {
            throw new InvalidQueryParameterException(
                "sortDirection",
                "Must be 'asc' or 'desc'"
            );
        }

        Specification<Task> filters = Specification.unrestricted();

        if (status != null) {
            filters = filters.and(TaskSpecifications.hasStatus(status));
        }

        if (createdBy != null) {
            filters = filters.and(TaskSpecifications.createdBy(createdBy));
        }

        if (dueBefore != null) {
            filters = filters.and(TaskSpecifications.dueBefore(dueBefore));
        }

        Sort sort = Sort.by(direction, sortBy.name())
            .and(Sort.by(Sort.Direction.ASC, "id"));

        return taskRepository.findAll(filters, sort);
    }
}
