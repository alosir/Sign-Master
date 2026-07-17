# 签到大师 - 开发者技术指南

**当前版本**: v1.1.2  
**最后更新**: 2026-07-17

## 目录

1. [技术架构](#技术架构)
2. [开发环境](#开发环境)
3. [核心模块详解](#核心模块详解)
4. [周期签到系统](#周期签到系统)
5. [状态管理](#状态管理)
6. [工程规范](#工程规范)
7. [常见问题](#常见问题)
8. [维护指南](#维护指南)

---

## 技术架构

### 整体分层

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  MainActivity │  │   Fragment   │  │ BottomSheet  │  │
│  │  BottomNav    │  │  Today/Tasks │  │  Add / Edit  │  │
│  │   + FAB       │  │ Statistics/  │  │              │  │
│  │               │  │   Profile    │  │              │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│           │              │                  │            │
│           └──────────────┴──────────────────┘            │
│                          ↓                               │
│  ┌────────────────────────────────────────────────────┐ │
│  │      CheckinListViewModel / StatisticsViewModel     │ │
│  │   • pendingItems / allPendingItems (LiveData)        │ │
│  │   • completedRecords (LiveData)                      │ │
│  │   • uiState (StatisticsUiState)                      │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Repository Layer                       │
│  ┌──────────────────┐  ┌──────────────────┐            │
│  │ CheckinItemRepo  │  │   ScriptRepo     │            │
│  │  • insertItem    │  │  • insertScript  │            │
│  │  • updateItem    │  │  • getByItemId   │            │
│  │  • deleteItem    │  │                  │            │
│  └──────────────────┘  └──────────────────┘            │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                      Data Layer (Room)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │CheckinItemDao│  │CheckinRecord │  │  ScriptDao   │  │
│  └──────────────┘  │     Dao      │  └──────────────┘  │
│                    └──────────────┘                     │
│                          ↓                               │
│  ┌─────────────────────────────────────────────────┐   │
│  │  CheckinDatabase (Room)                         │   │
│  │  • checkin_items                                │   │
│  │  • checkin_records (CASCADE)                    │   │
│  │  • automation_scripts (CASCADE)                 │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 数据流：点击签到 → UI 更新

```
用户点击完成按钮
  ↓
Adapter.onCompleteClick → Fragment → ViewModel.markCheckined(itemId)
  ↓
recordDao.insert(CheckinRecord(itemId, today, now))
itemDao.updateLastCheckinDate(itemId, today)
  ↓
refreshAll() / refreshCompletedRecords()
  ↓
LiveData 更新 → Fragment 观察 → Adapter.submitList()
  ↓
RecyclerView 局部刷新 / 整体切换 Tab
```

---

## 开发环境

### 必需

| 工具 | 版本 |
|------|------|
| Android Studio | 2023.1+ |
| JDK | 17 |
| Android SDK | API 34 |
| Gradle | 8.2（Wrapper 内置） |
| Kotlin | 1.9.20 |

### 配置

```bash
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

`local.properties`：
```properties
sdk.dir=/path/to/android-sdk
```

### 同步 / 编译

```bash
./gradlew clean
./gradlew assembleDebug     # Debug
./gradlew assembleRelease   # Release（需 keystore.properties）
./gradlew installDebug      # 安装到设备
./gradlew test              # 单元测试
```

---

## 核心模块详解

### 1. `CheckinListViewModel`

**位置**：`ui/viewmodel/CheckinListViewModel.kt`

**关键设计**：
- 与 `MainActivity` 共享的 AndroidViewModel
- `pendingItems`：仅今天待签到的任务（供「今日」页使用）
- `allPendingItems`：今天 + 未来待签到的任务（供「任务」页使用）
- `completedRecords`：全部历史签到记录
- 协程防抖：`refreshJob?.cancel()`

**核心方法**：

| 方法 | 职责 |
|------|------|
| `loadCheckinStatus()` | 入口：加载全部状态 |
| `markCheckined(itemId)` | 写记录 + 更新 lastCheckinDate + 刷新 |
| `resetCheckinTime(itemId)` | 清当日记录 + 清 lastCheckinDate + 刷新 |
| `deleteItem(item)` | 删除 item（CASCADE 自动清记录和脚本） |
| `restoreTodayRecord(record)` | 恢复今日已完成任务为待签到 |

### 2. `CycleCalculator`

**位置**：`util/CycleCalculator.kt`

**核心方法**：
- `isCheckinAvailable(item, date)`：判断某日是否可签到
- `isScheduledForDate(item, date)`：判断某日是否应签到（用于统计日历）
- `getNextCheckinDate(item)`：下次可签到日期
- `getCycleShortDescription(item)`：UI 周期文本

### 3. `AddCheckinBottomSheet`

**位置**：`ui/bottomsheet/AddCheckinBottomSheet.kt`

- 选择类型（APP / 网站 / 其他）后展开对应表单
- APP 选择使用 `AppListAdapter` + 搜索过滤
- 周期选择使用 `CyclePickerView`
- 保存任务后自动刷新 `CheckinListViewModel`
- 若设置了提醒时间，自动申请通知权限

### 4. 周期选择器 `CyclePickerView`

**位置**：`ui/view/CyclePickerView.kt`

- 每天 / 每周 / 每月 / 自定义 四种模式
- 每周：7 列等宽按钮，支持多选
- 每月：7 列网格，1-31 号 + 「最后一天」
- 自定义：双 NumberPicker 选择数值与单位

### 5. `ReminderScheduler` 与 `ReminderReceiver`

**位置**：`util/ReminderScheduler.kt` / `util/ReminderReceiver.kt`

- 使用 `AlarmManager` + `PendingIntent` 设置精确提醒
- 到达提醒时间时，`ReminderReceiver` 触发 `NotificationHelper`
- 通知点击打开 `MainActivity` 并切换到「今日」页

### 6. `NotificationPermissionHelper`

**位置**：`util/NotificationPermissionHelper.kt`

- Android 13+ 申请 `POST_NOTIFICATIONS`
- 使用 `NotificationManagerCompat.areNotificationsEnabled()` 检测系统通知开关状态
- 被拒绝时引导跳转系统设置

### 7. `MainActivity`

**职责**：
- `BottomNavigationView` + Fragment 容器管理四个主页面
- FAB 展开添加任务 BottomSheet
- 导出 / 导入数据
- 检查和申请必要权限

### 8. `StatisticsViewModel`

**位置**：`ui/viewmodel/StatisticsViewModel.kt`

- 加载月度统计数据
- 构建日历日状态（NONE / PARTIAL / FULL）
- 计算完成率、类型分布、连续签到排行

---

## 周期签到系统

### 周期类型

| 类型 | 常量值 | 描述 |
|------|--------|------|
| DAY | 0 | 每 N 天 |
| WEEK | 1 | 每周指定星期几，或每 N 周 |
| MONTH | 2 | 每月指定日期，或每 N 个月 |

### 状态判定逻辑

```kotlin
fun isCheckinAvailable(item, date):
    if date 应该跳过（节假日/周末）: return false
    if lastCheckinDate == date: return false
    if lastCheckinDate == null: return matchesCycleDate(item, date)
    
    // 根据 cycleType / cycleValue 与 lastCheckinDate 计算是否到达下一个签到日
    return 是否到达下一周期
```

### 关键点

1. 不卡时分秒：所有日期比较使用 `Calendar.clearTime()`
2. 每周/每月多选：通过 JSON 数组保存选中的星期几或日期
3. 自定义周期：通过 `cycleValue` 与 `cycleType` 组合实现

---

## 状态管理

### LiveData 状态源

```kotlin
// CheckinListViewModel
val pendingItems: LiveData<List<PendingItemUiModel>>       // 今日待签到
val allPendingItems: LiveData<List<PendingItemUiModel>>    // 全部未签到
val completedRecords: LiveData<List<CompletedRecordUiModel>> // 全部已签到记录

// StatisticsViewModel
val uiState: LiveData<StatisticsUiState>
```

### 排序

待签到列表排序规则：
```kotlin
compareBy(
    { it.nextDate },
    { reminderTimeToMinutes(it.item.reminderTime) },
    { it.item.name }
)
```

---

## 工程规范

### 命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `CheckinListFragment` |
| 方法名 | camelCase | `setupViewPager` |
| 变量名 | camelCase | `checkinType` |
| 常量 | UPPER_SNAKE_CASE | `LAST_DAY_OF_MONTH` |

### 注释

- 公共 API 用 KDoc
- 复杂算法（`CycleCalculator`）必须有行内注释

### 线程模型

- DB 读取/写入：IO 线程
- UI 更新：主线程
- Fragment 监听 LiveData：使用 `viewLifecycleOwner`

---

## 常见问题

### 编译

| 问题 | 原因 | 解决 |
|------|------|------|
| `Theme.Material3.Light.NoActionBar` 找不到 | material 依赖未下载 | 重新 sync Gradle |
| `R.id.xxx` 找不到 | 资源未生成 | `./gradlew clean` |

### 运行时

| 问题 | 原因 | 解决 |
|------|------|------|
| 状态不更新 | LiveData 未观察 | 确认 fragment 用 `viewLifecycleOwner` |
| 提醒不推送 | 通知权限未开启 / 精确闹钟被禁用 | 检查系统设置 |
| 日历状态错误 | 周期规则与预期不符 | 检查 `CycleCalculator.isScheduledForDate` |

---

## 维护指南

### 版本号规则

`MAJOR.MINOR.PATCH`：
- MAJOR：全面性或本质性变化的大型改版
- MINOR：新增功能
- PATCH：BUG 修复 / 已有功能调整 / 文案优化

### 发布步骤

1. `app/build.gradle.kts` 改 `versionCode` / `versionName`
2. 编译 release APK + 跑回归测试
3. 更新 README.md / PRD.md / DEVELOP.md 等文档的版本号与 changelog
4. 提交并 `git tag v1.x.x`

### 数据库 schema 变更

1. 修改 `@Entity` 类
2. 增 `version` 号
3. 写 `Migration(old, new)` 同步 SQL
4. 在 `CheckinDatabase` 注册到 `addMigrations(...)`

### 关键文件索引

| 文件 | 路径 | 说明 |
|------|------|------|
| 主 Activity | `ui/MainActivity.kt` | 底部导航 + FAB |
| 版本更新 Activity | `ui/VersionUpdateActivity.kt` | 更新日志 + 检查更新 |
| 今日 Fragment | `ui/fragment/TodayFragment.kt` | 今日待签到 / 已完成 |
| 任务 Fragment | `ui/fragment/TasksFragment.kt` | ViewPager + 未签到 / 已签到 |
| 统计 Fragment | `ui/fragment/StatisticsFragment.kt` | 日历 / 完成率 / 排行 |
| 我的 Fragment | `ui/fragment/ProfileFragment.kt` | 导入导出 / 通知权限 |
| ViewModel | `ui/viewmodel/CheckinListViewModel.kt` | 签到状态核心 |
| 添加 BottomSheet | `ui/bottomsheet/AddCheckinBottomSheet.kt` | 添加任务 |
| 角标管理 | `util/AppBadgeManager.kt` | 桌面图标数字角标 |
| 周期算法 | `util/CycleCalculator.kt` | 核心周期判断 |
| 提醒调度 | `util/ReminderScheduler.kt` | AlarmManager 提醒 |
| 数据库 | `data/CheckinDatabase.kt` | Room 数据库 |

---

## 参考资源

- [Kotlin 官方文档](https://kotlinlang.org/docs/)
- [Android 开发者文档](https://developer.android.com/docs)
- [Room 数据库](https://developer.android.com/training/data-storage/room)
- [Material Design 3](https://m3.material.io)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Coroutines 指南](https://developer.android.com/kotlin/coroutines)

---

**祝您开发顺利！**
