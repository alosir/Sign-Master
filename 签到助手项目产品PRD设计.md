# 签到助手项目产品 PRD 设计

**文档版本**: v3.3.4  
**对应代码版本**: versionCode 36, versionName "3.3.4"  
**最后更新**: 2026-06-27  

> 本文档严格依据当前 Android 项目代码与现有产品逻辑整理，覆盖所有已实现功能、数据模型、交互规则与验收标准。

---

## 目录

1. [产品概述](#1-产品概述)
2. [功能需求](#2-功能需求)
3. [非功能需求](#3-非功能需求)
4. [用户故事](#4-用户故事)
5. [验收标准](#5-验收标准)
6. [技术约束](#6-技术约束)
7. [版本历史](#7-版本历史)
8. [术语表](#8-术语表)

---

## 1. 产品概述

### 1.1 项目名称

**签到助手**（开发包名：`com.example.checkinmaster`，应用名：签到大师）

### 1.2 产品定位

一款面向 Android 用户的本地签到管理工具，帮助用户集中管理各类 APP、网站和自定义任务的签到/打卡行为，通过灵活的周期规则和清晰的状态展示，降低遗忘率并提升操作效率。

### 1.3 核心价值主张

- **集中管理**：所有签到任务统一入口，无需在多个应用间切换记忆。
- **一键触达**：点击卡片即可启动目标 APP 或打开网站。
- **周期灵活**：支持按天、周、月配置任意整数倍周期。
- **状态可视**：待签到与已完成分区展示，当前状态一目了然。
- **数据自主**：本地 Room 数据库存储，支持 JSON 导入导出。

### 1.4 目标用户

- 需要在多个 APP 中每日签到领取奖励的用户。
- 需要定期访问特定网站完成打卡的用户。
- 将日常习惯、任务以清单形式管理的效率工具用户。
- 偏好数据本地存储、不上云的 Android 用户。

### 1.5 使用场景

| 场景 | 说明 |
|------|------|
| 每日 APP 签到 | 用户早晨打开签到助手，在未签到列表点击目标 APP 卡片，自动跳转并标记完成。 |
| 每周/每月周期性任务 | 用户设置「每 2 周」或「每月 1 号」的签到项，系统只在周期到达时展示为待签到。 |
| 误操作恢复 | 用户不小心完成某项任务，可在已签到列表长按选择「重置时间」恢复。 |
| 换机迁移 | 用户导出 JSON 备份，在新设备上导入恢复全部签到项与历史记录。 |
| 自动化签到（框架阶段） | 用户开启无障碍服务后，可录制目标 APP 内的点击流程并回放。 |

---

## 2. 功能需求

### 2.1 签到项管理

#### 2.1.1 添加签到项

**FR-1.1.1 类型支持**

应用支持三种签到项类型，对应常量如下：

| 类型 | 常量值 | 触发行为 |
|------|--------|----------|
| APP 签到 | `CheckinType.APP = 0` | 点击启动已安装应用 |
| 网站签到 | `CheckinType.WEBSITE = 1` | 点击通过浏览器打开网址 |
| 其他任务 | `CheckinType.OTHER = 2` | 点击标记为已签到 |

**FR-1.1.2 APP 类型添加流程**

- 入口：`MainActivity` 的 FAB → PopupMenu「添加签到项目」→ `CheckinTypeSelectorDialog` → 选择 APP → `AddAppDialog`。
- `AddAppDialog` 展示设备已安装应用列表（排除自身和无可启动 Intent 的应用），按应用名升序排列。
- 顶部提供搜索框，实时过滤应用名，过滤不区分大小写。
- 用户选择应用后，界面切换为详情页，展示应用名、包名输入确认、描述输入、周期选择。
- 保存时创建 `CheckinItem`，`type = APP`，`packageName` 为所选包名，`iconPath` 留空（APP 图标通过 `IconManager.loadAppIcon` 从 PackageManager 动态加载）。

**FR-1.1.3 网站类型添加流程**

- 入口同上，选择网站后打开 `AddWebsiteDialog`。
- 用户输入网站名称、URL、描述。
- URL 自动补全：若用户未输入 `http://` 或 `https://` 前缀，自动补全为 `https://`。
- 保存时调用 `IconManager.saveDefaultIcon` 生成默认网站图标文件，`iconPath` 指向该文件。

**FR-1.1.4 其他类型添加流程**

- 入口同上，选择其他任务后打开 `AddOtherDialog`。
- 用户输入任务名称与描述。
- 保存时同样生成默认图标文件，`iconPath` 指向该文件。

**FR-1.1.5 周期配置**

- 三种 Add Dialog 与 `EditItemDialog` 共用 `CycleSettingsHelper` 组件。
- 周期类型：`DAY(0)` / `WEEK(1)` / `MONTH(2)`。
- 周期值：通过 SeekBar 选择，范围分别为 1~31 天、1~52 周、1~36 个月。
- 实时显示周期描述文本（如「每 3 天」「每 2 周」「每月 1 号」）。
- 默认周期为「每天」。

**FR-1.1.6 图标处理**

- APP：不保存图标文件，运行时从 `PackageManager` 动态获取。
- 网站/其他：保存默认占位图标到应用私有目录 `/files/icons/`。
- 网站 favicon 下载功能当前已废弃，统一使用默认图标。

#### 2.1.2 显示签到项

**FR-1.2.1 主界面结构**

- `MainActivity` 使用 `CoordinatorLayout` + `AppBarLayout` + `TabLayout` + `ViewPager2`。
- 双 Tab 导航：「未签到」/`PendingCheckinFragment`、`已签到」/`CompletedCheckinFragment`。
- 底部右侧悬浮 `FloatingActionButton`，点击展开 PopupMenu：添加 / 导出 / 导入。

**FR-1.2.2 列表排序规则**

`CheckinListViewModel.refreshCheckinLists()` 将所有签到项按以下规则排序：

```
type → cycleType → cycleValue → name
```

即先按类型（APP < 网站 < 其他），再按周期单位（天 < 周 < 月），再按周期值升序，最后按名称升序。

**FR-1.2.3 空状态**

- 当 Tab 内列表为空时，展示空状态视图：64dp 半透明勾选图标 + 主标题 + 副标题。
- 标题：「暂无签到项」；副标题：「点击右下角 + 添加第一个签到项目」。

**FR-1.2.4 卡片布局**

- 使用 `MaterialCardView`，8dp 圆角，1dp 阴影，4dp 外边距。
- 卡片内部水平排列：图标区（APP 显示应用图标，网站/其他隐藏）→ 状态指示器 → 文本区。
- 文本区包含：名称（16sp 加粗）、描述（13sp，可选隐藏）、周期文本（11sp 主题色）。
- 状态指示器：32dp 的 `ic_check_circle`，根据状态着色为成功绿或待办灰。

#### 2.1.3 签到项操作

**FR-1.3.1 未签到列表点击**

在 `PendingCheckinFragment` 中点击卡片：
- APP 类型：调用 `AppLauncher.launchApp` 启动目标应用；启动成功后调用 `markCheckedIn` 写入签到记录。
- 网站类型：调用 `AppLauncher.openWebsite` 打开浏览器；打开后调用 `markCheckedIn`。
- 其他类型：直接调用 `markCheckedIn` 标记完成。

**FR-1.3.2 右滑快速签到**

- 未签到列表支持向右滑动快速签到。
- 滑动触发 `handleItemClick(item)`，执行与点击相同的逻辑。
- 滑动后调用 `adapter.notifyItemChanged(position)` 恢复卡片位置。

**FR-1.3.3 已签到列表单击与双击**

在 `CompletedCheckinFragment` 中：
- 单击无反应，避免浏览时误触打开应用。
- 双击（同一 item 两次点击间隔 < 300ms）打开 APP 或网站。
- 双击判定使用 `item.id` 而非列表 position，防止 RecyclerView 刷新后位置错乱。

**FR-1.3.4 长按菜单**

- 未签到列表长按菜单：编辑 / 删除。
- 已签到列表长按菜单：编辑 / 重置时间 / 删除。

**FR-1.3.5 编辑签到项**

- 通过 `EditItemDialog` 编辑名称、描述、周期。
- 编辑对话框异步加载数据（`lifecycleScope` + `Dispatchers.IO`）。
- 若修改了周期类型或周期值，保存时调用 `repository.clearHistoryByItemId` 清空该 item 的历史签到记录。
- 保存成功后通过 `setOnEditSuccessListener` 回调通知 ViewModel 刷新列表。

**FR-1.3.6 删除签到项**

- 长按菜单选择删除后弹出二次确认对话框。
- 确认后调用 `ViewModel.deleteItem(item)`。
- 由于数据库外键设置了 `onDelete = CASCADE`，删除 item 会自动级联删除关联的 `checkin_records` 和 `automation_scripts`。

**FR-1.3.7 重置时间**

- 仅在已签到列表的长按菜单中显示。
- 选择后弹出确认对话框。
- 确认后调用 `ViewModel.resetCheckinTime(itemId)`：
  - 防御性检查：只有当今日存在签到记录时才执行。
  - 删除今日 `checkin_records` 中该 item 的记录。
  - 将 `checkin_items.last_checkin_date` 置为 `NULL`。
  - 触发 `refreshCheckinLists()`，卡片自动从已签到切回未签到。

---

### 2.2 签到状态管理

#### 2.2.1 状态枚举

```kotlin
enum class CheckinStatus {
    PENDING,    // 可签到
    COMPLETED,  // 已完成
    WAITING     // 保留值，当前逻辑中未独立使用
}
```

#### 2.2.2 状态判定逻辑

`CycleCalculator.getCheckinStatus(cycleType, cycleValue, lastCheckinDate)` 核心逻辑：

```text
if lastCheckinDate == null:
    return PENDING

nextDate = getNextCheckinDate(cycleType, cycleValue, lastCheckinDate)
if today < nextDate:
    return COMPLETED
else:
    return PENDING
```

- 所有日期比较忽略时分秒，仅比较年月日。
- 不同周期的「下次可签到日」计算：
  - DAY：lastDate + cycleValue 天。
  - WEEK：以上次签到所在周的周一为基准，+ cycleValue 周；仅周一可签到。
  - MONTH：以上次签到所在月的 1 号为基准，+ cycleValue 个月；仅每月 1 号可签到。
- 周期值严格按整数倍判断，确保「每 2 周」不会在第 1 周就变为可签到。

#### 2.2.3 签到记录

- 每次成功签到插入一条 `CheckinRecord`：
  - `itemId`: 关联签到项 ID
  - `checkinDate`: 当前日期（yyyy-MM-dd）
  - `checkinTime`: 当前时间戳（yyyy-MM-dd HH:mm:ss）
  - `isAuto`: 是否自动签到，默认 false
  - `status`: 状态，默认 `STATUS_SUCCESS = 1`
- 同时更新 `CheckinItem.last_checkin_date` 为当前日期。
- 通过 `LiveData` 驱动 UI 刷新。

#### 2.2.4 每日刷新 Worker

- `DailyCheckinRefreshWorker` 每日 0:00 执行。
- 清理 7 天前的 `checkin_records` 记录，避免数据无限增长。
- 由 `CheckinApplication.onCreate` 通过 `CheckinResetScheduler.schedule` 注册为唯一周期性任务。

---

### 2.3 数据管理

#### 2.3.1 数据导出

- 入口：`MainActivity` FAB → 导出数据。
- 导出所有 `checkin_items` 和 `checkin_records` 为 JSON。
- 文件名格式：`checkinmaster_export_yyyyMMdd_HHmmss.json`。
- 保存路径：应用外部私有目录 `/Android/data/com.example.checkinmaster/files/`。
- 导出数据包含版本号 `EXPORT_VERSION = "2.0.1"` 和导出时间戳。

#### 2.3.2 数据导入

- 入口：`MainActivity` FAB → 导入数据。
- 使用 `ActivityResultContracts.GetContent()` 选择文件。
- 读取 JSON 后，按 item ID 去重：已存在的 item 跳过，不覆盖。
- 导入记录直接插入，失败则忽略单条。
- 导入完成后 Toast 提示成功导入数量。

---

### 2.4 用户界面

#### 2.4.1 主界面

- 顶栏：纯色主题蓝（`#3498db`），包含双 Tab（未签到/已签到）。
- ViewPager2 实现 Tab 内容切换。
- FAB：右下角圆形加号按钮，点击弹出 PopupMenu。

#### 2.4.2 列表卡片

- 已签到卡片背景色为 `#e8e8e8`（深色模式下 `#2c2c3e`），文字 alpha 0.55。
- 未签到卡片背景色为白色（深色模式下 `#16213e`）。
- 状态指示器：已完成为绿色，未签到为灰色。

#### 2.4.3 添加/编辑弹窗

- 所有 Dialog 基于 `AlertDialog` + ViewBinding。
- 周期选择使用 RadioGroup（每天/每周/每月）+ SeekBar + 描述文本。
- 添加 APP 弹窗内嵌 RecyclerView 展示应用列表，带搜索过滤和加载进度条。

#### 2.4.4 Snackbar 与 Toast

- 未签到页签到成功后显示 Snackbar，时长强制 1 秒。
- 其他操作（添加成功、保存成功、重置成功等）使用 Toast 提示。

---

### 2.5 自动化签到（框架已就绪，UI 未接入）

#### 2.5.1 无障碍服务

- `AutomationService` 继承 `AccessibilityService`。
- 监听事件：点击、聚焦、窗口状态变化、窗口内容变化。
- 录制时通过 `ScriptRecorder` 记录用户操作。
- 回放时通过 `performAction` 执行 click / long_click / swipe / text / back / home / wait 等动作。

#### 2.5.2 动作信息

`ActionInfo` 数据结构：
- `type`: 动作类型
- `target`: 目标定位信息（id / text / desc / focus）
- `delay`: 执行前等待延迟
- `duration`: 滑动/等待持续时间
- `text`: 输入文本
- `startX/startY/endX/endY`: 滑动坐标
- `timestamp`: 时间戳

#### 2.5.3 脚本存储

- 脚本保存在 `automation_scripts` 表。
- 字段：`id`, `name`, `item_id`, `actions`（JSON 字符串）, `created_at`。
- 通过 `ScriptParser` 进行 JSON 与 Kotlin 对象的互转。

#### 2.5.4 当前限制

- 无障碍服务需要在系统设置中手动开启，应用内无引导入口。
- 录制/编辑/绑定脚本的 UI 尚未实装。
- Android 10+ 限制后台启动 Activity，自动签到需应用处于前台。
- 无法处理验证码、随机布局等反自动化机制。

---

## 3. 非功能需求

### 3.1 性能

| 指标 | 目标 |
|------|------|
| 应用冷启动时间 | < 3 秒 |
| Tab 切换响应 | < 100ms |
| 搜索过滤响应 | < 100ms |
| 列表滚动 | 60 FPS |
| 数据库查询 | < 50ms |
| 内存占用 | < 100MB |
| APK 大小 | < 20MB |

### 3.2 兼容性

| 项目 | 规格 |
|------|------|
| 最低 Android 版本 | Android 5.0 (API 21) |
| 推荐 Android 版本 | Android 8.0 (API 26) 及以上 |
| 目标 SDK | API 34 |
| 屏幕尺寸 | 4.7" ~ 12.9" |
| 屏幕方向 | 竖屏优先 |

### 3.3 安全与隐私

- 所有数据存储在应用私有目录或 Room 数据库中，不上传服务器。
- 导出 JSON 文件保存在应用外部私有目录，其他应用无法直接访问（Android 11+）。
- 权限遵循最小化原则，仅申请必要的 `QUERY_ALL_PACKAGES`、`INTERNET`、`POST_NOTIFICATIONS`、存储权限和无障碍服务权限。
- 自动化操作需用户主动开启无障碍服务。

### 3.4 无障碍

- 图标与状态指示器包含 `contentDescription`。
- 自动化服务为视障用户操作目标应用提供技术基础。

---

## 4. 用户故事

### US-1 集中管理签到

**作为** 拥有多个签到 APP 的用户，  
**我想要** 在一个应用中集中查看所有待签到任务，  
**以便** 不会遗漏任何每日签到。

### US-2 灵活周期配置

**作为** 有周期性任务的用户，  
**我想要** 设置「每 2 天」「每 3 周」或「每月 1 号」的签到周期，  
**以便** 任务只在真正到期时提醒我去完成。

### US-3 一键快速签到

**作为** 追求效率的用户，  
**我想要** 在未签到列表点击卡片就能直接打开目标 APP 或网站，  
**以便** 减少查找和切换应用的时间。

### US-4 误操作恢复

**作为** 不小心提前点击了签到的用户，  
**我想要** 在已签到列表长按卡片选择「重置时间」，  
**以便** 将任务恢复为待签到状态。

### US-5 数据迁移

**作为** 更换手机的用户，  
**我想要** 一键导出所有签到项和历史记录，并在新设备导入，  
**以便** 快速恢复使用环境。

### US-6 避免已签到误触

**作为** 浏览已签到列表的用户，  
**我想要** 单击已签到卡片不会打开应用，只有双击才打开，  
**以便** 浏览时不会误启动 APP。

### US-7 自动化签到（未来）

**作为** 需要重复点击完成签到的用户，  
**我想要** 录制目标 APP 内的操作并自动回放，  
**以便** 进一步减少手动操作。

---

## 5. 验收标准

### 5.1 功能验收

| 功能 | 验收标准 |
|------|----------|
| 添加 APP 签到 | 可从已安装应用列表搜索并选择应用，保存后出现在未签到列表。 |
| 添加网站签到 | 输入网址后自动补全 https://，保存后点击可打开浏览器。 |
| 添加其他任务 | 输入名称即可保存，点击后标记为完成。 |
| 周期计算 | 天/周/月严格按周期值的整数倍判断，不误提前标记为待签到。 |
| 状态显示 | PENDING 与 COMPLETED 在卡片背景色、状态指示器颜色上区分明显。 |
| 未签到点击 | APP 类型启动应用，网站类型打开浏览器，其他类型直接完成。 |
| 右滑快速签到 | 未签到卡片右滑后执行签到逻辑并恢复卡片位置。 |
| 已签到双击 | 已签到卡片双击打开 APP/网站，单击无反应。 |
| 长按菜单 | 未签到菜单为「编辑/删除」；已签到菜单为「编辑/重置时间/删除」。 |
| 重置时间 | 仅清除今日记录和 last_checkin_date，效果等同新建卡片。 |
| 编辑 | 异步保存不阻塞 UI；修改周期后清空该 item 历史记录。 |
| 删除 | 二次确认后删除 item，关联记录和脚本级联清除。 |
| 导入/导出 | JSON 文件格式正确，导入按 ID 去重，导出包含全部数据。 |
| 每日刷新 | WorkManager 每日 0:00 清理 7 天前旧记录。 |
| 深色模式 | Android 10+ 自动适配深色主题，颜色取自 values-night。 |

### 5.2 性能验收

| 指标 | 标准 |
|------|------|
| 冷启动 | < 3 秒 |
| Tab 切换 | < 100ms |
| 搜索响应 | < 100ms |
| 列表滚动 | 流畅，无明显掉帧 |
| 内存占用 | < 100MB |
| APK 大小 | < 20MB |

### 5.3 兼容性验收

| 设备/系统 | 标准 |
|-----------|------|
| Android 5.0+ | 可安装运行 |
| Android 8.0+ | 推荐使用，所有功能正常 |
| 手机/平板 | 适配 4.7" ~ 12.9" 屏幕 |

---

## 6. 技术约束

### 6.1 开发环境

| 工具 | 版本 |
|------|------|
| Android Studio | 2023.1+ |
| JDK | 17 |
| Android SDK | API 34 |
| Gradle | 8.2.0 |
| Kotlin | 1.9.20 |

### 6.2 技术栈

| 层次 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.20 |
| 架构 | MVVM + Repository |
| UI | Material Components 1.11.0 (Material 3) |
| 数据库 | Room 2.6.1 |
| 异步 | Kotlin Coroutines 1.7.3 |
| 后台 | WorkManager 2.9.0 |
| 图片 | Coil 2.5.0（已声明，暂未使用） |
| JSON | Gson 2.10.1 |
| 构建 | Gradle Kotlin DSL |

### 6.3 关键依赖

```kotlin
androidx.core:core-ktx
androidx.appcompat:appcompat
com.google.android.material:material
androidx.constraintlayout:constraintlayout
androidx.recyclerview:recyclerview
androidx.fragment:fragment-ktx
androidx.lifecycle:lifecycle-viewmodel-ktx
androidx.lifecycle:lifecycle-livedata-ktx
androidx.lifecycle:lifecycle-runtime-ktx
androidx.room:room-runtime / room-ktx / room-compiler
androidx.work:work-runtime-ktx
org.jetbrains.kotlinx:kotlinx-coroutines-android
com.google.code.gson:gson
io.coil-kt:coil
```

### 6.4 权限

| 权限 | 用途 | 必需 |
|------|------|------|
| `QUERY_ALL_PACKAGES` | 列举已安装应用 | 是 |
| `INTERNET` | 网站签到、图标下载（预留） | 是 |
| `POST_NOTIFICATIONS` | 通知权限（Android 13+，预留） | 否 |
| `BIND_ACCESSIBILITY_SERVICE` | 自动化签到 | 否 |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | 数据导入导出 | 否 |

---

## 7. 版本历史

| 版本 | 日期 | 关键改动 |
|------|------|----------|
| v1.0.0 | 2026-05-23 | 基础功能完成：添加、列表、签到、删除。 |
| v2.x | 2026-05-24 | 网站签到列表、APP 实时搜索、数据导入导出。 |
| v3.0.x | 2026-05-29 | 周期签到系统完整上线；数据库升级到 v3，新增 `cycle_type`、`cycle_value`、`last_checkin_date`。 |
| v3.1.x | 2026-05-29 | 周期整数倍严格判断；UI 紧凑化；`EditItemDialog` 重构。 |
| v3.2.0 | 2026-05-30 | 深色模式适配。 |
| v3.2.2 | 2026-05-31 | 状态枚举从 PENDING/COMPLETED/WAITING 简化为 PENDING/COMPLETED。 |
| v3.2.3 | 2026-06-02 | 已签到 Tab 支持双击打开 APP/网站。 |
| v3.3.0 | 2026-06-06 | 视觉风格统一（8dp 圆角卡片、Snackbar、状态点着色）；新增「重置时间」功能。 |
| v3.3.2 | 2026-06-06 | 工程健壮性：ActivityResultContracts、协程防抖、状态源唯一化、Payload 局部刷新、ViewHolder listener 复用、Snackbar 1 秒 dismiss、postDelayed Long 修复。 |
| v3.3.3 | 2026-06-13 | 提取 `CycleSettingsHelper` 复用周期选择逻辑；移除 `ReminderService`/`ReminderReceiver` 死代码；清理权限；增强单元测试。 |
| v3.3.4 | 2026-06-14 | versionCode 36；统一 README/PDR/DEVELOP 等文档到当前代码状态。 |

---

## 8. 术语表

| 术语 | 说明 |
|------|------|
| 签到项 (CheckinItem) | 用户需要签到的项目，包含名称、类型、周期、最后签到日期等。 |
| 签到记录 (CheckinRecord) | 每次签到产生的记录，关联签到项，包含日期、时间、是否自动、状态。 |
| 周期 (Cycle) | 签到重复规则，由周期类型（天/周/月）和周期值组成。 |
| 周期状态 (CheckinStatus) | 签到项当前状态：`PENDING`（可签到）或 `COMPLETED`（已完成）。 |
| 刷新日 | 周期判断的基准日期：DAY 为当天，WEEK 为周一，MONTH 为 1 号。 |
| 重置时间 | 将已签到状态恢复为待签到，清空当日记录和最后签到日期。 |
| CASCADE | 数据库外键级联删除，删除 item 时自动删除关联记录和脚本。 |
| 无障碍服务 | Android AccessibilityService，用于录制和回放用户界面操作。 |
| WorkManager | Android 后台任务调度组件，用于每日 0:00 清理旧签到记录。 |

---

**文档结束**
