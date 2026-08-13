# moira-modern（运行环境要求 JDK 21+）

七政四余排盘软件 Moira 的现代化升级版，基于 [moira_macOS](https://github.com/xdhuangsidi/moira_macOS) 升级而来。
使用 IntelliJ IDEA 打开即可运行调试（Linux / macOS / Windows 均可）。

## 升级内容（相对 moira_macOS）

- **JDK 11 → 21+**（需 JDK 21 或更高版本，SWT 4.39+ 的硬性要求）
- **SWT 4.20 → 4.40**（3.134.0，2026-06 GA）
- **JFace 3.22.200 → 3.39.100**、**equinox.common 3.15.0 → 3.20.400**、**core.commands 3.10.0 → 3.12.500**
  （注意：core.commands 是 JFace 的**运行期依赖**，不可删除）
- 删除死代码：`moiraApplet` 包（Applet API 已于 JDK 17 移除）、`awtext` 包（Swing 遗留）
- 源码统一为 UTF-8 编码（SweConst.java / Swecl.java 由 ISO-8859-1 转码）

## 依赖 jar（lib/ 目录）

| 文件 | 说明 |
|---|---|
| `swt.jar` | SWT 3.134.0 **GTK/Linux x86_64 版**（本机运行用） |
| `macos/swt-macos-aarch64-3.134.0.jar` | SWT 3.134.0 macOS ARM 版（macOS 交付用） |
| `org.eclipse.jface_3.39.100.jar` | JFace |
| `org.eclipse.equinox.common_3.20.400.jar` | equinox common |
| `org.eclipse.core.commands_3.12.500.jar` | core.commands（JFace 运行期依赖） |

SWT jar 按操作系统 + 架构分版（内含原生库），Windows 下需换用 `org.eclipse.swt.win32.win32.x86_64:3.134.0`，
并安装 WebView2 Runtime（Win11 基本预装）。

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

## 编译与运行

```bash
javac -encoding UTF-8 -d out -cp "lib/swt.jar:lib/org.eclipse.jface_3.39.100.jar:lib/org.eclipse.equinox.common_3.20.400.jar:lib/org.eclipse.core.commands_3.12.500.jar" $(find src -name '*.java')
java --enable-native-access=ALL-UNNAMED -cp "out:lib/swt.jar:lib/org.eclipse.jface_3.39.100.jar:lib/org.eclipse.equinox.common_3.20.400.jar:lib/org.eclipse.core.commands_3.12.500.jar:src" org.athomeprojects.moira.Moira
```

Linux 运行需 GTK3 与 webkit2gtk-4.1（Browser 控件用）。

## 运行调试截图

![Alt](https://raw.githubusercontent.com/xdhuangsidi/moira_macOS/master/screenshot.png)
