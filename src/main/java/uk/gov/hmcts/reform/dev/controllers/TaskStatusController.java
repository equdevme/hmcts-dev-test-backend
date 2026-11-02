package uk.gov.hmcts.reform.dev.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.reform.dev.dto.TaskStatusResponse;
import uk.gov.hmcts.reform.dev.services.TaskStatusService;

@RestController
@RequestMapping("/task-statuses")
public class TaskStatusController {

    private final TaskStatusService taskStatusService;

    public TaskStatusController(TaskStatusService taskStatusService) {
        this.taskStatusService = taskStatusService;
    }

    @GetMapping
    public List<TaskStatusResponse> getTaskStatuses() {
        return taskStatusService.getTaskStatuses().stream()
            .map(TaskStatusResponse::from)
            .toList();
    }
}
