package uk.gov.hmcts.reform.dev.services;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.dev.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;
import uk.gov.hmcts.reform.dev.models.Task;

@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final TaskRepository taskRepository;

    public CaseService(CaseRepository caseRepository, TaskRepository taskRepository) {
        this.caseRepository = caseRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<Case> getCases(Long ownerId) {
        if (ownerId != null) {
            return caseRepository.findByOwnerIdOrderByIdAsc(ownerId);
        }

        return caseRepository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Case getCase(Long id) {
        return caseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case", id));
    }

    @Transactional(readOnly = true)
    public List<Task> getCaseTasks(Long caseId) {
        // Ensure the case exists, even if it has no tasks.
        getCase(caseId);

        return taskRepository.findByAssociatedCaseIdOrderByDueAtAscIdAsc(caseId);
    }
}
