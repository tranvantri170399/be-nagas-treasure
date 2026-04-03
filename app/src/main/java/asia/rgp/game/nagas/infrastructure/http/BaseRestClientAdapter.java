package asia.rgp.game.nagas.infrastructure.http;

import asia.rgp.game.nagas.shared.error.ErrorCode;
import asia.rgp.game.nagas.shared.infrastructure.exception.ExternalServiceException;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Abstract base class for REST API client adapters. Provides common HTTP operations and utilities
 * for implementing REST API adapters.
 */
@Slf4j
public abstract class BaseRestClientAdapter {

  protected final RestTemplate RestTemplate;
  protected final String serviceName;

  /**
   * Constructor for BaseRestClientAdapter.
   *
   * @param RestTemplate the RestTemplate instance
   * @param serviceName the service name for logging purposes
   */
  protected BaseRestClientAdapter(RestTemplate RestTemplate, String serviceName) {
    this.RestTemplate = RestTemplate;
    this.serviceName = serviceName;
  }

  /**
   * Executes a GET request and returns the mapped result.
   *
   * @param url the URL to call
   * @param responseType the expected response type
   * @param responseMapper function to map response body to result
   * @param operationName the operation name for logging and error messages
   * @param errorCode the error code to use for service errors
   * @param unavailableCode the error code to use for service unavailable errors
   * @param <T> the response type
   * @param <R> the result type
   * @return the mapped result
   * @throws ExternalServiceException if the request fails
   */
  protected <T, R> R executeGetRequest(
      String url,
      Class<T> responseType,
      Function<T, R> responseMapper,
      String operationName,
      ErrorCode errorCode,
      ErrorCode unavailableCode) {
    return executeGetRequest(
        url, responseType, responseMapper, operationName, errorCode, unavailableCode, null);
  }

  /**
   * Executes a GET request with headers and returns the mapped result.
   *
   * @param url the URL to call
   * @param responseType the expected response type
   * @param responseMapper function to map response body to result
   * @param operationName the operation name for logging and error messages
   * @param errorCode the error code to use for service errors
   * @param unavailableCode the error code to use for service unavailable errors
   * @param headers the HTTP headers to include in the request (can be null)
   * @param <T> the response type
   * @param <R> the result type
   * @return the mapped result
   * @throws ExternalServiceException if the request fails
   */
  protected <T, R> R executeGetRequest(
      String url,
      Class<T> responseType,
      Function<T, R> responseMapper,
      String operationName,
      ErrorCode errorCode,
      ErrorCode unavailableCode,
      HttpHeaders headers) {
    log.debug("[{}] Executing GET request to: {}", serviceName, url);

    try {
      HttpEntity<?> httpEntity = headers != null ? new HttpEntity<>(headers) : null;
      ResponseEntity<T> response =
          RestTemplate.exchange(url, HttpMethod.GET, httpEntity, responseType);

      return RestClientHelper.validateAndMapResponse(
          response, responseMapper, operationName, errorCode);
    } catch (RestClientException e) {
      throw RestClientHelper.handleRestClientException(
          e, operationName, errorCode, unavailableCode);
    }
  }

  /**
   * Executes a POST request and returns the mapped result.
   *
   * @param url the URL to call
   * @param request the request body
   * @param responseType the expected response type
   * @param responseMapper function to map response body to result
   * @param operationName the operation name for logging and error messages
   * @param errorCode the error code to use for service errors
   * @param unavailableCode the error code to use for service unavailable errors
   * @param <TRequest> the request type
   * @param <TResponse> the response type
   * @param <R> the result type
   * @return the mapped result
   * @throws ExternalServiceException if the request fails
   */
  protected <TRequest, TResponse, R> R executePostRequest(
      String url,
      TRequest request,
      Class<TResponse> responseType,
      Function<TResponse, R> responseMapper,
      String operationName,
      ErrorCode errorCode,
      ErrorCode unavailableCode) {
    return executePostRequest(
        url,
        request,
        responseType,
        responseMapper,
        operationName,
        errorCode,
        unavailableCode,
        null);
  }

  /**
   * Executes a POST request with headers and returns the mapped result.
   *
   * @param url the URL to call
   * @param request the request body
   * @param responseType the expected response type
   * @param responseMapper function to map response body to result
   * @param operationName the operation name for logging and error messages
   * @param errorCode the error code to use for service errors
   * @param unavailableCode the error code to use for service unavailable errors
   * @param headers the HTTP headers to include in the request (can be null)
   * @param <TRequest> the request type
   * @param <TResponse> the response type
   * @param <R> the result type
   * @return the mapped result
   * @throws ExternalServiceException if the request fails
   */
  protected <TRequest, TResponse, R> R executePostRequest(
      String url,
      TRequest request,
      Class<TResponse> responseType,
      Function<TResponse, R> responseMapper,
      String operationName,
      ErrorCode errorCode,
      ErrorCode unavailableCode,
      HttpHeaders headers) {
    log.debug("[{}] Executing POST request to: {}", serviceName, url);

    try {
      HttpEntity<TRequest> httpEntity = new HttpEntity<>(request, headers);
      ResponseEntity<TResponse> response =
          RestTemplate.exchange(url, HttpMethod.POST, httpEntity, responseType);

      return RestClientHelper.validateAndMapResponse(
          response, responseMapper, operationName, errorCode);
    } catch (RestClientException e) {
      throw RestClientHelper.handleRestClientException(
          e, operationName, errorCode, unavailableCode);
    }
  }
}
