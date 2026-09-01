# Camigo 阶段7：异常处理设计

## 1. 依据与范围

唯一业务依据为 `修改意见_最终执行版.md` 的“包裹异常处理”和“阶段7：异常处理”。阶段7覆盖配送员上报、管理员恢复配送、恢复服务站分拣、确认退回，以及包裹、订单、配送员位置/容量、综合路线和审计记录的事务一致性。

异常订单不能通过通用包裹或订单状态接口直接恢复、完成或取消；所有恢复和退回操作必须来自管理员异常处理接口。

## 2. 数据模型

阶段2已经创建 `parcel_exception_reports`，阶段7保留原表并增加一个生成列和唯一索引：

```text
parcels 1 ─── N parcel_exception_reports N ─── 1 delivery_orders
                         │
                         ├── N:1 couriers（上报配送员）
                         └── N:1 users（处理管理员，可空）
```

核心字段包括异常类型、说明、是否需要管理员、处理状态、上报时间、处理人、处理时间、处理类型和处理说明。`pending_parcel_id` 只在 `status=PENDING` 时等于包裹编号，否则为 `NULL`；唯一索引 `uk_exception_pending_parcel` 保证同一包裹最多一个待处理异常。

规范化决策：异常报告独立于包裹和订单保存，不把异常说明或处理历史覆盖到主表；包裹和订单只保留当前状态，完整审计由异常报告与包裹状态日志共同承担。

## 3. 查询与索引

| 访问模式 | 索引 | 说明 |
| --- | --- | --- |
| 管理员按状态分页并按上报时间倒序 | `idx_exception_reports_status_time(status, reported_time)` | 待处理记录优先展示 |
| 查询包裹异常历史 | `idx_exception_reports_parcel_time(parcel_id, reported_time)` | 支持包裹时间线 |
| 查询订单或配送员异常 | 既有订单、配送员索引 | 支持详情和审计关联 |
| 并发创建待处理异常 | `uk_exception_pending_parcel(pending_parcel_id)` | 数据库最终一致性保护 |

MySQL `EXPLAIN FORMAT=TREE` 验证待处理列表使用 `idx_exception_reports_status_time` 反向索引查找。

## 4. API

| 方法 | 路径 | 角色 | 作用 |
| --- | --- | --- | --- |
| `POST` | `/api/courier-execution/orders/{orderId}/exceptions` | `COURIER` | 上报本人配送任务异常 |
| `GET` | `/api/admin/exceptions` | `ADMIN` | 按状态和关键字分页查询异常记录 |
| `POST` | `/api/admin/exceptions/{id}/resolution` | `ADMIN` | 恢复配送、恢复分拣或确认退回 |

异常类型限定为包裹破损、收件信息错误、柜机故障、无法联系用户和其他。说明与处理说明必填，长度最多500字符。

## 5. 上报事务

配送员只能在 `PICKING_UP / IN_TRANSIT / ARRIVED` 上报本人订单，包裹必须为 `DELIVERING` 且配送员已确认位置。事务按订单、配送员、包裹顺序加锁，并执行：

1. 创建 `PENDING` 异常报告；
2. 包裹 `DELIVERING → EXCEPTION` 并写状态日志；
3. 订单进入 `FAILED`；
4. 当前路线中该订单未完成节点标记为 `SKIPPED`；
5. 按剩余活动订单更新配送员容量状态；
6. 以当前位置重算并保存新路线版本。

任何一步失败都会整体回滚。数据库唯一键阻止并发重复上报。

## 6. 管理员处理事务

- `RESUME_DELIVERY`：问题已解决且包裹仍由原配送员持有；包裹 `EXCEPTION → DELIVERING`，订单 `FAILED → IN_TRANSIT`，重新加入独立 `DROPOFF` 节点并重算路线。
- `RETURN_TO_SORTING`：确认包裹已回原取件服务站；包裹 `EXCEPTION → SORTING`，原订单保持结束，配送员位置更新到服务站并重算剩余路线。管理员可在快递管理中点击“开始配送”，系统保留失败订单并新建配送单进入智能派单。
- `CONFIRM_RETURNED`：确认已经实际退回服务站；包裹 `EXCEPTION → RETURNED`，订单保持结束，配送员位置更新到服务站并重算路线。

处理完成时同时保存管理员、处理时间、处理类型和说明。处理过的报告不能重复处理；原配送员达到容量上限时不能恢复配送。

## 7. 兼容迁移

迁移脚本可重复执行，不删除现有数据。迁移前数据库有5个 `EXCEPTION` 包裹但没有异常报告；它们被补充为 `OTHER / PENDING`，说明固定为“迁移前遗留异常（原始上报详情缺失）”，从而能够进入管理员处理列表，同时避免伪造未知异常原因。5个既有 `RETURNED` 包裹和原状态记录保持不变。

## 8. 前端与验证

配送任务表为可上报状态提供“上报异常”操作，表单包含类型、现场说明、错误和重复提交保护。管理员异常页包含筛选、空/错误/加载状态、完整记录和三种处理方式；长文本、窄屏与并发处理均有降级表现。

真实 MySQL 集成测试覆盖上报暂停与移除路线、恢复配送、恢复分拣、确认退回、审计字段以及禁止通用接口绕过；控制器和 OpenAPI 测试覆盖新接口。前端通过 TypeScript 与生产构建，界面规范检测无发现。
