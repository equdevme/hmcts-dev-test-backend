package uk.gov.hmcts.reform.dev.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.reform.dev.dto.CaseResponse;
import uk.gov.hmcts.reform.dev.dto.TaskResponse;
import uk.gov.hmcts.reform.dev.services.CaseService;

@RestController
@RequestMapping("/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    public List<CaseResponse> getCases(
        @RequestParam(required = false) Long ownerId
    ) {
        return caseService.getCases(ownerId).stream()
            .map(CaseResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public CaseResponse getCase(@PathVariable Long id) {
        return CaseResponse.from(caseService.getCase(id));
    }

    @GetMapping("/{id}/tasks")
    public List<TaskResponse> getCaseTasks(@PathVariable Long id) {
        return caseService.getCaseTasks(id).stream()
            .map(TaskResponse::from)
            .toList();
    }
}
