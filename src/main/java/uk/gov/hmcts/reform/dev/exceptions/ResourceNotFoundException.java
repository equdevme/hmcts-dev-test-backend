package uk.gov.hmcts.reform.dev.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, Long id) {
        super(resourceType + " not found: " + id);
    }
}
