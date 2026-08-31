# Camigo 阶段2数据库增量迁移设计

**服务：** Camigo 校园配送系统
**日期：** 2026-08-31
**数据库：** MySQL 8.0.39 / InnoDB / utf8mb4
**状态：** 已按《修改意见_最终执行版》执行并验证

## 1. 范围

阶段2采用“先增加、后切换、再删除”的兼容迁移方式：增加校园道路、路线版本、路线停靠点、异常记录以及后续派单需要的字段；初始化路线图-v16的道路；回填地点能力、订单取件站和配送员位置。旧的经纬度、优先级、订单预计分钟数、单订单路线字段、`route_points`、预约及旧取件码结构均保留，等待后续阶段完成代码切换后再处理。

本阶段不创建新业务订单、不清理既有业务数据，也不把历史单订单路线直接当作新活动综合路线。

## 2. 实体关系

```text
campus_locations 1 ── N campus_route_edges N ── 1 campus_locations
campus_locations 1 ── N couriers.current_location_id
campus_locations 1 ── N delivery_orders.pickup_location_id

couriers 1 ── N delivery_routes
delivery_routes 1 ── N route_stops N ── 1 delivery_orders
campus_locations 1 ── N route_stops

parcels 1 ── N parcel_exception_reports
delivery_orders 1 ── N parcel_exception_reports
couriers 1 ── N parcel_exception_reports
users 1 ── N parcel_exception_reports.handled_by_user_id
```

## 3. 新增表

### `campus_route_edges`

保存地点图的带权边。当前14条道路均为双向；`from_location_id + to_location_id` 唯一。起点和终点分别建立状态索引，以支持从任一端扩展双向邻接点。

### `route_stops`

保存综合路线内的 `CURRENT / PICKUP / DROPOFF` 业务节点。同一路线的 `sequence_no` 唯一；`CURRENT` 仅表示配送员当前位置，因此 `delivery_order_id` 必须为空，取件和送达节点则必须关联订单。数据库保证基本类型、状态和外键，先取后送约束由阶段3的路线领域服务验证。

### `parcel_exception_reports`

保存异常上报和管理员处理事实。待处理记录不得预填处理人/时间；已解决或已退回记录必须有处理人和处理时间。

## 4. 兼容新增字段

| 表 | 字段 | 阶段2可空原因 |
|---|---|---|
| `couriers` | `current_location_id`, `location_update_time` | 允许旧程序创建档案；现有记录全部回填 |
| `delivery_orders` | `pickup_location_id` | 允许旧程序在切换前继续创建订单；现有记录全部回填 |
| `delivery_routes` | `version_no`, `status`, `total_distance_meters`, `estimated_duration_seconds`, `algorithm_version` | 保持旧的一单一路线插入兼容 |
| `delivery_assignments` | 邀请、过期、响应及拒绝原因字段 | 历史候选记录没有可靠的邀请事实，不伪造时间 |

`delivery_routes.active_courier_id` 是生成列，仅在 `status='ACTIVE'` 时返回配送员编号，并通过唯一索引保证每名配送员最多一条活动路线。

## 5. 道路数据

路线图-v16包含12个地点、14条双向道路。两处歧义按最终执行版固定为：

- 软件园图书馆 ↔ 美英澳公寓楼：200米；
- 西南2门 ↔ 六区下快递服务站：180米。

其余道路按图中标注录入。迁移前会验证12个地点编码全部存在，避免生成不完整图。

## 6. 回填策略

- `CAMIGO-DORM-07` 从旧的“七号学生公寓”校正为“美英澳公寓楼”；
- 六区下快递服务站的地点类型校正为 `STATION`；
- 两个服务站设置 `pickup_enabled=1, delivery_enabled=0`；
- 只有存在在线快递柜且已经配置格口的非服务站地点设置 `delivery_enabled=1`，当前占用情况不改变地点能力；
- 历史订单优先使用其旧路线起点作为取件站，无旧路线时回填食堂北快递服务站；
- 配送员位置优先使用最近一次已完成路线终点，无历史路线时回填食堂北快递服务站；
- 所有配送员容量及数据库默认值固定为8；兼容期保留旧 `BREAK` 结构值；
- 旧路线按配送员和创建时间生成历史版本号，完成路线标记为 `COMPLETED`，未完成旧路线标记为 `SUPERSEDED`，不生成伪 `ACTIVE` 路线。

## 7. 索引与访问模式

| 访问模式 | 支持索引 |
|---|---|
| 查找有效取件点/目的地 | `idx_locations_pickup_status`, `idx_locations_delivery_status` |
| 从图节点读取邻边 | `idx_route_edges_from_status`, `idx_route_edges_to_status` |
| 查询配送员当前位置 | `idx_couriers_current_location` |
| 按取件站和状态查订单 | `idx_orders_pickup_status` |
| 读取配送员路线版本 | `uk_routes_courier_version`, `idx_routes_courier_status_version` |
| 保证单配送员单活动路线 | `uk_routes_active_courier` |
| 按顺序读取路线停靠点 | `uk_route_stops_sequence`, `idx_route_stops_route_status_sequence` |
| 定时扫描过期邀请 | `idx_assignments_offer_expiry` |
| 查询配送员拒绝窗口 | `idx_assignments_rejection_window` |
| 管理员查询待处理异常 | `idx_exception_reports_status_time` |

## 8. 迁移与恢复

迁移脚本 `stage2-additive-migration.sql` 通过 `information_schema` 判断既有字段、索引和约束，可以重复执行。道路使用唯一键更新；回填只填空值或写入相同确定值。

MySQL DDL 会隐式提交，不能依赖事务整体回滚。执行顺序为：

1. 对当前开发库执行完整 `mysqldump`；
2. 在阶段1恢复验证库上执行两次迁移并比较结果；
3. 在开发库执行迁移；
4. 再执行一次验证幂等性；
5. 运行数据库约束检查、后端测试、应用启动与现有OpenAPI检查。

如需恢复阶段2之前状态，使用本阶段执行前的完整数据库备份恢复，不在开发库上手工逆向删除字段或表。
