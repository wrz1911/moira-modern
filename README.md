# moira-modern

七政四余（果老星宗）排盘软件 **Moira** 的现代化升级版。

- 需求:**JDK 22+**(推荐 26;FFM 原生接口转正版本)| 平台:Linux / macOS / Windows
- 来源:原工程(2004-2015,作者林国清)已丢失,本工程基于流传的 [moira_macOS](https://github.com/tutorial0/moira_macOS) 升级,移植到**现代 GTK4 / SWT 技术栈**

> ⚠️ **项目状态**:仍在持续完善中。当前放出代码**基本可用,但 bug 还不少**
> (以 Linux/GTK4 为主,悬停弹窗、标记线、窗口缩放等细节仍在打磨);
> 欢迎测试与反馈,请谨慎用于正式排盘场景。

## 主要改进

**环境现代化**
- JDK 11 → 21+,SWT / JFace / equinox / core.commands 全套升级
- 删除死代码(`moiraApplet`、`awtext`),源码统一 UTF-8

**现代 GTK4 技术栈**
- 运行于 GTK4(SWT_GTK4=1,失败自动回退 GTK3),悬停弹窗、角度标记线、工具栏等控件全部按 GTK4 适配
- 管理页改用现代 Master-Detail 布局

**界面现代化**
- HiDPI 自动适配:1080p/2K/4K 与 KDE/GNOME 分数缩放,窗口按屏幕比例、盘面按物理像素渲染
- 现代明亮配色、计算/八字数据自动着色(干支五行色、十神分类色)
- 星盘新增 **SVG 矢量导出**(保存图片对话框选 *.svg)

## 快速开始

Linux(Arch / CachyOS)依赖:

```bash
sudo pacman -S gtk4 webkitgtk-6.0
```

运行:

```bash
./run.sh   # 自动编译并启动,内置 SWT_GTK4=1 优先 GTK4(失败自动回退 GTK3)
```

IDE:IntelliJ IDEA 打开即可运行调试。

## 开发环境(本机)

- **操作系统**:Arch Linux(CachyOS),内核 7.1.8-1-cachyos
- **桌面环境**:KDE Plasma 6.7.4(Wayland 会话,程序经 XWayland 运行)
- **JDK**:OpenJDK 26.0.2(项目要求 21+)
- **SWT**:3.135(swt.jar,GTK3/GTK4 双原生库,默认 `SWT_GTK4=1` 优先 GTK4)
- **GTK 库**:GTK4 4.22.4 / GTK3 3.24.52 / webkitgtk-6.0 2.52.5
- **显示**:1920×1200 @ 125% 分数缩放(HiDPI 自动适配的实测环境)

> 以上为本机开发验证环境;其他 Linux 发行版安装 `gtk4` 与 `webkitgtk-6.0` 即可运行。

## 数据文件

| 文件 | 说明 |
|---|---|
| `ephe/*.se1` | Swiss Ephemeris 2026-05-26 官方版,覆盖 -5400 ~ 5400 年 |
| `ephe/fixstars.cat` | 固定恒星表(与星历同源) |
| `WMM2005/2010/2025.COF` | 地磁模型,自动选用最新(2025) |

缺文件时程序按需自动下载(`ephe_url` 指向 GitHub 官方源)。

## 依赖 jar(lib/)

| 文件 | 说明 |
|---|---|
| `swt.jar` | SWT 3.135 **Linux x86_64**(GTK3/GTK4 双原生库) |
| `macos/swt-macos-aarch64-3.134.0.jar` | macOS ARM 版 |
| `org.eclipse.jface_3.39.100.jar` 等 | JFace 及其运行期依赖 |
| `jfreesvg-3.4.4.jar` | SVG 导出(BSD) |

其他平台:Windows 需换用 `org.eclipse.swt.win32.win32.x86_64` 并安装 WebView2 Runtime(Win11 基本预装)。

## 致谢

向原作者**林国清**致敬,其毕生心血让七政四余(果老星宗)的推演得以在计算机上延续。
