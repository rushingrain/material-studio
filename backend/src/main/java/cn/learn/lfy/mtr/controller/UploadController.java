package cn.learn.lfy.mtr.controller;

import cn.learn.lfy.mtr.api.ApiResponse;
import cn.learn.lfy.mtr.api.UploadAssetResponse;
import cn.learn.lfy.mtr.auth.AuthConstants;
import cn.learn.lfy.mtr.domain.model.AppUser;
import cn.learn.lfy.mtr.domain.model.UploadAsset;
import cn.learn.lfy.mtr.service.UploadService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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
            @RequestAttribute(AuthConstants.REQUEST_USER) AppUser user,
            @RequestParam long templateId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(uploadService.upload(templateId, user.id(), file));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(
            @RequestAttribute(AuthConstants.REQUEST_USER) AppUser user,
            @PathVariable String id
    ) {
        UploadService.UploadContent content = uploadService.content(id, user.id());
        UploadAsset asset = content.asset();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType(asset.mimeType()))
                .contentLength(asset.sizeBytes())
                .body(new FileSystemResource(content.path()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestAttribute(AuthConstants.REQUEST_USER) AppUser user,
            @PathVariable String id
    ) {
        uploadService.delete(id, user.id());
        return ApiResponse.success();
    }
}
