# 快速开始指南

**当前版本**: v1.0.2  
**最后更新**: 2026-07-13

## 前置要求

- ✅ Android Studio 2023.1 或更高版本
- ✅ JDK 17
- ✅ Android SDK API 34
- ✅ Git（可选）

## 编译步骤

### 方法一：Android Studio（推荐）

1. **打开项目**
   ```
   File → Open → 选择 Sign-Master 目录
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
cd Sign-Master

# 编译 Debug
./gradlew assembleDebug

# 编译 Release（需先配置 keystore.properties）
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
2. 点击右下角 **「+」** 按钮
3. 选择签到类型：
   - **APP 签到**：从已安装应用列表选择并搜索
   - **网站签到**：输入名称和网址
   - **其他任务**：输入任务名称
4. 配置**签到周期**（每天 / 每周 / 每月 / 自定义）
5. 可选择设置**提醒时间**，到达时间会推送通知
6. 保存后卡片出现在「今日」页或「任务」页

## 日常交互

| 操作 | 行为 |
|------|------|
| 点击「今日」待签到卡片 | 启动 APP / 打开网站 / 标记完成 |
| 右滑「今日」待签到卡片 | 快速签到 |
| 点击「任务」页待签到卡片 | 启动 APP / 打开网站 / 标记完成 |
| 点击「任务」页已签到卡片 | 查看任务描述（描述显示/隐藏） |
| 长按任意卡片 | 弹出菜单（编辑 / 重置 / 删除） |
| 下拉列表 | 刷新数据 |

> 💡 **重置**：将今日已完成任务恢复为待签到状态。

## 数据迁移

### 导出

1. 进入「我的」页面
2. 点击「导出数据」
3. JSON 文件将保存到系统 `Download` 文件夹

### 导入

1. 进入「我的」页面
2. 点击「导入数据」
3. 选择之前导出的 JSON 文件
4. 导入结果会提示成功 / 重复 / 失败数量

## 常见问题

### Build FAILED

**原因**：Gradle 同步失败或依赖下载失败

**解决方案**：
```bash
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
adb uninstall com.alosir.task.debug
# 或
adb uninstall com.alosir.task
```

## 项目统计（v1.0.0）

| 项目 | 数量 |
|------|------|
| Kotlin 源文件 | ~45 |
| 资源文件 | 60+ |
| 数据库表 | 3 |
| 文档 | 8 份 |
| 总代码行数 | ~6000 |

## 下一步

- 阅读 [README.md](README.md) 了解完整功能
- 阅读 [DEVELOP.md](DEVELOP.md) 了解技术架构
- 阅读 [PRD.md](PRD.md) 了解需求规格与 Roadmap
- 查看 [CheckinMaster-Showcase.html](CheckinMaster-Showcase.html) 视觉展示
