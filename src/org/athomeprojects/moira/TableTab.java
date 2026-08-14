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

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringTokenizer;

import org.athomeprojects.base.BaseCalendar;
import org.athomeprojects.base.ChartMode;
import org.athomeprojects.base.City;
import org.athomeprojects.base.DataEntry;
import org.athomeprojects.base.DataSet;
import org.athomeprojects.base.FileIO;
import org.athomeprojects.base.Message;
import org.athomeprojects.base.Resource;
import org.athomeprojects.base.RuleEntry;
import org.athomeprojects.swtext.ColorManager;
import org.athomeprojects.swtext.FontMap;
import org.athomeprojects.swtext.LocationSpinner;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;



import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.FocusEvent;


import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;




import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;


import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;


import org.eclipse.swt.widgets.Text;

class TableTab {
    private final int INIT_ROW_SIZE = 100;

    private final int SHORT_DESC_LENGTH = 30;

    private final int NAME = 2;

    private final int DATE = 4;

    private final int BIRTHDAY = 4;

    private final int PLACE = 5;

    private final int BIRTHPLACE = 5;

    private final int DATA_NOTE = 6;

    private final int PICK_NOTE = 8;

    private int date_index, place_index, note_index;

    private Font font;

    private Group group;

    private Composite container, bottom_container, relationship,
            desc_container, detail_container;
    private SashForm list_area;

    private Combo chart_type;

    private DataTab desc;

    private Button open, add, save, save_as, show, hide;

    private String group_name, male, female, day_choice, night_choice;

    private boolean name_up, place_up, birthday_up, has_both_set, need_save;

    private Entry[] row;

    private DataEntry[][] row_data;

    private DataEntry[] selected_data;

    private int[] num_row;

    private int type, update_depth, top_index, find_row;

    private String edit_text;

    // Master-Detail 列表 UI:左侧命例列表 + 右侧详情表单
    private List entry_list;

    private Text name_field, birthday_field, place_field, note_field,
            mountain_field, zone_field;

    private Combo sex_combo, dayset_combo;

    private Button update_check, place_button, note_save;

    private Label detail_title, label, mountain_label,
            dayset_label;

    private int last_selected_index = -1;

