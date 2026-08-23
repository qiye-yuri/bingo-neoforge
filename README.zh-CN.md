# Neo Bingo

[English](README.md)

Neo Bingo 是一个全新且独立实现的 NeoForge 模组，依据公开的行为规格实现多人团队宾果游戏模式。

当前状态：正在开发首个可玩的服务端功能切片，已提供队伍大厅、5×5 标准、锁定及隐藏模式宾果卡、世界存档持久化和基础命令。

## 目标环境

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- 支持专用服务器和集成服务器
- 在可行范围内支持原版客户端；可选的 NeoForge 客户端模组提供额外界面
- 提供英文和简体中文命令反馈

## 构建

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
- `/neobingo start [seed]`：开始一局 5×5 标准模式游戏，仅管理员可用。
- `/neobingo start <standard|lockout|hidden> [seed]`：明确选择标准、锁定或隐藏模式并开始游戏，仅管理员可用。
- `/neobingo reroll [seed]`：保留队伍和模式并重新生成正在运行的卡片，仅管理员可用。
- `/neobingo card`：在原版聊天栏中显示所在队伍的宾果卡。
- `/neobingo claim`：立即检查玩家的服务端物品栏，并认领其中匹配的目标。
- `/neobingo status`：显示当前阶段、模式、种子、队伍及胜者。
- `/neobingo end`：结束正在运行的游戏，仅管理员可用。

游戏结束后，首位执行加入命令的玩家会创建一个新大厅。
游戏进行期间，服务器还会每秒检查一次在线队员的物品栏，并自动认领新出现的匹配目标。

## 独立实现规则

请勿复制或机械翻译 Yet Another Bingo 的源码、资源、翻译、界面布局、名称或文档。贡献代码前请阅读 [`docs/CLEAN_ROOM.zh-CN.md`](docs/CLEAN_ROOM.zh-CN.md)。

本项目与 HorrificDev 无关联，也未获得其认可。“Yet Another Bingo”仅用于标识独立实现记录中讨论的兼容性参考对象。

## 路线图

请参阅 [`docs/ROADMAP.zh-CN.md`](docs/ROADMAP.zh-CN.md) 和 [`docs/FUNCTIONAL_SPEC.zh-CN.md`](docs/FUNCTIONAL_SPEC.zh-CN.md)。

数据包作者还应阅读 [`docs/CARD_DEFINITIONS.zh-CN.md`](docs/CARD_DEFINITIONS.zh-CN.md)。

## 许可证

项目原创代码采用 MIT License。第三方组件继续适用其各自的许可证。若中文说明与英文许可证正文存在冲突，以英文许可证正文为准。
