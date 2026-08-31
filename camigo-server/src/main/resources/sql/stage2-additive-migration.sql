-- Camigo 最终执行版：阶段2数据库增量迁移
-- 目标数据库：MySQL 8.0.x
-- 使用方式：mysql --database=<目标库> --execute="SOURCE .../stage2-additive-migration.sql"
--
-- 本脚本只增加结构、初始化道路和回填兼容数据，不删除旧表、旧列或业务记录。
-- MySQL DDL 会隐式提交，因此执行前必须保留完整数据库备份。

SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS `_camigo_stage2_assert_database`$$
CREATE PROCEDURE `_camigo_stage2_assert_database`()
BEGIN
    IF DATABASE() IS NULL
       OR DATABASE() IN ('mysql', 'information_schema', 'performance_schema', 'sys') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Stage2 migration requires an explicitly selected non-system database';
    END IF;
END$$

DROP PROCEDURE IF EXISTS `_camigo_stage2_add_column`$$
CREATE PROCEDURE `_camigo_stage2_add_column`(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @stage2_ddl = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
            '` ADD COLUMN `', REPLACE(p_column_name, '`', '``'), '` ',
            p_definition
        );
        PREPARE stage2_stmt FROM @stage2_ddl;
        EXECUTE stage2_stmt;
        DEALLOCATE PREPARE stage2_stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS `_camigo_stage2_add_index`$$
CREATE PROCEDURE `_camigo_stage2_add_index`(
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
        SET @stage2_ddl = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
            '` ADD ', p_definition
        );
        PREPARE stage2_stmt FROM @stage2_ddl;
        EXECUTE stage2_stmt;
        DEALLOCATE PREPARE stage2_stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS `_camigo_stage2_add_constraint`$$
CREATE PROCEDURE `_camigo_stage2_add_constraint`(
    IN p_table_name VARCHAR(64),
    IN p_constraint_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = p_table_name
          AND constraint_name = p_constraint_name
    ) THEN
        SET @stage2_ddl = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
            '` ADD CONSTRAINT `', REPLACE(p_constraint_name, '`', '``'),
            '` ', p_definition
        );
        PREPARE stage2_stmt FROM @stage2_ddl;
        EXECUTE stage2_stmt;
        DEALLOCATE PREPARE stage2_stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS `_camigo_stage2_assert_locations`$$
CREATE PROCEDURE `_camigo_stage2_assert_locations`()
BEGIN
    DECLARE v_location_count INT DEFAULT 0;

    SELECT COUNT(DISTINCT location_code)
    INTO v_location_count
    FROM campus_locations
    WHERE location_code IN (
        'CAMIGO-GATE-E',
        'CAMIGO-DORM-01',
        'CAMIGO-DORM-02',
        'CAMIGO-DORM-03',
        'CAMIGO-DORM-04',
        'CAMIGO-DORM-05',
        'CAMIGO-DORM-06',
        'CAMIGO-DORM-07',
        'CAMIGO-DORM-08',
        'CAMIGO-TEACH-01',
        'CAMIGO-LIBRARY',
        'CAMIGO-GATE-W'
    );

    IF v_location_count <> 12 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Stage2 migration requires all 12 campus route-map locations';
    END IF;
END$$

DELIMITER ;

CALL `_camigo_stage2_assert_database`();
CALL `_camigo_stage2_assert_locations`();

-- 1. 地点能力字段。经纬度暂时保留，待新算法与前端完全切换后再删除。
CALL `_camigo_stage2_add_column`(
    'campus_locations',
    'pickup_enabled',
    'TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''是否允许作为配送取件点'' AFTER `latitude`'
);
CALL `_camigo_stage2_add_column`(
    'campus_locations',
    'delivery_enabled',
    'TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''是否允许作为配送目的地'' AFTER `pickup_enabled`'
);

-- 2. 配送员当前位置。
CALL `_camigo_stage2_add_column`(
    'couriers',
    'current_location_id',
    'BIGINT UNSIGNED NULL COMMENT ''当前校园地点'' AFTER `rating_avg`'
);
CALL `_camigo_stage2_add_column`(
    'couriers',
    'location_update_time',
    'DATETIME(3) NULL COMMENT ''当前位置更新时间'' AFTER `current_location_id`'
);

