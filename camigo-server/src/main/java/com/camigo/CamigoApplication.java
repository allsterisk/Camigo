package com.camigo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Camigo后端应用入口。
 */
@SpringBootApplication
@MapperScan("com.camigo.mapper")
@ConfigurationPropertiesScan
@EnableScheduling
public class CamigoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CamigoApplication.class, args);
    }
}
