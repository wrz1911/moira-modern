#!/usr/bin/env bash
# 增量编译:src 中有比 out 里对应 class 新的文件时重编译该文件;
# 输出最新 class 的 mtime 与 src 最新 mtime 比较,决定是否需要编译
cd "$(dirname "$0")/.."
CP="out:lib/swt.jar:lib/org.eclipse.jface_3.39.100.jar:lib/org.eclipse.equinox.common_3.20.400.jar:lib/org.eclipse.core.commands_3.12.500.jar:lib/jfreesvg-3.4.4.jar:src"
mkdir -p out

# 找出比对应 class 新的 java 文件(无 class 视为需编译)
STALE=$(find src -name '*.java' | while read j; do
    c="out/${j#src/}"
    c="${c%.java}.class"
    if [ ! -f "$c" ] || [ "$j" -nt "$c" ]; then
        echo "$j"
    fi
done)
if [ -z "$STALE" ]; then
    echo "无需编译"
    exit 0
fi
echo "编译 $(echo "$STALE" | wc -l) 个文件..."
# javac 编译 stale 文件;改动的符号引用由全量 classpath 解析,
# 若改到被广泛依赖的类(如 Resource),稳妥起见用全量编译兜底
echo "$STALE" | xargs javac -encoding UTF-8 -d out -cp "$CP" 2>&1 | grep -v "注:" | head -5
echo "编译完成"
