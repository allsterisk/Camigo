-- Camigo 阶段10：可重复联调数据重建
-- 唯一业务依据：修改意见_最终执行版.md 第13节与阶段10。
-- 执行前必须完成完整备份。本脚本保留地点、道路、快递柜、格口和角色等基础数据，
-- 按外键顺序清理开发业务数据，并重建 1 ADMIN + 3 COURIER + 5 USER + 40 parcels。

SET NAMES utf8mb4;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;

-- 1. 按外键依赖顺序清理业务数据，不修改自增编号。
DELETE FROM `evaluations`;
DELETE FROM `pickup_records`;
DELETE FROM `pickup_appointments`;
DELETE FROM `storage_records`;
DELETE FROM `route_points`;
DELETE FROM `route_stops`;
DELETE FROM `delivery_routes`;
DELETE FROM `parcel_exception_reports`;
DELETE FROM `delivery_assignments`;
DELETE FROM `parcel_status_logs`;
DELETE FROM `delivery_orders`;
DELETE FROM `parcels`;
DELETE FROM `user_behaviors`;
DELETE FROM `couriers`;
DELETE FROM `user_roles`;
DELETE FROM `users`;

-- 删除旧占用记录后释放对应格口；故障和停用格口保持原状。
UPDATE `locker_slots`
SET `status` = 'AVAILABLE'
WHERE `status` IN ('OCCUPIED', 'RESERVED');

-- 2. 创建九个确定账号。这里只保存 BCrypt 摘要，明文初始密码记录在阶段10报告中。
INSERT INTO `users`
    (`username`, `password_hash`, `real_name`, `phone`, `student_no`, `status`)
VALUES
    ('camigo_admin',     '$2a$10$EhXlgSSYLqkdIJuxxyZOn.DKOwobyOrgdWIlkTgnqEHe2Qx0UPEuy', '联调管理员',   '13910000001', NULL,       'ACTIVE'),
    ('camigo_courier01', '$2a$10$QAi2OIT3xLU2WQsRnuGgQe/eQIlcPFFuTv1Y08RZ9BHj0QEsImUoC', '联调配送员一', '13910000011', NULL,       'ACTIVE'),
    ('camigo_courier02', '$2a$10$0reLKp/WpfqtbU.jnxbIs.ohrJkW/.Ztl.hm/jVwf5yTXiatv/kGG', '联调配送员二', '13910000012', NULL,       'ACTIVE'),
    ('camigo_courier03', '$2a$10$zDEB5tE.jqXKJcc6Cx/uRuoY7/M7xvfeDU3GT0AEvZNKS7aWtGmzK', '联调配送员三', '13910000013', NULL,       'ACTIVE'),
    ('camigo_student01', '$2a$10$15YsCSBOWn7lhLTbUil3meEcHl3KP94HG/NUPZuMNVCpBHByObF.m', '联调学生一',   '13910000101', '20261001', 'ACTIVE'),
    ('camigo_student02', '$2a$10$Cj6XYtsqS2j2OW2NytG/HejcMMZf8LsICts43o1Kt9wDs2dn3wgaW', '联调学生二',   '13910000102', '20261002', 'ACTIVE'),
    ('camigo_student03', '$2a$10$r8AkLAAkjnhkQYiR4VmP7uw8u7Rr8xMkiRdYysl3QsOqAeWoAeQgW', '联调学生三',   '13910000103', '20261003', 'ACTIVE'),
    ('camigo_student04', '$2a$10$Gk92Gv.rkJQhLfewJxTIv.QoIMZqnkr9mRW2vCCpLQaYuYjIOngJ.', '联调学生四',   '13910000104', '20261004', 'ACTIVE'),
    ('camigo_student05', '$2a$10$v6FaBblE2Ijql86JC8daXuUGXNgdxMj3g5UbHXaVkKAN2BUazfBYG', '联调学生五',   '13910000105', '20261005', 'ACTIVE');

SET @stage10_admin_id = (SELECT `id` FROM `users` WHERE `username` = 'camigo_admin');

INSERT INTO `user_roles` (`user_id`, `role_id`, `granted_by_user_id`)
SELECT user_account.`id`, role_record.`id`, @stage10_admin_id
FROM `users` user_account
JOIN `roles` role_record
  ON role_record.`role_code` = CASE
      WHEN user_account.`username` = 'camigo_admin' THEN 'ADMIN'
      WHEN user_account.`username` LIKE 'camigo_courier%' THEN 'COURIER'
      ELSE 'USER'
  END
WHERE user_account.`username` LIKE 'camigo\_%';

INSERT INTO `couriers`
    (`user_id`, `courier_no`, `employment_status`, `availability_status`,
     `max_active_orders`, `rating_avg`, `current_location_id`, `location_update_time`)
SELECT user_account.`id`, courier_seed.`courier_no`, 'ACTIVE', 'AVAILABLE', 8,
       courier_seed.`rating_avg`, location_record.`id`, NOW(3)
