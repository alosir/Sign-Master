# 签到大师项目 - 完整索引

**当前版本**: v1.0.2  
**最后更新**: 2026-07-13

> 本文件是项目文件清单与功能索引，最新结构以 [README.md](README.md) 的「项目结构」段为准。

## 📂 文档位置

```
Sign-Master/
├── README.md                     # 项目说明文档 ⭐⭐⭐
├── PRD.md                        # 产品需求与 Roadmap ⭐⭐⭐
├── DEVELOP.md                    # 开发者技术指南 ⭐⭐⭐
├── QUICKSTART.md                 # 快速开始指南
├── BUILD.md                      # 构建指南
├── PROJECT_INDEX.md              # 本文件
├── 项目交付报告.md                # 项目交付说明
├── CheckinMaster-Showcase.html   # 视觉展示页
├── app/                          # Android 项目目录
├── my-release-key.keystore       # Release 签名（不提交 Git）
└── keystore.properties.sample    # Release 签名配置模板
```

## 📖 文档阅读顺序

### 对于 AI Agent / 开发者

1. **README.md** - 项目总体说明、功能特性
2. **PRD.md** - 产品需求与后续 Roadmap
3. **DEVELOP.md** - 技术架构、模块详解、维护指南
4. **QUICKSTART.md** - 编译运行
5. **BUILD.md** - 构建环境、命令与常见问题

### 对于用户

1. **README.md** - 了解功能特性
2. **QUICKSTART.md** - 安装和使用指南

## 📦 当前代码文件清单

### 核心代码

#### Application 和入口
- `CheckinApplication.kt`
- `ui/MainActivity.kt`

#### UI 层
- `ui/fragment/TodayFragment.kt`
- `ui/fragment/TasksFragment.kt`
- `ui/fragment/PendingCheckinFragment.kt`
- `ui/fragment/CompletedCheckinFragment.kt`
- `ui/fragment/StatisticsFragment.kt`
- `ui/fragment/ProfileFragment.kt`
- `ui/bottomsheet/AddCheckinBottomSheet.kt`
- `ui/bottomsheet/EditCheckinBottomSheet.kt`
- `ui/adapter/PendingCheckinAdapter.kt`
- `ui/adapter/CompletedCheckinAdapter.kt`
- `ui/adapter/AppListAdapter.kt`
- `ui/adapter/StreakRankAdapter.kt`
- `ui/view/CheckinCalendarView.kt`
- `ui/view/CyclePickerView.kt`
- `ui/view/PieChartView.kt`
- `ui/view/TimePickerView.kt`
- `ui/viewmodel/CheckinListViewModel.kt`
- `ui/viewmodel/StatisticsViewModel.kt`

#### 数据层
- `data/CheckinDatabase.kt`
- `data/dao/CheckinItemDao.kt`
- `data/dao/CheckinRecordDao.kt`
- `data/dao/ScriptDao.kt`
- `data/entity/CheckinItem.kt`
- `data/entity/CheckinRecord.kt`
- `data/entity/AutomationScript.kt`
- `data/repository/CheckinItemRepository.kt`
- `data/repository/ScriptRepository.kt`

#### 服务层
- `service/AutomationService.kt`
- `service/ActionInfo.kt`
- `worker/DailyCheckinRefreshWorker.kt`

#### 工具类
- `util/AppBadgeManager.kt`
- `util/AppLauncher.kt`
- `util/CycleCalculator.kt`
- `util/DataExportImport.kt`
- `util/DownloadFileHelper.kt`
- `util/IconManager.kt`
- `util/NotificationHelper.kt`
- `util/NotificationPermissionHelper.kt`
- `util/ReminderScheduler.kt`
- `util/ReminderReceiver.kt`
- `util/StatisticsCalculator.kt`
- `util/CrashLogger.kt`

#### 测试
- `test/CheckinItemTest.kt`
- `test/StatisticsCalculatorTest.kt`

### 构建配置

- `build.gradle.kts`（项目级）
- `settings.gradle.kts`
- `gradle.properties`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`

## 📊 代码统计（v1.0.0）

| 类别 | 文件数 | 代码行数（估算） |
|------|--------|-----------------|
| Kotlin 源代码 | ~45 | ~6,000 行 |
| XML 布局 | ~20 | ~1,500 行 |
| XML 资源 | ~30 | ~800 行 |
| 构建配置 | 6 | ~400 行 |
| 文档 | 8 | ~4,000 行 |
| **总计** | **~110** | **~12,700 行** |

## ✅ 功能实现状态

| 功能模块 | 实现状态 | 完成度 |
|---------|---------|--------|
| 底部导航四页架构 | ✅ 完成 | 100% |
| Room 数据库 | ✅ 完成 | 100% |
| MVVM 架构 | ✅ 完成 | 100% |
| APP / 网站 / 其他任务 | ✅ 完成 | 100% |
| 每天 / 每周 / 每月 / 自定义周期 | ✅ 完成 | 100% |
| 签到提醒与系统通知 | ✅ 完成 | 100% |
| 今日页待签到 / 已完成 | ✅ 完成 | 100% |
| 任务页未签到 / 已签到 | ✅ 完成 | 100% |
| 统计页日历 / 完成率 / 类型分布 / 连续签到排行 | ✅ 完成 | 100% |
| 数据导入导出 | ✅ 完成 | 100% |
| 通知权限管理 | ✅ 完成 | 100% |
| 深色模式 | ✅ 完成 | 100% |
| 下拉刷新 | ✅ 完成 | 100% |
| 无障碍服务框架 | ✅ 框架 | 待 UI 接入 |

## 🚀 快速编译命令

```bash
# 编译 Debug 版本
./gradlew assembleDebug

# 编译 Release 版本（需配置 keystore.properties）
./gradlew assembleRelease

# 运行测试
./gradlew test

# 清理项目
./gradlew clean

# 编译并安装到设备
./gradlew installDebug
```

详细构建流程见 [BUILD.md](BUILD.md)。

## 📱 编译输出的 APK 位置

```
Debug 版本：
app/build/outputs/apk/debug/app-debug.apk

Release 版本（已签名）：
app/build/outputs/apk/release/app-release.apk
```

## 📞 相关文档链接

- [README.md](README.md) - 项目说明
- [PRD.md](PRD.md) - 产品需求与 Roadmap
- [DEVELOP.md](DEVELOP.md) - 开发者指南
- [QUICKSTART.md](QUICKSTART.md) - 快速开始
- [BUILD.md](BUILD.md) - 构建指南
- [项目交付报告.md](项目交付报告.md) - 交付报告
- [CheckinMaster-Showcase.html](CheckinMaster-Showcase.html) - 视觉展示

---

*最后更新：2026-07-13*  
*项目版本：v1.0.2*
