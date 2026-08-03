package com.camigo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.camigo.CamigoApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 完整应用接口文档冒烟测试，确认六个业务模块已注册到 Spring MVC。
 */
@SpringBootTest(classes = CamigoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "CAMIGO_RUN_DB_TESTS", matches = "true")
class BusinessOpenApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldPublishAllBusinessModuleEndpoints() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://127.0.0.1:" + port + "/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("/api/users/register")
                .contains("/api/parcels")
                .contains("/api/parcels/my")
                .contains("/api/parcels/{id}/status")
                .contains("/api/delivery-orders")
                .contains("/api/delivery-orders/tasks")
                .contains("/api/delivery-orders/{id}/accept")
                .contains("/api/delivery-orders/{id}/status")
                .contains("/api/lockers/storage")
                .contains("/api/evaluations")
                .contains("/api/statistics/parcels/daily");
    }
}
