package cn.learn.lfy.mtr.mapper;

import cn.learn.lfy.mtr.domain.model.UploadAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UploadAssetMapper {

    int insert(
            @Param("id") String id,
            @Param("userId") long userId,
            @Param("templateId") long templateId,
            @Param("originalFilename") String originalFilename,
            @Param("storageKey") String storageKey,
            @Param("mimeType") String mimeType,
            @Param("sizeBytes") long sizeBytes
    );

    Optional<UploadAsset> findById(@Param("id") String id);

    Optional<UploadAsset> findByIdAndUserId(
            @Param("id") String id,
            @Param("userId") long userId
    );

    int deleteByIdAndUserId(
            @Param("id") String id,
            @Param("userId") long userId
    );
}
