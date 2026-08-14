//
// Moira - A Chinese Astrology Charting Program
// Copyright (C) 2004-2015 At Home Projects
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
package org.athomeprojects.moira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.athomeprojects.base.BaseTab;
import org.athomeprojects.base.ChartData;
import org.athomeprojects.base.City;
import org.athomeprojects.base.DataEntry;
import org.athomeprojects.base.DataSet;
import org.athomeprojects.base.DiagramTip;
import org.athomeprojects.base.FileIO;
import org.athomeprojects.base.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// 郑氏星案 40 案回归测试:无头运行核心排盘计算,
// ①快照回归(命度+14 星曜黄经,防计算代码/星历数据改动引入偏差)
// ②override 自洽(override 校准量在盘面值中生效且不漂移)
// ③基础 sanity(罗计对宫、命度范围)
// 基线文件 test/baseline/cheng_s.txt;首次运行(基线不存在)自动生成,
// 之后每次运行比对,偏差超过 0.0001° 即失败
public class ChartDataRegressionTest
{
    private static final double EPS = 1e-4;

    private static final String BASELINE = "test/baseline/cheng_s.txt";

    private static ChartData data;

    private static DataSet data_set;

    private static Field sign_pos_field, life_pos_field;

    static class StubTab extends BaseTab {
    }

    @BeforeAll
    static void init() throws Exception
    {
        System.setProperty("java.awt.headless", "true");
        FileIO.setBaseIO(new AppIO(System.getProperty("user.dir") + "/"));
        new Resource(ChartDataRegressionTest.class, "simplified", null, null,
                null);
        City.loadCities("cities.prop");
        data_set = new DataSet();
        assertTrue(data_set.loadData("example/cheng_s.mri"), "命例文件加载失败");
        StubTab tab = new StubTab();
        data = new ChartData(tab, tab, tab);
        sign_pos_field = ChartData.class.getDeclaredField("birth_sign_pos");
        sign_pos_field.setAccessible(true);
        life_pos_field = ChartData.class.getDeclaredField("life_sign_pos");
        life_pos_field.setAccessible(true);
    }

    // 命例可能没有 now(流年)数据:补一个以 birth 为 now 的副本,
    // 与程序界面「未设流年时按出生时间排盘」的行为一致
    private DataEntry ensureNowEntry(DataEntry entry)
    {
        if (entry.getNowDay() != null)
            return entry;
        DataEntry copy = new DataEntry();
        copy.setName(entry.getName());
        copy.setSex(entry.getSex());
        copy.setBirthDay(entry.getBirthDay());
        copy.setNowDay(entry.getBirthDay());
        copy.setCountry(entry.getCountry());
        copy.setCity(entry.getCity());
        copy.setZone(entry.getZone());
        copy.setOverride(entry.getOverride());
        copy.setNote(entry.getNote(false));
        return copy;
    }

    // 计算单个命例,返回「命度 + 14 星曜黄经」快照行
    private String computeCase(DataEntry entry) throws Exception
    {
        DataEntry now_entry = ensureNowEntry(entry);
        data.setOverrideString(entry.getOverride());
        data.compute(entry, now_entry, entry, new DiagramTip());
        double[] pos = (double[]) sign_pos_field.get(data);
        double life = (double) life_pos_field.get(data);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%.4f", life));
        for (int p = 0; p < 14; p++)
            sb.append(',').append(String.format("%.4f", pos[p]));
        return sb.toString();
    }

    @Test
    void testChengCasesSnapshot() throws Exception
    {
        List<String> lines = new ArrayList<>();
        int n = data_set.getMaxDataEntry(DataSet.DATA);
        assertTrue(n >= 40, "郑氏星案应有 40 案,实际 " + n);
        for (int i = 0; i < n; i++) {
            DataEntry e = data_set.getDataEntry(i, DataSet.DATA);
            assertNotNull(e, "案" + (i + 1) + " 数据缺失");
            lines.add(computeCase(e));
        }
        Path base = Paths.get(BASELINE);
        if (Files.exists(base)) {
            List<String> expected = Files.readAllLines(base);
            assertEquals(expected.size(), lines.size(), "命例数量与基线不一致");
            for (int i = 0; i < lines.size(); i++) {
                assertCaseEqual(expected.get(i), lines.get(i), i + 1);
            }
        } else {
            Files.createDirectories(base.getParent());
            Files.write(base, lines);
            System.out.println("基线已生成: " + BASELINE + "(请人工核对后提交)");
        }
    }

