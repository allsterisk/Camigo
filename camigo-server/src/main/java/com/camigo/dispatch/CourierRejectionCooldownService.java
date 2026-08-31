package com.camigo.dispatch;

import com.camigo.config.DispatchProperties;
import com.camigo.mapper.DeliveryAssignmentMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** 使用Memurai维护拒绝滑动窗口和15分钟短期冷却，并以MySQL审计记录兜底。 */
@Service
public class CourierRejectionCooldownService {

    private static final Logger log = LoggerFactory.getLogger(CourierRejectionCooldownService.class);
    private static final String KEY_PREFIX = "camigo:dispatch:rejections:";
    private static final String COOLDOWN_PREFIX = "camigo:dispatch:cooldown:";

    private final StringRedisTemplate redisTemplate;
    private final DeliveryAssignmentMapper assignmentMapper;
    private final DispatchProperties properties;

    public CourierRejectionCooldownService(StringRedisTemplate redisTemplate,
                                           DeliveryAssignmentMapper assignmentMapper,
                                           DispatchProperties properties) {
        this.redisTemplate = redisTemplate;
        this.assignmentMapper = assignmentMapper;
        this.properties = properties;
    }

    public boolean isCoolingDown(long courierId, LocalDateTime now) {
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey(courierId)))) {
                return true;
            }
        } catch (RuntimeException ex) {
            log.warn("Memurai冷却读取失败，改用MySQL拒绝记录判断, courierId={}", courierId);
        }
        long count = assignmentMapper.countRecentRejections(courierId,
                now.minusMinutes(properties.rejectionWindowMinutes()));
        if (count >= properties.rejectionThreshold()) {
            writeCooldown(courierId);
            return true;
        }
        return false;
    }

    public void recordRejection(long courierId, LocalDateTime responseTime) {
        String historyKey = historyKey(courierId);
        double score = responseTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double windowStart = responseTime.minusMinutes(properties.rejectionWindowMinutes())
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        try {
            redisTemplate.opsForZSet().add(historyKey, UUID.randomUUID().toString(), score);
            redisTemplate.opsForZSet().removeRangeByScore(historyKey, 0, windowStart);
            redisTemplate.expire(historyKey,
                    Duration.ofMinutes(properties.rejectionWindowMinutes()
                            + properties.cooldownMinutes()));
            Long count = redisTemplate.opsForZSet().zCard(historyKey);
            if (count != null && count >= properties.rejectionThreshold()) {
                writeCooldown(courierId);
            }
        } catch (RuntimeException ex) {
            log.warn("Memurai拒绝频率写入失败，MySQL审计记录仍可用于后续兜底, courierId={}", courierId);
        }
    }

    private void writeCooldown(long courierId) {
        try {
            redisTemplate.opsForValue().set(cooldownKey(courierId), "1",
                    Duration.ofMinutes(properties.cooldownMinutes()));
        } catch (RuntimeException ex) {
            log.warn("Memurai冷却标记写入失败, courierId={}", courierId);
        }
    }

    private String historyKey(long courierId) {
        return KEY_PREFIX + courierId;
    }

    private String cooldownKey(long courierId) {
        return COOLDOWN_PREFIX + courierId;
    }
}
