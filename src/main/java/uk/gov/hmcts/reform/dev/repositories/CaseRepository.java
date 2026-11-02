package uk.gov.hmcts.reform.dev.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.dev.models.Case;

public interface CaseRepository extends JpaRepository<Case, Long> {

    List<Case> findByOwnerIdOrderByIdAsc(Long ownerId);
}