    private void assertCaseEqual(String expected, String actual, int case_no)
    {
        StringTokenizer et = new StringTokenizer(expected, ",");
        StringTokenizer at = new StringTokenizer(actual, ",");
        int field = 0;
        while (et.hasMoreTokens() && at.hasMoreTokens()) {
            double ev = Double.parseDouble(et.nextToken());
            double av = Double.parseDouble(at.nextToken());
            assertEquals(ev, av, EPS, "案" + case_no + " 字段" + field
                    + " 与基线偏差超限");
            field++;
        }
    }

    // override 自洽:带 override 的命例,「含 override 值 - 不含 override 值」
    // 必须等于 override 中该星曜的偏移量(验证校准机制未被破坏)
    @Test
    void testOverrideConsistency() throws Exception
    {
        int checked = 0;
        int n = data_set.getMaxDataEntry(DataSet.DATA);
        for (int i = 0; i < n; i++) {
            DataEntry e = data_set.getDataEntry(i, DataSet.DATA);
            String override = e.getOverride();
            if (override == null)
                continue;
            DataEntry now_entry = ensureNowEntry(e);
            data.setOverrideString(override);
            data.compute(e, now_entry, e, new DiagramTip());
            // 注意:birth_sign_pos 是内部数组,后续 compute 会覆盖,
            // 必须 clone 保存快照
            double[] with = ((double[]) sign_pos_field.get(data)).clone();
            data.setOverrideString(null);
            data.compute(e, now_entry, e, new DiagramTip());
            double[] without = ((double[]) sign_pos_field.get(data)).clone();
            // override 格式:「H,daynight,n:val,n:val,...」或
            // 「a宫头,宫位,daynight,n:val,...」;解析 n:val 部分
            for (StringTokenizer st = new StringTokenizer(override, ","); st
                    .hasMoreTokens();) {
                String tok = st.nextToken();
                int colon = tok.indexOf(':');
                if (colon <= 0)
                    continue;
                int planet = FileIO.parseInt(tok.substring(0, colon), -1,
                        true);
                if (planet < 0 || planet >= 14)
                    continue;
                String val_str = tok.substring(colon + 1).trim();
                boolean flip = val_str.endsWith("f");
                if (flip)
                    val_str = val_str.substring(0, val_str.length() - 1);
                double shift = FileIO.parseDouble(val_str, 0.0, false);
                double diff = with[planet] - without[planet];
                // 偏移量必须精确生效(翻转 f 项同样携带偏移,
                // 翻转本身只影响速度显示不在此断言)
                double expected = City.normalizeDegree(shift);
                double got = City.normalizeDegree(diff + 360.0);
                assertEquals(expected, got, EPS, "案" + (i + 1) + " 星曜"
                        + planet + " override 偏移不符");
                checked++;
            }
        }
        assertTrue(checked > 50, "override 校验项过少: " + checked);
    }

    // override 字符串中是否含指定星曜的偏移项
    private boolean hasPlanetOverride(String override, int planet)
    {
        for (StringTokenizer st = new StringTokenizer(override, ","); st
                .hasMoreTokens();) {
            String tok = st.nextToken();
            int colon = tok.indexOf(':');
            if (colon > 0
                    && FileIO.parseInt(tok.substring(0, colon), -1, true) == planet)
                return true;
        }
        return false;
    }

    // 基础 sanity:罗计恒为对宫,命度在 [0,360)
    @Test
    void testSanity() throws Exception
    {
        int n = data_set.getMaxDataEntry(DataSet.DATA);
        for (int i = 0; i < n; i++) {
            DataEntry e = data_set.getDataEntry(i, DataSet.DATA);
            DataEntry now_entry = ensureNowEntry(e);
            data.setOverrideString(e.getOverride());
            data.compute(e, now_entry, e, new DiagramTip());
            double[] pos = (double[]) sign_pos_field.get(data);
            double life = (double) life_pos_field.get(data);
            assertTrue(life >= 0.0 && life < 360.0, "案" + (i + 1) + " 命度越界");
            // 星曜 10/11 = 罗睺/计都,互为对宫;
            // 人工校准盘(override 含罗计偏移)允许不对宫
            String override = e.getOverride();
            boolean node_overridden = override != null
                    && (hasPlanetOverride(override, 10)
                            || hasPlanetOverride(override, 11));
            if (!node_overridden) {
                double diff = City.normalizeDegree(pos[10] - pos[11] + 360.0);
                assertEquals(180.0, diff, 1e-3, "案" + (i + 1) + " 罗计非对宫");
            }
        }
    }

    public static void main(String[] args) throws Exception
    {
        init();
        ChartDataRegressionTest t = new ChartDataRegressionTest();
        t.testChengCasesSnapshot();
        t.testOverrideConsistency();
        t.testSanity();
        System.out.println("全部测试通过");
    }
}
