# waxx-english · 英语学习卡片 PDF 生成器

[![Release](https://img.shields.io/github/v/release/liangleionline/waxx-english?label=version&color=1F6FEB)](https://github.com/liangleionline/waxx-english/releases)
[![Downloads](https://img.shields.io/github/downloads/liangleionline/waxx-english/total?label=downloads&color=2EA043)](https://github.com/liangleionline/waxx-english/releases)
[![Latest APK](https://img.shields.io/github/downloads/liangleionline/waxx-english/latest/total?label=latest%20APK&color=2EA043)](https://github.com/liangleionline/waxx-english/releases/latest)
[![Last commit](https://img.shields.io/github/last-commit/liangleionline/waxx-english)](https://github.com/liangleionline/waxx-english/commits/main)
[![Repo size](https://img.shields.io/github/repo-size/liangleionline/waxx-english)](https://github.com/liangleionline/waxx-english)
[![Stars](https://img.shields.io/github/stars/liangleionline/waxx-english?style=social)](https://github.com/liangleionline/waxx-english/stargazers)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](#)
[![minSdk](https://img.shields.io/badge/minSdk-24-blue)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](#)

一款纯离线 Android App：把「英文 / 中文翻译」成对录入，一键生成 **A4 纸 4×4 共 16 格、无外边框、可裁剪**的单词卡片 PDF，并可通过系统分享直接发到微信。

- 包名：`com.waxx.englishcard`
- 版本：1.0.3（versionCode 4）
- 最低支持：Android 7.0（API 24），targetSdk 34

---

## 一、功能清单

### 1. 启动
- 打开 App **直接进入左右两栏卡片页**，不再有「创建卡片」首页。
- 首次安装时页面是空的，直接点底部【新增】录入即可；之后启动自动带出上次保存的全部内容。

### 2. 卡片展示页（左右分栏，只读）
- 左侧英文、右侧中文，逐行一一对应，**两个区域只读，不能直接打字**。
- 左右两栏放在同一个滚动容器内：滑动左侧，右侧同步跟随；滑动右侧，左侧同步跟随，永远保持同行对齐。
- 顶部显示「共 N 张卡片 · 每页 K 张 · M 页 PDF」，右上角为【清空】和【设置】。
- 底部固定两个按钮：**【新增】**、**【保存并生成 PDF】**。

### 3. 新增
- 点击【新增】弹出多行输入框，规则：**奇数行英文、偶数行中文**，可一次批量录入多组：
  ```
  apple
  苹果
  banana
  香蕉
  ```
- 保存时自动两两配对，追加到列表；**只校验英文重复**（重复英文自动跳过），中文允许重复。
- 行数为奇数时，最后一条没有中文的英文会被忽略并提示。

### 4. 修改（单击某一行）
- 单击左栏或右栏任意一行，弹出输入框，自动回填该行「第一行英文 / 第二行中文」。
- 修改后保存：校验英文不能与其它行重复，通过后**原位替换**。

### 5. 删除（长按某一行）
- 长按任意一行，弹出确认框「是否删除该卡片？」。
- 确定后**左右两栏同行内容一起删除**，其余内容自动上移。

### 6. 保存并生成 PDF
- 校验：只校验英文重复；存在重复英文时提示并终止。
- 校验通过后先把数据持久化，再生成 PDF，成功弹出提示：
  - 标题：**单词卡片已生成**
  - 按钮：左【确定】直接关闭弹窗；右【分享】保存文件并调起系统分享。

### 7. PDF 规格
- A4 纸张（1240×1754 px @150dpi）。
- 4 行 × 4 列 = **16 格**，单元格等大。
- 内部 3 条横线 + 3 条竖线，**纯黑实线，直接顶到纸张四边，整体没有外边框**，方便裁剪。
- 内容按从上到下、从左到右顺序填充，满 16 格自动分页。
- 两种排版方式，在【设置】页切换：
  - **英文、中文交替占格（默认）**：第 1 格英文、第 2 格中文、第 3 格英文、第 4 格中文……一格只放一段内容，整格水平+垂直居中。每页 16 格 = 8 张卡片。
  - **每格英文 + 中文**：一格一张卡片，英文在上半格、中文在下半格，各自居中。每页 16 格 = 16 张卡片。
- 长文本自动换行、按需缩小字号（54px 起逐级缩小），不溢出格子。
- **逗号自动换行**：单元格内容里出现中文逗号（，）或英文逗号（,）时，按逗号切分成多行显示，并且不显示逗号本身。例如 `a, an` 显示为两行 `a` / `an`；`你好，世界` 显示为 `你好` / `世界`。该处理只影响 PDF，App 界面仍显示原文。
- 文件名：`英语学习卡片_yyyyMMdd_HHmmss.pdf`。

### 8. 分享
- 点击【分享】时先把 PDF 复制到 **`Download/waxx/`**（目录不存在自动创建），再调起系统分享面板，可发给微信好友 / 文件传输助手等。

### 9. 崩溃日志（便于定位问题）
- App 内置全局异常捕获：任何未捕获异常都会自动生成日志文件
  - 文件：`Download/waxx_crash_yyyyMMdd_HHmmss.log`（Android 10+ 通过 MediaStore 写入，无需权限）
  - 内容：App 版本、设备型号、Android 版本、ABI、崩溃线程、完整堆栈。
- 下次启动会提示「检测到上次异常退出」，可点【查看日志】查看全文并一键分享该日志文件。
- 极端情况下写入 Download 失败时，日志会兜底保存在 App 私有目录，下次启动仍可查看。

### 9.1 设置页
- 入口：卡片页右上角【设置】。
- 选项：PDF 排版方式二选一（英文/中文交替占格 或 每格英文+中文），选中即保存，下次生成 PDF 生效；卡片数据不受影响。

### 9.2 数据导入 / 导出
- 入口：设置页「数据备份」。
- **导出数据**：把当前全部卡片写成 JSON，保存到 `Download/waxx/waxx_english_backup_时间戳.json`，并可直接通过系统分享发出去（换机、备份用）。
  - 文件结构：`{"app":"waxx-english","formatVersion":1,"exportedAt":"...","count":N,"cards":[{"en":"apple","zh":"苹果"}]}`
- **导入数据**：调系统文件选择器挑 JSON，解析后弹出二选一：
  - 覆盖现有数据（清空后导入）
  - 追加合并（保留现有卡片，自动跳过重复英文）
- 导入完成后返回卡片页会自动刷新列表。

### 10. 数据持久化
- 全部数据保存在本地 `SharedPreferences`（JSON），每次增删改即时落盘，重启 App 自动恢复。
- 全程离线运行，不需要网络权限。

---

## 二、构建方式

环境要求：JDK 17、Android SDK（platform 34 + build-tools 34.0.0）。

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`

或直接用 Android Studio 打开工程（Gradle 8.7 / AGP 8.4.1 / Kotlin 1.9.24）。

### 签名
release 使用仓库内 `keystore/waxx-release.jks`（alias `waxx`，口令 `waxx2026`）。
**请注意：该 keystore 已随仓库提交，仅用于个人自用 App 的稳定升级；如需公开发布请更换为你自己的私钥并加入 .gitignore。**

---

## 三、工程结构

```
app/src/main/java/com/waxx/englishcard/
├── MainActivity.kt        # 卡片页 + 新增/编辑/删除/生成弹窗
├── SettingsActivity.kt    # 设置页：PDF 排版方式
├── AppSettings.kt         # 设置持久化
├── CardStore.kt           # 数据模型与本地持久化、英文去重校验
├── CardPdfGenerator.kt    # A4 4×4 网格与文字居中排版
├── StorageHelper.kt       # 写入 Download（MediaStore / 传统 File 双兼容）
├── CrashLogger.kt         # 全局崩溃捕获与日志落盘
├── CrashDetailActivity.kt # 崩溃日志查看与分享
├── ShareHelper.kt         # FileProvider + 系统分享
└── WaxxApp.kt             # Application，安装崩溃捕获
app/src/main/assets/NotoSansSC-Regular.otf   # 内嵌中文字体，保证 PDF 中文不乱码
```

---

## 四、已知限制 / 待办

- 编辑弹窗对中文只按「第二行起合并为一段」处理，不支持中文内部换行。
- 暂不支持卡片排序、JSON 导入导出、PDF 预览。
- 未做深色模式适配。

---

## 五、访问与下载统计

顶部徽章里这几项是**实时**的（shields.io 直接读 GitHub 数据，刷新页面就会更新）：

| 徽章 | 含义 |
|---|---|
| `version` | 最新 Release 版本号 |
| `downloads` | 全部 Release 的 APK 累计下载次数 |
| `latest APK` | 只统计最新那个 Release 的下载次数 |
| `last-commit` / `repo-size` / `stars` | 最近提交时间 / 仓库体积 / Star 数 |

> shields.io 没有官方的「仓库访问量（views）」徽章，访问量和克隆量只能在 GitHub 后台或 API 里看。

### 当前快照（2026-11-10）

| 指标 | 数值 |
|---|---|
| 仓库访问量（近 14 天） | 0 次 / 0 独立访客 |
| 仓库克隆（近 14 天） | 0 次 / 0 独立克隆 |
| APK 累计下载 | 5 次 |
| ├ v1.0.3 | 2 次 |
| ├ v1.0.2 | 1 次 |
| └ v1.0.1 | 2 次 |

> Release 只保留最近 3 个版本；更早的 v1.0.0 已下线 Release（`v1.0.0` tag 仍保留，可随时从源码重建）。

> GitHub 的 Traffic 数据只保留**最近 14 天**的滚动窗口，且只对仓库有 push 权限的账号可见。

### 查看方式

1. **网页**：仓库 `Insights → Traffic` 看访问/克隆曲线；Release 页每个资产旁会显示 `Downloads` 次数。
2. **命令行**：仓库自带 `scripts/github_stats.sh`，一条命令打印访问、克隆、各版本下载量（需要 token，见脚本内说明）。

---

## 六、发版与版本保留策略

**规则：每次发版后只保留最新 3 个 Release，更早的 Release 删除。**

- 保留范围：最新的 3 个版本（含 APK 资产），例如当前为 v1.0.1 / v1.0.2 / v1.0.3。
- 删除范围：第 4 个及更早的 Release（连同其 APK 资产）。
- **tag 不删**：源码仍可从 tag 检出重建，避免历史版本彻底消失。
- 顺序：先删 Release 再考虑 tag，避免 Release 变成 `untagged-xxx` 残留。

发版后执行（默认只打印计划，加 `--apply` 才真删）：

```bash
GITHUB_TOKEN=xxx bash scripts/prune_releases.sh          # 预览
GITHUB_TOKEN=xxx bash scripts/prune_releases.sh --apply  # 执行
KEEP=3 为默认值，可用 KEEP=5 临时调整
```

> 注意：被删除 Release 的下载次数会从「累计下载」徽章里一起消失，这是正常现象，不代表数据丢失被统计过。
