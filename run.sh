#!/usr/bin/env bash
# Moira 一键启动脚本(Linux GTK 版)
# 首次运行会自动编译;后续直接启动
# GTK 版本:swt.jar 3.135 内含 GTK3/GTK4 双原生库,但 SWT 默认先加载 GTK3(pi3);
# 设环境变量 SWT_GTK4=1 强制先加载 GTK4(pi4),GTK4 加载失败自动回退 GTK3
cd "$(dirname "$0")"

CP="out:lib/swt.jar:lib/org.eclipse.jface_3.39.100.jar:lib/org.eclipse.equinox.common_3.20.400.jar:lib/org.eclipse.core.commands_3.12.500.jar:lib/jfreesvg-3.4.4.jar:src"

# out 目录没有 class 时先编译
if [ ! -d out ] || [ -z "$(find out -name '*.class' | head -1)" ]; then
    echo "首次运行,正在编译..."
    javac -encoding UTF-8 -d out -cp "$CP" $(find src -name '*.java') || exit 1
    echo "编译完成"
fi

export SWT_GTK4=1
exec java --enable-native-access=ALL-UNNAMED -Dorg.eclipse.swt.internal.gtk.cssFile=moira-gtk.css -cp "$CP" org.athomeprojects.moira.Moira "$@"
