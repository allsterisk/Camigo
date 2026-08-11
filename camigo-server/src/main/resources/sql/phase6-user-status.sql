-- Phase 6：合并重复的用户停用语义，仅保留 ACTIVE、DISABLED、CANCELLED。
-- 迁移策略：历史 LOCKED 账号统一转为 DISABLED；原 CANCELLED 账号保持不变。

START TRANSACTION;

UPDATE users
SET status = 'DISABLED'
WHERE status = 'LOCKED';

COMMIT;

ALTER TABLE users
    DROP CHECK ck_users_status;

ALTER TABLE users
    ADD CONSTRAINT ck_users_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'CANCELLED'));

SELECT status, COUNT(*) AS user_count
FROM users
GROUP BY status
ORDER BY status;
