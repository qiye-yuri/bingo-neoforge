# Neo Bingo

[English](README.md)

Neo Bingo 是一个全新且独立实现的 NeoForge 模组，依据公开的行为规格实现多人团队 Bingo 游戏模式。

当前状态：正在开发首个可玩的服务端功能切片，已提供队伍大厅、5×5 标准、锁定及隐藏模式 Bingo 卡、世界存档持久化和基础命令。

## 目标环境

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- 支持专用服务器和集成服务器
- 在可行范围内支持原版客户端；可选的 NeoForge 客户端模组提供额外界面
- 提供英文和简体中文命令反馈

安装模组的增强客户端使用可选且带版本号的 `neo_bingo:protocol_version` 通道。服务端不强制要求该通道，因此原版客户端仍可加入。

增强客户端会在右上角显示紧凑的 5×5 图标卡。按 `B` 打开带队伍主题色、完成状态、隐藏状态、物品图标和悬停说明的完整卡片，按 `H` 可隐藏或恢复 HUD。HUD 以 1920×1080 为默认参考并随 GUI 缩放；按 `P` 在四个角落间切换位置，按 `[` / `]` 以 10% 为步长在 50%～200% 之间调整大小。设置会保存到客户端配置。

HUD 与完整卡片会显示当前队伍得分；排位模式还会显示由服务端每秒同步的剩余时间。

## 安装与首次开局

1. 为 Minecraft 1.21.1 安装 NeoForge 21.1.248，并使用 Java 21 启动服务端。
2. 将构建生成的 `build/libs/neo_bingo-<版本>.jar` 放入服务端的 `mods` 文件夹。
3. 客户端可以不安装模组并通过聊天栏游玩；若需要 HUD、完整卡片和物品图标，则将同一个 JAR 放入客户端 `mods` 文件夹。
4. 启动服务端后，玩家使用 `/neobingo join <team>` 加入队伍。
5. 已加入队伍的玩家或管理员使用 `/neobingo start` 开始标准模式，也可使用下方命令选择其他模式。

玩家登录时会获得一本原版可交互的 `Bingo 设置书`，可在书中点击选择队伍、游戏模式和 MAX 至 D 难度。书内也集成了随机分队、指定玩家分队、移出大厅、重新生成卡片和结束游戏等管理员入口；需要参数或确认的命令会先填入聊天栏。背包中已有设置书时不会重复发放，旧版书会自动更新，丢失后可重新登录或使用 `/neobingo book` 补领。

服务端成功加载时，日志会显示 `Neo Bingo initialized` 和已加载的 5×5 Bingo 卡定义。

## 构建

### Windows 一键构建

安装 Java 21 后，直接双击仓库根目录的 [`构建模组.bat`](构建模组.bat)。脚本会完成以下操作：

1. 检查当前 `java` 是否为 Java 21。
2. 使用仓库自带的 Gradle Wrapper 执行完整构建和单元测试。
3. 验证 JAR 内含 NeoForge 模组元数据。
4. 将可安装 JAR 复制到 `dist`，并生成对应的 `.sha256` 校验文件。

构建成功后，把 `dist/neo_bingo-<版本>.jar` 放入 Minecraft 1.21.1 NeoForge 21.1.248 实例的 `mods` 文件夹。首次构建需要联网下载依赖，后续构建会复用 Gradle 缓存。

也可以在 PowerShell 中运行同一脚本：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-jar.ps1
```

### 常规构建

```text
./gradlew build
```

首次构建会下载 Minecraft 和 NeoForge 依赖。

使用以下命令运行包含模组加载器和真实世界环境的集成测试：

```text
./gradlew runGameTestServer
```

## 命令

- `/neobingo join <team>`：在大厅开放期间加入或更换队伍。
- `/neobingo leave`：在大厅开放期间离开当前队伍。
- `/neobingo book`：补领一本可点击选择队伍、模式和难度的 Bingo 设置书。
- `/neobingo randomteams [队伍数]`：将已加入大厅的玩家随机且尽量均衡地分为 2～8 队，默认 2 队，仅管理员可用。
- `/neobingo team assign <玩家> <队伍>`：将指定在线玩家分配到队伍，仅管理员可在大厅阶段使用。
- `/neobingo team remove <玩家>`：将指定在线玩家移出 Bingo 大厅，仅管理员可用。
- `/neobingo start [seed]`：开始一局 5×5 标准模式游戏，已加入队伍的玩家或管理员可用。
- `/neobingo start <standard|lockout|hidden> [seed]`：明确选择标准、锁定或隐藏模式并开始游戏。
- `/neobingo start ranked <秒数> [种子]`：开始按得分排名的限时游戏。
- `/neobingo reroll [seed]`：保留队伍和模式并重新生成正在运行的卡片，仅管理员可用。
- `/neobingo card`：在原版聊天栏中显示所在队伍的 Bingo 卡。
- `/neobingo claim`：立即检查玩家的服务端物品栏，并认领其中匹配的目标。
- `/neobingo status`：显示当前阶段、模式、种子、队伍及胜者。
- `/neobingo end`：结束正在运行的游戏，仅管理员可用。

游戏结束后，首位执行加入命令的玩家会创建一个新大厅。
游戏进行期间，服务器还会每秒检查一次在线队员的物品栏，并自动认领新出现的匹配目标。

## 路线图

请参阅 [`docs/ROADMAP.zh-CN.md`](docs/ROADMAP.zh-CN.md) 和 [`docs/FUNCTIONAL_SPEC.zh-CN.md`](docs/FUNCTIONAL_SPEC.zh-CN.md)。

数据包作者还应阅读 [`docs/CARD_DEFINITIONS.zh-CN.md`](docs/CARD_DEFINITIONS.zh-CN.md)。

## 许可证

项目原创代码采用 MIT License。第三方组件继续适用其各自的许可证。若中文说明与英文许可证正文存在冲突，以英文许可证正文为准。
