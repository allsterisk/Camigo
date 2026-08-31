package com.camigo.service;

import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** 生成保留前导零的四位取件码。 */
@Component
public class PickupCodeGenerator {

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        return format(random.nextInt(10_000));
    }

    String format(int value) {
        if (value < 0 || value > 9_999) {
            throw new IllegalArgumentException("取件码数值必须在0000到9999之间");
        }
        return String.format(Locale.ROOT, "%04d", value);
    }
}