    public Composite createTabFolderPage(CTabFolder tab_folder)
    {
        initFieldIndex(false);
        row = new Entry[INIT_ROW_SIZE];
        row_data = new DataEntry[DataSet.MAX_TYPE][];
        for (int iter = 0; iter < DataSet.MAX_TYPE; iter++)
            row_data[iter] = new DataEntry[INIT_ROW_SIZE];
        selected_data = new DataEntry[DataSet.MAX_TYPE];
        num_row = new int[DataSet.MAX_TYPE];
        Composite table_container = new Composite(tab_folder, SWT.NONE);
        table_container.setLayout(new FillLayout());
        group = new Group(table_container, SWT.NONE);
        group_name = "";
        group.setLayout(new FillLayout());
        container = new Composite(group, SWT.NONE);
        GridLayout container_layout = new GridLayout(1, false);
        container_layout.marginWidth = container_layout.marginHeight = 0;
        container.setLayout(container_layout);

        // Master-Detail 主区:左命例列表 + 右详情表单(可拖动分栏)
        list_area = new SashForm(container, SWT.HORIZONTAL);
        list_area.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.FILL_VERTICAL));

        // 左侧:排序按钮行 + 命例列表
        Composite list_panel = new Composite(list_area, SWT.NONE);
        list_panel.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout list_panel_layout = new GridLayout(1, false);
        list_panel_layout.marginWidth = list_panel_layout.marginHeight = 0;
        list_panel.setLayout(list_panel_layout);
        Composite sort_bar = new Composite(list_panel, SWT.NONE);
        sort_bar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sort_bar.setLayout(new RowLayout());
        Button sort_name = new Button(sort_bar, SWT.PUSH);
        sort_name.setText(Resource.getString("table_column_label")
                .split(",")[NAME].replaceAll("x", "") + " ↑");
        sort_name.setToolTipText(Resource.getString("tip_sort_name"));
        Moira.addFocusListener(sort_name);
        sort_name.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                sortName();
            }
        });
        Button sort_place = new Button(sort_bar, SWT.PUSH);
        sort_place.setText(Resource.getString("table_column_label")
                .split(",")[BIRTHPLACE].replaceAll("x", "") + " ↑");
        sort_place.setToolTipText(Resource.getString("tip_sort_place"));
        Moira.addFocusListener(sort_place);
        sort_place.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                sortPlace();
            }
        });
        Button sort_day = new Button(sort_bar, SWT.PUSH);
        sort_day.setText(Resource.getString("table_column_label")
                .split(",")[BIRTHDAY].replaceAll("x", "") + " ↑");
        sort_day.setToolTipText(Resource.getString("tip_sort_day"));
        Moira.addFocusListener(sort_day);
        sort_day.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                sortDay();
            }
        });
        Label legend = new Label(sort_bar, SWT.NONE);
        legend.setText(Resource.getString("list_legend"));
        legend.setForeground(Display.getCurrent().getSystemColor(
                SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
        label = new Label(sort_bar, SWT.NONE);
        label.setForeground(Display.getCurrent().getSystemColor(
                SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
        entry_list = new List(list_panel, SWT.BORDER | SWT.V_SCROLL
                | SWT.SINGLE);
        entry_list.setLayoutData(new GridData(GridData.FILL_BOTH));
        Moira.addFocusListener(entry_list);
        // 单击选中命例并排盘(与原表格单选列行为一致)
        entry_list.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                selectListEntry();
            }
        });
        // 回车:保存详情修改并立即排盘(与「更新」按钮一致)
        entry_list.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event)
            {
                if (event.keyCode == '\r' || event.keyCode == '\n') {
                    saveDetail();
                    updateChart(true);
                    Moira.getChart().resetCities();
                }
            }
        });
        // 双击切换勾选状态
        entry_list.addMouseListener(new MouseAdapter() {
            public void mouseDoubleClick(MouseEvent event)
            {
                int index = entry_list.getSelectionIndex();
                if (index < 0 || index >= num_row[type])
                    return;
                row_data[type][index].setSelected(!row_data[type][index]
                        .getSelected());
                need_save = true;
                refreshList();
                entry_list.setSelection(index);
                showDetail(index);
            }
        });

        // 右侧:详情表单
        detail_container = new Composite(list_area, SWT.NONE);
        detail_container.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout detail_layout = new GridLayout(2, false);
        detail_layout.marginWidth = 8;
        detail_layout.marginHeight = 4;
        detail_container.setLayout(detail_layout);
        detail_title = new Label(detail_container, SWT.NONE);
        detail_title.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_CENTER));
        detail_title.setText(Resource.getString("detail_title"));
        GridData title_data = (GridData) detail_title.getLayoutData();
        title_data.horizontalSpan = 2;
        // 按钮行:文件操作组 | 编辑操作组
        Composite button_bar = new Composite(detail_container, SWT.NONE);
        GridData bar_data = new GridData(GridData.FILL_HORIZONTAL);
        bar_data.horizontalSpan = 2;
        button_bar.setLayoutData(bar_data);
        RowLayout bar_layout = new RowLayout();
        bar_layout.marginTop = bar_layout.marginBottom = 2;
        button_bar.setLayout(bar_layout);
        open = new Button(button_bar, SWT.PUSH);
        open.setText(Resource.getString("open_button"));
        open.setToolTipText(Resource.getString("tip_open_button"));
        Moira.addFocusListener(open);
        open.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                openFile();
            }
        });
        add = new Button(button_bar, SWT.PUSH);
        add.setText(Resource.getString("add_button"));
        add.setToolTipText(Resource.getString("tip_add_button"));
        Moira.addFocusListener(add);
        add.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                openFile(true, false, true, null);
            }
        });
        save = new Button(button_bar, SWT.PUSH);
        save.setText(Resource.getString("save_button"));
        save.setToolTipText(Resource.getString("tip_save_button"));
        Moira.addFocusListener(save);
        save.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                saveFile(null, true);
            }
        });
        save_as = new Button(button_bar, SWT.PUSH);
        save_as.setText(Resource.getString("save_as_button"));
        save_as.setToolTipText(Resource.getString("tip_save_as_button"));
        Moira.addFocusListener(save_as);
        save_as.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                saveFile(null, false);
            }
        });
        Label sep1 = new Label(button_bar, SWT.NONE);
        sep1.setText("|");
        Button blank = new Button(button_bar, SWT.PUSH);
        blank.setText(Resource.getString("new_button"));
        blank.setToolTipText(Resource.getString("tip_new_button"));
        Moira.addFocusListener(blank);
        blank.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                newEntry();
            }
        });
        Button remove = new Button(button_bar, SWT.PUSH);
        remove.setText(Resource.getString("remove_button"));
        remove.setToolTipText(Resource.getString("tip_remove_button"));
        Moira.addFocusListener(remove);
        remove.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                removeSelection(true);
            }
        });
        Button update = new Button(button_bar, SWT.PUSH);
        update.setText(Resource.getString("update_button"));
        update.setToolTipText(Resource.getString("tip_update_button"));
        Moira.addFocusListener(update);
        update.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                updateChart(true);
                Moira.getChart().resetCities();
            }
        });
        Label name_label = new Label(detail_container, SWT.RIGHT);
        name_label.setText(Resource.getString("name_label"));
        GridData nl_data = new GridData();
        nl_data.widthHint = 70;
        name_label.setLayoutData(nl_data);
        name_field = new Text(detail_container, SWT.BORDER);
        name_field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Moira.addFocusListener(name_field);
        name_field.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent event)
            {
                saveDetail();
            }
        });
        Label sex_label = new Label(detail_container, SWT.RIGHT);
        sex_label.setText(Resource.getString("sex_label"));
        GridData sl_data = new GridData();
        sl_data.widthHint = 70;
        sex_label.setLayoutData(sl_data);
        sex_combo = new Combo(detail_container, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        sex_combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sex_combo.add(Resource.getString("male"));
        sex_combo.add(Resource.getString("female"));
        Moira.addFocusListener(sex_combo);
        sex_combo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                saveDetail();
            }
        });
        Label birthday_label = new Label(detail_container, SWT.RIGHT);
        birthday_label.setText(Resource.getString("birthday_label"));
        GridData bl_data = new GridData();
        bl_data.widthHint = 70;
        birthday_label.setLayoutData(bl_data);
        birthday_field = new Text(detail_container, SWT.BORDER);
        birthday_field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Moira.addFocusListener(birthday_field);
        birthday_field.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent event)
            {
                saveDetail();
            }
        });
        Label place_label = new Label(detail_container, SWT.RIGHT);
        place_label.setText(Resource.getString("place_label"));
        GridData pl_data = new GridData();
        pl_data.widthHint = 70;
        place_label.setLayoutData(pl_data);
        Composite place_row = new Composite(detail_container, SWT.NONE);
        place_row.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout place_row_layout = new GridLayout(2, false);
        place_row_layout.marginWidth = place_row_layout.marginHeight = 0;
        place_row.setLayout(place_row_layout);
        place_field = new Text(place_row, SWT.BORDER);
        place_field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Moira.addFocusListener(place_field);
        place_field.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent event)
            {
                if (!place_field.getText().equals(edit_text))
                    saveDetail();
            }
        });
        place_button = new Button(place_row, SWT.PUSH);
        place_button.setText(Resource.getString("place_select"));
        Moira.addFocusListener(place_button);
        place_button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                int index = entry_list.getSelectionIndex();
                if (index < 0 || index >= num_row[type])
                    return;
                edit_text = place_field.getText();
                LocationSpinner loc = Moira.getChart().getSpinner();
                String country = loc.getCountryName();
                String city = loc.getCityName();
                String zone = loc.getZoneName();
                DataEntry entry = row_data[type][index];
                loc.setCountryName(entry.getCountry());
                loc.setCityName(entry.getCity());
                loc.setZoneName(entry.getZone());
                (new LocationDialog(Moira.getShell())).open();
                String new_country = loc.getCountryName();
                String new_city = loc.getCityName();
                String new_zone = loc.getZoneName();
                entry.setCountry(new_country);
                entry.setCity(new_city);
                entry.setZone(new_zone);
                place_field.setText(new_city + ", " + new_country);
                zone_field.setText(new_zone);
                need_save = true;
                refreshList();
                loc.setCountryName(country);
                loc.setCityName(city);
                loc.setZoneName(zone);
            }
        });
        Label zone_label = new Label(detail_container, SWT.RIGHT);
        zone_label.setText(Resource.getString("zone_label"));
        GridData zl_data = new GridData();
        zl_data.widthHint = 70;
        zone_label.setLayoutData(zl_data);
        zone_field = new Text(detail_container, SWT.BORDER);
        zone_field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Moira.addFocusListener(zone_field);
        zone_field.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent event)
            {
                int index = entry_list.getSelectionIndex();
                if (index >= 0 && index < num_row[type]) {
                    row_data[type][index].setZone(zone_field.getText());
                    need_save = true;
                }
            }
        });
        mountain_label = new Label(detail_container, SWT.RIGHT);
        mountain_label.setText(Resource.getString("mountain_label"));
        GridData ml_data = new GridData();
        ml_data.widthHint = 70;
        mountain_label.setLayoutData(ml_data);
        mountain_field = new Text(detail_container, SWT.BORDER);
        mountain_field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Moira.addFocusListener(mountain_field);
        mountain_field.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent event)
            {
                saveDetail();
            }
        });
        dayset_label = new Label(detail_container, SWT.RIGHT);
        dayset_label.setText(Resource.getString("dayset_label"));
        GridData dl_data = new GridData();
        dl_data.widthHint = 70;
        dayset_label.setLayoutData(dl_data);
        dayset_combo = new Combo(detail_container, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        dayset_combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        dayset_combo.add(Resource.getString("day_choice"));
        dayset_combo.add(Resource.getString("night_choice"));
        Moira.addFocusListener(dayset_combo);
        dayset_combo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                saveDetail();
            }
        });
        update_check = new Button(detail_container, SWT.CHECK);
        update_check.setText(Resource.getString("update_check"));
        GridData check_data = new GridData(GridData.FILL_HORIZONTAL);
        check_data.horizontalSpan = 2;
        update_check.setLayoutData(check_data);
        Moira.addFocusListener(update_check);
        update_check.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                int index = entry_list.getSelectionIndex();
                if (index < 0 || index >= num_row[type])
                    return;
                row_data[type][index].setSelected(update_check
                        .getSelection());
                need_save = true;
                refreshList();
                entry_list.setSelection(index);
            }
        });
        Label note_label = new Label(detail_container, SWT.RIGHT
                | SWT.TOP);
        note_label.setText(Resource.getString("note_label"));
        GridData ntl_data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        ntl_data.widthHint = 70;
        note_label.setLayoutData(ntl_data);
        note_field = new Text(detail_container, SWT.BORDER | SWT.MULTI
                | SWT.V_SCROLL | SWT.WRAP);
        GridData note_data = new GridData(GridData.FILL_BOTH);
        note_data.heightHint = 80;
        note_field.setLayoutData(note_data);
        Moira.addFocusListener(note_field);
        note_field.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent event)
            {
                saveNote();
            }
        });
        // 描述区(DataTab,展开时占满主区)
        desc = new DataTab();
        desc_container = desc.createDataPage(TabManager.getPlaceHolder(), "",
                "table", false, true, false, false, true);

        // 底部容器:仅容纳关系图控件(占星模式,平时高度为 0)
        bottom_container = new Composite(container, SWT.NO_FOCUS);
        bottom_container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        bottom_container.setLayout(new GridLayout(1, false));
        relationship = new Composite(TabManager.getPlaceHolder(), SWT.NONE);        relationship = new Composite(TabManager.getPlaceHolder(), SWT.NONE);
        relationship.setLayout(new GridLayout(2, false));
        chart_type = new Combo(relationship, SWT.DROP_DOWN | SWT.READ_ONLY);
        String[] relationship_name = Resource
                .getStringArray("relationship_chart_name");
        for (int i = 0; i < relationship_name.length; i++)
            chart_type.add(relationship_name[i]);
        chart_type.select(Resource.getPrefInt("relationship_type"));
        chart_type.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                Resource.putPrefInt("relationship_type", chart_type
                        .getSelectionIndex());
            }
        });
        Button relationship_chart = new Button(relationship, SWT.PUSH);
        relationship_chart.setText(Resource.getString("relationship_chart"));
        relationship_chart.setToolTipText(Resource
                .getString("tip_relationship_chart_button"));
        Moira.addFocusListener(relationship_chart);
        relationship_chart.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event)
            {
                int index = -1;
                for (int i = 0; i < num_row[type]; i++) {
                    if (row_data[type][i].getSelected()) {
                        if (index >= 0) {
                            index = -1;
                            break;
                        }
                        index = i;
                    }
                }
                if (index < 0 || selected_data[type] == null
                        || selected_data[type] == row_data[type][index]) {
                    Message.info(Resource
                            .getString("dialog_no_relationship_selection"));
                    return;
                }
                if (Moira.needUpdate()) {
                    Moira.update(false, true);
                    return;
                }
                setMultiMode(index);
            }
        });
        setFont();
        name_up = place_up = birthday_up = need_save = false;
        has_both_set = true;
        update_depth = 0;
        top_index = -1;
        male = Resource.getString("male");
        female = Resource.getString("female");
        day_choice = Resource.getString("day_choice");
        night_choice = Resource.getString("night_choice");
        if (ChartMode.isChartMode(ChartMode.ASTRO_MODE))
            addChartButton();
        syncPickFields();
        resetSearch();
        list_area.setWeights(new int[] { 42, 58 });
        update();
        return table_container;
    }


    public boolean setMultiMode(int index)
    {
        if (index < 0 && index >= row_data[type].length)
            return false;
        switch (Resource.getPrefInt("relationship_type")) {
            case 0:
                MenuFolder.setAstroMode(ChartMode.RELATIONSHIP_MODE);
                break;
            case 1:
                MenuFolder.setAstroMode(ChartMode.COMPOSITE_MODE);
                break;
            default:
                MenuFolder.setAstroMode(ChartMode.COMPARISON_MODE);
                break;
        }
        Moira.getChart().setMultiMode(row_data[type][index]);
        return true;
    }

    public void openFile()
    {
        ChartTab.hideTip();
        if (!checkForSave())
            return;
        int index = openFile(false, true, true, null);
        if (index == Integer.MIN_VALUE)
            return;
        if (Resource.prefChanged())
            Moira.updateModEval();
        boolean table_on_top = TabManager.tabOnTop(TabManager.TABLE_TAB_ORDER);
        if (index >= 0) {
            updateData(index, table_on_top);
            String command = Resource.getAlternateCommand();
            if (command != null) {
                Moira.flushEvents(false);
                MenuFolder.processCommand(command, true);
                Moira.getChart().updateAttribute();
            }
        } else if (!table_on_top) {
            Moira.update(false, false);
        }
    }

    public boolean isDescShown()
    {
        return false;
    }

    public DataTab getDesc()
    {
        return desc;
    }

    public boolean findTableNextEntry(String key, boolean forward)
    {
        if (num_row[type] <= 0)
            return false;
        if (find_row < 0) {
            find_row = entry_list.getSelectionIndex();
            if (find_row < 0)
                find_row = 0;
            if (!forward) {
                find_row = Math.min(find_row, num_row[type] - 1);
            }
        } else {
            if (forward)
                find_row++;
            else
                find_row--;
        }
        if (forward) {
            for (int i = find_row; i < num_row[type]; i++) {
                int index = matchEntry(row_data[type][i], key);
                if (index >= 0) {
                    find_row = i;
                    showEntry(find_row, false);
                    return true;
                }
            }
            for (int i = 0; i < find_row; i++) {
                int index = matchEntry(row_data[type][i], key);
                if (index >= 0) {
                    find_row = i;
                    showEntry(find_row, false);
                    return true;
                }
            }
        } else {
            for (int i = find_row; i >= 0; i--) {
                int index = matchEntry(row_data[type][i], key);
                if (index >= 0) {
                    find_row = i;
                    showEntry(find_row, false);
                    return true;
                }
            }
            for (int i = num_row[type] - 1; i > find_row; i--) {
                int index = matchEntry(row_data[type][i], key);
                if (index >= 0) {
                    find_row = i;
                    showEntry(find_row, false);
                    return true;
                }
            }
        }
        Message.warn(Resource.getString("dialog_find_fail"));
        return false;
    }

    public void resetSearch()
    {
        find_row = -1;
    }

    private void showEntry(int index, boolean update)
    {
        entry_list.setSelection(index);
        entry_list.showSelection();
        showDetail(index);
        if (update)
            update();
    }

    private int matchEntry(DataEntry entry, String key)
    {
        int n = entry.getName().indexOf(key);
        if (n >= 0)
            return NAME;
        String str = entry.getCity() + ", " + entry.getCountry();
        n = str.indexOf(key);
        if (n >= 0)
            return date_index;
        str = BaseCalendar.formatDate(entry.getBirthDay(), false, false);
        n = str.indexOf(key);
        if (n >= 0)
            return place_index;
        n = str.indexOf(key);
        if (n >= 0)
            return place_index;
        str = entry.getNote(true);
        if (str != null) {
            n = str.indexOf(key);
            if (n >= 0)
                return note_index;
        }
        return -1;
    }

    public void setMode()
    {
        if (!initFieldIndex(true))
            return;
        syncPickFields();
        int index = getSelectedIndex();
        if (num_row[type] > 0 && index < 0)
            index = 0;
        if (index >= 0)
            updateData(index, true);
        else
            newEntry();
        update();
    }

    public void updateButtonState(boolean enable)
    {
        open.setEnabled(enable);
        add.setEnabled(enable);
        save.setEnabled(enable);
        save_as.setEnabled(enable);
    }

    public void addChartButton()
    {
        if (relationship.getParent() == bottom_container)
            return;
        relationship.setParent(bottom_container);
        GridLayout layout = (GridLayout) bottom_container.getLayout();
        layout.numColumns++;
        bottom_container.layout();
        bottom_container.update();
    }

    public void removeChartButton()
    {
        if (relationship.getParent() == TabManager.getPlaceHolder())
            return;
        relationship.setParent(TabManager.getPlaceHolder());
        GridLayout layout = (GridLayout) bottom_container.getLayout();
        layout.numColumns--;
        bottom_container.layout();
        bottom_container.update();
    }

    private boolean initFieldIndex(boolean check)
    {
        if (ChartMode.isChartMode(ChartMode.PICK_MODE)) {
            if (check && type == DataSet.PICK)
                return false;
            type = DataSet.PICK;
            date_index = DATE;
            place_index = PLACE;
            note_index = PICK_NOTE;
        } else {
            if (check && type == DataSet.DATA)
                return false;
            type = DataSet.DATA;
            date_index = BIRTHDAY;
            place_index = BIRTHPLACE;
            note_index = DATA_NOTE;
        }
        return true;
    }

    public int openFile(boolean multi, boolean clear, boolean check_mode,
            String file)
    {
        String path;
        String[] files;
        MenuFolder.disposeSubWin();
        FolderToolBar.resetSearch();
        if (file != null) {
            File f = new File(file);
            path = f.getParent();
            files = new String[1];
            files[0] = f.getName();
        } else {
            files = Moira.getIO().openFile(multi);
            path = Moira.getIO().getFilePath();
        }
        if (files != null) {
            if (clear)
                clearTable(false);
            int[] index = loadData(path, files, clear);
            ChartMode.setChartMode();
            refresh(true);
            if (clear) {
                if (check_mode && !Resource.hasAlternatePref()) {
                    int cur_type, other_type;
                    if (ChartMode.isChartMode(ChartMode.PICK_MODE)) {
                        cur_type = DataSet.PICK;
                        other_type = DataSet.DATA;
                    } else {
                        cur_type = DataSet.DATA;
                        other_type = DataSet.PICK;
                    }
                    if (num_row[cur_type] == 0 && num_row[other_type] > 0) {
                        // switch to the other mode
                        Moira
                                .getMenu()
                                .setChartMode(
                                        ChartMode
                                                .isChartMode(ChartMode.PICK_MODE) ? ChartMode.TRADITIONAL_MODE
                                                : ChartMode.PICK_MODE);
                    }
                }
                has_both_set = num_row[DataSet.DATA] > 0
                        && num_row[DataSet.PICK] > 0;
            }
            top_index = index[ChartMode.isChartMode(ChartMode.PICK_MODE) ? DataSet.PICK
                    : DataSet.DATA];
            need_save = false;
            return top_index;
        } else {
            return Integer.MIN_VALUE;
        }
    }

    private int[] loadData(String path_name, String[] files, boolean update)
    {
        if (path_name != null && path_name.equals(""))
            path_name = null;
        int[] index = new int[DataSet.MAX_TYPE];
        for (int iter = 0; iter < DataSet.MAX_TYPE; iter++)
            index[iter] = -1;
        for (int i = 0; i < files.length; i++) {
            String path_file_name;
            if (path_name != null)
                path_file_name = path_name + File.separator + files[i];
            else
                path_file_name = files[i];
            DataSet data_set = new DataSet();
            if (!data_set.loadData(path_file_name))
                continue;
            String str = data_set.getFooter();
            if (str != null) {
                desc.setText(str);
                desc.resetUndo();
            }
            for (int iter = 0; iter < DataSet.MAX_TYPE; iter++) {
                index[iter] = addEntry(data_set, iter, false, true);
                if (update && index[iter] >= 0)
                    selected_data[iter] = row_data[iter][index[iter]];
            }
            if (update) {
                Moira.getIO().setLastOpenPath(path_name);
                if (files.length == 1)
                    Moira.getIO().setLastOpenFile(files[i]);
                else
                    Moira.getIO().removeLastOpenFile();
            }
        }
        return index;
    }

    public void clearTable(boolean update)
    {
        clearRows(0, num_row[type], true);
        for (int iter = 0; iter < DataSet.MAX_TYPE; iter++) {
            for (int i = 0; i < num_row[iter]; i++)
                row_data[iter][i] = null;
            num_row[iter] = 0;
            selected_data[iter] = null;
        }
        desc.setText("");
        desc.resetUndo();
        if (update)
            update();
    }

    public void addCurrentEntry(boolean update)
    {
        ChartTab tab = Moira.getChart();
        DataSet data_set = new DataSet();
        data_set.setMaxDataEntry(1, type);
        DataEntry entry = data_set.getDataEntry(0, type);
        int[] date = new int[5];
        LocationSpinner loc = tab.getSpinner();
        String name = tab.getName();
        entry.setName(name);
        entry.setSex(tab.getSex());
        if (ChartMode.isChartMode(ChartMode.PICK_MODE)) {
            entry.setChoice(tab.getDaySet());
            entry.setMountainPos(City.formatMapPos(City.parseMapPos(tab
                    .getMountainPos()), true));
        } else {
            tab.getNowDate(date);
            entry.setNowDay(date);
        }
        entry.setCountry(loc.getCountryName());
        entry.setCity(loc.getCityName());
        entry.setZone(loc.getZoneName());
        tab.getBirthDate(date);
        entry.setBirthDay(date);
        entry.setOverride(ChartTab.getData().getOverrideString());
        String str = ChartTab.getTab(ChartTab.NOTE_TAB).getNote(false);
        if (ChartTab.getTab(ChartTab.NOTE_TAB).hasValidNote(str))
            entry.setNote(str);
        int index = addEntry(data_set, type, true, update);
        if (index >= 0) {
            selected_data[type] = row_data[type][index];
        }
        update();
        top_index = index;
    }

    public void updateNote(String str)
    {
        if (selected_data[type] == null)
            return;
        String name = Moira.getChart().getName();
        if (name == null || !name.equals(selected_data[type].getName()))
            return;
        if (selected_data[type].sameNote(str))
            return;
        need_save = true;
        selected_data[type].setNote(str);
        update();
    }

    public void updateData(int index, boolean delay_update)
    {
        selected_data[type] = row_data[type][index];
        updateData(selected_data[type], delay_update);
    }

    private void updateData(DataEntry entry, boolean delay_update)
    {
        Moira.getChart().removeAstroControl();
        MenuFolder.setAstroMode(ChartMode.NATAL_MODE);
        ChartTab tab = Moira.getChart();
        LocationSpinner loc = tab.getSpinner();
        if (delay_update)
            tab.clearCacheRecord(false, true);
        tab.setName(entry.getName());
        tab.setSex(entry.getSex());
        if (ChartMode.isChartMode(ChartMode.PICK_MODE)) {
            tab.setDaySet(entry.getChoice());
            tab.setMountainPos(entry.getMountainPos());
        } else {
            tab.setNowDate(entry.getNowDay());
        }
        tab.setBirthDate(entry.getBirthDay());
        loc.setCountryName(entry.getCountry());
        loc.setCityName(entry.getCity());
        loc.setZoneName(entry.getZone());
        ChartTab.getData().setOverrideString(entry.getOverride());
        ChartTab.getTab(ChartTab.NOTE_TAB).setNote(entry.getNote(true));
        Moira.update(delay_update, true);
    }

    public void saveFile(String file_name, boolean last)
    {
        ChartTab.getTab(ChartTab.NOTE_TAB).saveNote();
        boolean direct = file_name != null;
        if (!direct) {
            file_name = Moira.getIO().saveFile(last);
            if (file_name == null || file_name.equals("none")) {
                if (last)
                    saveFile(null, false);
                return;
            }
        }
        boolean single_entry = !Moira.isTableVisible();
        if (single_entry && selected_data[type] == null)
            return;
        DataSet data_set = new DataSet();
        if (single_entry) {
            data_set.setMaxDataEntry(1, type);
            data_set.setDataEntry(0, selected_data[type], type);
        } else {
            String str = desc.getText();
            if (!str.equals(""))
                data_set.setFooter(str);
            for (int iter = 0; iter < DataSet.MAX_TYPE; iter++) {
                if (num_row[iter] <= 0)
                    continue;
                int count = num_row[iter];
                if (count <= 0)
                    continue;
                data_set.setMaxDataEntry(count, iter);
                for (int i = 0; i < num_row[iter]; i++) {
                    data_set.setDataEntry(i, row_data[iter][i], iter);
                    if (count > 1 && selected_data[iter] == row_data[iter][i])
                        data_set.setLastIndex(i, iter);
                }
            }
            if (!direct && !(has_both_set && Moira.getIO().saveToLast())
                    && data_set.getMaxDataEntry(DataSet.DATA) > 0
                    && data_set.getMaxDataEntry(DataSet.PICK) > 0) {
                DataSetDialog dialog = new DataSetDialog(Moira.getShell());
                boolean save_data = false, save_pick = false;
                if (dialog.open() == Window.OK) {
                    save_data = dialog.saveData();
                    save_pick = dialog.savePick();
                }
                dialog.close();
                if (!save_data && !save_pick)
                    return;
                if (!save_data)
                    data_set.setMaxDataEntry(0, DataSet.DATA);
                if (!save_pick)
                    data_set.setMaxDataEntry(0, DataSet.PICK);
            }
        }
        if (direct) {
            data_set.saveData(file_name);
        } else {
            if (Resource.getPrefInt("backup") != 0) {
                if (!Moira.getIO().moveFile(file_name,
                        Moira.getIO().getFilePath(),
                        Resource.getPrefString("backup_dir"))) {
                    Message.warn(Resource.getString("dialog_backup_same"));
                    return;
                }
            }
            data_set.saveData(Moira.getIO().getFilePath() + File.separator
                    + file_name);
            Moira.getIO().setLastOpenPath(null);
            Moira.getIO().setLastOpenFile(file_name);
        }
        need_save = false;
    }

    public boolean checkForSave()
    {
        if (!need_save || !save.getEnabled()
                || Resource.getPrefInt("no_confirm_save") != 0)
            return true;
        ConfirmSaveDialog dialog = new ConfirmSaveDialog(Moira.getShell());
        int state = -1;
        if (dialog.open() == Window.OK)
            state = dialog.updateConfirmSave();
        dialog.close();
        if (state > 0)
            saveFile(null, true);
        return state >= 0;
    }

    private void removeSelection(boolean warn)
    {
        if (num_row[type] == 0)
            return;
        int count = 0;
        for (int i = 0; i < num_row[type]; i++) {
            if (row_data[type][i].getSelected())
                count++;
        }
        if (count == 0) {
            // 未勾选任何命例时,删除列表当前选中的那一行
            int index = entry_list.getSelectionIndex();
            if (index < 0 || index >= num_row[type]) {
                if (warn)
                    Message.info(Resource.getString("dialog_no_selection"));
                return;
            }
            row_data[type][index].setSelected(true);
            count = 1;
        }
        // 删除一律弹确认对话框
        if (!warn || Message.question(Resource
                .getString("dialog_remove_selection"))) {
            for (int i = 0; i < num_row[type]; i++) {
                if (row_data[type][i].getSelected()) {
                    if (selected_data[type] == row_data[type][i])
                        selected_data[type] = null;
                    row_data[type][i] = row[i].entry = null;
                }
            }
            int old_num_row = num_row[type];
            count = -1;
            for (int i = 0; i < num_row[type]; i++) {
                if (row_data[type][i] == null) {
                    if (count < 0)
                        count = i;
                } else if (count >= 0) {
                    row_data[type][count++] = row_data[type][i];
                }
            }
            num_row[type] = count;
            adjustSelection(old_num_row);
        }
    }

    private void adjustSelection(int old_num_row)
    {
        clearRows(num_row[type], old_num_row, true);
        refresh(true);
    }

    private void clearRows(int start, int end, boolean clear_data)
    {
        if (start >= end)
            return;
        for (int i = start; i < end; i++) {
            if (clear_data)
                row_data[type][i] = null;
            if (row[i] != null) {
                row[i].dispose();
                row[i] = null;
            }
        }
    }

    private void moveSelection(int iter, boolean up, boolean all, boolean warn)
    {
        if (num_row[iter] == 0)
            return;
        int shift = up ? -1 : 1;
        int first = row_data[iter][0].getName().equals("") ? 1 : 0;
        int s = up ? first : (num_row[iter] - 1);
        int e = up ? num_row[iter] : (first - 1);
        boolean shifted = false;
        for (int i = s; i != e; i -= shift) {
            if (!all && !row_data[iter][i].getSelected())
                continue;
            int n = i + shift;
            if (n < first || n >= num_row[iter])
                continue;
            DataEntry entry = row_data[iter][i];
            row_data[iter][i] = row_data[iter][n];
            row_data[iter][n] = entry;
            shifted = true;
        }
        if (shifted && iter == type)
            refresh(true);
        else if (warn)
            Message.info(Resource.getString("dialog_no_selection"));
    }

    public int getNumEntry()
    {
        return num_row[type];
    }

    public void newEntry()
    {
        ChartTab tab = Moira.getChart();
        unselect();
        MenuFolder.setAstroMode(ChartMode.NATAL_MODE);
        tab.setName(null);
        tab.setSex(true);
        ChartTab.getTab(ChartTab.NOTE_TAB).setNote(null);
        if (ChartMode.isChartMode(ChartMode.PICK_MODE)) {
            tab.setDaySet(true);
            tab.setBirthDate(null);
            addCurrentEntry(true);
        } else {
            tab.reset();
            addCurrentEntry(true);
            tab.refresh(true);
            Moira.updateOverride();
        }
    }

    public void unselect()
    {
        int index = getSelectedIndex();
        if (index >= 0)
            selected_data[type] = null;
    }

    private void sortName()
    {
        if (num_row[type] == 0)
            return;
        int first = row_data[type][0].getName().equals("") ? 1 : 0;
        Arrays.sort(row_data[type], first, num_row[type], new Comparator() {
            public int compare(Object a, Object b)
            {
                String c_a = ((DataEntry) a).getName();
                String c_b = ((DataEntry) b).getName();
                int n = c_a.compareTo(c_b);
                return name_up ? (-n) : n;
            }
        });
        name_up = !name_up;
        refresh(true);
    }

    private void sortPlace()
    {
        if (num_row[type] == 0)
            return;
        int first = row_data[type][0].getName().equals("") ? 1 : 0;
        Arrays.sort(row_data[type], first, num_row[type], new Comparator() {
            public int compare(Object a, Object b)
            {
                String c_a = ((DataEntry) a).getCountry();
                String c_b = ((DataEntry) b).getCountry();
                int n = c_a.compareTo(c_b);
                if (n != 0)
                    return place_up ? (-n) : n;
                c_a = ((DataEntry) a).getCity();
                c_b = ((DataEntry) b).getCity();
                n = c_a.compareTo(c_b);
                return place_up ? (-n) : n;
            }
        });
        place_up = !place_up;
        refresh(true);
    }

    private void sortDay()
    {
        if (num_row[type] == 0)
            return;
        int first = row_data[type][0].getName().equals("") ? 1 : 0;
        Arrays.sort(row_data[type], first, num_row[type], new Comparator() {
            public int compare(Object a, Object b)
            {
                int[] b_a = ((DataEntry) a).getBirthDayDirect();
                int[] b_b = ((DataEntry) b).getBirthDayDirect();
                for (int i = 0; i < b_a.length; i++) {
                    int n = b_a[i] - b_b[i];
                    if (n != 0)
                        return birthday_up ? (-n) : n;
                }
                return 0;
            }
        });
        birthday_up = !birthday_up;
        refresh(true);
    }

    private void refresh(boolean update)
    {
        for (int i = 0; i < num_row[type]; i++) {
            if (row[i] == null) {
                row[i] = new Entry(row_data[type][i]);
            } else if (row[i].entry != row_data[type][i]) {
                row[i].entry = row_data[type][i];
            }
        }
        if (update)
            update();
    }

    private int addEntry(DataSet data_set, int iter, boolean first,
            boolean update)
    {
        int index = -1, last_index = -1;
        int max_entry = data_set.getMaxDataEntry(iter);
        if (max_entry > 1)
            last_index = data_set.getLastIndex(iter);
        for (int i = 0; i < max_entry; i++) {
            if (!data_set.hasDataEntry(i, iter))
                continue;
            int n = addEntry(data_set.getDataEntry(i, iter), iter, first,
                    update);
            if (last_index == i || index < 0)
                index = n;
        }
        return index;
    }

    private int addEntry(DataEntry entry, int iter, boolean first,
            boolean update)
    {
        int j;
        String str = entry.getName();
        for (j = 0; j < num_row[iter]; j++) {
            if (str.equals(row_data[iter][j].getName())) {
                if (!entry.equals(row_data[iter][j], false)) {
                    entry.setSelected(row_data[iter][j].getSelected());
                    if (row_data[iter][j] == selected_data[iter])
                        selected_data[iter] = entry;
                    row_data[iter][j] = row[j].entry = entry;
                    if (update && iter == type && entry == selected_data[iter])
                        updateData(selected_data[iter], false);
                    if (!DataEntry.nowFieldDifferOnly())
                        need_save = true;
                }
                return j;
            }
        }
        if (!need_save && !entry.getName().equals(""))
            need_save = true;
        if (num_row[iter] > 0 && row_data[iter][0].getName().equals("")) {
            row_data[iter][0] = entry;
            if (iter == type) {
                row[0].entry = entry;
            }
            return 0;
        }
        if (num_row[iter] >= row.length) {
            int new_len = row.length + Math.min(INIT_ROW_SIZE, row.length / 2);
            Entry[] new_row = new Entry[new_len];
            for (j = 0; j < row.length; j++)
                new_row[j] = row[j];
            for (int i = 0; i < DataSet.MAX_TYPE; i++) {
                DataEntry[] new_row_data = new DataEntry[new_len];
                for (j = 0; j < row.length; j++)
                    new_row_data[j] = row_data[i][j];
                row_data[i] = new_row_data;
            }
            row = new_row;
        }
        row_data[iter][num_row[iter]] = entry;
        if (row[num_row[iter]] == null) {
            row[num_row[iter]] = new Entry(entry);
        } else {
            row[num_row[iter]].entry = entry;
        }
        num_row[iter]++;
        if (first) {
            moveSelection(iter, false, true, false);
            return 0;
        } else {
            return num_row[iter] - 1;
        }
    }

    private int getSelectedIndex()
    {
        if (selected_data[type] == null)
            return -1;
        for (int i = 0; i < num_row[type]; i++) {
            if (row_data[type][i] == selected_data[type])
                return i;
        }
        return -1;
    }

    public void updateChart(boolean update)
    {
        if (selected_data[type] == null)
            return;
        for (int i = 0; i < num_row[type]; i++) {
            if (row[i].entry == selected_data[type]) {
                if (update)
                    row[i].updateEntry();
                updateData(row[i].entry, false);
                break;
            }
        }
    }

    // PICK 模式的座山/昼夜字段按模式显隐(exclude 释放格子)
    private void syncPickFields()
    {
        boolean pick = ChartMode.isChartMode(ChartMode.PICK_MODE);
        setPickVisible(mountain_label, pick);
        setPickVisible(mountain_field, pick);
        setPickVisible(dayset_label, pick);
        setPickVisible(dayset_combo, pick);
        detail_container.layout();
    }

    private void setPickVisible(org.eclipse.swt.widgets.Control c,
            boolean visible)
    {
        if (c == null || c.isDisposed())
            return;
        Object data = c.getLayoutData();
        if (data instanceof GridData) {
            ((GridData) data).exclude = !visible;
            c.setVisible(visible);
        }
    }

    public void update()
    {
        label.setText(Resource.getString("entry_count_prefix")
                + Integer.toString(num_row[type]) + " "
                + Resource.getString("row_count"));
        refreshList();
        bottom_container.redraw();
        container.redraw();
    }

    // 重建列表行文本(◉/○ 当前命例,☑/☐ 参与批量更新)
    private void refreshList()
    {
        if (entry_list == null || entry_list.isDisposed())
            return;
        entry_list.setRedraw(false);
        entry_list.removeAll();
        for (int i = 0; i < num_row[type]; i++) {
            DataEntry entry = row_data[type][i];
            if (entry == null)
                continue;
            entry_list.add(getListText(i));
        }
        entry_list.setRedraw(true);
    }

    private String getListText(int index)
    {
        DataEntry entry = row_data[type][index];
        boolean cur = selected_data[type] == entry;
        String name = entry.getName();
        if (name == null || name.equals(""))
            name = " ";
        StringBuilder sb = new StringBuilder();
        sb.append(cur ? "\u25C9 " : "  ");
        sb.append(name);
        for (int pad = name.length(); pad < 4; pad++)
            sb.append(" ");
        sb.append("  ");
        sb.append(entry.getSex() ? male : female).append("  ");
        sb.append(BaseCalendar.formatDate(entry.getBirthDay(), false, false));
        if (ChartMode.isChartMode(ChartMode.PICK_MODE)) {
            sb.append("  ").append(entry.getMountainPos()).append("  ");
            sb.append(entry.getChoice() ? day_choice : night_choice);
        }
        // 勾选标记放行尾,与行首的当前命例指示分离
        sb.append("  ").append(entry.getSelected() ? "\u2611" : " ");
        return sb.toString();
    }

    // 列表单击:选中命例并填充详情;排盘延迟执行(留在管理页,
    // 切回星盘页时才重排,避免与原版一样立刻跳走)
    private void selectListEntry()
    {
        int index = entry_list.getSelectionIndex();
        if (index < 0 || index >= num_row[type])
            return;
        selected_data[type] = row_data[type][index];
        showDetail(index);
        updateData(selected_data[type], true);
    }

    // 详情表单填充
    private void showDetail(int index)
    {
        if (index < 0 || index >= num_row[type])
            return;
        last_selected_index = index;
        DataEntry entry = row_data[type][index];
        String title = entry.getName();
        detail_title.setText((title == null || title.equals(""))
                ? Resource.getString("detail_title") : title
                        + " " + Resource.getString("detail_title_suffix"));
        name_field.setText(entry.getName() == null ? "" : entry.getName());
        sex_combo.select(entry.getSex() ? 0 : 1);
        birthday_field.setText(BaseCalendar.formatDate(entry.getBirthDay(),
                false, false));
        place_field.setText(entry.getCity() + ", " + entry.getCountry());
        zone_field.setText(entry.getZone() == null ? "" : entry.getZone());
        note_field.setText(entry.getNote(true) == null ? "" : entry
                .getNote(true));
        update_check.setSelection(entry.getSelected());
        if (ChartMode.isChartMode(ChartMode.PICK_MODE)) {
            mountain_field.setText(entry.getMountainPos());
            dayset_combo.select(entry.getChoice() ? 0 : 1);
        }
    }

    // 表单失焦保存:audit 校验后写回命例
    private void saveDetail()
    {
        int index = entry_list.getSelectionIndex();
        if (index < 0 || index >= num_row[type])
            return;
        row[index].updateEntry();
        need_save = true;
        refreshList();
        entry_list.setSelection(index);
        showDetail(index);
    }

    private void saveNote()
    {
        int index = entry_list.getSelectionIndex();
        if (index < 0 || index >= num_row[type])
            return;
        row_data[type][index].setNote(note_field.getText());
        need_save = true;
    }

    public void setGroupName()
    {
        group_name = Resource.getString("table_label");
        String mode = ChartMode.getModeName(false, true);
        if (mode != null)
            group_name += " - " + mode;
        if (Resource.hasCustomData())
            group_name += " - " + Resource.getModName();
        group.setText(group_name);
    }

    public void updateOverride()
    {
        String str = ChartTab.getData().getOverridenStatus();
        if (str.equals("")) {
            group.setText(group_name);
        } else {
            group.setText(group_name + "    [" + str
                    + Resource.getString("mod_label") + "]");
        }
    }

    public void dispose()
    {
        if (font != null)
            font.dispose();
    }

    // 字体/配色入口(菜单调用);列表版由系统字体渲染,保留空实现
    // 以兼容 MenuFolder 的调用
    public void updateFont()
    {
    }

    public void setColor()
    {
    }

    private void setFont()
    {
        if (font != null)
            font.dispose();
        font = new Font(Display.getCurrent(), "Dialog.bold",
                Resource.getSwtDataFontSize(), MenuFolder.getSwtFontStyle());
        if (entry_list != null && !entry_list.isDisposed())
            entry_list.setFont(font);
        if (name_field != null && !name_field.isDisposed()) {
            name_field.setFont(font);
            birthday_field.setFont(font);
            place_field.setFont(font);
            zone_field.setFont(font);
            note_field.setFont(font);
            mountain_field.setFont(font);
        }
    }

    public boolean genPicture(String dir_name)
    {
        File dir = new File(dir_name);
        if (!dir.canWrite())
            return false;
        boolean success = true;
        for (int i = 0; i < num_row[type]; i++) {
            updateData(row[i].entry, false);
            if (!Moira.getMenu().captureImage(dir_name, "case_" + (i + 1),
                    false)) {
                success = false;
                break;
            }
        }
        updateChart(false);
        return success;
    }

    public boolean golden()
    {
        String file_name = Resource.hasPrefKey("last_open_file") ? Resource
                .getPrefString("last_open_file") : null;
        if (file_name == null)
            return false;
        String data_ext = "." + Resource.DATA_EXT;
        int data_len = data_ext.length();
        file_name = file_name.substring(0, file_name.length() - data_len)
                + ".au";
        String file_path = Resource.hasPrefKey("last_open_path") ? Resource
                .getPrefString("last_open_path") : null;
        String full_path_name;
        if (file_path != null)
            full_path_name = file_path + File.separator + file_name;
        else
            full_path_name = file_name;
        logResult(full_path_name);
        return true;
    }

    public int regression(String dir_name, boolean update)
    {
        File dir = new File(dir_name);
        if (!dir.canWrite())
            return -1;
        int num_failed = 0;
        String last_path_name = Moira.getIO().getLastOpenPath();
        String last_file_name = Moira.getIO().getLastOpenFile();
        FileIO log = new FileIO(dir_name + File.separator + "regression.log",
                false, true);
        String data_ext = "." + Resource.DATA_EXT;
        int data_len = data_ext.length();
        File[] array = dir.listFiles();
        for (int i = 0; i < array.length; i++) {
            File file = array[i];
            String file_name = file.getName();
            if (!file_name.endsWith(data_ext))
                continue;
            // process each file in directory
            String base_name = file_name.substring(0, file_name.length()
                    - data_len);
            String full_base_name = dir_name + File.separator + base_name;
            log.putLine("Test " + base_name + ":");
            boolean pass = true;
            int index = openFile(false, true, false, dir_name + File.separator
                    + file_name);
            if (index == Integer.MIN_VALUE) {
                pass = false;
                log.putLine("  Cannot load data!");
            } else {
                Moira.updateModEval();
                Moira.setShellTitle(null, "[" + base_name + "]", false, true);
                if (index >= 0)
                    updateData(index, true);
                updateChart(false);
                Moira.flushEvents(false);
                String command = Resource.getAlternateCommand();
                if (command != null) {
                    MenuFolder.processCommand(command, true);
                    Moira.getChart().updateAttribute();
                }
                Moira.flushEvents(false);
                if (update) {
                    logResult(full_base_name + ".au");
                } else {
                    logResult(full_base_name + ".log");
                    pass = log.fileDiff(full_base_name + ".log", full_base_name
                            + ".au", null, false);
                    if (pass && Resource.getPrefInt("check_save") == 1) {
                        saveFile(full_base_name + ".sav", true);
                        pass = log.fileDiff(full_base_name + ".sav",
                                full_base_name + ".mri", "pref=", true);
                        if (!pass) {
                            log.putLine(full_base_name + ".sav and "
                                    + full_base_name + ".mri are different.");
                        }
                    }
                }
            }
            log.putLine(pass ? "  Passed." : "  Failed.");
            if (!pass)
                num_failed++;
        }
        log.dispose();
        Moira.loadData(last_path_name + File.separator + last_file_name, -1,
                false);
        return num_failed;
    }

    private void logResult(String file_name)
    {
        FileIO reg = new FileIO(file_name, false, true);
        reg.putLine("---");
        reg.putString(ChartTab.getTab(ChartTab.DATA_TAB).getText());
        reg.putLine("---");
        reg.putString(ChartTab.getTab(ChartTab.POLE_TAB).getText());
        reg.putLine("---");
        DataTab eval_tab = ChartTab.getTab(ChartTab.EVAL_TAB);
        if (eval_tab != null && RuleEntry.hasRuleEntry(true)) {
            reg.putString(eval_tab.getText());
            reg.putLine("---");
        }
        reg.putString(ChartTab.getTab(ChartTab.NOTE_TAB).getText());
        reg.putLine("---");
        reg.dispose();
    }

    public boolean copyEntry()
    {
        if (selected_data[type] == null)
            return false;
        String data = selected_data[type].packEntry(type);
        if (data == null)
            return false;
        Moira.toClipboard(data);
        return true;
    }

    public boolean pasteEntry()
    {
        String data = Moira.fromClipboard();
        if (data == null || data.trim().equals(""))
            return false;
        newEntry();
        if (selected_data[type] == null)
            return false;
        if (!selected_data[type].unpackEntry(data, type))
            return false;
        updateData(selected_data[type], false);
        return true;
    }

    private class Entry {
        private DataEntry entry;

        public Entry(DataEntry data)
        {
            entry = data;
        }

        public void updateEntry()
        {
            auditName();
            String str = name_field.getText();
            entry.setName(str.equals("") ? null : str);
            entry.setSex(sex_combo.getSelectionIndex() == 0);
            entry.setBirthDay(auditDate());
            City city = auditPlace();
            str = place_field.getText();
            int index = str.lastIndexOf(',');
            String country_name = str.substring(index + 1).trim();
            String city_name = str.substring(0, index).trim();
            if (!city_name.equalsIgnoreCase(entry.getCity())
                    || !country_name.equalsIgnoreCase(entry.getCountry())) {
                if (city == null)
                    city = City.matchCity(city_name, country_name, false);
                if (city != null) {
                    entry.setCity(city_name);
                    entry.setCountry(country_name);
                    entry.setZone(city.getZoneName());
                }
            }
            if (ChartMode.isChartMode(ChartMode.PICK_MODE)) {
                auditMountain();
                entry.setMountainPos(mountain_field.getText());
                entry.setChoice(dayset_combo.getSelectionIndex() == 0);
            }
        }

        private void auditName()
        {
            String str = name_field.getText();
            if (!str.trim().equals(str))
                name_field.setText(str.trim());
        }

        private void auditMountain()
        {
            if (!ChartMode.isChartMode(ChartMode.PICK_MODE))
                return;
            String str = mountain_field.getText().trim();
            str = City.formatMapPos(City.parseMapPos(str), true);
            mountain_field.setText(str);
        }

        private int[] auditDate()
        {
            int[] date = new int[5];
            birthday_field.setText(BaseCalendar.auditDay(birthday_field
                    .getText(), date));
            return date;
        }

        private City auditPlace()
        {
            String str = place_field.getText();
            StringTokenizer st = new StringTokenizer(str, ",");
            int n_tok = st.countTokens();
            if (n_tok >= 1) {
                String tok_1 = st.nextToken();
                String tok_2 = (n_tok > 1) ? st.nextToken() : "";
                double long_val, lat_val;
                long_val = City.parseLongLatitude(tok_1, 'E', 'W');
                lat_val = City.parseLongLatitude(tok_2, 'N', 'S');
                if (long_val != City.INVALID && lat_val != City.INVALID) {
                    int iter;
                    City city = null;
                    for (iter = 0; iter < 2; iter++) {
                        city = City.matchCity(long_val, lat_val,
                                (iter > 0) ? City.ANY_MATCH_ERROR_SQ
                                        : City.MATCH_ERROR_SQ);
                        if (city != null)
                            break;
                    }
                    if (city != null) {
                        if (iter > 0) {
                            place_field.setText(City.formatLongLatitude(
                                    long_val, true, true, false)
                                    + ", "
                                    + City.formatLongLatitude(lat_val, false,
                                            true, false)
                                    + ", "
                                    + city.getCountryName());
                        } else {
                            place_field.setText(city.getCityName() + ", "
                                    + city.getCountryName());
                        }
                        return city;
                    }
                } else {
                    City city = City.matchCity(tok_1.trim(), tok_2.trim(),
                            false);
                    if (city != null) {
                        place_field.setText(city.getCityName() + ", "
                                + city.getCountryName());
                        return city;
                    }
                }
            }
            place_field.setText(City.getDefaultCity() + ", "
                    + City.getDefaultCountry());
            return null;
        }

        public void dispose()
        {
        }
    }
}