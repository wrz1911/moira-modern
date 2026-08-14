package org.athomeprojects.moira;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

// 屏幕缩放因子检测:适配 1080p/2K/4K 及 KDE/GNOME 的分数缩放
// 原理:AWT(XWayland/X11)报告物理分辨率,SWT(GTK)报告逻辑分辨率,
// 两者比值即桌面缩放因子。KDE 125% 下 1920x1200 / 1536x960 = 1.25。
public class HiDPIUtil
{
    // 返回桌面缩放因子,如 1.0(100%)、1.25、1.5、2.0;检测失败回退 1.0
    static public double getScreenScale()
    {
        try {
            GraphicsDevice gd = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice();
            DisplayMode dm = gd.getDisplayMode();
            Rectangle logic = Display.getDefault().getPrimaryMonitor()
                    .getBounds();
            if (dm == null || logic.width <= 0 || logic.height <= 0)
                return 1.0;
            double sx = (double) dm.getWidth() / logic.width;
            double sy = (double) dm.getHeight() / logic.height;
            double s = Math.min(sx, sy);
            // 合理性保护:正常桌面缩放 1.0 ~ 4.0,双屏分辨率不同等异常情况回退
            if (s >= 1.0 && s <= 4.0)
                return s;
        } catch (Throwable t) {
            // AWT 不可用(headless/纯 Wayland 无 XWayland)时按 1.0
        }
        return 1.0;
    }
}
