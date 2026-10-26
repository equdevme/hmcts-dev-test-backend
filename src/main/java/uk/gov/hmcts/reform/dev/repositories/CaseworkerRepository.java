package uk.gov.hmcts.reform.dev.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.dev.models.Caseworker;

public interface CaseworkerRepository
        extends JpaRepository<Caseworker, Long> {
}
