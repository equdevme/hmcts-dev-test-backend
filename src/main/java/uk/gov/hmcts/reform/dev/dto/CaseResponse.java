package uk.gov.hmcts.reform.dev.dto;

import uk.gov.hmcts.reform.dev.models.Case;

public record CaseResponse(
    Long id,
    Long ownerId,
    String title,
    String description
) {
    public static CaseResponse from(Case associatedCase) {
        return new CaseResponse(
            associatedCase.getId(),
            associatedCase.getOwner().getId(),
            associatedCase.getTitle(),
            associatedCase.getDescription()
        );
    }
}
