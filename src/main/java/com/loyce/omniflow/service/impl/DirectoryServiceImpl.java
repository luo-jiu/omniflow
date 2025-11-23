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
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
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
        String parentPath = nodeService.getFullPath(parentId, libraryId);
        String fileName = file.getOriginalFilename();
        String fullPath = parentPath + "/" + fileName;
        Boolean ok = true;
        try {
            // 2. 上传到 MinIO
            ok = minio.uploadFile(
                fullPath,
                file.getInputStream(),
                file.getSize(),
                file.getContentType());
            // 3. 创建节点
            NodeCreateReqDTO req = NodeCreateReqDTO.builder()
                    .name(fileName)
                    .type(1) // 一定是文件
                    .parentId(parentId)
                    .libraryId(libraryId)
                    .fileSize(file.getSize())
                    .mineType(FilenameUtils.getExtension(fileName))
                    .build();
            // 4. 在目录树创建数据库节点
            NodeDO nodeDO = nodeService.createNode(req);
            return BeanUtil.toBean(nodeDO, NodeRespDTO.class);
        } catch (Exception ignored) {
            ignored.printStackTrace();
            // 这里做 MinIO 补偿
            if (!ok) {
                try {
                    minio.deleteFile(fullPath);
                } catch (Exception deleteEx) {
                    // todo 可记录日志，不抛错
                    // log.error("文件上传失败，DB 事务回滚，但 MinIO 删除补偿失败: {}", fullPath, deleteEx);
                }
            }
            throw new ServiceException(FileErrorCodeEnum.File_UPLOAD_ERROR);
        }
    }
}
