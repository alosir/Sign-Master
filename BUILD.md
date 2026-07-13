# CheckinMaster 构建指南

**最后更新**: 2026-06-14 — v3.3.4

## 环境要求

### 必需软件
| 软件 | 版本要求 | 备注 |
|------|----------|------|
| **JDK** | 17+ | 通过 `JAVA_HOME` 指定 |
| **Gradle** | 8.2+ | 推荐用项目内置 `gradlew` 包装器 |
| **Android SDK** | API 21-34 | 通过 Android Studio 安装 |
| **Kotlin** | 1.9.20 | Gradle 自动管理 |

### 系统环境
- **操作系统**: Windows 10/11 / macOS / Linux
- **内存**: 最低 8GB（推荐 16GB）
- **存储**: 至少 50GB 可用空间

---

## 构建步骤

### 1. 环境变量配置

#### Windows CMD / PowerShell
```cmd
set JAVA_HOME=<你的 JDK 17 路径>
set PATH=%JAVA_HOME%\bin;%PATH%
```

#### macOS / Linux / Git Bash
```bash
export JAVA_HOME="<你的 JDK 17 路径>"
export PATH="$JAVA_HOME/bin:$PATH"
```

> 把 `<你的 JDK 17 路径>` 替换为你机器上的实际路径，例如 `D:\dev\JDK\17.0.19`（Windows）或 `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home`（macOS）。

### 2. 验证环境
```bash
# 验证 JDK
java -version
# 预期输出包含: 17.x.x

# 验证 Gradle（推荐使用项目内置包装器）
./gradlew --version
# 预期输出包含: Gradle 8.x
```

### 3. 编译 Debug 版本
```bash
# 项目根目录
./gradlew assembleDebug
```

**预期输出**:
```
BUILD SUCCESSFUL in Xs
```

Debug APK 产物：`app/build/outputs/apk/debug/app-debug.apk`

### 4. 编译 Release 版本
```bash
./gradlew assembleRelease
```

> Release 已配置内置签名 `my-release-key.keystore`（签名参数通过 `gradle.properties` 的 `RELEASE_*` 属性读取），无需用户额外配置即可产出已签名 APK。

Release APK 产物：`app/build/outputs/apk/release/app-release.apk`

---

## 常见问题与解决方案

### 问题 1: Plugin not found (org.jetbrains.kotlin.android)
**错误信息**:
```
Plugin [id: 'org.jetbrains.kotlin.android', version: '1.9.20'] was not found
```

**原因**: Gradle 无法从默认仓库下载 Kotlin 插件

**解决方案**: 在 `settings.gradle.kts` 中添加阿里云 Maven 镜像
```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

✅ **已应用到本项目**

---

### 问题 2: JAVA_HOME 路径格式错误
**错误信息**:
```
/usr/bin/bash: java: command not found
```

**原因**: Git Bash 中使用了 Windows 风格路径

**错误示例**:
```bash
export JAVA_HOME="D:\dev\JDK\17.0.19"  # ❌ 错误
```

**正确写法**:
```bash
export JAVA_HOME="/d/dev/JDK/17.0.19"  # ✅ 正确
```

---

### 问题 3: Gradle 守护进程占用文件
**错误信息**:
```
Edit error: EBUSY: resource busy or locked
```

**原因**: Gradle Daemon 正在运行并锁定了 `settings.gradle.kts`

**解决方案**:
```bash
# 方法 1: 停止 Gradle 守护进程
./gradlew --stop

# 方法 2: 强制结束 Java 进程
# Windows:
taskkill /F /IM java.exe
# macOS / Linux:
pkill -f java
```

✅ **推荐**: 遇到文件锁时，使用 `Write` 工具而非 `Edit`

---

### 问题 4: Git 提交时提示 "Author identity unknown"
**错误信息**:
```
*** Please tell me who you are.
```

**解决方案**: 配置 Git 用户信息
```bash
# 本地配置（推荐）
git config user.name "your-name"
git config user.email "your-email@example.com"

# 或全局配置
git config --global user.name "your-name"
git config --global user.email "your-email@example.com"
```

---

### 问题 5: 编译成功但找不到 APK
**APK 输出路径**:
```
Debug 版本:
app/build/outputs/apk/debug/app-debug.apk

Release 版本（已签名）:
app/build/outputs/apk/release/app-release.apk
```

**验证 APK 生成**:
```bash
# Linux / macOS / Git Bash
ls -lh app/build/outputs/apk/debug/*.apk
# Windows PowerShell
Get-ChildItem app/build/outputs/apk/debug/*.apk | Select-Object Name, Length
```

---

## 优化建议

### 1. 使用 Gradle 缓存
Gradle 会自动缓存依赖，第二次构建会快很多：
```
BUILD SUCCESSFUL in 2s
38 actionable tasks: 1 executed, 37 up-to-date
```

### 2. 配置 Gradle 守护进程
在 `gradle.properties` 中添加：
```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

### 3. 使用阿里云 Maven 镜像
已在 `settings.gradle.kts` 中配置，加速依赖下载

---

## 安装到设备

### 方法 1: 命令行安装
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 方法 2: Android Studio
1. 连接手机（开启 USB 调试）
2. 点击 ▶️ Run 按钮
3. 选择目标设备

### 方法 3: 手动安装
1. 复制 `app-debug.apk` 到手机
2. 在手机上允许"安装未知来源应用"
3. 点击 APK 文件安装

---

## 项目结构

```
CheckinMaster/
├── app/                          # 主应用模块
│   ├── build.gradle.kts          # 应用级构建配置
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # 应用清单
│   │   ├── java/.../           # Kotlin 源码
│   │   └── res/                # 资源文件
│   └── build/outputs/apk/      # 生成的 APK
├── build.gradle.kts             # 项目级构建配置
├── settings.gradle.kts          # 项目设置和插件管理
├── gradle.properties           # Gradle 配置
└── gradlew                     # Gradle Wrapper (Linux/Mac)
    gradlew.bat                  # Gradle Wrapper (Windows)
```

---

## 技术栈版本

| 技术 | 版本 | 用途 |
|------|------|------|
| **Android Gradle Plugin** | 8.2.0 | 构建系统 |
| **Kotlin** | 1.9.20 | 编程语言 |
| **Coroutines** | 1.7.3 | 异步编程 |
| **Room** | 2.6.1 | 数据库 |
| **Lifecycle** | 2.7.0 | 生命周期管理 |
| **WorkManager** | 2.9.0 | 后台任务 |
| **Coil** | 2.5.0 | 图片加载 |
| **Gson** | 2.10.1 | JSON 解析 |

---

## 快速命令参考

```bash
# 清理构建
./gradlew clean

# 编译 Debug
./gradlew assembleDebug

# 编译 Release（已签名）
./gradlew assembleRelease

# 运行测试
./gradlew test

# 查看任务列表
./gradlew tasks --all

# 停止守护进程
./gradlew --stop
```

---

## 更新日志

- **2026-06-14**: 通用化环境/路径描述；改用 `./gradlew` 包装器；Release 已配置内置签名
- **2026-06-11**: 初始版本，记录完整构建流程和常见问题
- **2026-06-11**: 添加阿里云 Maven 镜像配置
- **2026-06-11**: 修复 Gradle 插件下载失败问题

---

**维护者**: CheckinMaster Dev
**最后更新**: 2026-06-14
