-- Camigo 阶段4：智能派单状态、轮次和并发约束。
-- 只增加/调整字段、约束与索引，不删除历史业务记录。

USE `camigo`;

DROP PROCEDURE IF EXISTS `_camigo_stage4_add_column`;
DROP PROCEDURE IF EXISTS `_camigo_stage4_add_index`;
DROP PROCEDURE IF EXISTS `_camigo_stage4_add_constraint`;
DROP PROCEDURE IF EXISTS `_camigo_stage4_drop_constraint`;

DELIMITER $$

CREATE PROCEDURE `_camigo_stage4_add_column`(
    IN p_table_name VARCHAR(64), IN p_column_name VARCHAR(64), IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_definition);
        PREPARE statement_to_run FROM @ddl;
        EXECUTE statement_to_run;
        DEALLOCATE PREPARE statement_to_run;
    END IF;
END$$

CREATE PROCEDURE `_camigo_stage4_add_index`(
    IN p_table_name VARCHAR(64), IN p_index_name VARCHAR(64), IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD ', p_definition);
        PREPARE statement_to_run FROM @ddl;
        EXECUTE statement_to_run;
        DEALLOCATE PREPARE statement_to_run;
    END IF;
END$$

CREATE PROCEDURE `_camigo_stage4_add_constraint`(
    IN p_table_name VARCHAR(64), IN p_constraint_name VARCHAR(64), IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = p_table_name
          AND constraint_name = p_constraint_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD CONSTRAINT `',
                          p_constraint_name, '` ', p_definition);
        PREPARE statement_to_run FROM @ddl;
        EXECUTE statement_to_run;
        DEALLOCATE PREPARE statement_to_run;
    END IF;
END$$

CREATE PROCEDURE `_camigo_stage4_drop_constraint`(
    IN p_table_name VARCHAR(64), IN p_constraint_name VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = p_table_name
          AND constraint_name = p_constraint_name AND constraint_type = 'CHECK'
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` DROP CHECK `',
                          p_constraint_name, '`');
        PREPARE statement_to_run FROM @ddl;
        EXECUTE statement_to_run;
        DEALLOCATE PREPARE statement_to_run;
    END IF;
END$$

DELIMITER ;

CALL `_camigo_stage4_drop_constraint`('delivery_assignments', 'ck_assignments_decision');
CALL `_camigo_stage4_drop_constraint`('couriers', 'ck_couriers_availability');

-- 旧SELECTED记录保留原有审计含义，统一映射到最终状态ACCEPTED。
UPDATE `delivery_assignments`
SET `decision` = 'ACCEPTED',
    `decision_reason` = COALESCE(`decision_reason`, '阶段4迁移：旧SELECTED映射为ACCEPTED')
WHERE `decision` = 'SELECTED';

CALL `_camigo_stage4_add_column`(
    'delivery_assignments', 'route_increment_meters',
    '`route_increment_meters` INT UNSIGNED NULL COMMENT ''评分时的路线新增距离（米）'' AFTER `algorithm_version`'
);
CALL `_camigo_stage4_add_column`(
    'delivery_assignments', 'active_order_count',
    '`active_order_count` INT UNSIGNED NULL COMMENT ''评分时的活动订单数快照'' AFTER `route_increment_meters`'
);
CALL `_camigo_stage4_add_column`(
    'delivery_assignments', 'accepted_order_id',
    '`accepted_order_id` BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN `decision` = ''ACCEPTED'' THEN `delivery_order_id` ELSE NULL END) STORED COMMENT ''保证每个订单最多一个已接受配送员'' AFTER `selected_order_id`'
);
CALL `_camigo_stage4_add_column`(
    'delivery_assignments', 'offered_order_id',
    '`offered_order_id` BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN `decision` = ''OFFERED'' THEN `delivery_order_id` ELSE NULL END) STORED COMMENT ''保证每个订单同时最多一个未处理邀请'' AFTER `accepted_order_id`'
);

CALL `_camigo_stage4_add_column`(
    'delivery_orders', 'dispatch_round',
    '`dispatch_round` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''当前智能派单轮次'' AFTER `completed_time`'
);
CALL `_camigo_stage4_add_column`(
    'delivery_orders', 'next_dispatch_time',
    '`next_dispatch_time` DATETIME(3) NULL COMMENT ''保留的下一轮派单时间（当前由管理员人工启动）'' AFTER `dispatch_round`'
);
CALL `_camigo_stage4_add_column`(
    'delivery_orders', 'manual_dispatch_required',
    '`manual_dispatch_required` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''是否需要管理员人工处理'' AFTER `next_dispatch_time`'
);

UPDATE `delivery_orders` delivery_order
LEFT JOIN (
    SELECT `delivery_order_id`, MAX(`assignment_round`) max_round
    FROM `delivery_assignments`
    GROUP BY `delivery_order_id`
) assignment_summary
  ON assignment_summary.`delivery_order_id` = delivery_order.`id`
SET delivery_order.`dispatch_round` = GREATEST(
        delivery_order.`dispatch_round`, COALESCE(assignment_summary.`max_round`, 0)
    );

-- 根据最终规则归一化状态：非在职/原下线保持OFFLINE；满8单BUSY；其余AVAILABLE。
UPDATE `couriers` courier
LEFT JOIN (
    SELECT `assigned_courier_id`, COUNT(*) active_count
    FROM `delivery_orders`
    WHERE `assigned_courier_id` IS NOT NULL
      AND `status` IN ('ACCEPTED', 'PICKING_UP', 'IN_TRANSIT', 'ARRIVED')
    GROUP BY `assigned_courier_id`
) active_orders
  ON active_orders.`assigned_courier_id` = courier.`id`
SET courier.`availability_status` = CASE
    WHEN courier.`employment_status` <> 'ACTIVE' THEN 'OFFLINE'
    WHEN courier.`availability_status` = 'OFFLINE' THEN 'OFFLINE'
    WHEN COALESCE(active_orders.`active_count`, 0) >= 8 THEN 'BUSY'
    ELSE 'AVAILABLE'
END;

CALL `_camigo_stage4_add_index`(
    'delivery_assignments', 'uk_assignments_accepted_order',
    'UNIQUE INDEX `uk_assignments_accepted_order` (`accepted_order_id`)'
);
CALL `_camigo_stage4_add_index`(
    'delivery_assignments', 'uk_assignments_offered_order',
    'UNIQUE INDEX `uk_assignments_offered_order` (`offered_order_id`)'
);
CALL `_camigo_stage4_add_index`(
    'delivery_assignments', 'idx_assignments_order_courier_offer',
    'INDEX `idx_assignments_order_courier_offer` (`delivery_order_id`, `courier_id`, `decision`, `offer_expire_time`)'
);
CALL `_camigo_stage4_add_index`(
    'delivery_orders', 'idx_orders_dispatch_due',
    'INDEX `idx_orders_dispatch_due` (`status`, `manual_dispatch_required`, `next_dispatch_time`)'
);

CALL `_camigo_stage4_add_constraint`(
    'delivery_assignments', 'ck_assignments_decision',
    'CHECK (`decision` IN (''CANDIDATE'', ''OFFERED'', ''ACCEPTED'', ''REJECTED'', ''EXPIRED'', ''SKIPPED''))'
);
CALL `_camigo_stage4_add_constraint`(
    'delivery_assignments', 'ck_assignments_route_increment',
    'CHECK (`route_increment_meters` IS NULL OR `route_increment_meters` >= 0)'
);
CALL `_camigo_stage4_add_constraint`(
    'delivery_assignments', 'ck_assignments_active_count',
    'CHECK (`active_order_count` IS NULL OR `active_order_count` BETWEEN 0 AND 8)'
);
CALL `_camigo_stage4_add_constraint`(
    'delivery_orders', 'ck_orders_dispatch_round',
    'CHECK (`dispatch_round` >= 0)'
);
CALL `_camigo_stage4_add_constraint`(
    'delivery_orders', 'ck_orders_manual_dispatch',
    'CHECK (`manual_dispatch_required` IN (0, 1))'
);
CALL `_camigo_stage4_add_constraint`(
    'couriers', 'ck_couriers_availability',
    'CHECK (`availability_status` IN (''OFFLINE'', ''AVAILABLE'', ''BUSY''))'
);

DROP PROCEDURE IF EXISTS `_camigo_stage4_add_column`;
DROP PROCEDURE IF EXISTS `_camigo_stage4_add_index`;
DROP PROCEDURE IF EXISTS `_camigo_stage4_add_constraint`;
DROP PROCEDURE IF EXISTS `_camigo_stage4_drop_constraint`;

SELECT `decision`, COUNT(*) row_count
FROM `delivery_assignments`
GROUP BY `decision`
ORDER BY `decision`;
