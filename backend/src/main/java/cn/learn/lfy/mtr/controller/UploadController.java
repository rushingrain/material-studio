package cn.learn.lfy.mtr.controller;

import cn.learn.lfy.mtr.api.ApiResponse;
import cn.learn.lfy.mtr.api.UploadAssetResponse;
import cn.learn.lfy.mtr.service.UploadService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/uploads")
public class UploadController {
    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadAssetResponse> upload(
            @RequestParam long templateId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(uploadService.upload(templateId, file));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        uploadService.delete(id);
        return ApiResponse.success();
    }
}
