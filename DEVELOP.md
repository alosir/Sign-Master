# 签到大师 - 开发者技术指南

**当前版本**: v3.3.4
**versionCode**: 36
**最后更新**: 2026-06-14

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
│  │MainActivity  │  │  Fragment    │  │   Dialog     │  │
│  │  (ViewPager2 │  │  Pending     │  │  Add*        │  │
│  │   + Tab)     │  │  Completed   │  │  Edit        │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│           │              │                  │            │
│           └──────────────┴──────────────────┘            │
│                          ↓                               │
│  ┌────────────────────────────────────────────────────┐ │
│  │      CheckinListViewModel (AndroidViewModel)        │ │
│  │   • pendingCheckinItems  (LiveData)                  │ │
│  │   • completedCheckinItems (LiveData)                 │ │
│  │   • cycleStatusMap (LiveData<Map<Int,CheckinStatus>>│ │
│  │   • refreshJob: Job (防抖)                          │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Repository Layer                       │
│  ┌──────────────────┐  ┌──────────────────┐            │
│  │CheckinItemRepo   │  │  ScriptRepo      │            │
│  │  • insertItem    │  │  • insertScript  │            │
│  │  • updateItem    │  │  • getByItemId   │            │
│  │  • deleteItem    │  │                  │            │
│  │  • clearHistory  │  │                  │            │
│  └──────────────────┘  └──────────────────┘            │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                      Data Layer (Room)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │CheckinItemDao│  │CheckinRecord │  │  ScriptDao   │  │
│  │ +clearLast    │  │     Dao      │  │              │  │
│  │  CheckinDate  │  │+deleteByItem │  │              │  │
│  │ +updateLast   │  │  IdAndDate   │  │              │  │
│  │  CheckinDate  │  │+getByDate    │  │              │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                          ↓                               │
│  ┌─────────────────────────────────────────────────┐   │
│  │  CheckinDatabase (Room v3, MIGRATION_2_3)       │   │
│  │  • checkin_items (cycleType/cycleValue/         │   │
│  │                  lastCheckinDate)               │   │
│  │  • checkin_records (CASCADE)                    │   │
│  │  • automation_scripts (CASCADE)                 │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 数据流：点击 → UI 更新

```
用户点击卡片
  ↓
Fragment.handleItemClick(item)
  ↓
ViewModel.markCheckined(item.id)
  ├─ recordDao.insert(CheckinRecord(itemId, today))   // 写签到记录
  ├─ itemDao.updateLastCheckinDate(itemId, today)     // 写最后签到日
  └─ refreshCheckinLists()                            // 重算
        ↓
        CycleCalculator.getCheckinStatus(cycleType, cycleValue, lastCheckinDate)
        ↓
        _cycleStatusMap.postValue(cycleStatusMap)
        ↓
        _pendingCheckinItems.postValue(...)
        _completedCheckinItems.postValue(...)
        ↓
        Fragment.observe() → adapter.updateAllCycleStatus(...)
        ↓
        notifyItemRangeChanged(0, itemCount, PAYLOAD_STATUS)  // 局部刷新
        ↓
        ViewHolder.applyStatus(item)  // 仅更新背景色和状态点
```

### 重置时间流程

```
长按已签到卡片 → 选"重置时间"
  ↓
Fragment 弹确认对话框
  ↓
ViewModel.resetCheckinTime(itemId)
  ├─ 防御：getByDate(itemId, today) != null 才执行
  ├─ recordDao.deleteByItemIdAndDate(itemId, today)   // 清当日记录
  ├─ itemDao.clearLastCheckinDate(itemId)             // lastCheckinDate = NULL
  └─ refreshCheckinLists()                            // 触发 CycleCalculator
        ↓
        CycleCalculator 看到 lastCheckinDate == null → PENDING
        ↓
        卡片从「已签到」Tab 切回「未签到」Tab
```

---

## 开发环境

### 必需

| 工具 | 版本 |
|------|------|
| Android Studio | 2023.1+ (建议 Hedgehog / Iguana) |
| JDK | 17 |
| Android SDK | API 34 |
| Gradle | 8.0+ (Wrapper 内置 8.0) |
| Kotlin | 1.9.20 |

### 配置

```bash
export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

`local.properties`：
```properties
sdk.dir=/opt/android-sdk
```

### 同步 / 编译

```bash
./gradlew clean
./gradlew assembleDebug     # Debug
./gradlew assembleRelease   # Release
./gradlew installDebug      # 安装到设备
./gradlew test              # 单元测试
```

---

## 核心模块详解

### 1. `CheckinListViewModel`（核心）

**位置**：`ui/viewmodel/CheckinListViewModel.kt`

**关键设计**：
- **协程防抖**：`refreshJob?.cancel()` 保证并发刷新时只有最后一次结果生效
- **单次加载守卫**：`hasLoadedOnce` 避免多 fragment 同时 `onViewCreated` 触发多次完整刷新
- **状态源唯一**：`_cycleStatusMap: LiveData<Map<Int, CheckinStatus>>` 是唯一真实状态源；`pendingCheckinItems` / `completedCheckinItems` 是派生结果

**核心方法**：

| 方法 | 职责 |
|------|------|
| `loadCheckinStatus()` | 入口：受 `hasLoadedOnce` 守卫 |
| `refreshCheckinLists()` | 重算所有 item 的周期状态 + 分桶 PENDING/COMPLETED |
| `markCheckined(itemId)` | 写记录 + 更新 lastCheckinDate + 触发 refresh |
| `resetCheckinTime(itemId)` | 清当日记录 + 清 lastCheckinDate + 触发 refresh |
| `deleteItem(item)` | 删除 item（CASCADE 自动清记录和脚本） |

### 2. `CycleCalculator`（核心算法）

**位置**：`util/CycleCalculator.kt`

**核心方法**：
- `isCheckinAvailable(...)`：判断当前是否可签到
- `getNextCheckinDate(...)`：下次可签到日期
- `getCycleDescription(cycleType, cycleValue)`：UI 文本
- `getCheckinStatus(cycleType, cycleValue, lastCheckinDate)`：核心判断

**关键枚举**：
```kotlin
enum class CheckinStatus {
    PENDING,    // 可签到
    COMPLETED   // 周期窗口内已完成
}
```

### 3. `CheckinItemAdapter`（UI 性能）

**位置**：`ui/adapter/CheckinItemAdapter.kt`

**性能优化**：
- **Payload 模式局部更新**：`onBindViewHolder(holder, position, payloads)` + `PAYLOAD_STATUS` 标记
- **Listener 仅创建一次**：在 `init {}` 块中保存 `View.OnClickListener` 引用
- **状态源统一**：删除 `checkinStatusMap`，只用 `cycleStatusMap`

### 4. 双 Tab Fragment

| Fragment | 职责 | 布局 | 交互 |
|----------|------|------|------|
| `PendingCheckinFragment` | 显示 PENDING 项 | LinearLayoutManager | 单击启动 / 右滑快速签到 / 长按菜单 |
| `CompletedCheckinFragment` | 显示 COMPLETED 项 | LinearLayoutManager | 单击无反应 / 双击打开 / 长按菜单（含重置时间） |

**关键差异**：
- 双击检测用 `item.id` 而非 `position`（防 RecyclerView 刷新后错位）
- PendingCheckinFragment 的 Snackbar 时长 1 秒（`postDelayed` 强制 dismiss）
- CompletedCheckinFragment 的长按菜单多了「重置时间」

### 5. `EditItemDialog`（异步编辑）

**位置**：`ui/dialog/EditItemDialog.kt`

**关键设计**：
- 用 `lifecycleScope` + `withContext(Dispatchers.IO)` 避免阻塞 UI
- 保存时如果改了周期，会 `repository.clearHistoryByItemId(itemId)`
- 通过 `setOnEditSuccessListener` 回调通知列表刷新

### 6. 三个 Add Dialog

都包含周期设置：
- RadioGroup：每天/每周/每月
- SeekBar：1~31（天）/ 1~52（周）/ 1~36（月）
- 实时显示周期描述文本

**位置**：`ui/dialog/AddAppDialog.kt`、`AddWebsiteDialog.kt`、`AddOtherDialog.kt`

### 6.1 `CycleSettingsHelper`（周期选择复用，v3.3.3 提取）

**位置**：`util/CycleSettingsHelper.kt`

**背景**：v3.3.3 之前，`AddAppDialog` / `AddWebsiteDialog` / `AddOtherDialog` / `EditItemDialog` 各自实现一遍 "RadioGroup 选周期类型 + SeekBar 调周期值 + 文本描述更新" 逻辑，约 200 行重复代码。

**接口**：

```kotlin
object CycleSettingsHelper {
    fun setup(
        radioGroup: RadioGroup,
        seekBar: SeekBar,
        descriptionText: TextView,
        initialCycleType: CheckinCycleType,
        initialCycleValue: Int,
        onCycleChanged: (CheckinCycleType, Int) -> Unit
    )
}
```

4 个 Dialog 现在只调一次 `setup(...)`，并在 `onCycleChanged` 回调里把 `(type, value)` 写回各自的局部变量。

### 7. `MainActivity`

**职责**：
- ViewPager2 + TabLayoutMediator 管理 2 个 Fragment
- ExtendedFloatingActionButton 弹出 PopupMenu（添加 / 导出 / 导入）
- 导出 / 导入用 `ActivityResultContracts.GetContent()`（v3.3.0+ 替代废弃的 `startActivityForResult`）

### 8. 数据库迁移

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE checkin_items ADD COLUMN cycle_type INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE checkin_items ADD COLUMN cycle_value INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE checkin_items ADD COLUMN last_checkin_date TEXT")
    }
}
```

---

## 周期签到系统

### 三种周期单位

| 类型 | 常量值 | 描述 | 基准日 |
|------|--------|------|--------|
| DAY | 0 | 每X天 | 签到当天 |
| WEEK | 1 | 每X周 | 签到所在周的周一 |
| MONTH | 2 | 每X月 | 签到所在月的1号 |

### 状态判定逻辑

```kotlin
// 伪代码
fun getCheckinStatus(cycleType, cycleValue, lastCheckinDate):
    if lastCheckinDate == null:
        return PENDING  // 新建卡片或被重置

    nextDate = getNextCheckinDate(cycleType, cycleValue, lastCheckinDate)
    if today < nextDate:
        return COMPLETED  // 仍在周期窗口内
    else:
        return PENDING    // 已过周期，可签到
```

### 关键点

1. **不卡时分秒**：`Calendar.set(HOUR_OF_DAY, 0)` + `MINUTE/SECOND/MILLISECOND = 0`
2. **基准日算法**：
   - DAY：以 `lastDate + cycleValue 天`
   - WEEK：回退到 `lastDate` 所在周的周一 + `cycleValue * 7 天`
   - MONTH：`lastDate` 所在月的1号 + `cycleValue 个月`（用 while 循环逐月加）
3. **整数倍严格判断**（v3.1.2+）：每X周/月必须正好是 X 的整数倍

---

## 状态管理

### 状态源

```kotlin
private val _cycleStatusMap = MutableLiveData<Map<Int, CheckinStatus>>()
val cycleStatusMap: LiveData<Map<Int, CheckinStatus>> = _cycleStatusMap
```

### 派生结果

```kotlin
private val _pendingCheckinItems = MutableLiveData<List<CheckinItem>>()    // filter PENDING
private val _completedCheckinItems = MutableLiveData<List<CheckinItem>>()  // filter COMPLETED
```

### 排序

```kotlin
.sortedWith(compareBy(
    { it.type },         // 类别：APP < WEBSITE < OTHER
    { it.cycleType },    // 周期单位：DAY < WEEK < MONTH
    { it.cycleValue },   // 周期值：1 < 2 < 3 ...
    { it.name }          // 名称升序
))
```

---

## 工程规范

### 命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `CheckinListFragment` |
| 方法名 | camelCase | `setupViewPager` |
| 变量名 | camelCase | `checkinType` |
| 常量 | UPPER_SNAKE_CASE | `ONE_SECOND_MS_MS` |
| DAO 方法 | camelCase + 表语义 | `getAllByType` / `clearLastCheckinDate` |
| DAO 写入 | 动宾结构 | `insert` / `update` / `delete` / `clearLastCheckinDate` |

### 注释

- 公共 API 用 KDoc
- 复杂算法（CycleCalculator）必须有行内注释说明基准日算法
- TODO 标记待完成功能

### 线程模型

- **DB 读取/写入**：必须在 IO 线程（`withContext(Dispatchers.IO)` 或 `lifecycleScope.launch(Dispatchers.IO)`）
- **UI 更新**：必须在主线程（默认 `lifecycleScope` 是 Main，可直接 setText / show）
- **Fragment 监听 LiveData**：`viewLifecycleOwner` 而非 `this`

### 协程作用域

| 场景 | 作用域 |
|------|--------|
| ViewModel | `viewModelScope` |
| Fragment | `lifecycleScope` |
| Dialog | `lifecycleScope` |
| Service / Worker | 自建 `CoroutineScope(Dispatchers.Main + SupervisorJob())` |

---

## 常见问题

### 编译

| 问题 | 原因 | 解决 |
|------|------|------|
| `Theme.Material3.Light.NoActionBar` 找不到 | material:1.11.0 已含 | 重新 sync Gradle |
| `R.id.xxx` 找不到 | 资源未生成 | `./gradlew clean` |
| `Type mismatch: Int vs Long` | `postDelayed` 需 Long | 把变量改为 `Long` + 后缀 `L` |
| `Operator '==' cannot be applied to 'Int' and 'Long'` | 同上 | 同上 |

### 运行时

| 问题 | 原因 | 解决 |
|------|------|------|
| 数据库升级失败 | 缺 migration | 检查 `MIGRATION_2_3` 是否注册 |
| 状态不更新 | LiveData 未观察 | 确认 fragment 用 `viewLifecycleOwner` |
| 菜单没显示 | `getString(R.string.xxx)` 拼错 | 查 strings.xml |
| 右滑无反应 | `ItemTouchHelper` 没 attach | `touchHelper.attachToRecyclerView(rv)` |
| Snackbar 一直转 | `snackbar.dismiss()` 没调用 | 改用 `postDelayed` 自定义时长 |

---

## 维护指南

### 版本号规则

`MAJOR.MINOR.PATCH`：
- MAJOR：架构重大变更
- MINOR：新功能（如重置时间 / 自动化 / 数据导入）
- PATCH：BUG 修复 / 性能优化 / 文案润色

发布步骤：
1. `app/build.gradle.kts` 改 `versionCode` / `versionName`
2. `app/src/main/res/values/strings.xml` 改 `version` 字符串
3. 编译 release APK + 跑回归测试
4. 在 README.md / PDR.md / DEVELOP.md 加 changelog 条目
5. 提交 `git tag v3.x.x`

### 数据库 schema 变更

1. 修改 `@Entity` 类
2. 增 `version` 号
3. 写 `Migration(old, new)` 同步 SQL
4. 在 `CheckinDatabase` 注册到 `addMigrations(...)`
5. 写测试：跑通 schema 升级路径

### 新增 Fragment / Dialog

- 必须在 `MainActivity` 或对应父 Fragment 中显示
- 用 ViewBinding（`viewBinding = true`）
- 监听 VM 用 `viewLifecycleOwner`
- Dialog 在 `onDestroyView` 中 `_binding = null` 防内存泄漏

### 性能调优检查项

- [ ] RecyclerView 是否 `setHasFixedSize(true)`
- [ ] Adapter 是否避免 `notifyDataSetChanged`
- [ ] DiffUtil ItemCallback 是否正确实现
- [ ] 长列表是否启用 `removeClippedSubviews`
- [ ] 是否有主线程磁盘 IO（File.exists / File.readText）

---

## 关键文件索引

| 文件 | 路径 | 说明 |
|------|------|------|
| 主 Activity | `ui/MainActivity.kt` | ViewPager2 + FAB + ActivityResultContracts |
| 待签到 Fragment | `ui/fragment/PendingCheckinFragment.kt` | 右滑快速签到 + Snackbar 1秒 |
| 已签到 Fragment | `ui/fragment/CompletedCheckinFragment.kt` | 双击打开 + 重置时间菜单 |
| ViewModel | `ui/viewmodel/CheckinListViewModel.kt` | 协程防抖 + 状态源唯一 |
| Adapter | `ui/adapter/CheckinItemAdapter.kt` | Payload 局部更新 |
| 数据库 | `data/CheckinDatabase.kt` | Room v3 + MIGRATION_2_3 |
| 实体 | `data/entity/CheckinItem.kt` | 含 cycleType / cycleValue / lastCheckinDate |
| 周期算法 | `util/CycleCalculator.kt` | 核心状态判定 + CheckinStatus 枚举 |
| 周期选择辅助 | `util/CycleSettingsHelper.kt` | v3.3.3 提取，4 个 Dialog 共用 |
| 编辑对话框 | `ui/dialog/EditItemDialog.kt` | 异步 + 改周期清历史 |
| 添加对话框 | `ui/dialog/AddApp/Website/OtherDialog.kt` | 三个 Add 流程 |
| Worker | `worker/DailyCheckinRefreshWorker.kt` | 每日 0:00 触发 |
| 调度 | `util/CheckinResetScheduler.kt` | WorkManager 配置 |
| Application | `CheckinApplication.kt` | 启动时调度 Worker |

---

## 参考资源

- [Kotlin 官方文档](https://kotlinlang.org/docs/)
- [Android 开发者文档](https://developer.android.com/docs)
- [Room 数据库](https://developer.android.com/training/data-storage/room)
- [Material Design 3](https://m3.material.io)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Coroutines 指南](https://developer.android.com/kotlin/coroutines)
- [ActivityResultContracts](https://developer.android.com/training/basics/intents/result)

---

## 版本历史

| 版本 | 日期 | 关键改动 |
|------|------|----------|
| v3.3.4 | 2026-06-14 | versionCode 36；统一文档至当前代码（移除 Reminder 相关引用） |
| v3.3.3 | 2026-06-13 | versionCode 35；提取 `CycleSettingsHelper`、移除 `ReminderService`/`ReminderReceiver`（含 receiver 目录）+ 相关 Manifest 声明/权限、清理未用变量/常量/线程、增强单测 |
| v3.3.2 | 2026-06-06 | 工程健壮性（ActivityResultContracts、协程防抖、状态源唯一化、postDelayed Long 修复） |
| v3.3.0 | 2026-06-06 | 「重置时间」功能 + Material 3 视觉升级 |
| v3.2.3 | 2026-06-02 | 已签到 Tab 双击打开 |
| v3.2.2 | 2026-05-31 | 状态枚举简化 |
| v3.2.0 | 2026-05-30 | 深色模式 |
| v3.0.x | 2026-05-29 | 周期签到系统 + 数据库 v3 + MIGRATION_2_3 |

> 已删除代码（ReminderService / ReminderReceiver）的完整档案见 [memory.html](memory.html)。

---

**祝您开发顺利！**
