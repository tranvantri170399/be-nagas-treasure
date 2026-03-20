package asia.rgp.game.nagas.infrastructure.http;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import asia.rgp.game.nagas.shared.error.ErrorCode;
import asia.rgp.game.nagas.shared.infrastructure.exception.ExternalServiceException;

import java.util.List;
import java.util.function.Function;

/**
 * Utility class for REST client operations.
 * Provides static methods for response validation, mapping, and exception
 * handling.
 */
@Slf4j
public final class RestClientHelper {

    private RestClientHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates the HTTP response status code and body.
     *
     * @param response      the HTTP response to validate
     * @param operationName the operation name for error messages
     * @param errorCode     the error code to use for service errors
     * @param <T>           the response body type
     * @return the response body if valid
     * @throws ExternalServiceException if the response is invalid
     */
    private static <T> T validateResponse(
        ResponseEntity<T> response,
        String operationName,
        ErrorCode errorCode
    ) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ExternalServiceException(
                String.format("Failed to %s: Invalid HTTP status %s", operationName, response.getStatusCode()),
                errorCode
            );
        }

        T responseBody = response.getBody();
        if (responseBody == null) {
            throw new ExternalServiceException(
                String.format("Failed to %s: Empty response from service", operationName),
                errorCode
            );
        }

        return responseBody;
    }

    /**
     * Validates the HTTP response and maps it to the result.
     *
     * @param response       the HTTP response
     * @param responseMapper function to map response body to result
     * @param operationName  the operation name for error messages
     * @param errorCode      the error code to use for service errors
     * @param <T>            the response type
     * @param <R>            the result type
     * @return the mapped result
     * @throws ExternalServiceException if the response is invalid
     */
    public static <T, R> R validateAndMapResponse(
        ResponseEntity<T> response,
        Function<T, R> responseMapper,
        String operationName,
        ErrorCode errorCode
    ) {
        T responseBody = validateResponse(response, operationName, errorCode);
        R result = responseMapper.apply(responseBody);
        log.debug("Operation '{}' completed successfully", operationName);
        return result;
    }

    /**
     * Validates the HTTP response containing an array and maps it to the result.
     *
     * @param response       the HTTP response containing a list
     * @param responseMapper function to map response body list to result
     * @param operationName  the operation name for error messages
     * @param errorCode      the error code to use for service errors
     * @param <T>            the list element type
     * @param <R>            the result type
     * @return the mapped result
     * @throws ExternalServiceException if the response is invalid
     */
    public static <T, R> R validateAndMapArrayResponse(
        ResponseEntity<List<T>> response,
        Function<List<T>, R> responseMapper,
        String operationName,
        ErrorCode errorCode
    ) {
        List<T> responseBody = validateResponse(response, operationName, errorCode);
        R result = responseMapper.apply(responseBody);
        log.debug("Operation '{}' completed successfully", operationName);
        return result;
    }

    /**
     * Handles RestClientException and converts it to ExternalServiceException.
     *
     * @param e                the exception to handle
     * @param operationName    the operation name for error messages
     * @param serviceErrorCode the error code for service errors
     * @param unavailableCode  the error code for service unavailable errors
     * @return ExternalServiceException with appropriate error code
     */
    public static ExternalServiceException handleRestClientException(
        RestClientException e,
        String operationName,
        ErrorCode serviceErrorCode,
        ErrorCode unavailableCode
    ) {
        if (e instanceof HttpClientErrorException httpError) {
            return handleHttpClientError(httpError, operationName, serviceErrorCode);
        }
        if (e instanceof HttpServerErrorException serverError) {
            log.error("HTTP server error while {}: {} - {}", operationName, serverError.getStatusCode(), serverError.getMessage());
            return new ExternalServiceException(
                String.format("Service error while %s: %s", operationName, serverError.getMessage()),
                serviceErrorCode,
                serverError
            );
        }
        if (e instanceof ResourceAccessException accessError) {
            log.error("Connection error while {}: {}", operationName, accessError.getMessage());
            return new ExternalServiceException(
                String.format("Service is unavailable while %s: %s", operationName, accessError.getMessage()),
                unavailableCode,
                accessError
            );
        }

        log.error("Unexpected error while {}: {}", operationName, e.getMessage(), e);
        return new ExternalServiceException(
            String.format("Unexpected error while %s: %s", operationName, e.getMessage()),
            serviceErrorCode,
            e
        );
    }

    /**
     * Handles HttpClientErrorException with specific error codes.
     *
     * @param e                the HTTP client error exception
     * @param operationName    the operation name for error messages
     * @param serviceErrorCode the error code for service errors
     * @return ExternalServiceException with appropriate error code
     */
    public static ExternalServiceException handleHttpClientError(
        HttpClientErrorException e,
        String operationName,
        ErrorCode serviceErrorCode
    ) {
        log.error("HTTP client error while {}: {} - {}", operationName, e.getStatusCode(), e.getMessage());

        ErrorCode errorCode;
        String message;

        if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
            errorCode = ErrorCode.VALIDATION_ERROR;
            message = String.format("Invalid request while %s: %s", operationName, e.getMessage());
        } else if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            errorCode = ErrorCode.VALIDATION_ERROR; // Use VALIDATION_ERROR as fallback
            message = String.format("Unauthorized while %s: %s", operationName, e.getMessage());
        } else if (e.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
            errorCode = ErrorCode.VALIDATION_ERROR; // Use VALIDATION_ERROR as fallback
            message = String.format("Forbidden while %s: %s", operationName, e.getMessage());
        } else if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            errorCode = ErrorCode.NOT_FOUND;
            message = String.format("Resource not found while %s: %s", operationName, e.getMessage());
        } else if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
            errorCode = ErrorCode.CONFLICT;
            message = String.format("Conflict while %s: %s", operationName, e.getMessage());
        } else {
            errorCode = serviceErrorCode;
            message = String.format("Failed to %s: %s", operationName, e.getMessage());
        }

        return new ExternalServiceException(message, errorCode, e);
    }
}
