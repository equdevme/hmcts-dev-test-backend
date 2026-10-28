package uk.gov.hmcts.reform.dev.controllers;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.reform.dev.dto.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.TaskResponse;
import uk.gov.hmcts.reform.dev.dto.UpdateTaskStatusRequest;
import uk.gov.hmcts.reform.dev.models.TaskSortField;
import uk.gov.hmcts.reform.dev.models.TaskStatus;
import uk.gov.hmcts.reform.dev.services.TaskService;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@Valid @RequestBody CreateTaskRequest request) {
        var task = taskService.createTask(
            request.caseId(),
            request.createdBy(),
            request.title(),
            request.description(),
            request.dueAt()
        );

        return TaskResponse.from(task);
    }

    @GetMapping
    public List<TaskResponse> getTasks(
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) Long createdBy,
        @RequestParam(required = false) Instant dueBefore,
        @RequestParam(defaultValue = "dueAt") TaskSortField sortBy,
        @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        return taskService.getTasks(
            status,
            createdBy,
            dueBefore,
            sortBy,
            sortDirection
        ).stream()
            .map(TaskResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable Long id) {
        return TaskResponse.from(taskService.getTask(id));
    }

    @PatchMapping("/{id}")
    public TaskResponse updateTaskStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return TaskResponse.from(taskService.updateTaskStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

}
