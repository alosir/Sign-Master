# 快速开始指南

**当前版本**: v3.3.4 (versionCode: 36)
**最后更新**: 2026-06-14

## 前置要求

- ✅ Android Studio 2023.1 或更高版本
- ✅ JDK 17
- ✅ Android SDK API 34
- ✅ Git（可选）

## 编译步骤

### 方法一：Android Studio（推荐）

1. **打开项目**
   ```
   File → Open → 选择 CheckinMaster 目录
   ```

2. **等待 Gradle 同步**
   - 首次打开会自动下载 Gradle 和依赖
   - 同步完成后右下角显示 "Sync Successful"

3. **编译项目**
   ```
   Build → Make Project (或 Ctrl+F9)
   ```

4. **运行到设备**
   ```
   Run → Run 'app' (或 Shift+F10)
   ```

### 方法二：命令行

```bash
cd CheckinMaster

# 首次需要 gradlew 包装器
gradle wrapper   # 可选

# 编译 Debug
./gradlew assembleDebug

# 编译 Release
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug

# 跑单元测试
./gradlew test
```

## 安装 APK

### 方式一：ADB

```bash
adb devices                              # 确认设备已连接
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 方式二：Android Studio

1. 顶部工具栏选择目标设备
2. 点击 Run ▶️ 或按 Shift+F10

## 首次使用

1. 打开"签到大师"应用
2. 点击右下角 **「添加签到项目」** 按钮
3. 在类型选择对话框中选择：
   - **APP 签到**：从已安装应用列表选择
   - **网站签到**：输入名称和网址
   - **其他任务**：输入任务名称
4. 在每个添加对话框中可配置**签到周期**（每天/每周/每月 + 数量）
5. 完成！卡片出现在「未签到」Tab

## 日常交互

| 操作 | 行为 |
|------|------|
| 单击「未签到」卡片 | 启动 APP / 打开网站 / 标记已签到 |
| 右滑「未签到」卡片 | 快速签到（无 Snackbar） |
| 单击「已签到」卡片 | 无反应 |
| 双击「已签到」卡片 | 打开 APP / 网站 |
| 长按任意卡片 | 弹出菜单（编辑 / 重置时间 / 删除） |

> 💡 **重置时间**：仅在「已签到」Tab 中可见。点击后该卡片恢复为「未签到」状态，等同新加卡片。

## 常见问题

### Build FAILED

**原因**：Gradle 同步失败或依赖下载失败

**解决方案**：
```bash
# 检查网络
# 切换阿里云镜像（settings.gradle.kts 中加）

# 清理重建
./gradlew clean
./gradlew build --refresh-dependencies
```

### SDK not found

**原因**：Android SDK 路径未配置

**解决方案**：
- Tools → SDK Manager
- 安装 Android SDK Platform 34 + Build Tools 34
- 在 `local.properties` 中设置 `sdk.dir=...`

### APK 安装失败

**原因**：旧版本签名冲突或空间不足

**解决方案**：
```bash
adb uninstall com.example.checkinmaster.debug
# 或
adb uninstall com.example.checkinmaster
```

### 编译错误：`Type mismatch: Long but Int was expected`

**原因**：`postDelayed(delayMillis: Long, ...)` 必须传 `Long`

**解决方案**：常量加 `L` 后缀
```kotlin
private const val ONE_SECOND_MS_MS = 1000L  // 不要漏掉 L
```

## 项目统计（v3.3.4）

| 项目 | 数量 |
|------|------|
| Kotlin 源文件 | 32 |
| 资源文件 | 60+ |
| 数据库表 | 3 |
| 文档 | 8 份 |
| 总代码行数 | ~4500 |

## 下一步

- 阅读 [README.md](README.md) 了解完整功能
- 阅读 [DEVELOP.md](DEVELOP.md) 了解技术架构
- 阅读 [PDR.md](PDR.md) 了解需求规格
- 阅读 [DOCS.md](DOCS.md) 文档总览
- 查看 [../CheckinMaster-Showcase.html](../CheckinMaster-Showcase.html) 视觉展示
