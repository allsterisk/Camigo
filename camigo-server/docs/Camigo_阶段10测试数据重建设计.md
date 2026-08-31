# Camigo 阶段10测试数据重建设计

## 1. 目标与边界

阶段10只重建开发环境 Camigo 联调数据，保留角色、校园地点、道路、快递柜和格口等基础数据。最终数据固定为 1 名管理员、3 名配送员、5 名学生和 40 个包裹；不重置任何表的自增编号。

可重复初始化脚本位于：

```text
camigo-server/src/main/resources/sql/stage10-rebuild-test-data.sql
```

## 2. 清理策略

脚本在 SERIALIZABLE 事务中按外键依赖顺序删除评价、取件、预约、入柜、路线、异常、派单、状态日志、订单、包裹、用户行为、配送员、用户角色和用户数据。删除旧入柜记录后，只把 `OCCUPIED` 或 `RESERVED` 格口恢复为 `AVAILABLE`；故障和停用格口保持不变。

脚本任一 SQL 或验收断言失败时，客户端连接结束会回滚整个未提交事务。执行前备份可以完整恢复阶段9结束时的数据。

## 3. 账号结构

- 管理员：`camigo_admin`
- 配送员：`camigo_courier01`～`camigo_courier03`
- 学生：`camigo_student01`～`camigo_student05`

数据库只保存 BCrypt 摘要。明文初始密码不提交到 Git，记录在阶段10执行报告中并直接反馈给项目负责人。

## 4. 包裹场景矩阵

每名学生固定拥有 8 个包裹，各覆盖一种场景：

| 场景 | 包裹状态 | 配送订单状态 | 主要关联数据 |
|---|---|---|---|
| 待分配 | `WAITING_ASSIGNMENT` | `CREATED` | 三名候选评分 |
| 等待确认 | `WAITING_ASSIGNMENT` | `ASSIGNING` | 一条有效邀请、两条候选 |
| 待取件 | `DELIVERING` | `ACCEPTED` | 已接受派单、活动路线的 PICKUP/DROPOFF |
| 配送中 | `DELIVERING` | `IN_TRANSIT` | 已完成 PICKUP、待完成 DROPOFF |
| 已入柜 | `READY_PICKUP` | `COMPLETED` | 活动入柜记录、唯一四位取件码、占用格口 |
| 已取件 | `PICKED_UP` | `COMPLETED` | 已释放入柜记录、成功取件记录、评价 |
| 异常 | `EXCEPTION` | `FAILED` | 待管理员处理异常 |
| 退回 | `RETURNED` | `FAILED` | 已处理异常和管理员退回审计 |

因此订单状态分布为 `CREATED` 5、`ASSIGNING` 5、`ACCEPTED` 5、`IN_TRANSIT` 5、`COMPLETED` 10、`FAILED` 10。

## 5. 路线和同位置节点

每名配送员保存一个历史完成路线版本和一个活动路线版本。路线内始终先保存独立 `CURRENT` 节点，再保存订单的 `PICKUP`/`DROPOFF` 业务节点。初始化数据特意包含 `CURRENT.location_id = PICKUP.location_id` 的相邻节点，证明两类节点分别保存且业务节点仍需单独确认；先取后送顺序由提交前断言和集成测试共同验证。

## 6. 可重复性与验证

初始化脚本提交前使用带 CHECK 约束的临时断言表验证账号、角色、每学生包裹数、八类场景、候选唯一性、格口、取件、异常、活动路线、先取后送和同位置双节点。脚本已在独立临时数据库连续执行两次并得到相同结构。

`Stage10TestDataIntegrityIntegrationTest` 进一步只读验证：

- 当前包裹状态与最新状态日志一致；
- 包裹与配送订单状态组合合法；
- 每单三名候选且最多一名已接受配送员；
- 每名配送员最多一条活动路线，业务节点先取后送；
- 活动格口、四位取件码、取件事实和异常处理一致。

完整后端测试共 134 项；阶段10执行后全部通过。Vue 3 前端生产构建同时通过。
