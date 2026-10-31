package cn.learn.lfy.mtr.api;

public record UploadAssetResponse(
        String id,
        String url,
        String originalFilename,
        String mimeType,
        long sizeBytes
) {
}
