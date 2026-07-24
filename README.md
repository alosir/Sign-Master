# 签到大师 (Sign Master)

一款 Android 签到管理应用，帮助用户集中管理各类 APP、网站和其他类型的签到任务，支持灵活的周期规则、签到提醒、数据统计与数据迁移。

[![Android API](https://img.shields.io/badge/API-21%2B-blue.svg)](https://android-arsenal.com/api?level=21)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/Material-3-success.svg)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## 当前版本

**v1.2.0**

- 版本号规则
  - 修复 bug / 修改已有功能 → 第三位 +1，例如 `v1.1.2`
  - 新增功能 → 第二位 +1，例如 `v1.2.0`
  - 全面性或本质性变化的大型改版 → 第一位 +1，例如 `v2.0.0`

## ✨ 核心功能

### 1. 多种签到类型

| 类型 | 说明 | 触发行为 |
|------|------|----------|
| **APP 签到** | 已安装应用，保存包名 | 完成时自动启动目标 APP |
| **网站签到** | 记录网址 | 完成时自动通过浏览器打开 |
| **其他任务** | 自定义任务 | 直接标记为已完成 |

### 2. 周期签到系统

- **每天 / 每周 / 每月 / 自定义** 四种周期模式
- 每周支持选择周一 ~ 周日任意多天
- 每月支持选择 1 ~ 31 号及「最后一天」
- 自定义模式支持每 N 天 / 周 / 月
- 可设置跳过法定节假日或双休日
- 所有周期判断只比较年月日，不卡时分秒

### 3. 四大主页面

- **今日**：展示当天待签到任务与当天已完成任务
- **任务**：以「未签到 / 已签到」双 Tab 展示全部任务
- **统计**：月度日历、完成率、类型分布、连续签到排行
- **我的**：数据导出 / 导入、通知权限管理

### 4. 签到提醒

- 创建任务时可设置提醒时间（HH:mm）
- 到达提醒时间推送系统通知
- 点击通知自动打开「今日」页面
- 创建带提醒的任务时自动申请通知权限，并支持引导至系统设置

### 5. 数据导入 / 导出

- JSON 格式导出全部签到项与历史记录
- 导出文件保存至系统 `Download` 文件夹
- 导入时按 ID 去重，已存在或无效数据跳过并提示
- 文件格式错误时给出明确失败原因

### 6. 统计与可视化

- 月度日历：以圆点标记「全部完成 / 部分完成」日期
- 本月完成率、本周完成率
- APP / 网站 / 其他 类型分布饼图
- 连续签到天数排行

### 7. 视觉与交互

- Material Design 3 风格
- 底部导航 + 右下角悬浮添加按钮
- 下拉刷新
- 深色模式自动适配

## 🛠 技术栈

- **语言**: Kotlin 1.9.20
- **架构**: MVVM + Repository
- **数据库**: Room 2.6.1
- **UI**: Material Components 1.11.0
- **异步**: Kotlin Coroutines 1.7.3
- **后台**: AlarmManager + WorkManager
- **导航**: BottomNavigationView + Fragment
- **构建**: Gradle 8.2 + Kotlin DSL
- **min/target/compile SDK**: 21 / 34 / 34

## 系统要求

- 最低：Android 5.0 (API 21)
- 推荐：Android 8.0 (API 26) 及以上
- 深色模式：Android 10 (API 29) 自动适配

## 快速开始

```bash
# 1. 克隆
git clone https://github.com/alosir/Sign-Master.git
cd Sign-Master

# 2. 编译 Debug
./gradlew assembleDebug

# 3. 安装
./gradlew installDebug
# 或
adb install app/build/outputs/apk/debug/app-debug.apk
```

APK 位置：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 项目结构

```
app/src/main/
├── java/com/alosir/task/
│   ├── CheckinApplication.kt            # Application
│   ├── data/                             # 数据层
│   │   ├── CheckinDatabase.kt
│   │   ├── dao/                          # CheckinItemDao / CheckinRecordDao / ScriptDao
│   │   ├── entity/                       # CheckinItem / CheckinRecord / AutomationScript
│   │   └── repository/                   # CheckinItemRepository / ScriptRepository
│   ├── service/                          # 无障碍服务（AutomationService）
│   ├── ui/                               # UI 层
│   │   ├── MainActivity.kt
│   │   ├── adapter/                      # PendingCheckinAdapter / CompletedCheckinAdapter / AppListAdapter / StreakRankAdapter
│   │   ├── bottomsheet/                  # AddCheckinBottomSheet / EditCheckinBottomSheet
│   │   ├── fragment/                     # TodayFragment / TasksFragment / StatisticsFragment / ProfileFragment
│   │   ├── view/                         # CheckinCalendarView / CyclePickerView / PieChartView / TimePickerView
│   │   └── viewmodel/                    # CheckinListViewModel / StatisticsViewModel
│   ├── util/                             # 工具类
│   │   ├── AppLauncher.kt
│   │   ├── CycleCalculator.kt
│   │   ├── DataExportImport.kt
│   │   ├── DownloadFileHelper.kt
│   │   ├── IconManager.kt
│   │   ├── NotificationHelper.kt
│   │   ├── NotificationPermissionHelper.kt
│   │   ├── ReminderScheduler.kt
│   │   └── StatisticsCalculator.kt
│   └── worker/                           # DailyCheckinRefreshWorker
├── res/                                  # 资源文件
└── AndroidManifest.xml
```

## 权限

| 权限 | 用途 | 必需 |
|------|------|------|
| `QUERY_ALL_PACKAGES` | 列举已安装应用 | 是 |
| `INTERNET` | 网站签到 | 是 |
| `POST_NOTIFICATIONS` | 签到提醒通知（Android 13+） | 否 |
| `SCHEDULE_EXACT_ALARM` | 精确提醒 | 否 |
| `WRITE_EXTERNAL_STORAGE` | 导出到系统 Download（Android 9 及以下） | 否 |
| `BIND_ACCESSIBILITY_SERVICE` | 自动化签到 | 否 |

## 已知限制

1. 网站签到使用默认图标，暂不支持下载 favicon
2. Android 10+ 对后台启动 Activity 有严格限制
3. 自动化签到需用户手动开启无障碍服务
4. 无法处理验证码等反自动化机制

## 文档索引

- [PRD.md](PRD.md) — 产品需求与 Roadmap
- [DEVELOP.md](DEVELOP.md) — 开发者技术指南
- [QUICKSTART.md](QUICKSTART.md) — 5 分钟快速上手
- [BUILD.md](BUILD.md) — 构建指南
- [PROJECT_INDEX.md](PROJECT_INDEX.md) — 项目文件索引
- [项目交付报告.md](项目交付报告.md) — 项目交付说明
- [CheckinMaster-Showcase.html](CheckinMaster-Showcase.html) — 视觉展示页
- [index.html](index.html) — APP 官网页（含最新 Release APK 下载）

## 版本历史

| 版本 | 改动 |
|------|------|
| v1.2.0 | 新增任务终止逻辑（按次数/按日期/手动）；新增后台保活与自启动引导；统一任务详情弹窗；已签到列表左滑改为详情；统计页排行显示描述 |
| v1.1.2 | 修复应用启动闪退；补全 VersionUpdateActivity 声明；增强桌面角标容错 |
| v1.1.1 | 优化检查更新逻辑：无 GitHub Release/APK 时提示「已是最新版本」 |
| v1.1.0 | 新增版本更新页面与 GitHub 检查更新；我的页新增当前版本卡片；统一四页顶部边距；更换 APP 桌面图标 |
| v1.0.2 | 统一统计页标题为 Toolbar 样式；我的页新增居中的版本号与更新日期；更换 APP 桌面图标为新版日历主题图标 |
| v1.0.1 | 新增桌面图标数字角标，实时显示「今日」待签到任务数量；待签到为 0 时自动移除角标 |
| v1.0.0 | 全新重构发布：底部导航四页架构、周期选择器升级、签到提醒、统计页、通知权限、导出到 Download |

---

**最后更新**: 2026-07-18  
**项目版本**: v1.2.0
