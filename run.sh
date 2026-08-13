#!/usr/bin/env bash
# Moira 一键启动脚本(Linux GTK 版)
# 首次运行会自动编译;后续直接启动
cd "$(dirname "$0")"

CP="out:lib/swt.jar:lib/org.eclipse.jface_3.39.100.jar:lib/org.eclipse.equinox.common_3.20.400.jar:lib/org.eclipse.core.commands_3.12.500.jar:src"

# out 目录没有 class 时先编译
if [ ! -d out ] || [ -z "$(find out -name '*.class' | head -1)" ]; then
    echo "首次运行,正在编译..."
    javac -encoding UTF-8 -d out -cp "$CP" $(find src -name '*.java') || exit 1
    echo "编译完成"
fi

exec java --enable-native-access=ALL-UNNAMED -Dorg.eclipse.swt.internal.gtk.cssFile=moira-gtk.css -cp "$CP" org.athomeprojects.moira.Moira "$@"
