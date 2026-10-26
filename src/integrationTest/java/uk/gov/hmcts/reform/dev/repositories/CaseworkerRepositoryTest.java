package uk.gov.hmcts.reform.dev.repositories;

import uk.gov.hmcts.reform.dev.models.Caseworker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CaseworkerRepositoryTest {

    @Autowired
    private CaseworkerRepository caseworkerRepository;

    @DisplayName("Should retrieve seeded caseworkers")
    @Test
    void shouldRetrieveSeededCaseworkers() {
        var caseworkers = caseworkerRepository.findAll();

        assertThat(caseworkers)
                .extracting(Caseworker::getName)
                .contains("Danger Mouse", "Count Duckula", "Super Ted");
    }
}