FROM (
    SELECT 'camigo_courier01' AS `username`, 'CAMIGO-C001' AS `courier_no`,
           4.80 AS `rating_avg`, 'CAMIGO-GATE-E' AS `location_code`
    UNION ALL
    SELECT 'camigo_courier02', 'CAMIGO-C002', 4.60, 'CAMIGO-TEACH-01'
    UNION ALL
    SELECT 'camigo_courier03', 'CAMIGO-C003', 4.40, 'CAMIGO-LIBRARY'
) courier_seed
JOIN `users` user_account ON user_account.`username` = courier_seed.`username`
JOIN `campus_locations` location_record
  ON location_record.`location_code` = courier_seed.`location_code`;

-- 3. 建立 40 行确定场景种子。每名学生依次覆盖八种业务场景：
--    1待分配、2等待确认、3待取件、4配送中、5已入柜、6已取件、7异常、8退回。
DROP TEMPORARY TABLE IF EXISTS `_stage10_seed`;
CREATE TEMPORARY TABLE `_stage10_seed` (
    `seed_no` int unsigned NOT NULL,
    `student_index` int unsigned NOT NULL,
    `scenario_index` int unsigned NOT NULL,
    `recipient_user_id` bigint unsigned NOT NULL,
    `pickup_location_id` bigint unsigned NOT NULL,
    `destination_location_id` bigint unsigned NOT NULL,
    `target_locker_id` bigint unsigned NOT NULL,
    `size_class` varchar(10) NOT NULL,
    `parcel_status` varchar(24) NOT NULL,
    `assigned_rank` int unsigned NOT NULL,
    `parcel_id` bigint unsigned DEFAULT NULL,
    `delivery_order_id` bigint unsigned DEFAULT NULL,
    PRIMARY KEY (`seed_no`)
) ENGINE=InnoDB;

INSERT INTO `_stage10_seed`
    (`seed_no`, `student_index`, `scenario_index`, `recipient_user_id`,
     `pickup_location_id`, `destination_location_id`, `target_locker_id`,
     `size_class`, `parcel_status`, `assigned_rank`)
WITH RECURSIVE number_seed (`seed_no`) AS (
    SELECT 1
    UNION ALL
    SELECT `seed_no` + 1 FROM number_seed WHERE `seed_no` < 40
), normalized_seed AS (
    SELECT `seed_no`,
           FLOOR((`seed_no` - 1) / 8) + 1 AS `student_index`,
           MOD(`seed_no` - 1, 8) + 1 AS `scenario_index`
    FROM number_seed
)
SELECT normalized_seed.`seed_no`,
       normalized_seed.`student_index`,
       normalized_seed.`scenario_index`,
       student.`id`,
       pickup_location.`id`,
       destination.`id`,
       locker_record.`id`,
       CASE MOD(normalized_seed.`seed_no` - 1, 3)
           WHEN 0 THEN 'S' WHEN 1 THEN 'M' ELSE 'L'
       END,
       CASE normalized_seed.`scenario_index`
           WHEN 1 THEN 'WAITING_ASSIGNMENT'
           WHEN 2 THEN 'WAITING_ASSIGNMENT'
           WHEN 3 THEN 'DELIVERING'
           WHEN 4 THEN 'DELIVERING'
           WHEN 5 THEN 'READY_PICKUP'
           WHEN 6 THEN 'PICKED_UP'
           WHEN 7 THEN 'EXCEPTION'
           ELSE 'RETURNED'
       END,
       MOD(normalized_seed.`seed_no` - 1, 3) + 1
FROM normalized_seed
JOIN `users` student
  ON student.`username` = CONCAT('camigo_student', LPAD(normalized_seed.`student_index`, 2, '0'))
JOIN `campus_locations` destination
  ON destination.`location_code` = CONCAT('CAMIGO-DORM-0', normalized_seed.`student_index`)
JOIN `lockers` locker_record
  ON locker_record.`location_id` = destination.`id`
 AND locker_record.`operation_status` = 'ONLINE'
JOIN `campus_locations` pickup_location
  ON pickup_location.`location_code` = CASE
      WHEN MOD(normalized_seed.`seed_no`, 2) = 0 THEN 'CAMIGO-TEACH-01'
      ELSE 'CAMIGO-GATE-E'
  END;

INSERT INTO `parcels`
    (`tracking_no`, `carrier_code`, `recipient_user_id`, `sender_name`,
     `sender_phone_masked`, `destination_location_id`, `target_locker_id`,
     `size_class`, `weight_kg`, `current_status`, `arrive_time`, `create_time`)
SELECT CONCAT('STG10', LPAD(seed.`seed_no`, 10, '0')),
       ELT(MOD(seed.`seed_no` - 1, 4) + 1, 'SF', 'YTO', 'ZTO', 'JD'),
       seed.`recipient_user_id`,
       ELT(MOD(seed.`seed_no` - 1, 5) + 1, '王老师', '李同学', '张先生', '赵女士', '校内商店'),
       CONCAT('13', MOD(seed.`seed_no`, 10), '****', LPAD(1000 + seed.`seed_no`, 4, '0')),
       seed.`destination_location_id`, seed.`target_locker_id`, seed.`size_class`,
       ROUND(0.50 + seed.`seed_no` * 0.125, 3), seed.`parcel_status`,
       TIMESTAMPADD(DAY, CAST(seed.`scenario_index` AS signed) - 10, NOW(3)),
       TIMESTAMPADD(DAY, CAST(seed.`scenario_index` AS signed) - 10, NOW(3))
