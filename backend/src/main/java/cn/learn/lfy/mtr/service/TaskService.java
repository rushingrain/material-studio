package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.api.ApiErrorCode;
import cn.learn.lfy.mtr.api.BusinessException;
import cn.learn.lfy.mtr.api.CreateTaskRequest;
import cn.learn.lfy.mtr.api.TaskCreatedResponse;
import cn.learn.lfy.mtr.domain.enums.TaskStage;
import cn.learn.lfy.mtr.domain.enums.TaskStatus;
import cn.learn.lfy.mtr.domain.event.TaskSubmittedEvent;
import cn.learn.lfy.mtr.domain.model.Task;
import cn.learn.lfy.mtr.domain.model.TaskAsset;
import cn.learn.lfy.mtr.domain.model.Template;
import cn.learn.lfy.mtr.domain.model.UploadAsset;
import cn.learn.lfy.mtr.mapper.TaskAssetMapper;
import cn.learn.lfy.mtr.mapper.TaskMapper;
import cn.learn.lfy.mtr.mapper.TemplateMapper;
import cn.learn.lfy.mtr.mapper.UploadAssetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TaskService {
    private final TaskMapper taskMapper;
    private final TaskAssetMapper taskAssetMapper;
    private final TemplateMapper templateMapper;
    private final UploadAssetMapper uploadAssetMapper;
    private final UploadStorage uploadStorage;
    private final ApplicationEventPublisher eventPublisher;
    private final long demoUserId;

    public TaskService(
            TaskMapper taskMapper,
            TaskAssetMapper taskAssetMapper,
            TemplateMapper templateMapper,
            UploadAssetMapper uploadAssetMapper,
            UploadStorage uploadStorage,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.demo-user-id}") long demoUserId
    ) {
        this.taskMapper = taskMapper;
        this.taskAssetMapper = taskAssetMapper;
        this.templateMapper = templateMapper;
        this.uploadAssetMapper = uploadAssetMapper;
        this.uploadStorage = uploadStorage;
        this.eventPublisher = eventPublisher;
        this.demoUserId = demoUserId;
    }

    @Transactional
    public TaskCreatedResponse submit(CreateTaskRequest request) {
        if (request == null || request.templateId() == null) {
            throw new BusinessException(ApiErrorCode.INVALID_REQUEST, "请选择模板");
        }

        Template template = templateMapper.findActiveById(request.templateId())
                .orElseThrow(() -> new BusinessException(ApiErrorCode.TEMPLATE_NOT_FOUND));
        List<String> assetIds = normalizeAssetIds(request.assetIds());
        if (assetIds.size() != template.requiredImageCount()) {
            throw new BusinessException(
                    ApiErrorCode.INVALID_REQUEST,
                    "该模板需要 " + template.requiredImageCount() + " 张素材"
            );
        }

        List<UploadAsset> assets = new ArrayList<>(assetIds.size());
        for (String assetId : assetIds) {
            UploadAsset asset = uploadAssetMapper.findById(assetId)
                    .orElseThrow(() -> new BusinessException(ApiErrorCode.ASSET_NOT_FOUND));
            validateAsset(asset, template);
            assets.add(asset);
        }

        String taskId = UUID.randomUUID().toString();
        Task task = new Task(
                taskId,
                demoUserId,
                template.id(),
                TaskStatus.PENDING,
                TaskStage.QUEUED,
                null,
                null,
                null,
                null,
                null,
                null
        );
        taskMapper.insert(task);

        for (int index = 0; index < assets.size(); index++) {
            UploadAsset asset = assets.get(index);
            taskAssetMapper.insert(new TaskAsset(
                    null,
                    taskId,
                    asset.originalFilename(),
                    asset.storageKey(),
                    asset.mimeType(),
                    asset.sizeBytes(),
                    index,
                    null
            ));
        }

        eventPublisher.publishEvent(new TaskSubmittedEvent(taskId));
        return new TaskCreatedResponse(taskId);
    }

    @Transactional(readOnly = true)
    public Task findById(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new BusinessException(ApiErrorCode.TASK_NOT_FOUND);
        }
        return taskMapper.findById(taskId.trim())
                .orElseThrow(() -> new BusinessException(ApiErrorCode.TASK_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Task> findForCurrentUser() {
        return taskMapper.findByUserId(demoUserId);
    }

    private static List<String> normalizeAssetIds(List<String> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            throw new BusinessException(ApiErrorCode.INVALID_REQUEST, "请上传所需素材");
        }

        List<String> normalized = new ArrayList<>(assetIds.size());
        for (String assetId : assetIds) {
            if (assetId == null || assetId.isBlank()) {
                throw new BusinessException(ApiErrorCode.INVALID_REQUEST, "素材 ID 不能为空");
            }
            normalized.add(assetId.trim());
        }
        if (new HashSet<>(normalized).size() != normalized.size()) {
            throw new BusinessException(ApiErrorCode.INVALID_REQUEST, "素材 ID 不能重复");
        }
        return normalized;
    }

    private void validateAsset(UploadAsset asset, Template template) {
        if (!asset.templateId().equals(template.id())) {
            throw new BusinessException(ApiErrorCode.INVALID_REQUEST, "素材与所选模板不匹配");
        }
        String mimeType = normalizeMimeType(asset.mimeType());
        if (template.allowedMimeTypes().stream()
                .map(TaskService::normalizeMimeType)
                .noneMatch(mimeType::equals)) {
            throw new BusinessException(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "素材格式不符合模板要求");
        }
        if (asset.sizeBytes() == null || asset.sizeBytes() <= 0
                || asset.sizeBytes() > template.maxImageSizeBytes()) {
            throw new BusinessException(ApiErrorCode.FILE_TOO_LARGE, "素材大小不符合模板要求");
        }
        if (!uploadStorage.containsFile(asset.storageKey(), asset.sizeBytes())) {
            throw new BusinessException(ApiErrorCode.ASSET_NOT_FOUND, "素材文件不存在或已损坏，请重新上传");
        }
    }

    private static String normalizeMimeType(String mimeType) {
        if (mimeType == null) return "";
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        return "image/jpg".equals(normalized) ? "image/jpeg" : normalized;
    }
}
