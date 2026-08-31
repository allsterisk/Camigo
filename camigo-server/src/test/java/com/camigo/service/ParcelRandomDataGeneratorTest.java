package com.camigo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ParcelRandomDataGeneratorTest {
    private static final Set<String> CARRIERS = Set.of("SF", "YT", "ZTO", "JD", "STO");
    private static final Set<String> SENDERS = Set.of(
            "李明", "王芳", "张伟", "刘洋", "陈晨", "赵敏", "周宁", "孙悦");

    @Test
    void shouldOnlyGenerateValuesFromFiniteDictionaries() {
        ParcelRandomDataGenerator generator = new ParcelRandomDataGenerator();

        for (int index = 0; index < 200; index++) {
            ParcelRandomDataGenerator.RandomParcelData value = generator.next();
            assertThat(value.carrierCode()).isIn(CARRIERS);
            assertThat(value.senderName()).isIn(SENDERS);
            assertThat(value.trackingNo())
                    .startsWith(value.carrierCode())
                    .matches("(?:SF|YT|ZTO|JD|STO)\\d{23}");
            assertThat(value.senderPhoneMasked()).matches("1[3-9]\\d\\*{4}\\d{4}");
        }
    }
}
