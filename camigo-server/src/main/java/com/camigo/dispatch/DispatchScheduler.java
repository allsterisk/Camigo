package com.camigo.dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定期以数据库过期时间为准处理超时邀请；下一轮只能由管理员启动。 */
@Component
public class DispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(DispatchScheduler.class);
    private final IntelligentDispatchService dispatchService;

    public DispatchScheduler(IntelligentDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Scheduled(fixedDelayString = "${camigo.dispatch.scheduler-delay-ms:5000}")
    public void processDispatchTimers() {
        for (Long assignmentId : dispatchService.expiredOfferIds()) {
            try {
                dispatchService.expireOffer(assignmentId);
            } catch (RuntimeException ex) {
                log.error("处理超时派单失败, assignmentId={}", assignmentId, ex);
            }
        }
    }
}