FROM `_stage10_seed` seed
ORDER BY seed.`seed_no`;

UPDATE `_stage10_seed` seed
JOIN `parcels` parcel_record
  ON parcel_record.`tracking_no` = CONCAT('STG10', LPAD(seed.`seed_no`, 10, '0'))
SET seed.`parcel_id` = parcel_record.`id`;

INSERT INTO `delivery_orders`
    (`order_no`, `parcel_id`, `pickup_location_id`, `destination_location_id`,
     `assigned_courier_id`, `priority`, `status`, `predicted_minutes`,
     `accepted_time`, `completed_time`, `dispatch_round`, `next_dispatch_time`,
     `manual_dispatch_required`, `create_time`)
SELECT CONCAT('STG10-DO-', LPAD(seed.`seed_no`, 4, '0')),
       seed.`parcel_id`, seed.`pickup_location_id`, seed.`destination_location_id`,
       CASE WHEN seed.`scenario_index` >= 3 THEN courier_record.`id` ELSE NULL END,
       'NORMAL',
       CASE seed.`scenario_index`
           WHEN 1 THEN 'CREATED'
           WHEN 2 THEN 'ASSIGNING'
           WHEN 3 THEN 'ACCEPTED'
           WHEN 4 THEN 'IN_TRANSIT'
           WHEN 5 THEN 'COMPLETED'
           WHEN 6 THEN 'COMPLETED'
           ELSE 'FAILED'
       END,
       NULL,
       CASE WHEN seed.`scenario_index` >= 3
            THEN TIMESTAMPADD(HOUR, 1, parcel_record.`arrive_time`) ELSE NULL END,
       CASE WHEN seed.`scenario_index` IN (5, 6)
            THEN TIMESTAMPADD(HOUR, 4, parcel_record.`arrive_time`) ELSE NULL END,
       CASE WHEN seed.`scenario_index` = 1 THEN 0 ELSE 1 END,
       NULL, 0, parcel_record.`create_time`
FROM `_stage10_seed` seed
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`
JOIN `couriers` courier_record
  ON courier_record.`courier_no` = CONCAT('CAMIGO-C00', seed.`assigned_rank`)
ORDER BY seed.`seed_no`;

UPDATE `_stage10_seed` seed
JOIN `delivery_orders` order_record
  ON order_record.`order_no` = CONCAT('STG10-DO-', LPAD(seed.`seed_no`, 4, '0'))
SET seed.`delivery_order_id` = order_record.`id`;

-- 4. 为每个订单保存三名候选人的完整评分快照和决策结果。
DROP TEMPORARY TABLE IF EXISTS `_stage10_candidates`;
CREATE TEMPORARY TABLE `_stage10_candidates` AS
SELECT seed.`seed_no`, seed.`scenario_index`, seed.`delivery_order_id`,
       courier_record.`id` AS `courier_id`,
       CAST(RIGHT(courier_record.`courier_no`, 1) AS unsigned) AS `courier_rank`,
       seed.`assigned_rank`,
       80 + seed.`seed_no` * 4 + CAST(RIGHT(courier_record.`courier_no`, 1) AS unsigned) * 25
           AS `route_increment_meters`,
       MOD(seed.`seed_no` + CAST(RIGHT(courier_record.`courier_no`, 1) AS unsigned), 4)
           AS `active_order_count`,
       courier_record.`rating_avg`
FROM `_stage10_seed` seed
CROSS JOIN `couriers` courier_record;

INSERT INTO `delivery_assignments`
    (`delivery_order_id`, `courier_id`, `assignment_round`, `algorithm_version`,
     `route_increment_meters`, `active_order_count`, `distance_score`,
     `workload_score`, `service_score`, `total_score`, `decision`,
     `decision_reason`, `offer_time`, `offer_expire_time`, `response_time`,
     `reject_reason_code`, `reject_reason_detail`, `decided_time`, `create_time`)
SELECT scored.`delivery_order_id`, scored.`courier_id`, 1, 'dispatch-score-v1',
       scored.`route_increment_meters`, scored.`active_order_count`,
       scored.`distance_score`, scored.`workload_score`, scored.`service_score`,
       ROUND(scored.`distance_score` * 0.40 + scored.`workload_score` * 0.35
             + scored.`service_score` * 0.25, 4),
       CASE
           WHEN scored.`scenario_index` = 1 THEN 'CANDIDATE'
           WHEN scored.`scenario_index` = 2 AND scored.`courier_rank` = scored.`assigned_rank`
               THEN 'OFFERED'
           WHEN scored.`scenario_index` = 2 THEN 'CANDIDATE'
           WHEN scored.`courier_rank` = scored.`assigned_rank` THEN 'ACCEPTED'
           ELSE 'SKIPPED'
       END,
       CASE
           WHEN scored.`scenario_index` = 1 THEN '候选评分已生成，等待系统发起派单'
           WHEN scored.`scenario_index` = 2 AND scored.`courier_rank` = scored.`assigned_rank`
               THEN '综合得分最高，等待配送员确认'
           WHEN scored.`scenario_index` = 2 THEN '等待当前候选人响应'
           WHEN scored.`courier_rank` = scored.`assigned_rank`
               THEN '综合得分最高且配送员在有效期内接受'
           ELSE '其他候选人已接受订单'
       END,
       CASE WHEN scored.`courier_rank` = scored.`assigned_rank`
                 AND scored.`scenario_index` >= 2
            THEN CASE WHEN scored.`scenario_index` = 2
                      THEN NOW(3)
                      ELSE TIMESTAMPADD(HOUR, 1, parcel_record.`arrive_time`)
                 END ELSE NULL END,
       CASE WHEN scored.`courier_rank` = scored.`assigned_rank`
                 AND scored.`scenario_index` >= 2
            THEN CASE WHEN scored.`scenario_index` = 2
                      THEN TIMESTAMPADD(MINUTE, 3, NOW(3))
                      ELSE TIMESTAMPADD(MINUTE, 3, TIMESTAMPADD(HOUR, 1, parcel_record.`arrive_time`))
                 END ELSE NULL END,
       CASE WHEN scored.`courier_rank` = scored.`assigned_rank`
                 AND scored.`scenario_index` >= 3
            THEN TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, 1, parcel_record.`arrive_time`))
            ELSE NULL END,
       NULL, NULL,
       CASE WHEN scored.`scenario_index` >= 3
            THEN TIMESTAMPADD(MINUTE, 1, TIMESTAMPADD(HOUR, 1, parcel_record.`arrive_time`))
            ELSE NULL END,
       parcel_record.`create_time`
FROM (
    SELECT candidate_record.*,
           ROUND(100 / (1 + candidate_record.`route_increment_meters` / 500), 4)
               AS `distance_score`,
           ROUND(100 * (8 - candidate_record.`active_order_count`) / 8, 4)
               AS `workload_score`,
           ROUND(COALESCE(candidate_record.`rating_avg`, 3.0) / 5 * 100, 4)
               AS `service_score`
    FROM `_stage10_candidates` candidate_record
) scored
JOIN `_stage10_seed` seed ON seed.`seed_no` = scored.`seed_no`
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`;

