package cn.learn.lfy.mtr.api;

public record ApiResponse<T>(int code, String msg, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "成功", data);
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> failure(ApiErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null);
    }

    public static ApiResponse<Void> failure(ApiErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(), message, null);
    }
}
