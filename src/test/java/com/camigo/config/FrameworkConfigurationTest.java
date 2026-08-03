package com.camigo.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import java.time.LocalDateTime;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;

class FrameworkConfigurationTest {

    @Test
    void shouldConfigureMySqlPagination() {
        MybatisPlusInterceptor interceptor = new MyBatisPlusConfig().mybatisPlusInterceptor();

        assertThat(interceptor.getInterceptors()).hasSize(1);
    }

    @Test
    void shouldFillAuditTimestamps() {
        AuditFields fields = new AuditFields();
        AuditingMetaObjectHandler handler = new AuditingMetaObjectHandler();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, AuditFields.class);

        handler.insertFill(SystemMetaObject.forObject(fields));

        assertThat(fields.getCreateTime()).isNotNull();
        assertThat(fields.getUpdateTime()).isNotNull();
    }

    @Test
    void shouldPublishBearerSecurityScheme() {
        OpenAPI openAPI = new SwaggerConfig().camigoOpenApi();

        assertThat(openAPI.getInfo().getTitle()).contains("Camigo");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("BearerAuth");
    }

    @TableName("test_audit_fields")
    static class AuditFields {
        @TableId
        private Long id;

        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;

        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updateTime;

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }
    }
}
