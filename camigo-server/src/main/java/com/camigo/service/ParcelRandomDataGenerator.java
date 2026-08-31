package com.camigo.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** 阶段8有限字典随机数据，只生成非业务选择字段。 */
@Component
public class ParcelRandomDataGenerator {
    private static final String[] CARRIERS = {"SF", "YT", "ZTO", "JD", "STO"};
    private static final String[] SENDERS = {"李明", "王芳", "张伟", "刘洋", "陈晨", "赵敏", "周宁", "孙悦"};
    private final SecureRandom random = new SecureRandom();

    public RandomParcelData next() {
        String carrier = CARRIERS[random.nextInt(CARRIERS.length)];
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String trackingNo = carrier + stamp + String.format("%06d", random.nextInt(1_000_000));
        String sender = SENDERS[random.nextInt(SENDERS.length)];
        String phone = "1" + (3 + random.nextInt(7)) + random.nextInt(10)
                + "****" + String.format("%04d", random.nextInt(10_000));
        return new RandomParcelData(carrier, trackingNo, sender, phone);
    }

    public record RandomParcelData(String carrierCode, String trackingNo,
                                   String senderName, String senderPhoneMasked) {}
}
