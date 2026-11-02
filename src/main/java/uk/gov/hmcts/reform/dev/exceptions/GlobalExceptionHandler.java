package uk.gov.hmcts.reform.dev.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.getMessage()
        );

        problem.setTitle("Resource not found");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationFailure(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request validation failed"
        );

        problem.setTitle("Validation failed");

        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableMessage(
        HttpMessageNotReadableException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request body contains invalid JSON or incompatible field values"
        );

        problem.setTitle("Invalid request body");

        Throwable cause = exception.getCause();

        if (cause instanceof InvalidFormatException invalidFormat) {
            String field = invalidFormat.getPath().stream()
                .map(reference -> reference.getPropertyName())
                .filter(name -> name != null)
                .reduce((first, second) -> first + "." + second)
                .orElse(null);

            if (field != null) {
                problem.setProperty(
                    "errors",
                    Map.of(field, "Invalid value or type")
                );
            }
        }

        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleQueryParameterTypeMismatch(
        MethodArgumentTypeMismatchException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Invalid query parameter: " + exception.getName()
        );

        problem.setTitle("Invalid query parameter");
        problem.setProperty(
            "errors",
            Map.of(exception.getName(), "Invalid value or type")
        );

        return problem;
    }

    @ExceptionHandler(InvalidQueryParameterException.class)
    public ProblemDetail handleInvalidQueryParameter(
        InvalidQueryParameterException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Invalid query parameter: " + exception.getParameter()
        );

        problem.setTitle("Invalid query parameter");
        problem.setProperty(
            "errors",
            Map.of(exception.getParameter(), exception.getMessage())
        );

        return problem;
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(
        InvalidRequestException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request validation failed"
        );

        problem.setTitle("Invalid request body");
        problem.setProperty(
            "errors",
            Map.of(exception.getField(), exception.getMessage())
        );

        return problem;
    }
}