-- 5. 保存严格按领域状态机顺序生成的包裹状态日志。
INSERT INTO `parcel_status_logs`
    (`parcel_id`, `from_status`, `to_status`, `operator_user_id`,
     `source_type`, `occurred_time`, `remark`)
SELECT seed.`parcel_id`, NULL, 'REGISTERED', @stage10_admin_id, 'ADMIN',
       parcel_record.`arrive_time`, '阶段10联调包裹登记'
FROM `_stage10_seed` seed
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`;

INSERT INTO `parcel_status_logs`
    (`parcel_id`, `from_status`, `to_status`, `operator_user_id`,
     `source_type`, `occurred_time`, `remark`)
SELECT seed.`parcel_id`, 'REGISTERED', 'WAITING_ASSIGNMENT', @stage10_admin_id, 'ADMIN',
       TIMESTAMPADD(MINUTE, 5, parcel_record.`arrive_time`), '进入智能派单'
FROM `_stage10_seed` seed
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`;

INSERT INTO `parcel_status_logs`
    (`parcel_id`, `from_status`, `to_status`, `operator_user_id`,
     `source_type`, `occurred_time`, `remark`)
SELECT seed.`parcel_id`, 'WAITING_ASSIGNMENT', 'DELIVERING', courier_user.`id`, 'COURIER',
       TIMESTAMPADD(HOUR, 1, parcel_record.`arrive_time`), '配送员接受智能派单'
FROM `_stage10_seed` seed
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`
JOIN `couriers` courier_record
  ON courier_record.`courier_no` = CONCAT('CAMIGO-C00', seed.`assigned_rank`)
JOIN `users` courier_user ON courier_user.`id` = courier_record.`user_id`
WHERE seed.`scenario_index` >= 3;

INSERT INTO `parcel_status_logs`
    (`parcel_id`, `from_status`, `to_status`, `operator_user_id`,
     `source_type`, `occurred_time`, `remark`)
SELECT seed.`parcel_id`, 'DELIVERING', 'READY_PICKUP', courier_user.`id`, 'LOCKER',
       TIMESTAMPADD(HOUR, 3, parcel_record.`arrive_time`), '配送员完成入柜并生成取件码'
FROM `_stage10_seed` seed
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`
JOIN `couriers` courier_record
  ON courier_record.`courier_no` = CONCAT('CAMIGO-C00', seed.`assigned_rank`)
JOIN `users` courier_user ON courier_user.`id` = courier_record.`user_id`
WHERE seed.`scenario_index` IN (5, 6);

INSERT INTO `parcel_status_logs`
    (`parcel_id`, `from_status`, `to_status`, `operator_user_id`,
     `source_type`, `occurred_time`, `remark`)
SELECT seed.`parcel_id`, 'READY_PICKUP', 'PICKED_UP', seed.`recipient_user_id`, 'USER',
       TIMESTAMPADD(HOUR, 4, parcel_record.`arrive_time`), '学生使用四位取件码取件成功'
