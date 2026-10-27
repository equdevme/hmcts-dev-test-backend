package uk.gov.hmcts.reform.dev.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.dev.models.Caseworker;
import uk.gov.hmcts.reform.dev.repositories.CaseworkerRepository;

@Service
public class CaseworkerService {

    private final CaseworkerRepository caseworkerRepository;

    public CaseworkerService(CaseworkerRepository caseworkerRepository) {
        this.caseworkerRepository = caseworkerRepository;
    }

    @Transactional(readOnly = true)
    public List<Caseworker> getCaseworkers() {
        return caseworkerRepository.findAll();
    }
}
