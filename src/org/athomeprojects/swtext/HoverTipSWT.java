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
	private Shell shell;

	private Font font;

	private Label text;

	private boolean mouse_down, timer_hint, hidden;

	// GTK4 下 popover 定位与实际期望位置存在主题相关偏移(gap),显示后实测校准补偿
	private boolean offset_calibrated;

	private int offset_x, offset_y;

	private Rectangle pending_bounds;

	private DiagramTip tip = null;

	public HoverTipSWT(Shell parent) {
		final Display display = parent.getDisplay();
		shell = new Shell(parent, SWT.ON_TOP);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 2;
		gridLayout.marginHeight = 2;
		shell.setLayout(gridLayout);
		shell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		text = new Label(shell, SWT.NONE);
		text.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
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
		hide();
	}

	public void mouseHover(MouseEvent event) {
		if (tip == null || mouse_down || timer_hint)
			return;
		String str = tip.getTipFromPoint(event.x, event.y);
		if (str == null)
			return;
		Control ctrl = (Control) event.getSource();
		Point pos = ctrl.toDisplay(new Point(event.x, event.y));
		text.setText(str);
		shell.pack();
		setHoverLocation(pos);
		shell.setVisible(true);
		calibrateOffset();
		hidden = false;
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
	}

	public void mouseExit(MouseEvent event) {
		hide();
	}

	public void activateHoverHelp(Control ctrl) {
		ctrl.addMouseListener(this);
		ctrl.addMouseMoveListener(this);
		ctrl.addMouseTrackListener(this);
	}

	private void setHoverLocation(Point position) {
		Rectangle display_bounds = shell.getDisplay().getClientArea();
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

	// GTK4 下 shell 为 GtkPopover,SWT 记录的 setBounds 位置与 popover
	// 实际显示位置之间存在主题相关 gap,这里统一补偿
	private void applyBounds(Rectangle bounds) {
		pending_bounds = new Rectangle(bounds.x, bounds.y, bounds.width,
				bounds.height);
		shell.setBounds(bounds.x + offset_x, bounds.y + offset_y, bounds.width,
				bounds.height);
	}

	// 显示后实测 popover 真实位置,与期望位置求差得 gap 并缓存;
	// 校准成功后立即重定位一次,使首次显示即贴切
	private void calibrateOffset() {
		if (!shell.isVisible() || pending_bounds == null)
			return;
		Point actual = shell.toDisplay(0, 0);
		int dx = pending_bounds.x - actual.x;
		int dy = pending_bounds.y - actual.y;
		if (Math.abs(dx) > 50 || Math.abs(dy) > 50)
			return; // GTK 因屏幕边缘自动调整过位置,不校准
		if (dx == offset_x && dy == offset_y) {
			offset_calibrated = true;
			return;
		}
		offset_x = dx;
		offset_y = dy;
		shell.setBounds(pending_bounds.x + offset_x, pending_bounds.y
				+ offset_y, pending_bounds.width, pending_bounds.height);
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