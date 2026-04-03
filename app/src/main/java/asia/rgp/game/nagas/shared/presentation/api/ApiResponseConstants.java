package asia.rgp.game.nagas.shared.presentation.api;

/**
 * Constants for API response codes and descriptions. Reusable across all REST controllers to avoid
 * hardcoding.
 *
 * <p>These constants can be used for: - Swagger/OpenAPI annotations (@ApiResponse) - HTTP status
 * codes in ResponseEntity - Error response descriptions
 */
public final class ApiResponseConstants {

  private ApiResponseConstants() {
    // Utility class - prevent instantiation
  }

  // Response Codes
  public static final String OK = "200";
  public static final String CREATED = "201";
  public static final String BAD_REQUEST = "400";
  public static final String NOT_FOUND = "404";
  public static final String INTERNAL_SERVER_ERROR = "500";

  // Success Descriptions (Generic)
  public static final String SUCCESS = "Operation completed successfully";
  public static final String CREATED_SUCCESS = "Resource created successfully";
  public static final String RETRIEVED_SUCCESS = "Resources retrieved successfully";
  public static final String FOUND_SUCCESS = "Resource found successfully";
  public static final String UPDATED_SUCCESS = "Resource updated successfully";
  public static final String DELETED_SUCCESS = "Resource deleted successfully";

  // Error Descriptions
  public static final String BAD_REQUEST_DESC = "Invalid input data";
  public static final String NOT_FOUND_DESC = "Resource not found";
  public static final String INTERNAL_SERVER_ERROR_DESC = "Internal server error";
  public static final String UNAUTHORIZED_DESC = "Unauthorized access";
  public static final String FORBIDDEN_DESC = "Forbidden access";
  public static final String VALIDATION_ERROR_DESC = "Validation error";
}
