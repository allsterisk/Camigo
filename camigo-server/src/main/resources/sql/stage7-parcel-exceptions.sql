-- 阶段7：异常处理。保留阶段2已有表和历史数据，仅补充“每个包裹最多一个待处理异常”的并发约束。
DROP PROCEDURE IF EXISTS stage7_add_pending_exception_guard;

-- 将迁移前仍处于EXCEPTION且具备原配送上下文的演示数据纳入管理员待处理列表。
-- 不推测原异常原因，明确标记为遗留记录；重复执行不会重复插入。
INSERT INTO parcel_exception_reports (
    parcel_id, delivery_order_id, courier_id, exception_type, description,
    requires_admin, status, reported_time, create_time, update_time
)
SELECT p.id, d.id, d.assigned_courier_id, 'OTHER',
       '迁移前遗留异常（原始上报详情缺失）', 1, 'PENDING',
       COALESCE(p.update_time, p.create_time), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
FROM parcels p
JOIN delivery_orders d ON d.parcel_id = p.id
WHERE p.current_status = 'EXCEPTION'
  AND d.status = 'FAILED'
  AND d.assigned_courier_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM parcel_exception_reports r WHERE r.parcel_id = p.id
  );
DELIMITER $$
CREATE PROCEDURE stage7_add_pending_exception_guard()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'parcel_exception_reports'
          AND column_name = 'pending_parcel_id'
    ) THEN
        ALTER TABLE parcel_exception_reports
            ADD COLUMN pending_parcel_id BIGINT UNSIGNED
                GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN parcel_id ELSE NULL END) STORED
                COMMENT '待处理异常的包裹唯一键';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'parcel_exception_reports'
          AND index_name = 'uk_exception_pending_parcel'
    ) THEN
        ALTER TABLE parcel_exception_reports
            ADD UNIQUE KEY uk_exception_pending_parcel (pending_parcel_id);
    END IF;
END$$
DELIMITER ;
CALL stage7_add_pending_exception_guard();
DROP PROCEDURE IF EXISTS stage7_add_pending_exception_guard;
