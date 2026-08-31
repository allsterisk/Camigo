-- Camigo 阶段3：把 delivery_routes 从“一单一路线”切换为“配送员综合路线版本”。
-- 本脚本只放宽旧字段并收紧阶段2新增字段，不删除列、不删除历史记录。
-- 可重复执行；执行前应先完成 stage2-additive-migration.sql。

USE `camigo`;

DROP PROCEDURE IF EXISTS `_camigo_stage3_add_index`;
DROP PROCEDURE IF EXISTS `_camigo_stage3_drop_index`;

DELIMITER $$

CREATE PROCEDURE `_camigo_stage3_add_index`(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD ', p_definition);
        PREPARE statement_to_run FROM @ddl;
        EXECUTE statement_to_run;
        DEALLOCATE PREPARE statement_to_run;
    END IF;
END$$

CREATE PROCEDURE `_camigo_stage3_drop_index`(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` DROP INDEX `', p_index_name, '`');
        PREPARE statement_to_run FROM @ddl;
        EXECUTE statement_to_run;
        DEALLOCATE PREPARE statement_to_run;
    END IF;
END$$

DELIMITER ;

-- 先补普通索引以继续支撑旧订单外键和兼容查询，再移除“一单一路线”的唯一限制。
CALL `_camigo_stage3_add_index`(
    'delivery_routes',
    'idx_routes_legacy_order_type',
    'INDEX `idx_routes_legacy_order_type` (`delivery_order_id`, `route_type`)'
);
CALL `_camigo_stage3_drop_index`('delivery_routes', 'uk_routes_order_type');

-- 新综合路线不再属于单张订单，也不再依赖旧的首尾地点摘要。
ALTER TABLE `delivery_routes`
    MODIFY COLUMN `delivery_order_id` BIGINT UNSIGNED NULL
        COMMENT '兼容旧路线的配送订单编号；综合路线为空',
    MODIFY COLUMN `route_type` VARCHAR(16) NULL
        COMMENT '兼容旧路线类型；综合路线为空',
    MODIFY COLUMN `start_location_id` BIGINT UNSIGNED NULL
        COMMENT '兼容旧路线起点；综合路线以CURRENT停靠点为准',
    MODIFY COLUMN `end_location_id` BIGINT UNSIGNED NULL
        COMMENT '兼容旧路线终点；综合路线以最后停靠点为准',
    MODIFY COLUMN `version_no` INT UNSIGNED NOT NULL
        COMMENT '配送员路线版本号',
    MODIFY COLUMN `status` VARCHAR(20) NOT NULL
        COMMENT '路线版本状态',
    MODIFY COLUMN `total_distance_meters` INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '当前路线总距离（米）',
    MODIFY COLUMN `estimated_duration_seconds` INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '当前路线预计剩余耗时（秒）',
    MODIFY COLUMN `algorithm_version` VARCHAR(32) NOT NULL
        COMMENT '路线算法版本';

DROP PROCEDURE IF EXISTS `_camigo_stage3_add_index`;
DROP PROCEDURE IF EXISTS `_camigo_stage3_drop_index`;

-- 只读验收摘要。
SELECT COUNT(*) AS route_count,
       SUM(`delivery_order_id` IS NULL) AS composite_route_count,
       SUM(`status` = 'ACTIVE') AS active_route_count
FROM `delivery_routes`;
