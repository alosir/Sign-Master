# Sign Master 构建指南

**最后更新**: 2026-07-17 — v1.1.2

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

> 把 `<你的 JDK 17 路径>` 替换为你机器上的实际路径。

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

Release 需要签名。项目已提供 `keystore.properties.sample` 模板。

#### 4.1 配置 Release 签名

1. 生成 keystore（如还没有）：
```bash
keytool -genkey -v -keystore my-release-key.keystore -alias alosir_task -keyalg RSA -keysize 2048 -validity 10000
```

2. 复制模板并填写：
```bash
cp keystore.properties.sample keystore.properties
```

3. 编辑 `keystore.properties`：
```properties
RELEASE_STORE_FILE=../my-release-key.keystore
RELEASE_STORE_PASSWORD=你的库密码
RELEASE_KEY_ALIAS=alosir_task
RELEASE_KEY_PASSWORD=你的别名密码
```

> `keystore.properties` 和 `my-release-key.keystore` 已在 `.gitignore` 中，不会被提交。

#### 4.2 编译 Release
```bash
./gradlew assembleRelease
```

Release APK 产物：`app/build/outputs/apk/release/app-release.apk`

---

## 常见问题与解决方案

### 问题 1: Plugin not found (org.jetbrains.kotlin.android)
**错误信息**:
```
Plugin [id: 'org.jetbrains.kotlin.android', version: '1.9.20'] was not found
```

**原因**: Gradle 无法从默认仓库下载 Kotlin 插件

**解决方案**: 在 `settings.gradle.kts` 中配置国内镜像
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

**原因**: Gradle Daemon 正在运行并锁定了文件

**解决方案**:
```bash
./gradlew --stop
```

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
```

---

### 问题 5: 编译成功但找不到 APK
**APK 输出路径**:
```
Debug 版本:
app/build/outputs/apk/debug/app-debug.apk

Release 版本:
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
Sign-Master/
├── app/                          # 主应用模块
│   ├── build.gradle.kts          # 应用级构建配置
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # 应用清单
│   │   ├── java/.../             # Kotlin 源码
│   │   └── res/                  # 资源文件
│   └── build/outputs/apk/        # 生成的 APK
├── build.gradle.kts              # 项目级构建配置
├── settings.gradle.kts           # 项目设置和插件管理
├── gradle.properties             # Gradle 配置
├── gradlew                       # Gradle Wrapper (Linux/Mac)
└── gradlew.bat                   # Gradle Wrapper (Windows)
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
| **Gson** | 2.10.1 | JSON 解析 |

---

## 快速命令参考

```bash
# 清理构建
./gradlew clean

# 编译 Debug
./gradlew assembleDebug

# 编译 Release
./gradlew assembleRelease

# 运行测试
./gradlew test

# 查看任务列表
./gradlew tasks --all

# 停止守护进程
./gradlew --stop
```

---

**维护者**: Sign Master Dev  
**最后更新**: 2026-07-13
