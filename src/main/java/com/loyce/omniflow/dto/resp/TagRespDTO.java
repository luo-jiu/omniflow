package com.loyce.omniflow.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagRespDTO {
    private Long id;
    private String name;
    private String type;
    private String targetKey;
    private Long ownerUserId;
    private String color;
    private String textColor;
    private Integer sortOrder;
    private Integer enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
