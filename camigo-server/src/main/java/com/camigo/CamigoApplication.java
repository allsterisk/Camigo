package com.camigo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Camigo后端应用入口。
 */
@SpringBootApplication
@MapperScan("com.camigo.mapper")
@ConfigurationPropertiesScan
public class CamigoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CamigoApplication.class, args);
    }
}