-- 3. 订单取件服务站。兼容期保持可空，旧后端仍可创建订单。
CALL `_camigo_stage2_add_column`(
    'delivery_orders',
    'pickup_location_id',
    'BIGINT UNSIGNED NULL COMMENT ''本次配送的取件服务站'' AFTER `parcel_id`'
);

-- 4. 综合路线版本字段。旧的一单一路线字段仍保留。
CALL `_camigo_stage2_add_column`(
    'delivery_routes',
    'version_no',
    'INT UNSIGNED NULL COMMENT ''配送员路线版本号'' AFTER `courier_id`'
);
CALL `_camigo_stage2_add_column`(
    'delivery_routes',
    'status',
    'VARCHAR(20) NULL COMMENT ''路线版本状态'' AFTER `version_no`'
);
CALL `_camigo_stage2_add_column`(
    'delivery_routes',
    'total_distance_meters',
    'INT UNSIGNED NULL COMMENT ''当前路线总距离（米）'' AFTER `status`'
);
CALL `_camigo_stage2_add_column`(
    'delivery_routes',
    'estimated_duration_seconds',
    'INT UNSIGNED NULL COMMENT ''当前路线预计剩余耗时（秒）'' AFTER `total_distance_meters`'
);
CALL `_camigo_stage2_add_column`(
    'delivery_routes',
    'algorithm_version',
    'VARCHAR(32) NULL COMMENT ''路线算法版本'' AFTER `estimated_duration_seconds`'
);
CALL `_camigo_stage2_add_column`(
    'delivery_routes',
    'active_courier_id',
    'BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN `status` = ''ACTIVE'' THEN `courier_id` ELSE NULL END) STORED COMMENT ''保证每名配送员最多一条活动路线'' AFTER `algorithm_version`'
);

-- 5. 派单邀请、响应和拒绝审计字段。
CALL `_camigo_stage2_add_column`(
    'delivery_assignments',
    'offer_time',
    'DATETIME(3) NULL COMMENT ''邀请发出时间'' AFTER `decision_reason`'
);
CALL `_camigo_stage2_add_column`(
    'delivery_assignments',
    'offer_expire_time',
    'DATETIME(3) NULL COMMENT ''邀请数据库过期时间'' AFTER `offer_time`'
);
CALL `_camigo_stage2_add_column`(
    'delivery_assignments',
    'response_time',
    'DATETIME(3) NULL COMMENT ''配送员响应时间'' AFTER `offer_expire_time`'
);
CALL `_camigo_stage2_add_column`(
    'delivery_assignments',
    'reject_reason_code',
    'VARCHAR(32) NULL COMMENT ''拒绝原因编码'' AFTER `response_time`'
);
CALL `_camigo_stage2_add_column`(
    'delivery_assignments',
    'reject_reason_detail',
    'VARCHAR(255) NULL COMMENT ''其他拒绝原因说明'' AFTER `reject_reason_code`'
);

-- 6. 校园道路图。
CREATE TABLE IF NOT EXISTS `campus_route_edges` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '校园道路编号',
    `from_location_id` BIGINT UNSIGNED NOT NULL COMMENT '起点地点',
    `to_location_id` BIGINT UNSIGNED NOT NULL COMMENT '终点地点',
    `distance_meters` INT UNSIGNED NOT NULL COMMENT '道路距离（米）',
    `bidirectional` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否双向通行',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '道路状态',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_route_edges_direction` (`from_location_id`, `to_location_id`),
    KEY `idx_route_edges_from_status` (`from_location_id`, `status`),
    KEY `idx_route_edges_to_status` (`to_location_id`, `status`),
    CONSTRAINT `fk_route_edges_from_location`
        FOREIGN KEY (`from_location_id`) REFERENCES `campus_locations` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_route_edges_to_location`
        FOREIGN KEY (`to_location_id`) REFERENCES `campus_locations` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `ck_route_edges_distance` CHECK (`distance_meters` > 0),
    CONSTRAINT `ck_route_edges_distinct_locations` CHECK (`from_location_id` <> `to_location_id`),
    CONSTRAINT `ck_route_edges_bidirectional` CHECK (`bidirectional` IN (0, 1)),
    CONSTRAINT `ck_route_edges_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='校园地点之间的带权道路';

