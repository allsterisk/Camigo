# Camigo 阶段4：智能派单设计

## 1. 范围与业务规则

本阶段实现智能派单闭环：候选人筛选、评分快照、3分钟限时邀请、拒绝/超时递补、管理员启动下一轮、拒单冷却、接单并发上限以及管理端审计明细。按照用户后续明确规则，本轮所有候选均拒绝或超时后不自动重试，也不限制人工重启轮数。

候选配送员必须同时满足：

- `employment_status = ACTIVE`
- `availability_status = AVAILABLE`
- 已确认 `current_location_id`
- 活动订单小于 8
- 不在拒单冷却期
- 没有同订单未过期邀请

评分公式为：

- 路线分 = `100 / (1 + 增量距离 / 500)`
- 负载分 = `100 * (8 - 活动订单数) / 8`
- 服务分 = `COALESCE(rating_avg, 3) / 5 * 100`
- 总分 = `路线分 * 40% + 负载分 * 35% + 服务分 * 25%`

## 2. 数据模型

`delivery_assignments` 保存每一个候选快照和决策过程，状态限定为 `CANDIDATE / OFFERED / ACCEPTED / REJECTED / EXPIRED / SKIPPED`。快照包含路线增量、活动订单数、三项分数、总分、算法版本、邀请时间、过期时间和拒绝原因。

MySQL 生成列与唯一索引保证：

- 一个订单最多一条 `ACCEPTED`
- 一个订单最多一条 `OFFERED`

`delivery_orders` 增加 `dispatch_round`、`next_dispatch_time`、`manual_dispatch_required`，将轮次和到期依据持久化到 MySQL。数据库迁移为幂等脚本，不删除现有业务行。

## 3. 派单流程

1. 锁定订单行，检查已有邀请、人工重启标记和当前轮次。
2. 查询合格配送员，调用阶段3路线插入算法计算增量，写入全部候选快照。
3. 按总分降序、配送员ID升序选取下一人，邀请前再次锁行并校验资格。
4. 接单时同时锁定订单和配送员，重算路线、复查8单上限，原子绑定订单并保存新路线版本。
5. 拒绝或超时后递补本轮下一候选人；本轮耗尽后立即设置人工重启标记，包裹继续对外显示“已到校”。
6. 管理员在快递管理中点击“开始配送”后才创建下一轮候选；配送员接受邀请后，包裹才进入“配送中”。

## 4. 冷却、容错与事务

Memurai 有序集合记录最10分钟拒绝；达到3次时写入15分钟冷却键。Redis 暂时不可用时，筛选使用 MySQL 拒绝记录降级，不放宽业务条件。Redis 写入在事务提交后执行，避免数据库回滚却已记录拒单。

订单行锁、配送员行锁、条件更新和 MySQL 唯一约束共同处理并发。当前轮仍有邀请时重复启动不会新建轮次；调度器只处理邀请超时，不再自动启动下一轮。

## 5. API

- `GET /api/delivery-assignments/current`：配送员查看当前邀请
- `PUT /api/delivery-assignments/{id}/accept`：接受邀请
- `PUT /api/delivery-assignments/{id}/reject`：带必填拒绝原因拒绝
- `POST /api/delivery-assignments/orders/{orderId}/start`：管理员启动/继续派单
- `POST /api/delivery-assignments/parcels/{parcelId}/start`：管理员在快递管理中启动首轮或下一轮派单
- `GET /api/delivery-assignments/orders/{orderId}`：管理员查看全部候选与决策明细

旧接单 URL 保留兼容，但只能接受系统当前发给本人且未过期的邀请，不再允许自由抢单。

## 6. 验收结果

- Maven 全量测试：102 项通过，0 失败，0 错误，0 跳过
- 真实 MySQL 派单集成测试：5 项通过，测试事务回滚
- 前端生产构建：通过
- MySQL 迁移脚本重复执行：通过
- 迁移前后21张表行数一致，未清理数据
