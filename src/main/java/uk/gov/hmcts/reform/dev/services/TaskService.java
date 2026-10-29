package uk.gov.hmcts.reform.dev.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.dev.exceptions.InvalidQueryParameterException;
import uk.gov.hmcts.reform.dev.exceptions.InvalidRequestException;
import uk.gov.hmcts.reform.dev.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskSortField;
import uk.gov.hmcts.reform.dev.models.TaskStatus;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;
import uk.gov.hmcts.reform.dev.repositories.CaseworkerRepository;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;
import uk.gov.hmcts.reform.dev.specifications.TaskSpecifications;
import uk.gov.hmcts.reform.dev.repositories.TaskStatusRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final CaseworkerRepository caseworkerRepository;
    private final CaseRepository caseRepository;

    public TaskService(
        TaskRepository taskRepository,
        TaskStatusRepository taskStatusRepository,
        CaseworkerRepository caseworkerRepository,
        CaseRepository caseRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskStatusRepository = taskStatusRepository;
        this.caseworkerRepository = caseworkerRepository;
        this.caseRepository = caseRepository;
    }

    public Task createTask(
        Long caseId,
        Long createdBy,
        String title,
        String description,
        Instant dueAt
    ) {
        Case associatedCase = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case", caseId));

        var caseworker = caseworkerRepository.findById(createdBy)
            .orElseThrow(() -> new ResourceNotFoundException("Caseworker", createdBy));

        TaskStatus defaultStatus = taskStatusRepository.findByIsDefaultTrue()
            .orElseThrow(() -> new IllegalStateException(
                "No default task status configured"
            ));

        var task = new Task(associatedCase, caseworker, title, description, dueAt);
        task.setStatus(defaultStatus);

        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasks(
        String status,
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
            TaskStatus taskStatus = taskStatusRepository.findById(status)
                .orElseThrow(() -> new InvalidQueryParameterException(
                    "status",
                    "Unknown task status"
                ));

            filters = filters.and(TaskSpecifications.hasStatus(taskStatus));
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

    @Transactional(readOnly = true)
    public Task getTask(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    @Transactional
    public Task updateTaskStatus(Long id, String statusCode) {
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        TaskStatus status = taskStatusRepository.findById(statusCode)
            .orElseThrow(() -> new InvalidRequestException(
                "status",
                "Unknown task status: " + statusCode
            ));

        task.setStatus(status);

        return task;
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        taskRepository.delete(task);
    }

}
