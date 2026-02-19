package com.loyce.omniflow.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 节点删除事件
 * 在事务提交后触发，用于删除MinIO中的文件
 */
@Getter
public class NodeDeleteEvent extends ApplicationEvent {
    
    /**
     * 要删除的文件路径列表（objectKey）
     */
    private final List<String> filePaths;
    
    /**
     * 库ID
     */
    private final Long libraryId;

    public NodeDeleteEvent(Object source, List<String> filePaths, Long libraryId) {
        super(source);
        this.filePaths = filePaths;
        this.libraryId = libraryId;
    }
}
