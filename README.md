# 签到大师 (Checkin Master)

一款 Android 签到管理应用，帮助用户集中管理各类 APP、网站和其他类型的签到任务，支持灵活的周期签到功能。

[![Android API](https://img.shields.io/badge/API-21%2B-blue.svg)](https://android-arsenal.com/api?level=21)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/Material-3-success.svg)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## 当前版本

**v3.3.4** — 2026-06-14

- `versionCode = 36`
- `versionName = "3.3.4"`

## ✨ 核心功能

### 1. 多种签到类型

| 类型 | 说明 | 触发行为 |
|------|------|----------|
| **APP 签到** (`CheckinType.APP = 0`) | 已安装应用，保存包名+图标 | 点击启动 APP |
| **网站签到** (`CheckinType.WEBSITE = 1`) | 记录网址，自动补全 `https://` 前缀 | 点击通过浏览器打开 |
| **其他签到** (`CheckinType.OTHER = 2`) | 自定义任务清单 | 点击标记为已签到 |

### 2. 周期签到系统（核心亮点）

- **三种周期单位**：`DAY`(0)、`WEEK`(1)、`MONTH`(2)
- **每X天/周/月严格间隔模式**（基准日 = 上次签到所在周期的"刷新日"）
- **不卡时分秒**：所有周期判断只比较年月日
- **历史签到永久保存**：修改周期可选择性清空历史（`EditItemDialog` 内部处理）
- **`lastCheckinDate`** 单字段记录最近一次签到日期

### 3. 状态驱动 UI

两种状态（`CheckinStatus` 枚举）：

| 状态 | 视觉 | 含义 |
|------|------|------|
| `PENDING` | 白色卡片 + 灰色状态点 | 当前可签到 |
| `COMPLETED` | 浅灰卡片 + 绿色状态点 + 文字 alpha 0.55 | 周期窗口内已完成 |

### 4. 双 Tab 浏览架构

- **待签到 Tab** (`PendingCheckinFragment`) — `LinearLayoutManager` + 右侧滑快速签到
- **已签到 Tab** (`CompletedCheckinFragment`) — 单击无反应，**双击 300ms 内**打开 APP/网站

### 5. 长按菜单

| 列表 | 菜单项 |
|------|--------|
| 待签到 | 编辑 / 删除 |
| **已签到** | 编辑 / **重置时间** / 删除 |

> **重置时间**（v3.3.0+）：将卡片状态恢复为"可签到"，清空当日 `checkin_records` + 将 `last_checkin_date` 置 `NULL`，效果等同新加卡片。

### 6. 数据导入 / 导出

- **JSON 格式**（Gson 序列化）
- 文件名：`checkinmaster_export_yyyyMMdd_HHmmss.json`
- 路径：`/Android/data/com.example.checkinmaster/files/`
- 基于 `ActivityResultContracts.GetContent()` 选文件（v3.3.0+）

### 7. 视觉风格

- 顶栏使用主题色 `primary`（纯蓝 `#3498db`）
- 8dp 圆角卡片 + 细腻阴影
- 周期文本（11sp 主题色）
- 深色模式自动适配（`values-night/colors.xml`）
- FAB 浮动按钮（矢量加号 `ic_add_rounded`）
- 空状态：64dp 半透明勾选图标 + 标题 + 副标题

### 8. 工程健壮性（v3.3.0+）

- ✅ `MainActivity` 改用 `ActivityResultContracts` 替代废弃 API
- ✅ ViewModel 协程防抖（`refreshJob?.cancel()`）+ `hasLoadedOnce` 单次加载守卫
- ✅ `cycleStatusMap` 为唯一状态源，删除冗余 `checkinStatusMap`
- ✅ `notifyItemRangeChanged(PAYLOAD)` 部分更新，保留 RecyclerView 动画
- ✅ ViewHolder listener 仅创建一次
- ✅ Snackbar 1 秒强制 dismiss（`postDelayed`）
- ✅ `resetCheckinTime` 防御性空检查
- ✅ 双击判定用 `item.id` 替代 `position`（防 RecyclerView 刷新错乱）

### 9. 自动化签到（实验性）

- 无障碍服务 (`AutomationService`) — 录制用户点击/输入并回放
- 脚本存储在 `automation_scripts` 表（`actions` JSON 字段）
- `ScriptRecorder` 监听 `TYPE_VIEW_CLICKED` / `TYPE_VIEW_FOCUSED` 事件
- `ScriptParser` 负责 JSON ↔ Kotlin 互转
- v3.3.4 当前状态：框架就绪、UI 入口未实装（详见 [memory.html](memory.html)）

## 🛠 技术栈

- **语言**: Kotlin 1.9.20
- **架构**: MVVM + Repository（Repository 已实装，VM 直接调 DAO 是历史遗留）
- **数据库**: Room 2.6.1（version = 3, 含 MIGRATION_2_3）
- **UI**: Material Components 1.11.0（Theme.Material3.Light/Dark.NoActionBar）
- **异步**: Kotlin Coroutines 1.7.3
- **导航**: ViewPager2 + FragmentStateAdapter（2 个 Tab）
- **后台**: WorkManager 2.9.0（每日 0:00 触发 `DailyCheckinRefreshWorker`）
- **图片**: Coil 2.5.0（依赖已声明，暂未使用）
- **构建**: Gradle 8.2.0 + Kotlin DSL
- **min/target/compile SDK**: 21 / 34 / 34

## 系统要求

- 最低：Android 5.0 (API 21)
- 推荐：Android 8.0 (API 26) 及以上
- 深色模式：Android 10 (API 29) 自动适配

## 快速开始

```bash
# 1. 克隆
git clone <repository-url>
cd CheckinMaster

# 2. 编译 Debug
./gradlew assembleDebug

# 3. 安装
./gradlew installDebug
# 或
adb install app/build/outputs/apk/debug/app-debug.apk
```

APK 位置：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## 项目结构

```
app/src/main/
├── java/com/example/checkinmaster/
│   ├── CheckinApplication.kt           # Application：调度每日 0:00 Worker
│   ├── data/                            # 数据层
│   │   ├── CheckinDatabase.kt          # Room 数据库 v3
│   │   ├── dao/                         # CheckinItemDao / CheckinRecordDao / ScriptDao
│   │   ├── entity/                      # CheckinItem / CheckinRecord / AutomationScript
│   │   └── repository/                  # CheckinItemRepository / ScriptRepository
│   ├── service/
│   │   ├── AutomationService.kt         # 无障碍服务（录制/回放）
│   │   └── ActionInfo.kt                # 动作数据类
│   ├── ui/
│   │   ├── MainActivity.kt              # ViewPager2 + Tab + FAB + ActivityResultContracts
│   │   ├── adapter/
│   │   │   ├── CheckinItemAdapter.kt    # 列表项 + 状态点 + cycle chip
│   │   │   └── AppListAdapter.kt        # APP 选择器（搜索过滤）
│   │   ├── dialog/
│   │   │   ├── AddAppDialog.kt          # 添加 APP（含周期设置）
│   │   │   ├── AddWebsiteDialog.kt      # 添加网站（含周期设置）
│   │   │   ├── AddOtherDialog.kt        # 添加其他任务（含周期设置）
│   │   │   ├── EditItemDialog.kt        # 编辑（异步加载、协程保存、修改周期清历史）
│   │   │   └── CheckinTypeSelectorDialog.kt # 类型选择
│   │   ├── fragment/
│   │   │   ├── PendingCheckinFragment.kt    # 待签到 + 右滑快速签到 + Snackbar 1秒
│   │   │   ├── CompletedCheckinFragment.kt  # 已签到 + 双击打开 + 重置时间菜单
│   │   │   └── CheckinListFragment.kt       # 备用（未在 MainActivity ViewPager 中使用）
│   │   └── viewmodel/
│   │       └── CheckinListViewModel.kt  # MVVM 核心（协程防抖、状态源单一）
│   └── util/
│       ├── AppLauncher.kt               # 启动 APP / 打开网站
│       ├── CheckinResetScheduler.kt     # WorkManager 调度
│       ├── CycleCalculator.kt           # 周期计算核心 + CheckinStatus 枚举
│       ├── CycleSettingsHelper.kt       # 周期选择 UI 辅助（v3.3.3 提取）
│       ├── DataExportImport.kt          # JSON 导入导出
│       ├── IconManager.kt               # APP / 网站 / 默认图标
│       ├── ScriptParser.kt              # 脚本 JSON 解析
│       └── ScriptRecorder.kt            # 录制无障碍事件
│   └── worker/
│       └── DailyCheckinRefreshWorker.kt # 每日 0:00 触发
├── res/                                # 资源
│   ├── layout/                          # activity_main / fragment_checkin_list / item_checkin_list / item_app_list / 4 个 dialog
│   ├── values/                          # strings / colors / themes
│   ├── values-night/                    # 深色模式 colors / themes
│   ├── drawable/                        # 8 个（ic_add_rounded / ic_check_circle / ic_default_app / ic_default_website / ic_default_other / bg_default_app_icon / bg_status_indicator / ic_launcher_foreground）
│   ├── menu/main_menu.xml
│   └── xml/accessibility_service_config.xml
└── AndroidManifest.xml                  # 含 AutomationService
```

## 核心类关系

```
MainActivity
  ├─ ViewPager2
  │   ├─ PendingCheckinFragment  → CheckinListViewModel.cycleStatusMap (PENDING)
  │   └─ CompletedCheckinFragment → CheckinListViewModel.cycleStatusMap (COMPLETED)
  └─ ExtendedFloatingActionButton → PopupMenu → CheckinTypeSelectorDialog → Add*Dialog

CheckinListViewModel (AndroidViewModel)
  ├─ appItems / websiteItems / otherItems: LiveData<List<CheckinItem>>
  ├─ _cycleStatusMap: LiveData<Map<Int, CheckinStatus>>  ← 唯一状态源
  ├─ _pendingCheckinItems / _completedCheckinItems: LiveData<List<CheckinItem>>
  └─ refreshJob: Job  // 协程防抖
       └─ CycleCalculator.getCheckinStatus()  // 核心计算
```

## 权限

| 权限 | 用途 | 必需 |
|------|------|------|
| `QUERY_ALL_PACKAGES` | 列举已安装应用 | 是 |
| `INTERNET` | 网站签到 + 图标下载 | 是 |
| `POST_NOTIFICATIONS` | 通知权限（Android 13+，预留） | 否 |
| `BIND_ACCESSIBILITY_SERVICE` | 自动化签到 | 否 |
| `READ/WRITE_EXTERNAL_STORAGE` | 数据导入导出 | 否 |

> v3.3.3 已移除的权限：`FOREGROUND_SERVICE` / `RECEIVE_BOOT_COMPLETED` / `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`（对应删除的 ReminderService / ReminderReceiver）。详见 [memory.html](memory.html)。

## 已知限制

1. **图标限制**：网站签到目前使用默认图标（`IconManager.saveDefaultIcon`），不再下载 favicon
2. **后台 Activity 启动限制**：Android 10+ 严格限制后台启动 Activity，自动签到需前台运行
3. **反自动化检测**：部分 APP 可能阻止自动化操作；验证码无法处理
4. **无障碍服务未配置入口**：UI 层面没有引导开启入口（`AutomationService` 框架就绪）
5. **Repository 与 ViewModel 直调 DAO 重复**：业务逻辑散落两处，未做下沉

## 文档索引

- [PDR.md](PDR.md) — 产品需求文档
- [DEVELOP.md](DEVELOP.md) — 开发者技术指南
- [DOCS.md](DOCS.md) — 文档总览
- [QUICKSTART.md](QUICKSTART.md) — 5 分钟快速上手
- [BUILD.md](BUILD.md) — 构建指南（环境/命令/常见问题）
- [PROJECT_INDEX.md](PROJECT_INDEX.md) — 项目文件索引（历史快照）
- [项目交付报告.md](项目交付报告.md) — 项目交付说明
- [memory.html](memory.html) — 历史 BUG、变更与删除代码档案
- [README.html](README.html) — 浏览器友好的项目说明
- [PROJECT_SUMMARY.html](PROJECT_SUMMARY.html) — 详细开发总结
- [DOCS.html](DOCS.html) — 文档中心
- [CheckinMaster-Showcase.html](CheckinMaster-Showcase.html) — 视觉展示页

## 版本历史

| 版本 | 日期 | 改动 |
|------|------|------|
| v3.3.4 | 2026-06-14 | versionCode 36；统一文档至当前代码（移除 Reminder 相关引用） |
| v3.3.3 | 2026-06-13 | versionCode 35；提取 `CycleSettingsHelper`、移除 ReminderService/ReminderReceiver、清理未用代码、增强单测 |
| v3.3.2 | 2026-06-06 | 工程健壮性：ActivityResultContracts、协程防抖、状态源唯一化、编译修复 |
| v3.3.0 | 2026-06-06 | 视觉风格统一 + 「重置时间」功能 + Snackbar 1 秒强制 dismiss |
| v3.2.3 | 2026-06-02 | 已签到 Tab 双击打开 APP/网站 |
| v3.2.2 | 2026-05-31 | 状态枚举从 PENDING/COMPLETED/WAITING 简化为 PENDING/COMPLETED |
| v3.2.0 | 2026-05-30 | 深色模式 |
| v3.0.x | 2026-05-29 | 周期签到系统 + 数据库 v3 + MIGRATION_2_3 |
| v1.0.0 | 2026-05-23 | 基础功能完成 |

## 故障排除

```bash
# 编译失败
./gradlew clean && ./gradlew build --refresh-dependencies

# R 文件找不到
./gradlew clean

# 数据库升级失败
# 检查 CheckinDatabase 的 version 与 MIGRATION_2_3 一致
```

---

**最后更新**: 2026-06-14
**项目版本**: v3.3.4
