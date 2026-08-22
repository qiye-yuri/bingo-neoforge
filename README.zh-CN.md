# Neo Bingo

[English](README.md)

Neo Bingo 是一个全新且独立实现的 NeoForge 模组，依据公开的行为规格实现多人团队宾果游戏模式。

当前状态：正在开发首个可玩的服务端功能切片，已提供队伍大厅、5×5 标准宾果卡、世界存档持久化和基础命令。

## 目标环境

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- 支持专用服务器和集成服务器
- 在可行范围内支持原版客户端；可选的 NeoForge 客户端模组提供额外界面

## 构建

```text
./gradlew build
```

首次构建会下载 Minecraft 和 NeoForge 依赖。

## 命令

- `/neobingo join <team>`：在大厅开放期间加入或更换队伍。
- `/neobingo start [seed]`：开始一局 5×5 标准模式游戏，仅管理员可用。
- `/neobingo card`：在原版聊天栏中显示所在队伍的宾果卡。
- `/neobingo end`：结束正在运行的游戏，仅管理员可用。

游戏结束后，首位执行加入命令的玩家会创建一个新大厅。

## 独立实现规则

请勿复制或机械翻译 Yet Another Bingo 的源码、资源、翻译、界面布局、名称或文档。贡献代码前请阅读 [`docs/CLEAN_ROOM.zh-CN.md`](docs/CLEAN_ROOM.zh-CN.md)。

本项目与 HorrificDev 无关联，也未获得其认可。“Yet Another Bingo”仅用于标识独立实现记录中讨论的兼容性参考对象。

## 路线图

请参阅 [`docs/ROADMAP.zh-CN.md`](docs/ROADMAP.zh-CN.md) 和 [`docs/FUNCTIONAL_SPEC.zh-CN.md`](docs/FUNCTIONAL_SPEC.zh-CN.md)。

## 许可证

项目原创代码采用 MIT License。第三方组件继续适用其各自的许可证。若中文说明与英文许可证正文存在冲突，以英文许可证正文为准。
