package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.api.ApiErrorCode;
import cn.learn.lfy.mtr.api.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class UploadStorage {
    private final Path root;

    public UploadStorage(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path resolve(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ApiErrorCode.INTERNAL_ERROR, "素材存储路径无效");
        }
        return target;
    }

    public boolean containsFile(String storageKey, long expectedSize) {
        Path target = resolve(storageKey);
        try {
            return Files.isRegularFile(target) && Files.size(target) == expectedSize;
        } catch (IOException exception) {
            return false;
        }
    }
}
