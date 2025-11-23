package com.loyce.omniflow.service;

import com.loyce.omniflow.dto.resp.NodeRespDTO;
import org.springframework.web.multipart.MultipartFile;

public interface DirectoryService {

    /**
     * 上传文件并创建节点
     * @param file
     * @param parentId
     * @return
     */
    NodeRespDTO uploadAndCreateNode(MultipartFile file, Long parentId, Long libraryId);
}
