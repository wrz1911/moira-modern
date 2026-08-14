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
package org.athomeprojects.swtext;

import org.athomeprojects.base.DiagramTip;
import org.athomeprojects.base.Resource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class HoverTipSWT implements MouseListener, MouseMoveListener,
		MouseTrackListener {
	// 是否运行在 GTK4 下(SWT 3.135 起 GTK4 为可选后端,由 SWT_GTK4=1 启用)
	// GTK4 下 ON_TOP shell 是 GtkPopover,其 pointing_to 矩形以主窗口 vbox
	// 坐标系解释;GTK3 下是顶层窗口,setBounds 用屏幕坐标
	private static final boolean GTK4 = isGTK4();


	// 弹窗当前是否显示:角度标记线(DrawSWT.drawMarker)绘制时若弹窗
	// 显示中,先擦除标记线并暂停跟随,避免 hideTip 与 hover 重弹的闪烁循环
	static public volatile boolean showing;

	// 角度标记(相位测量)模式:由 DrawSWT.initMarker/endMarker 设置;
	// 测量模式下完全禁用悬停弹窗(十字光标+标线,再按中键恢复)
	static public volatile boolean marker_mode;

	private Shell shell;

	// 主窗口(构造时传入),GTK4 下用于屏幕坐标转主窗口坐标
	private final Shell parent_shell;

	private Font font;

	private Label text;

	private boolean mouse_down, timer_hint, hidden;

	// 悬停框显示时指针的控件坐标,用于识别 GTK4 popover 派发的坐标不变的虚假 motion
	private Point shown_pos;

	// popover popup 的时刻(nanoTime)。GTK4 popup 会同步派发坐标错乱的虚假
	// motion 事件(实测恒为真实位置减 (23,145)),且紧随 popup 之后几 ms 内,
	// 因此 popup 后 200ms 窗口内的 motion 一律忽略,只放行用户真实移动
	private long shown_time;

	// GTK4 popover popup 会在显示后派发两发坐标完全相同的虚假 motion(实测
	// 恒为真实位置减 (23,145),首发约 280ms、二发约 460ms):首发由「首个忽略」
	// 吃掉,二发按「与上一发同坐标」拦截;真实移动坐标逐发变化,不受影响
	private boolean first_motion_seen;
	// 显示期间上一发 motion 的坐标与时刻,用于拦截同坐标重复发的虚假二发
	private Point last_motion_pos;
	private long last_motion_time;

	// GTK4 下 popover 定位与实际期望位置存在主题相关偏移(gap),显示后实测校准补偿
	private boolean offset_calibrated;

	private int offset_x, offset_y;

	private Rectangle pending_bounds;

	// GTK4 下 ON_TOP shell 的 GtkPopover 句柄(Shell.shellHandle 字段,
	// 包私有故反射读取;handle 字段是容器 SwtFixed,不是 popover)
	private long popover_handle;

	private long popoverHandle() {
		if (popover_handle != 0)
			return popover_handle;
		try {
			java.lang.reflect.Field f = Shell.class
					.getDeclaredField("shellHandle");
			f.setAccessible(true);
			popover_handle = f.getLong(shell);
		} catch (Throwable t) {
		}
		return popover_handle;
	}

	private DiagramTip tip = null;

	private static boolean isGTK4() {
		try {
			Class<?> gtk = Class.forName("org.eclipse.swt.internal.gtk.GTK");
			return gtk.getField("GTK4").getBoolean(null);
		} catch (Throwable t) {
			return false;
		}
	}

	public HoverTipSWT(Shell parent) {
		final Display display = parent.getDisplay();
		parent_shell = parent;
		shell = new Shell(parent, SWT.ON_TOP);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 2;
		gridLayout.marginHeight = 2;
		shell.setLayout(gridLayout);
		shell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		text = new Label(shell, SWT.NONE);
		// 不用 COLOR_INFO_FOREGROUND:GTK4 下该系统色返回白色,
		// 与米白背景同色导致文字不可见;改用 prop 可配前景色
		text.setForeground(ColorManager.getColor("tip_fg_color"));
		text.setBackground(ColorManager.getColor("tip_bg_color"));
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_CENTER));
	/*	font = new Font(Display.getCurrent(), FontMap.getSwtFontName(),
				Resource.getInt("swt_tip_font_size"), SWT.NORMAL);*/

		 font = new Font(Display.getCurrent(), "Dialog.bold",
					Resource.getInt("swt_tip_font_size"), SWT.NORMAL);
		text.setFont(font);
		hidden = true;
	}

	public void setTipData(DiagramTip data) {
		tip = data;
	}

	public boolean isTipShell(Shell sh) {
		return sh == shell;
	}

	public void hide() {
		showing = false;
		if (!hidden && !timer_hint && shell != null && !shell.isDisposed()
				&& shell.isVisible()) {
			shell.setVisible(false);
			shell.update();
			hidden = true;
		}
	}

	public void mouseDown(MouseEvent event) {
		timer_hint = false;
		hide();
		mouse_down = true;
	}

	public void mouseUp(MouseEvent event) {
		mouse_down = false;
	}

	public void mouseDoubleClick(MouseEvent event) {
	}

	public void mouseMove(MouseEvent event) {
		// GTK4 下 popover popup 会派发坐标错乱的虚假 motion 事件(实测恒为真实
		// 位置减 (23,145),与 popover 位置无关),紧随 popup 几 ms 内到达;popup
		// 后 200ms 窗口内的 motion 一律忽略。
		// 实测每次 popup 共发两发、坐标完全相同(首发约 280ms、二发约 460ms):
		// 首发由「首个忽略」吃掉,二发按「与上一发同坐标」拦截;
		// 真实移动坐标逐发变化,不受影响
		if (shown_time != 0 && System.nanoTime() - shown_time < 200_000_000L) {
			return;
		}
		if (shown_pos != null && !first_motion_seen) {
			first_motion_seen = true;
			last_motion_pos = new Point(event.x, event.y);
			last_motion_time = System.nanoTime();
			return;
		}
		if (shown_pos != null && last_motion_pos != null
				&& event.x == last_motion_pos.x && event.y == last_motion_pos.y
				&& System.nanoTime() - last_motion_time < 600_000_000L) {
			return;
		}
		if (shown_pos != null && Math.abs(event.x - shown_pos.x) <= 1
				&& Math.abs(event.y - shown_pos.y) <= 1) {
			return;
		}
		hide();
	}

	public void mouseHover(MouseEvent event) {
		if (tip == null || mouse_down || timer_hint || marker_mode)
			return;
		// GTK4 popover popup 会重置 SWT 的 hover 状态,指针静止时 mouseHover
		// 重复派发(实测 400-800ms 三连发),重复 popup 造成闪烁;
		// 同位置且已显示时直接跳过
		Control ctrl = (Control) event.getSource();
		// 完全同步:不信任 event 坐标。MouseHover 事件携带的是定时器建立时
		// 的旧坐标,若处理瞬间光标已移走,弹窗会显示旧位置/旧内容(实测偶发
		// 「不一样」)。改用实时光标位置:内容判定、弹窗定位、shown_pos 全部
		// 以真实光标为准,弹窗与光标完全同步。
		Point cur = ctrl.getDisplay().getCursorLocation(); // surface 坐标
		Point real = ctrl.toControl(cur); // 控件坐标
		if (!hidden && shell.isVisible() && shown_pos != null
				&& Math.abs(cur.x - shown_pos.x) <= 1
				&& Math.abs(cur.y - shown_pos.y) <= 1) {
			return;
		}
		// 坐标域修复:实测 GTK4 下 mouseHover 的 event 坐标是 surface(display)
		// 坐标而非控件坐标(诊断:event = 真实指针 surface 坐标;控件坐标 =
		// event - toDisplay(0,0),本机偏移 (8,132))。此前 getTipFromPoint 拿
		// surface 坐标当控件坐标用,会命中相邻区域,导致「停在火星却显示别的
		// 解释」;而 pos = toDisplay(event) 双重平移,导致弹窗恒偏 (8,132),
		// 即「弹窗距鼠标太远」的根因。修复:内容判定用控件坐标、弹窗定位用
		// surface 坐标,且全部取自实时光标位置而非事件坐标。
		String str = tip.getTipFromPoint(real.x, real.y);
		if (str == null) {
			// 光标在无提示区域:若旧弹窗还显示着,隐藏它,避免内容与位置错配
			if (!hidden && shell.isVisible()) {
				hide();
			}
			return;
		}
		Point pos = cur; // 实时光标 surface 坐标,直接用于弹窗定位
		text.setText(str);
		shell.pack();
		setHoverLocation(pos);
		shown_time = System.nanoTime();
		shell.setVisible(true);
		calibrateOffset();
		hidden = false;
		showing = true;
		shown_pos = new Point(cur.x, cur.y); // 记录实时光标位置(与 mouseMove event 同域)
		first_motion_seen = false;
		last_motion_pos = null;
	}

	public void showTimerHint(String mesg, Canvas canvas, int x, int y,
			int second) {
		hide();
		text.setText(mesg.replace('|', '\n'));
		shell.pack();
		Rectangle bounds = shell.getBounds();
		if (x < 0)
			x += canvas.getClientArea().width - bounds.width;
		if (y < 0)
			y += canvas.getClientArea().height - bounds.height;
		Point pt = canvas.toDisplay(x, y);
		bounds.x = pt.x;
		bounds.y = pt.y;
		applyBounds(bounds);
		shell.setVisible(true);
		calibrateOffset();
		timer_hint = true;
		hidden = false;
		first_motion_seen = false;
		last_motion_pos = null;
		Display.getCurrent().timerExec(second * 1000, new Runnable() {
			public void run() {
				if (!timer_hint || shell.isDisposed())
					return;
				hideTimerHint();
			}
		});
	}

	public void hideTimerHint() {
		timer_hint = false;
		hide();
	}

	public void mouseEnter(MouseEvent event) {
		shown_pos = null;
		last_motion_pos = null;
	}

	public void mouseExit(MouseEvent event) {
		// GTK4 下 popover 显示会 grab 指针,并向源控件派发虚假的 leave 事件,
		// 造成「显示 -> 隐藏 -> 再显示」循环闪烁;仅在指针真正离开控件时才隐藏
		Control ctrl = (Control) event.getSource();
		if (ctrl == null || ctrl.isDisposed())
			return;
		Point p = ctrl.getDisplay().getCursorLocation();
		if (ctrl.getBounds().contains(ctrl.toControl(p))) {
			return;
		}
		hide();
	}

	public void activateHoverHelp(Control ctrl) {
		ctrl.addMouseListener(this);
		ctrl.addMouseMoveListener(this);
		ctrl.addMouseTrackListener(this);
	}

	private void setHoverLocation(Point position) {
		// GTK4 下 popover 只能出现在主窗口 client 区(vbox)内,超出会被 GTK
		// 截断高度导致文字被切,须按主窗口 client 区 clamp;GTK3 下是顶层窗口,
		// 用整个屏幕
		// position 是 surface 坐标,GTK4 下 clamp 域也须换算到 surface 域:
		// client 区原点 = toDisplay(0,0),尺寸 = getClientArea 尺寸
		Rectangle display_bounds;
		if (GTK4 && parent_shell != null && !parent_shell.isDisposed()) {
			Rectangle ca = parent_shell.getClientArea();
			Point ca_origin = parent_shell.toDisplay(0, 0);
			display_bounds = new Rectangle(ca_origin.x, ca_origin.y,
					ca.width, ca.height);
		} else {
			display_bounds = shell.getDisplay().getClientArea();
		}
		Rectangle shell_bounds = shell.getBounds();
		shell_bounds.x = Math.max(Math.min(position.x, display_bounds.width
				- shell_bounds.width), 0);
		shell_bounds.y = Math.max(Math.min(position.y + 16,
				display_bounds.height - shell_bounds.height), 0);
		if (shell_bounds.contains(position)) {
			shell_bounds.y = position.y - shell_bounds.height - 2;
			shell_bounds.y = Math.max(0, shell_bounds.y);
			if (shell_bounds.contains(position))
				shell_bounds.x += 16;
		}
		applyBounds(shell_bounds);
	}

	// 统一入口:入参为期望的 surface 坐标(ctrl.toDisplay 的结果)。
	// GTK4 下 shell 是 GtkPopover,SWT positionPopover 把 setBounds 的坐标
	// 当作主窗口 surface 坐标、自行 translate 到 vbox(translate_coordinates
	// (root.shellHandle, vboxHandle, ...)),因此直接传入即可,勿做转换。
	private void applyBounds(Rectangle bounds) {
		pending_bounds = new Rectangle(bounds.x, bounds.y, bounds.width,
				bounds.height);
		shell.setBounds(bounds.x + offset_x, bounds.y + offset_y, bounds.width,
				bounds.height);
	}

	// 显示后实测 popover 真实位置,与期望位置求差得 gap 并缓存;
	// 校准成功后立即重定位一次,使首次显示即贴切。
	// 注意 gap 需累加补偿且只校准一次:setBounds 后 toDisplay 已含 offset 的贡献,
	// 若每次用 dx 覆盖 offset,会在 0 与 -gap 之间振荡,窗口位置反复跳动
	private void calibrateOffset() {
		if (offset_calibrated || !shell.isVisible() || pending_bounds == null)
			return;
		if (!GTK4) {
			// GTK3:ON_TOP 是顶层窗口,setBounds 后 toDisplay 即为真实位置
			Point actual = shell.toDisplay(0, 0);
			int dx = pending_bounds.x - actual.x;
			int dy = pending_bounds.y - actual.y;
			if (Math.abs(dx) > 50 || Math.abs(dy) > 50)
				return; // GTK 因屏幕边缘自动调整过位置,不校准
			offset_x += dx;
			offset_y += dy;
			offset_calibrated = true;
			return;
		}
		// GTK4:shell 是 GtkPopover,SWT 的 toDisplay 返回记录值无法实测;
		// popover surface 在 popup 后异步就位,延迟用 FFM 读真实 origin 求 gap
		final Display display = shell.getDisplay();
		display.timerExec(80, new Runnable() {
			public void run() {
				try {
					calibrateGTK4();
				} catch (Throwable t) {
				}
			}

			private void calibrateGTK4() {
				if (offset_calibrated || shell.isDisposed()
						|| !shell.isVisible() || pending_bounds == null)
					return;
				int[] o = Gtk4SurfacePos.origin(popoverHandle());
				if (o == null)
					return;
				// pending_bounds 是 surface 坐标,与真实 origin 同域,直接求差
				int dx = pending_bounds.x - o[0];
				int dy = pending_bounds.y - o[1];
				if (Math.abs(dx) > 50 || Math.abs(dy) > 50)
					return;
				offset_x += dx;
				offset_y += dy;
				offset_calibrated = true;
				// 不立即重定位:popover 重定位会再次 popup 引发闪烁,
				// offset 在下次显示时自然生效
			}
		});
	}

	public void dispose() {
		if (shell == null)
			return;
		text.dispose();
		font.dispose();
		shell.close();
		shell = null;
	}
}