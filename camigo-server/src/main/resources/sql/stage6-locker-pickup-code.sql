-- Camigo 阶段6：格口并发占用和后端生成四位唯一取件码。
-- 数据保护：保留 pickup_code_hash、pickup_appointments、appointment_id 和历史验证记录；
-- 新业务只写 pickup_code，历史结构待用户明确批准清理后再删除。

USE `camigo`;

DROP PROCEDURE IF EXISTS `_camigo_stage6_add_column`;
DROP PROCEDURE IF EXISTS `_camigo_stage6_add_index`;

DELIMITER $$

CREATE PROCEDURE `_camigo_stage6_add_column`(
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
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `',
                          p_column_name, '` ', p_definition);
        PREPARE statement_to_run FROM @ddl;
        EXECUTE statement_to_run;
        DEALLOCATE PREPARE statement_to_run;
    END IF;
END$$

CREATE PROCEDURE `_camigo_stage6_add_index`(
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

DELIMITER ;

CALL `_camigo_stage6_add_column`(
    'storage_records',
    'pickup_code',
    'CHAR(4) NULL COMMENT ''后端生成的活动四位取件码；取件或释放后置空'' AFTER `released_time`'
);

-- 旧摘要只供迁移前活动记录兼容校验；新记录不再写摘要。
ALTER TABLE `storage_records`
    MODIFY COLUMN `pickup_code_hash` VARCHAR(255) NULL
        COMMENT '历史取件码摘要；阶段6新记录为空',
    MODIFY COLUMN `code_expire_time` DATETIME(3) NULL
        COMMENT '历史取件码过期时间；阶段6新记录为空';

CALL `_camigo_stage6_add_index`(
    'storage_records',
    'uk_storage_active_pickup_code',
    'UNIQUE KEY `uk_storage_active_pickup_code` (`pickup_code`)'
);

CALL `_camigo_stage6_add_index`(
    'locker_slots',
    'idx_locker_slots_compatible',
    'INDEX `idx_locker_slots_compatible` (`locker_id`, `status`, `size_class`, `id`)'
);

DROP PROCEDURE IF EXISTS `_camigo_stage6_add_column`;
DROP PROCEDURE IF EXISTS `_camigo_stage6_add_index`;

-- 验收：非空活动码无重复、格口和包裹无重复活动占用。
SELECT COUNT(*) AS duplicate_pickup_code_groups
FROM (
    SELECT `pickup_code`
    FROM `storage_records`
    WHERE `pickup_code` IS NOT NULL
    GROUP BY `pickup_code`
    HAVING COUNT(*) > 1
) duplicate_codes;

SELECT COUNT(*) AS duplicate_active_slot_groups
FROM (
    SELECT `active_slot_id`
    FROM `storage_records`
    WHERE `active_slot_id` IS NOT NULL
    GROUP BY `active_slot_id`
    HAVING COUNT(*) > 1
) duplicate_slots;

SELECT COUNT(*) AS duplicate_active_parcel_groups
FROM (
    SELECT `active_parcel_id`
    FROM `storage_records`
    WHERE `active_parcel_id` IS NOT NULL
    GROUP BY `active_parcel_id`
    HAVING COUNT(*) > 1
) duplicate_parcels;
