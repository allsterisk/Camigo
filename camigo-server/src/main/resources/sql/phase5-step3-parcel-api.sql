-- Phase 5 / Step 3：快递目标柜关联与 COMPLETED 终态。
-- MySQL DDL 会自动提交；脚本可在当前 Phase 2 结构上重复执行。

SET @schema_name = DATABASE();

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'parcels'
      AND COLUMN_NAME = 'target_locker_id'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE parcels ADD COLUMN target_locker_id BIGINT UNSIGNED NULL COMMENT ''目标快递柜'' AFTER destination_location_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'parcels'
      AND INDEX_NAME = 'idx_parcels_target_locker_status'
);
SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE parcels ADD INDEX idx_parcels_target_locker_status (target_locker_id, current_status)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @schema_name
      AND TABLE_NAME = 'parcels'
      AND CONSTRAINT_NAME = 'fk_parcels_target_locker'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @ddl = IF(
    @fk_exists = 0,
    'ALTER TABLE parcels ADD CONSTRAINT fk_parcels_target_locker FOREIGN KEY (target_locker_id) REFERENCES lockers(id) ON DELETE RESTRICT ON UPDATE RESTRICT',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE parcels
    DROP CHECK ck_parcels_status,
    ADD CONSTRAINT ck_parcels_status CHECK (
        current_status IN (
            'REGISTERED', 'SORTING', 'WAITING_ASSIGNMENT', 'DELIVERING',
            'STORED', 'READY_PICKUP', 'PICKED_UP', 'COMPLETED', 'EXCEPTION', 'RETURNED'
        )
    );

ALTER TABLE parcel_status_logs
    DROP CHECK ck_parcel_logs_from_status,
    DROP CHECK ck_parcel_logs_to_status,
    ADD CONSTRAINT ck_parcel_logs_from_status CHECK (
        from_status IS NULL OR from_status IN (
            'REGISTERED', 'SORTING', 'WAITING_ASSIGNMENT', 'DELIVERING',
            'STORED', 'READY_PICKUP', 'PICKED_UP', 'COMPLETED', 'EXCEPTION', 'RETURNED'
        )
    ),
    ADD CONSTRAINT ck_parcel_logs_to_status CHECK (
        to_status IN (
            'REGISTERED', 'SORTING', 'WAITING_ASSIGNMENT', 'DELIVERING',
            'STORED', 'READY_PICKUP', 'PICKED_UP', 'COMPLETED', 'EXCEPTION', 'RETURNED'
        )
    );

CREATE OR REPLACE SQL SECURITY INVOKER VIEW v_daily_parcel_statistics AS
SELECT DATE(p.create_time) AS stat_date,
       p.carrier_code,
       COUNT(*) AS parcel_count,
       SUM(p.current_status IN ('PICKED_UP', 'COMPLETED')) AS picked_up_count,
       SUM(p.current_status = 'EXCEPTION') AS exception_count
FROM parcels p
GROUP BY DATE(p.create_time), p.carrier_code;
