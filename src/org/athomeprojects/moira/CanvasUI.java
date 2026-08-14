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

import org.athomeprojects.base.Resource;
import org.athomeprojects.swtext.ColorManager;
import org.athomeprojects.swtext.ImageManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public class CanvasUI extends Canvas implements MouseMoveListener {
	private Composite button_composite, entry_composite, info_composite;

	private Text name;

	private Button male, female;

	private Button expand;

	private boolean pin_ui, show_ui, skip_overlay;

	private int trigger, info_overlay;

	private double info_overlay_ratio;

	private Point cursor_location;

	public CanvasUI(Composite parent, int style) {
		super(parent, style);
		pin_ui = Resource.getPrefInt("pin_ui") != 0;
		show_ui = Resource.getPrefInt("show_ui") != 0;
		info_overlay = Resource.getPrefInt("info_overlay");
		info_overlay_ratio = Resource.getPrefDouble("info_overlay_ratio");
		// GTK4 下 Canvas 的 FormLayout 布局失效(实测所有 FormData
		// 子控件被摆在 (0,0),expand 图标跑到左上角、点击命中不了);
		// 改为 resize 时手动 setBounds 摆位
		button_composite = new Composite(this, SWT.NONE);
		MenuFolder.addCommandListener(button_composite);
		GridLayout grid_layout = new GridLayout(1, false);
		grid_layout.marginWidth = grid_layout.marginHeight = 0;
		button_composite.setLayout(grid_layout);
		// 现代交互:原作 13px 图标太不起眼,改为带文字的显眼按钮
		expand = new Button(button_composite, SWT.PUSH);
		updateColor(false);
		expand.setImage(ImageManager.getImage("expand_icon"));
		expand.setText(Resource.getString("input_button"));
		expand.setToolTipText(Resource.getString("tip_expand_button"));
		expand.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				showUI();
			}
		});
		button_composite.setVisible(!show_ui);
		entry_composite = new Composite(this, SWT.NONE);
		grid_layout = new GridLayout(1, false);
		grid_layout.marginWidth = grid_layout.marginHeight = grid_layout.verticalSpacing = 0;
		entry_composite.setLayout(grid_layout);
		entry_composite.setVisible(false);
		trigger = 0;
		Composite composite = new Composite(entry_composite, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		grid_layout = new GridLayout(2, false);
		grid_layout.marginWidth = grid_layout.marginHeight = 0;
		composite.setLayout(grid_layout);

		Composite shrink_container = new Composite(composite, SWT.NONE);
		GridData shrink_data = new GridData(GridData.FILL_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_END);
		shrink_container.setLayoutData(shrink_data);
		GridLayout shrink_layout = new GridLayout(1, false);
		shrink_layout.marginWidth = shrink_layout.marginHeight = 0;
		shrink_container.setLayout(shrink_layout);
		// GTK4 下 Label 的 mouseDown 不触发,隐藏按钮也改用 Button
		Button shrink = new Button(shrink_container, SWT.PUSH);
		shrink.setImage(ImageManager.getImage("shrink_icon"));
		shrink.setText(Resource.getString("hide_input_button"));
		shrink.setToolTipText(Resource.getString("tip_shrink_button"));
		shrink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				show_ui = false;
				Resource.putPrefInt("show_ui", 0);
				entry_composite.setVisible(false);
				button_composite.setVisible(true);
			}
		});
		composite = new Composite(entry_composite, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		grid_layout = new GridLayout(2, false);
		grid_layout.marginWidth = grid_layout.marginHeight = 0;
		composite.setLayout(grid_layout);
		Group group = new Group(composite, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(new GridLayout(1, false));
		group.setText(Resource.getString("dialog_name_name"));
		name = new Text(group, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		name.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		name.setText(Moira.getChart().getName());
		name.addListener(SWT.FocusOut, new Listener() {
			public void handleEvent(Event event) {
				ChartTab tab = Moira.getChart();
				tab.setName(name.getText());
				name.setText(tab.getName());
			}
		});
		group = new Group(composite, SWT.NONE);
		group.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL));
		group.setText(Resource.getString("sex"));
		group.setLayout(new GridLayout(2, false));
		male = new Button(group, SWT.RADIO);
		male.setText(Resource.getString("male"));
		male.setSelection(Moira.getChart().getSex());
		male.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				Moira.getChart().setSex(male.getSelection());
			}
		});
		female = new Button(group, SWT.RADIO);
		female.setText(Resource.getString("female"));
		female.setSelection(!Moira.getChart().getSex());
		female.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				Moira.getChart().setSex(!female.getSelection());
			}
		});
		info_composite = new Composite(this, SWT.NONE);
		FillLayout fill_layout = new FillLayout();
		fill_layout.marginWidth = fill_layout.marginHeight = fill_layout.spacing = 0;
		info_composite.setLayout(fill_layout);
		addMouseMoveListener(this);
		info_composite.setVisible(false);
		entry_composite.setVisible(show_ui);
		addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent event) {
				layoutChildren();
			}
		});
		layoutChildren();
	}

	// GTK4 下手动摆位(替代失效的 FormLayout):
	// 按钮组(expand)在顶部中央——盘面圆外空白区,右上角会盖住八字环文字;
	// 输入表格(entry)在右上角,展开时整片遮住盘面属预期;信息浮层占满画布
	private void layoutChildren() {
		if (isDisposed())
			return;
		Point size = getSize();
		if (size.x <= 0 || size.y <= 0)
			return;
		Point bs = button_composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		button_composite.setBounds((size.x - bs.x) / 2, 0, bs.x, bs.y);
		Point es = entry_composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		entry_composite.setBounds(size.x - es.x, 0, es.x, es.y);
		info_composite.setBounds(0, 0, size.x, size.y);
	}

	public void hideInfo() {
		skip_overlay = false;
		if (info_composite.getVisible()) {
			info_composite.setVisible(false);
			ChartTab.getTab(info_overlay).setAlternateContainer(null);
		}
	}

	public void showUI() {
		show_ui = true;
		Resource.putPrefInt("show_ui", 1);
		button_composite.setVisible(false);
		entry_composite.setVisible(true);
		disableUIHint();
	}

	public void moveToNameField() {
		Moira.moveToControl(name);
	}

	public void mouseMove(MouseEvent event) {
		// 输入表格展开后常驻,不再随鼠标移出自动收起(原作行为是
		// 「点开即闪没」的主要来源,现代交互改为显式收起)
		if (info_overlay >= 0 && ChartTab.getTab(info_overlay).isTabVisible()) {
			if (event.stateMask == SWT.CONTROL) {
				skip_overlay = true;
				return;
			}
			Rectangle area = getClientArea();
			int width = (info_overlay_ratio < 0.0) ? area.height
					: ((int) (info_overlay_ratio * area.width));
			width = Math.min(width, area.width - 20);
			Rectangle bounds = info_composite.getBounds();
			if (bounds.width != width) {
				bounds.width = width;
				info_composite.setBounds(bounds.x, bounds.y, bounds.width,
						bounds.height);
			}
			boolean visible = bounds.contains(event.x, event.y);
			if (!visible)
				skip_overlay = false;
			else if (skip_overlay)
				return;
			if (visible != info_composite.getVisible()) {
				ChartTab.getTab(info_overlay).setAlternateContainer(
						visible ? info_composite : null);
				info_composite.setVisible(visible);
				info_composite.layout();
			}
		}
	}

	public int getOverlay() {
		return info_overlay;
	}

	public void toggleOverlay(int index) {
		info_overlay = (info_overlay == index) ? -1 : index;
		Resource.putPrefInt("info_overlay", info_overlay);
	}

	public void setOverlayBoundary() {
		Point pt = toControl(cursor_location);
		Rectangle area = getClientArea();
		if (pt.x <= area.height) {
			Resource.removePref("info_overlay_ratio");
		} else {
			Resource.putPrefDouble("info_overlay_ratio", ((double) pt.x)
					/ area.width);
		}
		info_overlay_ratio = Resource.getPrefDouble("info_overlay_ratio");
		int width = (info_overlay_ratio < 0.0) ? area.height
				: ((int) (info_overlay_ratio * area.width));
		width = Math.min(width, area.width - 20);
		Rectangle bounds = info_composite.getBounds();
		if (bounds.width != width) {
			bounds.width = width;
			info_composite.setBounds(bounds.x, bounds.y, bounds.width,
					bounds.height);
		}
	}

	public void setMouseLocation() {
		cursor_location = Display.getCurrent().getCursorLocation();
	}

	public void setTrigger(int val) {
		trigger = val;
	}

	public void setName(String str) {
		name.setText((str == null) ? "" : str);
	}

	public void setSex(boolean sex) {
		male.setSelection(sex);
		female.setSelection(!sex);
	}

	public Composite getEntryField() {
		return entry_composite;
	}

	public void updateColor(boolean no_color) {
		Color color;
		if (no_color) {
			color = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
		} else {
			color = ColorManager.getColor("chart_window_bg_color");
		}
		if (color != button_composite.getBackground()) {
			button_composite.setBackground(color);
			expand.setBackground(color);
		}
	}

	public boolean showUIHint() {
		return Resource.getPrefInt("show_ui_hint") != 0;
	}

	private void disableUIHint() {
		if (!showUIHint())
			return;
		Resource.putPrefInt("show_ui_hint", 0);
		ChartTab.hideTimerHint();
	}
}