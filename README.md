# stage1st-reader

![Build](https://github.com/reed-overflow/stage1st-reader/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/19768-stage1st-reader.svg)](https://plugins.jetbrains.com/plugin/19768-stage1st-reader)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/19768-stage1st-reader.svg)](https://plugins.jetbrains.com/plugin/19768-stage1st-reader)


<!-- Plugin description -->
A configurable Discuz client for https://stage1st.com/2b/ inside IntelliJ IDEA.
Includes text browsing, account/Cookie login, posting and replies, local reading bookmarks,
and discreet tool windows.
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "stage1st-reader"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/reed-overflow/stage1st-reader/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


## Configuration

Set the forum root URL under <kbd>Settings/Preferences</kbd> > <kbd>Tools</kbd> >
<kbd>Stage1st Reader</kbd>. The value is shared by all projects and is used by
all browsing, login, and posting requests. Default: `https://stage1st.com/2b/`.

## 使用

- 在 **Settings / Preferences → Tools → Stage1st Reader** 配置论坛根地址。保留 `/2b/`，不要填写 `forum.php`、查询参数或用户名密码。更换地址会清除当前会话及打开的阅读内容。
- 打开 **S1 Selector** 版块窗口，双击版块即可打开阅读窗口；选择主题阅读。伪装模式开启时窗口标题为 **Index / Output**。
- 版块工具栏提供 **登录 / 切换账号、退出登录、设置**。账号登录支持用户名、UID、邮箱、手机号（是否可用由论坛决定）和安全提问；登录框中可查看当前账号。
- 如果要求验证码、二次验证或出现风控，在登录框的 **浏览器 Cookie** 页打开论坛并自行完成登录。浏览器开发者工具 **Network → 当前论坛请求 → Request Headers → Cookie** 中复制完整的单行值，然后导入。不要使用 `document.cookie`，它无法读取 HttpOnly 登录 Cookie。
- 登录成功或导入 Cookie 后，账号信息和 Cookie 自动保存在本机 IDE 设置 `stage1st-reader.xml` 中，所有项目共用，重启 IDE 后恢复。Cookie 保留原有效期，过期后需重新登录；密码和安全提问答案不保存。退出登录或更换论坛地址会清除保存的会话，不会注销系统浏览器。切换账号后重新选择版块即可浏览相应权限下的内容。
- 阅读工具栏支持刷新、主题 ID 打开、主题／正文翻页及页码跳转、当前页标题／作者筛选、当前页正文查找。
- **发帖** 会读取当前版块的主题分类，必选分类不能遗漏；**回复** 面向当前主题；在正文某楼点击后使用 **引用**，会生成含原楼链接的 BBCode 引用。
- 编辑器明确显示目标站点和主题，只有点击 **发布到论坛** 才发送。失败时保留编辑器中的草稿；超时后请先刷新或在浏览器核对，避免重复发布。待审核内容会单独提示。
- **书签** 保存／打开／删除当前主题和页码，按论坛地址隔离，保存到本机 IDE 设置。正文和图片不缓存；图片、表情、链接以文字及地址显示，附件可从浏览器查看。
- 伪装模式提供中性窗口名和等宽纯文本；**Ctrl+Alt+Shift+F12** 一键隐藏两个窗口，可在 IDEA Keymap 搜索 **Hide Reader Windows** 修改快捷键。

## 范围与验证

目前实现常规浏览、账号会话、普通主题、回复、BBCode 引用和本地书签。
附件上传、投票创建、私信、评分、服务端收藏及验证码交互尚未在插件内实现，可通过浏览器处理。
引用采用普通 BBCode 和原楼链接，不承诺触发 Discuz 原生引用通知。搜索范围是当前页。
登录权限、发帖间隔、版块维护、审核和风控均遵循服务器返回结果。

## 构建与依赖维护

安装 JDK 17（推荐 Temurin），将 IDEA 的 **Gradle JVM** 和命令行的 `JAVA_HOME` 设为 JDK 17，使用仓库自带的 Wrapper：

```sh
./gradlew --no-daemon build verifyPlugin
```

Windows 使用 `.\gradlew.bat`。`build` 执行编译、插桩、测试和打包，产物位于
`build/distributions/`；`verifyPlugin` 检查 IDE 兼容性。`runIde` 启动带插件的开发 IDE。

构建使用 IntelliJ Platform Gradle Plugin **2.13.1**、Gradle **9.8.0** 和 Java **17**。
`gradle/gradle-daemon-jvm.properties` 将构建进程固定为 **Temurin 17**，未安装时由 Gradle 自动下载，
避免 Ant 对 Microsoft JDK 的错误识别。此配置优先于本机选择的 Gradle JVM，不包含机器专属路径。
以 IDEA **2022.3（223）** 为编译基线，不设置版本上限。IDE 和测试运行时由 2.x 插件管理，
不再需要 Java 11、手工 JDK 类归档或 Ant 插桩补丁。开发 IDE、运行时和依赖首次构建时自动下载。

`verifyPlugin` 复用最低版本 SDK，并检查 `pluginVerifierIdeVersions` 指定的 **2025.2.6.3 社区版**。
兼容性错误、无效插件和缺失依赖会阻止发布；这些检查不代表已经验证所有中间及未来版本。

Dependabot 每月检查更新。构建插件 **2.14+ 要求最低 IDEA 2023.3**，因此暂限在 2.13.x；
提高构建插件版本前需先评估最低 IDE 支持范围。更新 Wrapper 时同步更新 `gradleVersion`、
官方 SHA-256 校验值及 Wrapper 文件。签名和发布继续使用现有环境变量。

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
