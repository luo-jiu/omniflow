package com.loyce.omniflow.dto.req;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeCreateReqDTO {
    private String name;
    private String ext;
    private String mimeType;
    private Long fileSize;
    private String storageKey;
    private Integer type;
    private Long parentId;
    private Long libraryId;
}
