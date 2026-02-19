package com.loyce.omniflow.listener;

import com.loyce.omniflow.event.NodeDeleteEvent;
import com.loyce.omniflow.util.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 节点删除事件监听器
 * 在事务提交后删除MinIO中的文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeDeleteEventListener {

    private final MinioUtils minioUtils;

    /**
     * 监听节点删除事件，在事务提交后删除MinIO文件
     * 使用异步执行，避免阻塞主线程
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNodeDeleteEvent(NodeDeleteEvent event) {
        List<String> filePaths = event.getFilePaths();
        Long libraryId = event.getLibraryId();
        
        if (filePaths == null || filePaths.isEmpty()) {
            log.debug("节点删除事件：没有需要删除的文件，libraryId={}", libraryId);
            return;
        }

        log.info("开始批量删除MinIO文件，数量={}，libraryId={}", filePaths.size(), libraryId);
        
        int successCount = 0;
        int failCount = 0;
        
        for (String filePath : filePaths) {
            try {
                minioUtils.deleteFile(filePath);
                successCount++;
                log.debug("成功删除MinIO文件: {}", filePath);
            } catch (Exception e) {
                failCount++;
                // 记录失败日志，但不抛出异常，避免影响其他文件的删除
                log.error("删除MinIO文件失败: {}, libraryId={}, 错误信息: {}", 
                    filePath, libraryId, e.getMessage(), e);
                // TODO: 可以考虑将失败的文件路径记录到补偿任务表中，后续重试
            }
        }
        
        log.info("MinIO文件删除完成，成功={}，失败={}，libraryId={}", 
            successCount, failCount, libraryId);
        
        if (failCount > 0) {
            log.warn("部分MinIO文件删除失败，建议检查补偿任务或手动清理，libraryId={}", libraryId);
        }
    }
}
