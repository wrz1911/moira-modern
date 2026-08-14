# moira-modern（运行环境要求 JDK 21+）

七政四余排盘软件 Moira 的现代化升级版，基于 [moira_macOS](https://github.com/xdhuangsidi/moira_macOS) 升级而来。
使用 IntelliJ IDEA 打开即可运行调试（Linux / macOS / Windows 均可）。

## 原作者与项目沿革

**Moira 原作者：林国清**，2004 年起开发 Moira 免费排盘程序，专为七政四余（果老星宗）推演设计，
持续更新至 2015 年左右；**原工程文件已丢失**。本工程来源于后来流传的 macOS 工程
（[moira_macOS](https://github.com/xdhuangsidi/moira_macOS)）。

平台沿革：最初仅 **Windows** → 后来 **macOS** → 现在（本工程）首次移植 **Linux**。
本项目自始至终未在 Linux 上调试过，原代码按 Windows/macOS 控件行为编写；
本仓库包含的 GTK/Linux 平台适配修复均属首次移植工作。
谨此向前辈致敬，其毕生心血让七政四余（果老星宗）的推演得以在计算机上延续。

## 升级内容（相对 moira_macOS）

- **JDK 11 → 21+**（需 JDK 21 或更高版本，SWT 4.39+ 的硬性要求）
- **SWT 4.20 → 4.40+**（Linux 本机现用 3.135.0，2026-08-12 构建，含 GTK4 popover 闪退修复；
  2026-09 SWT 4.41 正式版发布后可换正式 jar）
- **JFace 3.22.200 → 3.39.100**、**equinox.common 3.15.0 → 3.20.400**、**core.commands 3.10.0 → 3.12.500**
  （注意：core.commands 是 JFace 的**运行期依赖**，不可删除）
- 删除死代码：`moiraApplet` 包（Applet API 已于 JDK 17 移除）、`awtext` 包（Swing 遗留）
- 源码统一为 UTF-8 编码（SweConst.java / Swecl.java 由 ISO-8859-1 转码）

## 依赖 jar（lib/ 目录）

| 文件 | 说明 |
|---|---|
| `swt.jar` | SWT 3.135.0 **GTK/Linux x86_64 版**（本机运行用，内含 GTK3/GTK4 双原生库，设 `SWT_GTK4=1` 后优先 GTK4，否则默认 GTK3） |
| `macos/swt-macos-aarch64-3.134.0.jar` | SWT 3.134.0 macOS ARM 版（macOS 交付用，仍为 3.134；GTK 修复仅影响 Linux） |
| `org.eclipse.jface_3.39.100.jar` | JFace |
| `org.eclipse.equinox.common_3.20.400.jar` | equinox common |
| `org.eclipse.core.commands_3.12.500.jar` | core.commands（JFace 运行期依赖） |
| `jfreesvg-3.4.4.jar` | JFreeSVG 3.4.4（BSD），星盘「保存图片」新增 **SVG 矢量导出**（复用 pageDiagram 绘制管线，`ImageControl.captureSVG`） |

SWT jar 按操作系统 + 架构分版（内含原生库），Windows 下需换用 `org.eclipse.swt.win32.win32.x86_64`（3.134.0 及以上），
并安装 WebView2 Runtime（Win11 基本预装）。

## 数据文件（星历 / 星表 / 地磁）

- **瑞士星历（`ephe/*.se1`）**：Swiss Ephemeris 官方 2026-05-26 版，格式 3（文件头 `SWISSEPH 3`），
  新增 BCE 段，覆盖 -5400 ~ 5400 年（TestRange 实测全部通过）。600 年一段：
  正年文件带下划线（如 `sepl_00.se1`），BCE 段无下划线（如 `seplm06.se1`，编号 = ceil(|year|/100)）。
- **固定恒星表（`ephe/fixstars.cat`）**：与星历同源同版（含 1000+ 恒星，供 `SwissEph.getFixStar` 使用）。
- **地磁模型（`WMM2005.COF` / `WMM2010.COF` / `WMM2025.COF`）**：WMM2025（2024-11-13 发布）已就位，
  三个版本共存，Geomag 探测从最新年份倒序匹配，2025 优先。
- **下载与校验**：`ephe_url` 指向 GitHub 官方源，缺文件时程序按需下载（`Calculate.java:1525 loadEphIndex`）；
  文件经 jsDelivr CDN 获取（本机直连 GitHub raw 极慢），以 GitHub API size 与 SWISSEPH 文件头双重校验。

## 对 moira 源码的修改点

1. **恢复 `DrawSWT.java` 的 `drawMarkerLines` 调用**：原版因 macOS SWT 旧版 bug 注释掉了角距标注线，
   导致（选项（P）-> 选择角距显示(O)...）设置无法生效。本次在 SWT 4.40 下恢复该功能，
   并对 `initMarker` / `drawMarkerLines` 的数组访问做了越界防御
   （`angle_marker_color` / `angle_marker_display` 为偏好配置，长度不受控）。
2. 删除 `moiraApplet` 与 `awtext` 死代码包。
3. `SweConst.java` / `Swecl.java` 转码为 UTF-8。
4. **排盘画面抗锯齿**：排盘图由 Java2D（`Graphics2D` + `BufferedImage`）离屏渲染，
   原代码未设置任何渲染提示，Linux 下文字与线条锯齿明显。已在
   `ChartTab.showDiagram` 与 `DrawAWT.init` 中开启
   `KEY_ANTIALIASING` / `KEY_TEXT_ANTIALIASING`，并在 `run.sh` 中加
   `-Dawt.useSystemAAFontSettings=lcd` 强制文本次像素渲染。
5. **星盘字体改为各平台原生字体**：不再指定特定字体（如宋体），由 SWT 在
   Linux（GTK）/ macOS / Windows 上自行解析系统字体，各平台显示均正常。
6. **悬停解释框定位校准**（`HoverTipSWT.java`）：GTK4 下 ON_TOP shell 是 GtkPopover，
   SWT 的 `toDisplay` 返回记录值无法实测真实落点。修复：`positionPopover` 用 height=0
   的 pointing rect 让 GTK 自动定位；显示后经 FFM 直调 `gdk_popup_get_position_x/y`
   （`Gtk4SurfacePos.java`）读 popover surface 真实 origin，与期望位置求差得 gap 并
   累加补偿（只校准一次，offset 下次显示生效），实测弹窗紧贴指针 +16px。
7. **TableTab 列表尺寸估算改用 `getSize`**：GTK4 下 `getClientArea` 会触发
   `forceResize` 反馈环，引起 resize 风暴。`getNumVisibleRow` 改用 `getSize`
   估算，不再调用 `getClientArea`。

## 编译与运行

```bash
javac -encoding UTF-8 -d out -cp "lib/swt.jar:lib/org.eclipse.jface_3.39.100.jar:lib/org.eclipse.equinox.common_3.20.400.jar:lib/org.eclipse.core.commands_3.12.500.jar" $(find src -name '*.java')
SWT_GTK4=1 java --enable-native-access=ALL-UNNAMED -Dorg.eclipse.swt.internal.gtk.cssFile=moira-gtk.css -cp "out:lib/swt.jar:lib/org.eclipse.jface_3.39.100.jar:lib/org.eclipse.equinox.common_3.20.400.jar:lib/org.eclipse.core.commands_3.12.500.jar:src" org.athomeprojects.moira.Moira
```

Linux 系统依赖（Arch / CachyOS 包名）：**gtk4** 与 **webkitgtk-6.0**（Browser 控件用，SWT 按需 dlopen 探测）。
GTK 版本由环境变量 `SWT_GTK4` 控制：swt.jar 3.135 同时内含 GTK3 / GTK4 原生库，SWT **默认加载 GTK3（pi3）**；
设 `SWT_GTK4=1`（run.sh 已内置）则优先加载 GTK4（pi4），GTK4 加载失败自动回退 GTK3。
本机（CachyOS）已实测 GTK4 运行（/proc/PID/maps 确认加载 libgtk-4.so.1）。

## GTK4 兼容性验证（2026-08-13）

- swt.jar 3.135.0（2026-08-12 构建）已修复 SWT 上游 GTK4 移植的两个闪退 bug（菜单 Popover
  销毁后仍被访问、`gtk_widget_destroy` 的 use-after-free），这两个 bug 在 3.134.0 上可稳定复现崩溃。
- 运行日志中偶发的 `Gtk-CRITICAL` 断言警告（如启动期 7 次
  `gtk_tree_view_scroll_to_cell: 'tree_view->priv->tree != NULL'`）为 SWT 上游 GTK4 移植遗留：
  失败仅打印并安全返回，不影响功能；实测正常操作全程无断言。
- 建议 2026-09 SWT 4.41 正式版发布后升级 lib/swt.jar，届时上述上游遗留应已修复。

## Linux 工具栏布局（2026-08-14）

- SWT 3.135 GTK4 的 CTabFolder topRight 布局存在缺陷（内部 SwtFixed 被分配 -1 尺寸，
  工具栏被布局到窗口外，表现为「点一下 tab 才出现」）；GTK3 亦有同类问题。
- 修复方案：工具栏不再挂 CTabFolder 的 topRight，改为**窗口内独立一行**（菜单栏下方、
  tab 栏上方，`Moira.java` 的 content/toolbar_row 布局）。
- 同步去掉了 CoolBar 层（拖拽/换行/显示不全的根源）与「操作帮助」按钮，
  4 组图标（文件/编辑/选项/查找）由 RowLayout 固定单行排列，不可拖动、始终完整显示。

## 运行调试截图

![Alt](https://raw.githubusercontent.com/xdhuangsidi/moira_macOS/master/screenshot.png)
