package cn.learn.lfy.mtr.api;

public enum ApiErrorCode {
    SUCCESS(0, "成功"),
    INVALID_REQUEST(10001, "请求参数无效"),
    VALIDATION_FAILED(10002, "参数校验失败"),
    TEMPLATE_NOT_FOUND(20001, "模板不存在"),
    TASK_NOT_FOUND(20002, "任务不存在"),
    USER_NOT_FOUND(20003, "用户不存在"),
    ASSET_NOT_FOUND(20004, "素材不存在"),
    FILE_TOO_LARGE(30001, "文件超过大小限制"),
    FILE_TYPE_NOT_ALLOWED(30002, "文件类型不支持"),
    BUSINESS_CONFLICT(40001, "请求与当前资源状态冲突"),
    INTERNAL_ERROR(90000, "服务器内部错误");

    private final int code;
    private final String message;

    ApiErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
