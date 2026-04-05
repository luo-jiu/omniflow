package com.loyce.omniflow.job;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.loyce.omniflow.dao.mapper.NodeMapper;
import com.loyce.omniflow.dto.resp.NodeRecycleRespDTO;
import com.loyce.omniflow.service.NodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecycleBinCleanupJob {

    private final NodeMapper nodeMapper;
    private final NodeService nodeService;
    @Value("${omniflow.recycle-bin.retention-days:30}")
    private Integer retentionDays;

    @Scheduled(cron = "${omniflow.recycle-bin.cleanup-cron:0 30 4 * * ?}")
    public void cleanupExpiredItems() {
        if (retentionDays <= 0) {
            log.warn("回收站自动清理已跳过：retentionDays={} 非法", retentionDays);
            return;
        }

        LocalDateTime expiredBefore = LocalDateTime.now().minusDays(retentionDays);
        List<Long> libraryIds = nodeMapper.selectLibraryIdsWithDeletedNodes();
        if (CollectionUtils.isEmpty(libraryIds)) {
            return;
        }

        int purgedCount = 0;
        for (Long libraryId : libraryIds) {
            try {
                List<NodeRecycleRespDTO> recycleItems = nodeService.getRecycleBinItems(libraryId);
                if (CollectionUtils.isEmpty(recycleItems)) {
                    continue;
                }
                for (NodeRecycleRespDTO item : recycleItems) {
                    if (item.getDeletedAt() != null && item.getDeletedAt().isBefore(expiredBefore)) {
                        boolean deleted = nodeService.hardDeleteNodeAndChildren(item.getId(), libraryId);
                        if (deleted) {
                            purgedCount++;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("回收站自动清理失败, libraryId={}, err={}", libraryId, e.getMessage(), e);
            }
        }

        if (purgedCount > 0) {
            log.info("回收站自动清理完成，已彻底删除 {} 条过期记录", purgedCount);
        }
    }
}
