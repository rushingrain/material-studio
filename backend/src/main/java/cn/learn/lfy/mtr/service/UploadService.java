package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.api.ApiErrorCode;
import cn.learn.lfy.mtr.api.BusinessException;
import cn.learn.lfy.mtr.api.UploadAssetResponse;
import cn.learn.lfy.mtr.domain.model.Template;
import cn.learn.lfy.mtr.domain.model.UploadAsset;
import cn.learn.lfy.mtr.mapper.TemplateMapper;
import cn.learn.lfy.mtr.mapper.UploadAssetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class UploadService {
    private static final Logger log = LoggerFactory.getLogger(UploadService.class);
    private static final DateTimeFormatter STORAGE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final Map<String, String> MIME_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final TemplateMapper templateMapper;
    private final UploadAssetMapper uploadAssetMapper;
    private final UploadStorage uploadStorage;

    public UploadService(
            TemplateMapper templateMapper,
            UploadAssetMapper uploadAssetMapper,
            UploadStorage uploadStorage
    ) {
        this.templateMapper = templateMapper;
        this.uploadAssetMapper = uploadAssetMapper;
        this.uploadStorage = uploadStorage;
    }

    public UploadAssetResponse upload(long templateId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ApiErrorCode.INVALID_REQUEST, "请选择要上传的图片");
        }

        Template template = templateMapper.findActiveById(templateId)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.TEMPLATE_NOT_FOUND));
        if (file.getSize() > template.maxImageSizeBytes()) {
            throw new BusinessException(
                    ApiErrorCode.FILE_TOO_LARGE,
                    "单张图片不能超过 " + formatSize(template.maxImageSizeBytes())
            );
        }

        String mimeType = detectMimeType(file);
        if (template.allowedMimeTypes().stream().map(UploadService::normalizeMimeType).noneMatch(mimeType::equals)) {
            throw new BusinessException(
                    ApiErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "该模板支持 " + formatAllowedTypes(template)
            );
        }

        String id = UUID.randomUUID().toString();
        String storageKey = buildStorageKey(id, mimeType);
        Path target = uploadStorage.resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.size(target) != file.getSize()) {
                Files.deleteIfExists(target);
                throw new BusinessException(ApiErrorCode.INVALID_REQUEST, "图片上传不完整，请重试");
            }
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new BusinessException(ApiErrorCode.INTERNAL_ERROR, "图片保存失败，请稍后重试");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename(), MIME_EXTENSIONS.get(mimeType));
        try {
            uploadAssetMapper.insert(id, templateId, originalFilename, storageKey, mimeType, file.getSize());
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        }

        return new UploadAssetResponse(
                id,
                "/uploads/" + storageKey,
                originalFilename,
                mimeType,
                file.getSize()
        );
    }

    public void delete(String id) {
        UploadAsset asset = uploadAssetMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.ASSET_NOT_FOUND));
        uploadAssetMapper.deleteById(id);
        try {
            Files.deleteIfExists(uploadStorage.resolve(asset.storageKey()));
        } catch (IOException exception) {
            log.warn("删除上传文件失败：{}", asset.storageKey(), exception);
        }
    }

    private String detectMimeType(MultipartFile file) {
        byte[] header = new byte[12];
        int length;
        try (InputStream inputStream = file.getInputStream()) {
            length = inputStream.readNBytes(header, 0, header.length);
        } catch (IOException exception) {
            throw new BusinessException(ApiErrorCode.INVALID_REQUEST, "图片内容无法读取");
        }

        if (length >= 3 && isJpeg(header)) return "image/jpeg";
        if (length >= 8 && isPng(header)) return "image/png";
        if (length >= 12 && isWebp(header)) return "image/webp";
        throw new BusinessException(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 JPG、PNG、WebP 图片");
    }

    private static boolean isJpeg(byte[] header) {
        return (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] header) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        for (int index = 0; index < signature.length; index++) {
            if (header[index] != signature[index]) return false;
        }
        return true;
    }

    private static boolean isWebp(byte[] header) {
        return header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
    }

    private static String normalizeMimeType(String mimeType) {
        return "image/jpg".equalsIgnoreCase(mimeType) ? "image/jpeg" : mimeType.toLowerCase(Locale.ROOT);
    }

    private String buildStorageKey(String id, String mimeType) {
        return LocalDate.now().format(STORAGE_DATE_FORMAT)
                + "/" + id + "." + MIME_EXTENSIONS.get(mimeType);
    }

    private static String sanitizeFilename(String filename, String extension) {
        if (filename == null || filename.isBlank()) {
            return "image." + extension;
        }
        String safeName;
        try {
            safeName = Paths.get(filename).getFileName().toString();
        } catch (InvalidPathException exception) {
            safeName = "image." + extension;
        }
        safeName = safeName.replaceAll("[\\p{Cntrl}]", "").trim();
        if (safeName.isBlank()) {
            safeName = "image." + extension;
        }
        return safeName.length() > 255 ? safeName.substring(safeName.length() - 255) : safeName;
    }

    private static String formatSize(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.0f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0);
    }

    private static String formatAllowedTypes(Template template) {
        return template.allowedMimeTypes().stream()
                .map(mime -> switch (mime.toLowerCase(Locale.ROOT)) {
                    case "image/jpeg", "image/jpg" -> "JPG";
                    case "image/png" -> "PNG";
                    case "image/webp" -> "WebP";
                    default -> mime;
                })
                .distinct()
                .reduce((left, right) -> left + "、" + right)
                .orElse("图片");
    }

    private static void deleteQuietly(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            log.warn("清理未完成的上传文件失败：{}", target, exception);
        }
    }
}
