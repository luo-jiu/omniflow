package com.loyce.omniflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.loyce.omniflow.common.convention.exception.ServiceException;
import com.loyce.omniflow.common.enums.FileErrorCodeEnum;
import com.loyce.omniflow.dao.entity.NodeDO;
import com.loyce.omniflow.dto.req.NodeCreateReqDTO;
import com.loyce.omniflow.dto.resp.NodeRespDTO;
import com.loyce.omniflow.service.DirectoryService;
import com.loyce.omniflow.service.NodeService;
import com.loyce.omniflow.util.MinioUtils;
import com.loyce.omniflow.util.StorageKeyUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {

    private final MinioUtils minio;
    private final NodeService nodeService;

    @Override
    @Transactional
    public NodeRespDTO uploadAndCreateNode(MultipartFile file, Long parentId, Long libraryId) {
        // 1. 构建路径
//        String parentPath = nodeService.getFullPath(parentId, libraryId);
//        String fileName = file.getOriginalFilename();
//        String fullPath = parentPath + "/" + fileName;
        // 1. 解析文件名
        String originalFilename = file.getOriginalFilename();
        String ext = FilenameUtils.getExtension(originalFilename);
        String name = FilenameUtils.getBaseName(originalFilename);

        // 2. 生成稳定的 storageKey
        String storageKey = StorageKeyUtil.generate(libraryId);

//        System.out.println("地址:" + fullPath);
//        Boolean ok = true;
        boolean uploaded = false;
        try {
            // 3. 识别 MIME（服务端）
            Tika tika = new Tika();
            String mimeType = tika.detect(file.getInputStream());

            // 2. 上传到 MinIO
            minio.uploadFile(
                storageKey,
                file.getInputStream(),
                file.getSize(),
                mimeType
            );

            // 5. 创建节点
            NodeCreateReqDTO req = NodeCreateReqDTO.builder()
                    .name(name)
                    .ext(ext)
                    .type(1)
                    .parentId(parentId)
                    .libraryId(libraryId)
                    .fileSize(file.getSize())
                    .mimeType(mimeType)
                    .storageKey(storageKey)
                    .build();

            NodeDO nodeDO = nodeService.createNode(req);
            return BeanUtil.toBean(nodeDO, NodeRespDTO.class);
        } catch (Exception ignored) {
            ignored.printStackTrace();
            // 这里做 MinIO 补偿
            if (!uploaded) {
                try {
                    minio.deleteFile(storageKey);
                } catch (Exception deleteEx) {
                    // todo 可记录日志，不抛错
                    // log.error("文件上传失败，DB 事务回滚，但 MinIO 删除补偿失败: {}", fullPath, deleteEx);
                }
            }
            throw new ServiceException(FileErrorCodeEnum.File_UPLOAD_ERROR);
        }
    }

    @Override
    public String getPresignedUrl(Long nodeId, Long libraryId, Integer expiryMinutes) {
        String filePath = nodeService.getStorageKey(nodeId);
        System.out.println("地址1: " + filePath);
        int expirySeconds = expiryMinutes * 60;
        try {
            String preignedUrl = minio.getPreignedUrl(filePath, expirySeconds);
            System.out.println("地址2: " + preignedUrl);
            return preignedUrl;
        } catch (Exception deleteEx) {
            // todo 可记录日志，不抛错
        }
        return null;
    }
}