FROM `_stage10_seed` seed
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`
WHERE seed.`scenario_index` = 6;

INSERT INTO `parcel_status_logs`
    (`parcel_id`, `from_status`, `to_status`, `operator_user_id`,
     `source_type`, `occurred_time`, `remark`)
SELECT seed.`parcel_id`, 'DELIVERING', 'EXCEPTION', courier_user.`id`, 'COURIER',
       TIMESTAMPADD(HOUR, 2, parcel_record.`arrive_time`), '配送员上报异常，等待管理员处理'
FROM `_stage10_seed` seed
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`
JOIN `couriers` courier_record
  ON courier_record.`courier_no` = CONCAT('CAMIGO-C00', seed.`assigned_rank`)
JOIN `users` courier_user ON courier_user.`id` = courier_record.`user_id`
WHERE seed.`scenario_index` IN (7, 8);

INSERT INTO `parcel_status_logs`
    (`parcel_id`, `from_status`, `to_status`, `operator_user_id`,
     `source_type`, `occurred_time`, `remark`)
SELECT seed.`parcel_id`, 'EXCEPTION', 'RETURNED', @stage10_admin_id, 'ADMIN',
       TIMESTAMPADD(HOUR, 3, parcel_record.`arrive_time`), '管理员确认包裹已退回取件服务站'
FROM `_stage10_seed` seed
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`
WHERE seed.`scenario_index` = 8;

-- 6. 生成入柜、取件、异常及评价事实。
INSERT INTO `storage_records`
    (`parcel_id`, `locker_slot_id`, `stored_by_user_id`, `status`,
     `stored_time`, `released_time`, `pickup_code`, `pickup_code_hash`, `code_expire_time`)
SELECT seed.`parcel_id`,
       (
           SELECT slot_record.`id`
           FROM `locker_slots` slot_record
           WHERE slot_record.`locker_id` = seed.`target_locker_id`
             AND slot_record.`status` = 'AVAILABLE'
             AND CASE seed.`size_class`
                 WHEN 'S' THEN slot_record.`size_class` IN ('S', 'M', 'L', 'XL')
                 WHEN 'M' THEN slot_record.`size_class` IN ('M', 'L', 'XL')
                 WHEN 'L' THEN slot_record.`size_class` IN ('L', 'XL')
                 ELSE slot_record.`size_class` = 'XL'
             END
           ORDER BY FIELD(slot_record.`size_class`, 'S', 'M', 'L', 'XL'), slot_record.`id`
           LIMIT 1
       ),
       courier_user.`id`,
       CASE WHEN seed.`scenario_index` = 5 THEN 'STORED' ELSE 'PICKED_UP' END,
       TIMESTAMPADD(HOUR, 3, parcel_record.`arrive_time`),
       CASE WHEN seed.`scenario_index` = 6
            THEN TIMESTAMPADD(HOUR, 4, parcel_record.`arrive_time`) ELSE NULL END,
       CASE WHEN seed.`scenario_index` = 5
            THEN LPAD(6000 + seed.`student_index`, 4, '0') ELSE NULL END,
       NULL, NULL
FROM `_stage10_seed` seed
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`
JOIN `couriers` courier_record
  ON courier_record.`courier_no` = CONCAT('CAMIGO-C00', seed.`assigned_rank`)
JOIN `users` courier_user ON courier_user.`id` = courier_record.`user_id`
WHERE seed.`scenario_index` IN (5, 6);

UPDATE `locker_slots` slot_record
JOIN `storage_records` storage_record
  ON storage_record.`active_slot_id` = slot_record.`id`
SET slot_record.`status` = 'OCCUPIED';

INSERT INTO `pickup_records`
    (`storage_record_id`, `user_id`, `appointment_id`, `verification_type`,
     `result`, `pickup_time`, `failure_reason`)
SELECT storage_record.`id`, seed.`recipient_user_id`, NULL, 'PICKUP_CODE', 'SUCCESS',
       storage_record.`released_time`, NULL
FROM `_stage10_seed` seed
JOIN `storage_records` storage_record ON storage_record.`parcel_id` = seed.`parcel_id`
WHERE seed.`scenario_index` = 6;

INSERT INTO `parcel_exception_reports`
    (`parcel_id`, `delivery_order_id`, `courier_id`, `exception_type`,
     `description`, `requires_admin`, `status`, `reported_time`,
     `handled_by_user_id`, `handled_time`, `resolution_type`, `resolution_note`)
SELECT seed.`parcel_id`, seed.`delivery_order_id`, courier_record.`id`,
       CASE WHEN seed.`scenario_index` = 7 THEN 'LOCKER_FAILURE' ELSE 'PACKAGE_DAMAGED' END,
       CASE WHEN seed.`scenario_index` = 7
            THEN '目标柜机暂时不可用，等待管理员处理'
            ELSE '外包装破损，已实际退回原取件服务站' END,
       1,
       CASE WHEN seed.`scenario_index` = 7 THEN 'PENDING' ELSE 'RETURNED' END,
       TIMESTAMPADD(HOUR, 2, parcel_record.`arrive_time`),
       CASE WHEN seed.`scenario_index` = 8 THEN @stage10_admin_id ELSE NULL END,
       CASE WHEN seed.`scenario_index` = 8
            THEN TIMESTAMPADD(HOUR, 3, parcel_record.`arrive_time`) ELSE NULL END,
       CASE WHEN seed.`scenario_index` = 8 THEN 'RETURN_TO_STATION' ELSE NULL END,
       CASE WHEN seed.`scenario_index` = 8 THEN '确认已经退回原取件服务站' ELSE NULL END
