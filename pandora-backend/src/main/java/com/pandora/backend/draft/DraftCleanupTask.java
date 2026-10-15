package com.pandora.backend.draft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DraftCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(DraftCleanupTask.class);

    @Autowired
    private DraftService draftService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredDrafts() {
        try {
            int deleted = draftService.cleanupExpired();
            log.info("Draft cleanup task completed: deleted {} expired drafts", deleted);
        } catch (Exception e) {
            log.error("Draft cleanup task failed", e);
        }
    }
}