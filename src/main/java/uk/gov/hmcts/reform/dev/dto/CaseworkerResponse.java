package uk.gov.hmcts.reform.dev.dto;

import uk.gov.hmcts.reform.dev.models.Caseworker;

public record CaseworkerResponse(
    Long id,
    String name
) {
    public static CaseworkerResponse from(Caseworker caseworker) {
        return new CaseworkerResponse(
            caseworker.getId(),
            caseworker.getName()
        );
    }
}
