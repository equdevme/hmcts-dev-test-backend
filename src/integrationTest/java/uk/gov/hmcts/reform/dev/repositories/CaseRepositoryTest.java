package uk.gov.hmcts.reform.dev.repositories;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.models.Caseworker;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CaseRepositoryTest {

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private CaseworkerRepository caseworkerRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveCase() {
        var owner = caseworkerRepository.save(
            new Caseworker("Test Caseworker")
        );

        var associatedCase = caseRepository.save(
            new Case(
                owner,
                "Suspicious activity investigation",
                "Investigate reports from headquarters"
            )
        );

        entityManager.flush();
        entityManager.clear();

        var retrievedCase = caseRepository.findById(associatedCase.getId())
            .orElseThrow();

        assertThat(retrievedCase.getTitle())
            .isEqualTo("Suspicious activity investigation");

        assertThat(retrievedCase.getDescription())
            .isEqualTo("Investigate reports from headquarters");

        assertThat(retrievedCase.getOwner().getId())
            .isEqualTo(owner.getId());
    }

    @Test
    void shouldFindCasesByOwner() {
        var firstOwner = caseworkerRepository.save(
            new Caseworker("First Caseworker")
        );

        var secondOwner = caseworkerRepository.save(
            new Caseworker("Second Caseworker")
        );

        final var firstCase = caseRepository.save(
            new Case(firstOwner, "First case", null)
        );

        final var secondCase = caseRepository.save(
            new Case(firstOwner, "Second case", null)
        );

        caseRepository.save(
            new Case(secondOwner, "Other case", null)
        );

        entityManager.flush();
        entityManager.clear();

        var results = caseRepository.findByOwnerIdOrderByIdAsc(
            firstOwner.getId()
        );

        assertThat(results)
            .extracting(Case::getId)
            .containsExactly(firstCase.getId(), secondCase.getId());
    }

    @Test
    void shouldReturnEmptyListWhenOwnerHasNoCases() {
        var owner = caseworkerRepository.save(
            new Caseworker("Caseworker Without Cases")
        );

        var results = caseRepository.findByOwnerIdOrderByIdAsc(
            owner.getId()
        );

        assertThat(results).isEmpty();
    }
}
