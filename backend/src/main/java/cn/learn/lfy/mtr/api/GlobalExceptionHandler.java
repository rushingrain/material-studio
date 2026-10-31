package cn.learn.lfy.mtr.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import jakarta.validation.ConstraintViolationException;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ApiErrorCode code = exception.getErrorCode();
        HttpStatus status = switch (code) {
            case TEMPLATE_NOT_FOUND, TASK_NOT_FOUND, USER_NOT_FOUND, ASSET_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FILE_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case FILE_TYPE_NOT_ALLOWED -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case BUSINESS_CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiResponse.failure(code, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ApiErrorCode.VALIDATION_FAILED.message());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ApiErrorCode.VALIDATION_FAILED, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableRequest() {
        return ResponseEntity.badRequest().body(ApiResponse.failure(ApiErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch() {
        return ResponseEntity.badRequest().body(ApiResponse.failure(ApiErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidMultipartRequest() {
        return ResponseEntity.badRequest().body(ApiResponse.failure(ApiErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse(ApiErrorCode.VALIDATION_FAILED.message());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ApiErrorCode.VALIDATION_FAILED, message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.failure(ApiErrorCode.FILE_TOO_LARGE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ApiErrorCode.INTERNAL_ERROR));
    }
}