-- 7. 综合路线停靠点。
CREATE TABLE IF NOT EXISTS `route_stops` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '路线停靠点编号',
    `delivery_route_id` BIGINT UNSIGNED NOT NULL COMMENT '所属路线版本',
    `delivery_order_id` BIGINT UNSIGNED NULL COMMENT '关联配送订单，CURRENT节点为空',
    `location_id` BIGINT UNSIGNED NOT NULL COMMENT '校园地点',
    `sequence_no` INT UNSIGNED NOT NULL COMMENT '路线内顺序',
    `stop_type` VARCHAR(16) NOT NULL COMMENT '停靠点类型',
    `stop_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '停靠点状态',
    `estimated_arrival_time` DATETIME(3) NULL COMMENT '预计到达时间',
    `actual_arrival_time` DATETIME(3) NULL COMMENT '实际到达时间',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_route_stops_sequence` (`delivery_route_id`, `sequence_no`),
    KEY `idx_route_stops_route_status_sequence`
        (`delivery_route_id`, `stop_status`, `sequence_no`),
    KEY `idx_route_stops_order_type` (`delivery_order_id`, `stop_type`),
    KEY `idx_route_stops_location_eta` (`location_id`, `estimated_arrival_time`),
    CONSTRAINT `fk_route_stops_route`
        FOREIGN KEY (`delivery_route_id`) REFERENCES `delivery_routes` (`id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_route_stops_order`
        FOREIGN KEY (`delivery_order_id`) REFERENCES `delivery_orders` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_route_stops_location`
        FOREIGN KEY (`location_id`) REFERENCES `campus_locations` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `ck_route_stops_sequence` CHECK (`sequence_no` > 0),
    CONSTRAINT `ck_route_stops_type` CHECK (`stop_type` IN ('CURRENT', 'PICKUP', 'DROPOFF')),
    CONSTRAINT `ck_route_stops_order_requirement`
        CHECK (
            (`stop_type` = 'CURRENT' AND `delivery_order_id` IS NULL)
            OR
            (`stop_type` IN ('PICKUP', 'DROPOFF') AND `delivery_order_id` IS NOT NULL)
        ),
    CONSTRAINT `ck_route_stops_status`
        CHECK (`stop_status` IN ('PENDING', 'ARRIVED', 'COMPLETED', 'SKIPPED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='综合配送路线停靠点；相邻节点允许使用相同location_id';

-- 兼容已执行过早期阶段2脚本的数据库；放宽CURRENT节点的订单外键可空性。
ALTER TABLE `route_stops`
    MODIFY COLUMN `delivery_order_id` BIGINT UNSIGNED NULL
        COMMENT '关联配送订单，CURRENT节点为空';

-- 8. 包裹异常报告。
CREATE TABLE IF NOT EXISTS `parcel_exception_reports` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '异常报告编号',
    `parcel_id` BIGINT UNSIGNED NOT NULL COMMENT '异常包裹',
    `delivery_order_id` BIGINT UNSIGNED NOT NULL COMMENT '关联配送订单',
    `courier_id` BIGINT UNSIGNED NOT NULL COMMENT '上报配送员',
    `exception_type` VARCHAR(32) NOT NULL COMMENT '异常类型',
    `description` VARCHAR(500) NULL COMMENT '异常说明',
    `requires_admin` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否需要管理员处理',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
    `reported_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '上报时间',
    `handled_by_user_id` BIGINT UNSIGNED NULL COMMENT '管理员用户编号',
    `handled_time` DATETIME(3) NULL COMMENT '处理时间',
    `resolution_type` VARCHAR(32) NULL COMMENT '处理结果类型',
    `resolution_note` VARCHAR(500) NULL COMMENT '处理说明',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_exception_reports_status_time` (`status`, `reported_time`),
    KEY `idx_exception_reports_parcel_time` (`parcel_id`, `reported_time`),
    KEY `idx_exception_reports_order` (`delivery_order_id`),
    KEY `idx_exception_reports_courier_time` (`courier_id`, `reported_time`),
    KEY `idx_exception_reports_handler_time` (`handled_by_user_id`, `handled_time`),
    CONSTRAINT `fk_exception_reports_parcel`
        FOREIGN KEY (`parcel_id`) REFERENCES `parcels` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_exception_reports_order`
        FOREIGN KEY (`delivery_order_id`) REFERENCES `delivery_orders` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_exception_reports_courier`
        FOREIGN KEY (`courier_id`) REFERENCES `couriers` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_exception_reports_handler`
        FOREIGN KEY (`handled_by_user_id`) REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `ck_exception_reports_type`
        CHECK (`exception_type` IN (
            'PACKAGE_DAMAGED',
            'RECIPIENT_INFO_ERROR',
            'LOCKER_FAILURE',
            'USER_UNREACHABLE',
            'OTHER'
        )),
    CONSTRAINT `ck_exception_reports_requires_admin` CHECK (`requires_admin` IN (0, 1)),
    CONSTRAINT `ck_exception_reports_status`
        CHECK (`status` IN ('PENDING', 'RESOLVED', 'RETURNED')),
    CONSTRAINT `ck_exception_reports_handled`
        CHECK (
            (`status` = 'PENDING' AND `handled_by_user_id` IS NULL AND `handled_time` IS NULL)
            OR
            (`status` IN ('RESOLVED', 'RETURNED')
                AND `handled_by_user_id` IS NOT NULL
                AND `handled_time` IS NOT NULL)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='配送过程中的包裹异常与管理员处理记录';

-- 9. 路线图地点校正与能力回填。
UPDATE `campus_locations`
SET `location_name` = '美英澳公寓楼',
    `address_detail` = '美英澳公寓楼'
WHERE `location_code` = 'CAMIGO-DORM-07'
  AND `location_name` = '七号学生公寓';

UPDATE `campus_locations`
SET `location_type` = 'STATION'
WHERE `location_code` = 'CAMIGO-TEACH-01'
  AND `location_name` = '六区下快递服务站';

UPDATE `campus_locations` location
SET location.`pickup_enabled` = CASE
        WHEN location.`location_code` IN ('CAMIGO-GATE-E', 'CAMIGO-TEACH-01') THEN 1
        ELSE 0
    END,
    location.`delivery_enabled` = CASE
        WHEN location.`location_code` NOT IN ('CAMIGO-GATE-E', 'CAMIGO-TEACH-01')
         AND EXISTS (
             SELECT 1
             FROM `lockers` locker
             WHERE locker.`location_id` = location.`id`
               AND locker.`operation_status` = 'ONLINE'
               AND EXISTS (
                   SELECT 1
                   FROM `locker_slots` slot
                   WHERE slot.`locker_id` = locker.`id`
               )
         ) THEN 1
        ELSE 0
    END;

-- 10. 由路线图-v16初始化14条双向道路。
CREATE TEMPORARY TABLE `_camigo_stage2_route_seed` (
    `source_code` VARCHAR(32) NOT NULL,
    `target_code` VARCHAR(32) NOT NULL,
    `distance_meters` INT UNSIGNED NOT NULL,
    PRIMARY KEY (`source_code`, `target_code`)
) ENGINE=InnoDB;

INSERT INTO `_camigo_stage2_route_seed` (`source_code`, `target_code`, `distance_meters`)
VALUES
    ('CAMIGO-LIBRARY', 'CAMIGO-DORM-08', 150),
    ('CAMIGO-LIBRARY', 'CAMIGO-DORM-07', 200),
    ('CAMIGO-DORM-08', 'CAMIGO-GATE-W', 100),
    ('CAMIGO-GATE-W', 'CAMIGO-TEACH-01', 180),
    ('CAMIGO-TEACH-01', 'CAMIGO-DORM-01', 190),
    ('CAMIGO-TEACH-01', 'CAMIGO-GATE-E', 260),
    ('CAMIGO-DORM-07', 'CAMIGO-DORM-06', 30),
    ('CAMIGO-DORM-06', 'CAMIGO-DORM-05', 65),
    ('CAMIGO-DORM-05', 'CAMIGO-DORM-02', 75),
    ('CAMIGO-DORM-02', 'CAMIGO-DORM-01', 30),
    ('CAMIGO-DORM-02', 'CAMIGO-DORM-04', 160),
    ('CAMIGO-DORM-04', 'CAMIGO-DORM-03', 30),
    ('CAMIGO-DORM-03', 'CAMIGO-GATE-E', 130),
    ('CAMIGO-DORM-01', 'CAMIGO-GATE-E', 90);

UPDATE `campus_route_edges` edge
JOIN `campus_locations` source_location
  ON source_location.`id` = edge.`from_location_id`
JOIN `campus_locations` target_location
  ON target_location.`id` = edge.`to_location_id`
JOIN `_camigo_stage2_route_seed` seed
  ON seed.`source_code` = source_location.`location_code`
 AND seed.`target_code` = target_location.`location_code`
SET edge.`distance_meters` = seed.`distance_meters`,
    edge.`bidirectional` = 1,
    edge.`status` = 'ACTIVE';

INSERT INTO `campus_route_edges` (
    `from_location_id`,
    `to_location_id`,
    `distance_meters`,
    `bidirectional`,
    `status`
)
SELECT source_location.`id`,
       target_location.`id`,
       seed.`distance_meters`,
       1,
       'ACTIVE'
FROM `_camigo_stage2_route_seed` seed
JOIN `campus_locations` source_location
  ON source_location.`location_code` = seed.`source_code`
JOIN `campus_locations` target_location
  ON target_location.`location_code` = seed.`target_code`
WHERE NOT EXISTS (
    SELECT 1
    FROM `campus_route_edges` existing_edge
    WHERE existing_edge.`from_location_id` = source_location.`id`
      AND existing_edge.`to_location_id` = target_location.`id`
);

DROP TEMPORARY TABLE `_camigo_stage2_route_seed`;

-- 11. 历史订单取件站：优先沿用旧路线起点，否则使用食堂北服务站。
UPDATE `delivery_orders` delivery_order
LEFT JOIN (
    SELECT route.`delivery_order_id`, MIN(route.`start_location_id`) pickup_location_id
    FROM `delivery_routes` route
    JOIN `campus_locations` location
      ON location.`id` = route.`start_location_id`
     AND location.`pickup_enabled` = 1
    GROUP BY route.`delivery_order_id`
) legacy_pickup
  ON legacy_pickup.`delivery_order_id` = delivery_order.`id`
JOIN `campus_locations` fallback_pickup
  ON fallback_pickup.`location_code` = 'CAMIGO-GATE-E'
SET delivery_order.`pickup_location_id` = COALESCE(
    delivery_order.`pickup_location_id`,
    legacy_pickup.`pickup_location_id`,
    fallback_pickup.`id`
)
WHERE delivery_order.`pickup_location_id` IS NULL;

-- 12. 配送员位置：最近一次已完成路线终点优先，否则使用有效取件站。
UPDATE `couriers` courier
LEFT JOIN (
    SELECT ranked.`courier_id`, ranked.`end_location_id`, ranked.`end_time`
    FROM (
        SELECT route.`courier_id`,
               route.`end_location_id`,
               route.`end_time`,
               ROW_NUMBER() OVER (
                   PARTITION BY route.`courier_id`
                   ORDER BY route.`end_time` DESC, route.`id` DESC
               ) row_no
        FROM `delivery_routes` route
        WHERE route.`end_time` IS NOT NULL
    ) ranked
    WHERE ranked.`row_no` = 1
) latest_location
  ON latest_location.`courier_id` = courier.`id`
JOIN `campus_locations` fallback_location
  ON fallback_location.`location_code` = 'CAMIGO-GATE-E'
SET courier.`current_location_id` = COALESCE(
        courier.`current_location_id`,
        latest_location.`end_location_id`,
        fallback_location.`id`
    ),
    courier.`location_update_time` = COALESCE(
        courier.`location_update_time`,
        latest_location.`end_time`,
        CURRENT_TIMESTAMP(3)
    )
WHERE courier.`current_location_id` IS NULL
   OR courier.`location_update_time` IS NULL;

-- 固定配送员容量为8；兼容期暂不删除BREAK检查值。
UPDATE `couriers`
SET `max_active_orders` = 8
WHERE `max_active_orders` <> 8;

ALTER TABLE `couriers`
    MODIFY COLUMN `max_active_orders` INT UNSIGNED NOT NULL DEFAULT 8
        COMMENT '最大活动订单数，固定为8';

-- 13. 把旧的一单一路线冻结为兼容历史版本，不创建伪活动综合路线。
UPDATE `delivery_routes` route
JOIN (
    SELECT ranked.`id`, ranked.`calculated_version`
    FROM (
        SELECT route_for_rank.`id`,
               ROW_NUMBER() OVER (
                   PARTITION BY route_for_rank.`courier_id`
                   ORDER BY route_for_rank.`create_time`, route_for_rank.`id`
               ) calculated_version
        FROM `delivery_routes` route_for_rank
    ) ranked
) version_seed
  ON version_seed.`id` = route.`id`
SET route.`version_no` = version_seed.`calculated_version`
WHERE route.`version_no` IS NULL;

UPDATE `delivery_routes` route
JOIN `delivery_orders` delivery_order
  ON delivery_order.`id` = route.`delivery_order_id`
SET route.`status` = CASE
        WHEN route.`end_time` IS NOT NULL
          OR delivery_order.`status` IN ('COMPLETED', 'CANCELLED', 'FAILED')
            THEN 'COMPLETED'
        ELSE 'SUPERSEDED'
    END,
    route.`total_distance_meters` = COALESCE(
        route.`total_distance_meters`,
        route.`distance_meters`
    ),
    route.`estimated_duration_seconds` = COALESCE(
        route.`estimated_duration_seconds`,
        route.`duration_seconds`
    ),
    route.`algorithm_version` = COALESCE(
        route.`algorithm_version`,
        'legacy-v1'
    )
WHERE route.`status` IS NULL
   OR route.`total_distance_meters` IS NULL
   OR route.`estimated_duration_seconds` IS NULL
   OR route.`algorithm_version` IS NULL;

-- 14. 新字段的索引与约束。所有外键列都有支持索引。
CALL `_camigo_stage2_add_index`(
    'campus_locations',
    'idx_locations_pickup_status',
    'INDEX `idx_locations_pickup_status` (`pickup_enabled`, `status`)'
);
CALL `_camigo_stage2_add_index`(
    'campus_locations',
    'idx_locations_delivery_status',
    'INDEX `idx_locations_delivery_status` (`delivery_enabled`, `status`)'
);
CALL `_camigo_stage2_add_index`(
    'couriers',
    'idx_couriers_current_location',
    'INDEX `idx_couriers_current_location` (`current_location_id`)'
);
CALL `_camigo_stage2_add_index`(
    'delivery_orders',
    'idx_orders_pickup_status',
    'INDEX `idx_orders_pickup_status` (`pickup_location_id`, `status`)'
);
CALL `_camigo_stage2_add_index`(
    'delivery_routes',
    'uk_routes_courier_version',
    'UNIQUE INDEX `uk_routes_courier_version` (`courier_id`, `version_no`)'
);
CALL `_camigo_stage2_add_index`(
    'delivery_routes',
    'uk_routes_active_courier',
    'UNIQUE INDEX `uk_routes_active_courier` (`active_courier_id`)'
);
CALL `_camigo_stage2_add_index`(
    'delivery_routes',
    'idx_routes_courier_status_version',
    'INDEX `idx_routes_courier_status_version` (`courier_id`, `status`, `version_no`)'
);
CALL `_camigo_stage2_add_index`(
    'delivery_assignments',
    'idx_assignments_offer_expiry',
    'INDEX `idx_assignments_offer_expiry` (`decision`, `offer_expire_time`)'
);
CALL `_camigo_stage2_add_index`(
    'delivery_assignments',
    'idx_assignments_rejection_window',
    'INDEX `idx_assignments_rejection_window` (`courier_id`, `decision`, `response_time`)'
);

CALL `_camigo_stage2_add_constraint`(
    'campus_locations',
    'ck_locations_pickup_enabled',
    'CHECK (`pickup_enabled` IN (0, 1))'
);
CALL `_camigo_stage2_add_constraint`(
    'campus_locations',
    'ck_locations_delivery_enabled',
    'CHECK (`delivery_enabled` IN (0, 1))'
);
CALL `_camigo_stage2_add_constraint`(
    'campus_locations',
    'ck_locations_capability_exclusive',
    'CHECK (NOT (`pickup_enabled` = 1 AND `delivery_enabled` = 1))'
);
CALL `_camigo_stage2_add_constraint`(
    'couriers',
    'fk_couriers_current_location',
    'FOREIGN KEY (`current_location_id`) REFERENCES `campus_locations` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT'
);
CALL `_camigo_stage2_add_constraint`(
    'couriers',
    'ck_couriers_capacity_fixed',
    'CHECK (`max_active_orders` = 8)'
);
CALL `_camigo_stage2_add_constraint`(
    'couriers',
    'ck_couriers_location_pair',
    'CHECK ((`current_location_id` IS NULL AND `location_update_time` IS NULL) OR (`current_location_id` IS NOT NULL AND `location_update_time` IS NOT NULL))'
);
CALL `_camigo_stage2_add_constraint`(
    'delivery_orders',
    'fk_orders_pickup_location',
    'FOREIGN KEY (`pickup_location_id`) REFERENCES `campus_locations` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT'
);
CALL `_camigo_stage2_add_constraint`(
    'delivery_routes',
    'ck_routes_version_no',
    'CHECK (`version_no` IS NULL OR `version_no` > 0)'
);
CALL `_camigo_stage2_add_constraint`(
    'delivery_routes',
    'ck_routes_version_status',
    'CHECK (`status` IS NULL OR `status` IN (''DRAFT'', ''ACTIVE'', ''SUPERSEDED'', ''COMPLETED''))'
);
CALL `_camigo_stage2_add_constraint`(
    'delivery_routes',
    'ck_routes_total_distance',
    'CHECK (`total_distance_meters` IS NULL OR `total_distance_meters` >= 0)'
);
CALL `_camigo_stage2_add_constraint`(
    'delivery_routes',
    'ck_routes_estimated_duration',
    'CHECK (`estimated_duration_seconds` IS NULL OR `estimated_duration_seconds` >= 0)'
);
CALL `_camigo_stage2_add_constraint`(
    'route_stops',
    'ck_route_stops_order_requirement',
    'CHECK ((`stop_type` = ''CURRENT'' AND `delivery_order_id` IS NULL) OR (`stop_type` IN (''PICKUP'', ''DROPOFF'') AND `delivery_order_id` IS NOT NULL))'
);
CALL `_camigo_stage2_add_constraint`(
    'delivery_assignments',
    'ck_assignments_offer_window',
    'CHECK (`offer_expire_time` IS NULL OR (`offer_time` IS NOT NULL AND `offer_expire_time` >= `offer_time`))'
);
CALL `_camigo_stage2_add_constraint`(
    'delivery_assignments',
    'ck_assignments_response_time',
    'CHECK (`response_time` IS NULL OR (`offer_time` IS NOT NULL AND `response_time` >= `offer_time`))'
);

-- 15. 执行后只读摘要，便于人工和自动化验收。
SELECT 'campus_route_edges' object_name, COUNT(*) row_count
FROM `campus_route_edges`
UNION ALL
SELECT 'route_stops', COUNT(*) FROM `route_stops`
UNION ALL
SELECT 'parcel_exception_reports', COUNT(*) FROM `parcel_exception_reports`;

SELECT `pickup_enabled`, `delivery_enabled`, COUNT(*) location_count
FROM `campus_locations`
GROUP BY `pickup_enabled`, `delivery_enabled`
ORDER BY `pickup_enabled` DESC, `delivery_enabled` DESC;

SELECT COUNT(*) couriers_without_location
FROM `couriers`
WHERE `current_location_id` IS NULL
   OR `location_update_time` IS NULL;

SELECT COUNT(*) orders_without_pickup
FROM `delivery_orders`
WHERE `pickup_location_id` IS NULL;

DROP PROCEDURE IF EXISTS `_camigo_stage2_assert_locations`;
DROP PROCEDURE IF EXISTS `_camigo_stage2_add_constraint`;
DROP PROCEDURE IF EXISTS `_camigo_stage2_add_index`;
DROP PROCEDURE IF EXISTS `_camigo_stage2_add_column`;
DROP PROCEDURE IF EXISTS `_camigo_stage2_assert_database`;
