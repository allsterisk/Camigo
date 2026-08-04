package com.camigo;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.camigo.config.SwaggerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Phase 4.1 启动冒烟测试：验证 Web 上下文、OpenAPI 和 Swagger UI。
 * 数据访问自动配置在本测试中关闭，避免测试对开发数据库产生读写。
 */
@SpringBootTest(
        classes = ApplicationSmokeTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "camigo.jwt.secret=camigo-test-secret-must-be-at-least-32-bytes-long"
        })
class ApplicationSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldExposeOpenApiAndSwaggerUi() {
        ResponseEntity<String> openApi = restTemplate.getForEntity(url("/v3/api-docs"), String.class);
        ResponseEntity<String> swagger = restTemplate.getForEntity(
                url("/swagger-ui/index.html"), String.class);

        assertThat(openApi.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(openApi.getBody()).contains("Camigo");
        assertThat(swagger.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(swagger.getBody()).contains("Swagger UI");
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
    @Import(SwaggerConfig.class)
    static class TestApplication {
    }
}
