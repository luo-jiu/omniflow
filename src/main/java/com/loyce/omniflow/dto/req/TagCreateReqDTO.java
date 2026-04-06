package com.loyce.omniflow.dto.req;

import lombok.Data;

@Data
public class TagCreateReqDTO {
    private String name;
    private String type;
    private String targetKey;
    private String color;
    private String textColor;
    private Integer sortOrder;
    private Integer enabled;
    private String description;
}
