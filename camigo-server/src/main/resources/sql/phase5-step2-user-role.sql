-- Phase 5 / Step 2：将普通用户角色编码从 STUDENT 统一为 USER。
-- user_roles 通过 role_id 关联，因此本迁移不会改变现有用户的角色关系。
-- 可重复执行：如果 USER 已存在，则不会再次修改。

START TRANSACTION;

UPDATE roles AS student_role
LEFT JOIN roles AS user_role ON user_role.role_code = 'USER'
SET student_role.role_code = 'USER',
    student_role.role_name = '普通用户',
    student_role.description = '普通用户（学生）'
WHERE student_role.role_code = 'STUDENT'
  AND user_role.id IS NULL;

COMMIT;
