#!/usr/bin/env bash
# 郑氏星案回归测试:无头运行 40 案排盘计算
# ①快照回归(test/baseline/cheng_s.txt,首次运行自动生成)
# ②override 校准自洽 ③基础 sanity(罗计对宫/命度范围)
cd "$(dirname "$0")/.."
CP="out:src:lib/swt.jar:lib/junit-platform-console-standalone-1.11.4.jar"
mkdir -p test_out
javac -encoding UTF-8 -cp "$CP" -d test_out test/org/athomeprojects/moira/ChartDataRegressionTest.java || exit 1
java -Djava.awt.headless=true -jar lib/junit-platform-console-standalone-1.11.4.jar \
    execute --class-path "test_out:out:src:lib/swt.jar" \
    --select-class org.athomeprojects.moira.ChartDataRegressionTest