FROM `_stage10_seed` seed
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`
JOIN `couriers` courier_record
  ON courier_record.`courier_no` = CONCAT('CAMIGO-C00', seed.`assigned_rank`)
WHERE seed.`scenario_index` IN (7, 8);

INSERT INTO `evaluations`
    (`delivery_order_id`, `user_id`, `courier_id`, `delivery_score`,
     `service_score`, `content`, `status`, `evaluated_time`)
SELECT seed.`delivery_order_id`, seed.`recipient_user_id`, courier_record.`id`,
       5, 5, '阶段10联调数据：配送及时，服务规范。', 'VISIBLE',
       TIMESTAMPADD(HOUR, 5, parcel_record.`arrive_time`)
FROM `_stage10_seed` seed
JOIN `parcels` parcel_record ON parcel_record.`id` = seed.`parcel_id`
JOIN `couriers` courier_record
  ON courier_record.`courier_no` = CONCAT('CAMIGO-C00', seed.`assigned_rank`)
WHERE seed.`scenario_index` = 6;

-- 7. 为每名配送员保存一条历史路线和一条当前活动路线。
INSERT INTO `delivery_routes`
    (`delivery_order_id`, `courier_id`, `version_no`, `status`,
     `total_distance_meters`, `estimated_duration_seconds`, `algorithm_version`,
     `route_type`, `start_location_id`, `end_location_id`, `distance_meters`,
     `duration_seconds`, `start_time`, `end_time`)
SELECT NULL, courier_record.`id`, 1, 'COMPLETED',
       COUNT(seed.`delivery_order_id`) * 350,
       COUNT(seed.`delivery_order_id`) * 300,
       'dijkstra-insertion-v1', NULL, NULL, NULL,
       COUNT(seed.`delivery_order_id`) * 350,
       COUNT(seed.`delivery_order_id`) * 300,
       TIMESTAMPADD(DAY, -8, NOW(3)), TIMESTAMPADD(DAY, -2, NOW(3))
FROM `couriers` courier_record
LEFT JOIN `_stage10_seed` seed
  ON seed.`assigned_rank` = CAST(RIGHT(courier_record.`courier_no`, 1) AS unsigned)
 AND seed.`scenario_index` BETWEEN 5 AND 8
GROUP BY courier_record.`id`;

INSERT INTO `delivery_routes`
    (`delivery_order_id`, `courier_id`, `version_no`, `status`,
     `total_distance_meters`, `estimated_duration_seconds`, `algorithm_version`,
     `route_type`, `start_location_id`, `end_location_id`, `distance_meters`,
     `duration_seconds`, `start_time`, `end_time`)
SELECT NULL, courier_record.`id`, 2, 'ACTIVE',
       COUNT(seed.`delivery_order_id`) * 400,
       COUNT(seed.`delivery_order_id`) * 350,
       'dijkstra-insertion-v1', NULL, NULL, NULL,
       COUNT(seed.`delivery_order_id`) * 400,
       COUNT(seed.`delivery_order_id`) * 350,
       NOW(3), NULL
FROM `couriers` courier_record
LEFT JOIN `_stage10_seed` seed
  ON seed.`assigned_rank` = CAST(RIGHT(courier_record.`courier_no`, 1) AS unsigned)
 AND seed.`scenario_index` IN (3, 4)
GROUP BY courier_record.`id`;

-- 每条路线的 CURRENT 节点独立保存。
INSERT INTO `route_stops`
    (`delivery_route_id`, `delivery_order_id`, `location_id`, `sequence_no`,
     `stop_type`, `stop_status`, `estimated_arrival_time`, `actual_arrival_time`)
SELECT route_record.`id`, NULL, courier_record.`current_location_id`, 1,
       'CURRENT', 'COMPLETED', route_record.`start_time`, route_record.`start_time`
FROM `delivery_routes` route_record
JOIN `couriers` courier_record ON courier_record.`id` = route_record.`courier_id`;

INSERT INTO `route_stops`
    (`delivery_route_id`, `delivery_order_id`, `location_id`, `sequence_no`,
     `stop_type`, `stop_status`, `estimated_arrival_time`, `actual_arrival_time`)
SELECT historical_tasks.`route_id`, historical_tasks.`delivery_order_id`,
       historical_tasks.`location_id`, historical_tasks.`sequence_no` + 1,
       historical_tasks.`stop_type`, historical_tasks.`stop_status`,
       TIMESTAMPADD(MINUTE, historical_tasks.`sequence_no` * 5, historical_tasks.`route_start`),
       CASE WHEN historical_tasks.`stop_status` = 'COMPLETED'
            THEN TIMESTAMPADD(MINUTE, historical_tasks.`sequence_no` * 5, historical_tasks.`route_start`)
            ELSE NULL END
FROM (
    SELECT route_record.`id` AS `route_id`, route_record.`start_time` AS `route_start`,
           seed.`delivery_order_id`,
           CASE task_kind.`type_order`
               WHEN 1 THEN seed.`pickup_location_id`
               ELSE seed.`destination_location_id`
           END AS `location_id`,
           task_kind.`stop_type`,
           CASE
               WHEN task_kind.`type_order` = 2 AND seed.`scenario_index` IN (7, 8)
                   THEN 'SKIPPED'
               ELSE 'COMPLETED'
           END AS `stop_status`,
           ROW_NUMBER() OVER (
               PARTITION BY route_record.`id`
               ORDER BY seed.`seed_no`, task_kind.`type_order`
           ) AS `sequence_no`
    FROM `delivery_routes` route_record
    JOIN `couriers` courier_record ON courier_record.`id` = route_record.`courier_id`
    JOIN `_stage10_seed` seed
      ON seed.`assigned_rank` = CAST(RIGHT(courier_record.`courier_no`, 1) AS unsigned)
     AND seed.`scenario_index` BETWEEN 5 AND 8
    CROSS JOIN (
        SELECT 1 AS `type_order`, 'PICKUP' AS `stop_type`
        UNION ALL SELECT 2, 'DROPOFF'
    ) task_kind
    WHERE route_record.`version_no` = 1
) historical_tasks;

INSERT INTO `route_stops`
    (`delivery_route_id`, `delivery_order_id`, `location_id`, `sequence_no`,
     `stop_type`, `stop_status`, `estimated_arrival_time`, `actual_arrival_time`)
SELECT active_tasks.`route_id`, active_tasks.`delivery_order_id`, active_tasks.`location_id`,
       active_tasks.`sequence_no` + 1, active_tasks.`stop_type`, active_tasks.`stop_status`,
       TIMESTAMPADD(MINUTE, active_tasks.`sequence_no` * 5, NOW(3)),
       CASE WHEN active_tasks.`stop_status` = 'COMPLETED'
            THEN TIMESTAMPADD(MINUTE, active_tasks.`sequence_no` * 5, NOW(3)) ELSE NULL END
FROM (
    SELECT route_record.`id` AS `route_id`, seed.`delivery_order_id`,
           CASE task_kind.`type_order`
               WHEN 1 THEN seed.`pickup_location_id`
               ELSE seed.`destination_location_id`
           END AS `location_id`,
           task_kind.`stop_type`,
           CASE
               WHEN task_kind.`type_order` = 1 AND seed.`scenario_index` = 4
                   THEN 'COMPLETED'
               ELSE 'PENDING'
           END AS `stop_status`,
           ROW_NUMBER() OVER (
               PARTITION BY route_record.`id`
               ORDER BY seed.`seed_no`, task_kind.`type_order`
           ) AS `sequence_no`
    FROM `delivery_routes` route_record
    JOIN `couriers` courier_record ON courier_record.`id` = route_record.`courier_id`
    JOIN `_stage10_seed` seed
      ON seed.`assigned_rank` = CAST(RIGHT(courier_record.`courier_no`, 1) AS unsigned)
     AND seed.`scenario_index` IN (3, 4)
    CROSS JOIN (
        SELECT 1 AS `type_order`, 'PICKUP' AS `stop_type`
        UNION ALL SELECT 2, 'DROPOFF'
    ) task_kind
    WHERE route_record.`version_no` = 2
) active_tasks;

-- 8. 在提交前用 CHECK 临时表强制验证核心验收条件；任一条件失败即整事务回滚。
DROP TEMPORARY TABLE IF EXISTS `_stage10_assertion`;
CREATE TEMPORARY TABLE `_stage10_assertion` (
    `assertion_name` varchar(100) NOT NULL,
    `passed` tinyint unsigned NOT NULL,
    CONSTRAINT `ck_stage10_assertion_passed` CHECK (`passed` = 1)
) ENGINE=InnoDB;

INSERT INTO `_stage10_assertion` VALUES
    ('总账号数为9', (SELECT COUNT(*) = 9 FROM `users`)),
    ('管理员账号数为1', (SELECT COUNT(*) = 1 FROM `users` user_account
        JOIN `user_roles` role_link ON role_link.`user_id` = user_account.`id`
        JOIN `roles` role_record ON role_record.`id` = role_link.`role_id`
        WHERE role_record.`role_code` = 'ADMIN')),
    ('配送员账号数为3', (SELECT COUNT(*) = 3 FROM `couriers`)),
    ('学生账号数为5', (SELECT COUNT(*) = 5 FROM `users` user_account
        JOIN `user_roles` role_link ON role_link.`user_id` = user_account.`id`
        JOIN `roles` role_record ON role_record.`id` = role_link.`role_id`
        WHERE role_record.`role_code` = 'USER')),
    ('所有配送员容量固定为8', (SELECT COUNT(*) = 0 FROM `couriers`
        WHERE `max_active_orders` <> 8 OR `current_location_id` IS NULL)),
    ('包裹总数为40', (SELECT COUNT(*) = 40 FROM `parcels`)),
    ('每名学生恰有8个包裹', (SELECT COUNT(*) = 5 FROM (
        SELECT `recipient_user_id` FROM `parcels`
        GROUP BY `recipient_user_id` HAVING COUNT(*) = 8
    ) parcel_group)),
    ('待分配和等待确认各5单', (SELECT
        SUM(`status` = 'CREATED') = 5 AND SUM(`status` = 'ASSIGNING') = 5
        FROM `delivery_orders`)),
    ('订单状态场景完整', (SELECT
        SUM(`status` = 'ACCEPTED') = 5
        AND SUM(`status` = 'IN_TRANSIT') = 5
        AND SUM(`status` = 'COMPLETED') = 10
        AND SUM(`status` = 'FAILED') = 10
        FROM `delivery_orders`)),
    ('派单审计每单3名候选', (SELECT COUNT(*) = 40 FROM (
        SELECT `delivery_order_id` FROM `delivery_assignments`
        GROUP BY `delivery_order_id` HAVING COUNT(*) = 3
    ) assignment_group)),
    ('每个已处理订单最多一个接受人', (SELECT COUNT(*) = 0 FROM (
        SELECT `delivery_order_id` FROM `delivery_assignments`
        WHERE `decision` = 'ACCEPTED'
        GROUP BY `delivery_order_id` HAVING COUNT(*) > 1
    ) accepted_group)),
    ('所有待确认邀请窗口均为3分钟', (SELECT COUNT(*) = 0
        FROM `delivery_assignments`
        WHERE `decision` = 'OFFERED'
          AND TIMESTAMPDIFF(SECOND, `offer_time`, `offer_expire_time`) <> 180)),
    ('入柜和取件数据各5条', (SELECT
        SUM(`status` = 'STORED' AND `released_time` IS NULL AND `pickup_code` IS NOT NULL) = 5
        AND SUM(`status` = 'PICKED_UP' AND `released_time` IS NOT NULL AND `pickup_code` IS NULL) = 5
        FROM `storage_records`)),
    ('成功取件记录为5条', (SELECT COUNT(*) = 5 FROM `pickup_records`
        WHERE `result` = 'SUCCESS' AND `verification_type` = 'PICKUP_CODE')),
    ('异常待处理和退回各5条', (SELECT
        SUM(`status` = 'PENDING') = 5 AND SUM(`status` = 'RETURNED') = 5
        FROM `parcel_exception_reports`)),
    ('每名配送员只有一条活动路线', (SELECT COUNT(*) = 3 FROM (
        SELECT `courier_id` FROM `delivery_routes`
        WHERE `status` = 'ACTIVE'
        GROUP BY `courier_id` HAVING COUNT(*) = 1
    ) active_route_group)),
    ('路线全部满足先取后送', (SELECT COUNT(*) = 0
        FROM `route_stops` dropoff_stop
        JOIN `route_stops` pickup_stop
          ON pickup_stop.`delivery_route_id` = dropoff_stop.`delivery_route_id`
         AND pickup_stop.`delivery_order_id` = dropoff_stop.`delivery_order_id`
         AND pickup_stop.`stop_type` = 'PICKUP'
        WHERE dropoff_stop.`stop_type` = 'DROPOFF'
          AND dropoff_stop.`sequence_no` <= pickup_stop.`sequence_no`)),
    ('存在CURRENT与同地点业务节点', (SELECT COUNT(*) > 0
        FROM `route_stops` current_stop
        JOIN `route_stops` business_stop
          ON business_stop.`delivery_route_id` = current_stop.`delivery_route_id`
         AND business_stop.`sequence_no` = current_stop.`sequence_no` + 1
         AND business_stop.`location_id` = current_stop.`location_id`
        WHERE current_stop.`stop_type` = 'CURRENT'
          AND business_stop.`stop_type` IN ('PICKUP', 'DROPOFF'))),
    ('预约和旧路线点均为空', (SELECT
        (SELECT COUNT(*) FROM `pickup_appointments`) = 0
        AND (SELECT COUNT(*) FROM `route_points`) = 0));

COMMIT;

DROP TEMPORARY TABLE IF EXISTS `_stage10_assertion`;
DROP TEMPORARY TABLE IF EXISTS `_stage10_candidates`;
DROP TEMPORARY TABLE IF EXISTS `_stage10_seed`;

-- 返回稳定的验收摘要，便于命令行和自动化留档。
SELECT 'users' AS `metric`, COUNT(*) AS `value` FROM `users`
UNION ALL SELECT 'couriers', COUNT(*) FROM `couriers`
UNION ALL SELECT 'parcels', COUNT(*) FROM `parcels`
UNION ALL SELECT 'delivery_orders', COUNT(*) FROM `delivery_orders`
UNION ALL SELECT 'delivery_assignments', COUNT(*) FROM `delivery_assignments`
UNION ALL SELECT 'delivery_routes', COUNT(*) FROM `delivery_routes`
UNION ALL SELECT 'route_stops', COUNT(*) FROM `route_stops`
UNION ALL SELECT 'storage_records', COUNT(*) FROM `storage_records`
UNION ALL SELECT 'pickup_records', COUNT(*) FROM `pickup_records`
UNION ALL SELECT 'parcel_exception_reports', COUNT(*) FROM `parcel_exception_reports`;
