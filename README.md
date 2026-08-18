# 胡牌追踪器

一个运行在 Android 手机上的麻将胡牌记分工具。应用面向单设备、离线使用场景，支持 2 到 4 名玩家，自动推进圈风和庄家，并在结算前允许修改记录。

## 当前功能

- 创建 2 到 4 人牌局
- 等额和非等额两种记分模式
- 自动显示当前圈风和庄家，例如 `东风东`、`东风南`、`南风东`
- 等额模式只记录得分，负分在结算时根据差额计算
- 非等额模式支持为每位玩家输入正分、负分或 0 分
- 非等额模式中所有正分记录都计为胡牌
- 修改和删除未结算的记分记录
- 点击记录后显示修改和删除操作
- 结算预览显示净分、得分、胡牌次数和平均得分
- 结算后锁定牌局和全部记录
- 使用 Room 保存本地数据，应用重启后可以恢复牌局
- 每局玩家统计使用浅色背景区分玩家

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX ViewModel
- Room
- Kotlin Symbol Processing（KSP）
- JUnit

## 环境要求

- Android Studio
- JDK 25
- Android SDK 36
- Gradle 9.7.0
- 最低支持 Android 8.0（API 26）

## 快速运行

使用 Android Studio 打开项目根目录，等待 Gradle Sync 完成后，选择模拟器或 Android 真机运行 `app` 模块。

也可以使用命令行构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

运行单元测试：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

生成的 Debug APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Gradle 国内镜像

项目 wrapper 已配置腾讯 Gradle 镜像：

```text
https://mirrors.cloud.tencent.com/gradle/gradle-9.7.0-all.zip
```

配置文件：

```text
gradle/wrapper/gradle-wrapper.properties
```

如果镜像下载失败，也可以在 Android Studio 的 Gradle 设置中使用本地 Gradle 发行版，或手动准备 Gradle 9.7.0。

## 记分规则

### 等额模式

每笔记录选择一名得分玩家并输入分数。应用只保存这笔正分，不在记录中保存其他玩家的负分。结算时按照所有玩家的得分合计计算差额和付款明细。

### 非等额模式

每笔记录需要输入所有玩家的分数，所有分数之和必须为 0。正分玩家自动计为胡牌，胡牌次数不需要额外指定。

平均得分计算方式：

```text
平均得分 = 得分 / 胡牌次数
```

没有胡牌记录时平均得分显示为 `0.00`。

## 项目结构

```text
app/src/main/java/com/hutracker/
├── data/       Room 数据库、DAO 和牌局存储
├── domain/     牌局模型、圈庄推进和计分逻辑
└── ui/         Compose 界面和 ViewModel
```

## 当前未完成

以下功能已记录在设计和任务文档中，但目前尚未完成：

- JSON 导入和导出
- 当前牌局 CSV 分享
- 完整的 UI 自动化测试
- 真机和多种玩家人数的完整冒烟测试

## 文档

- [需求文档](docs/Requirement.md)
- [设计文档](docs/Design.md)
- [任务清单](docs/TaskList.md)

## License

当前项目尚未指定开源许可证。如需公开发布，建议根据项目用途补充 LICENSE 文件。
