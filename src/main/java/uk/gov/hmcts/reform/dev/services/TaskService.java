package uk.gov.hmcts.reform.dev.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.dev.dto.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.repositories.CaseworkerRepository;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;
import uk.gov.hmcts.reform.dev.exceptions.ResourceNotFoundException;

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

    public Task createTask(CreateTaskRequest request) {
        var caseworker = caseworkerRepository.findById(request.createdBy())
            .orElseThrow(() -> new ResourceNotFoundException("Caseworker", request.createdBy()));

        var task = new Task(
            caseworker,
            request.title(),
            request.description(),
            request.dueAt()
        );

        return taskRepository.save(task);
    }
}
