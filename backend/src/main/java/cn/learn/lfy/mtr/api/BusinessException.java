package cn.learn.lfy.mtr.api;

public class BusinessException extends RuntimeException {
    private final ApiErrorCode errorCode;

    public BusinessException(ApiErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public BusinessException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }
}
