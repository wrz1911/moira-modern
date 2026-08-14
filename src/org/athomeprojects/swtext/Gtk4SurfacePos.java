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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import org.eclipse.swt.internal.gtk4.GTK4;

// GTK4 下读取 GtkPopover 真实布局位置(相对主窗口 surface 原点)。
// SWT 3.135 的 Shell.getBounds/toDisplay 在 GTK4 下返回期望值(记录语义),
// 无法用于实测校准;ON_TOP shell 在 GTK4 下是 GtkPopover,句柄存于
// Shell.shellHandle(其 handle 是容器 SwtFixed,不实现 GtkNative)。
// 经 SWT 自带原生方法 GTK4.gtk_widget_get_native/gtk_native_get_surface
// 拿到 popover 的 GdkSurface,再用 GDK 4.10+ 的 gdk_popup_get_position_x/y
// (FFM 直调,SWT 未包装)读 popup surface 相对 parent toplevel surface
// 原点的真实布局坐标。Wayland 客户端拿不到屏幕绝对坐标(GTK 4.18+ 亦已
// 移除 gdk_surface_get_origin),而 SWT GTK4 的 toDisplay 返回的正是同一
// surface 相对坐标系,两者可直接求差得定位 gap。X11 后端同样适用。
public final class Gtk4SurfacePos {
	private static final Linker LINKER = Linker.nativeLinker();
	private static final MethodHandle GDK_POPUP_GET_POSITION_X;
	private static final MethodHandle GDK_POPUP_GET_POSITION_Y;


	static {
		SymbolLookup gtk = SymbolLookup.libraryLookup("libgtk-4.so.1",
				Arena.global());
		GDK_POPUP_GET_POSITION_X = LINKER.downcallHandle(gtk
				.find("gdk_popup_get_position_x").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT,
						ValueLayout.ADDRESS));
		GDK_POPUP_GET_POSITION_Y = LINKER.downcallHandle(gtk
				.find("gdk_popup_get_position_y").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT,
						ValueLayout.ADDRESS));
	}

	// JDK 24+ 的 MemorySegment.ofAddress(ptr) 返回零长度段,须 reinterpret
	// 出大小后才能作为 downcall 参数安全传递
	private static MemorySegment seg(long ptr) {
		return MemorySegment.ofAddress(ptr).reinterpret(256, Arena.global(),
				null);
	}

	// 返回 popover surface 相对主窗口 surface 原点的布局位置 [x, y];
	// 失败返回 null
	static int[] origin(long popoverHandle) {
		try (Arena arena = Arena.ofConfined()) {
			if (popoverHandle == 0)
				return null;
			long nativePtr = GTK4.gtk_widget_get_native(popoverHandle);
			if (nativePtr == 0)
				return null;
			long surf = GTK4.gtk_native_get_surface(nativePtr);
			if (surf == 0)
				return null;
			int x = (int) GDK_POPUP_GET_POSITION_X.invoke(seg(surf));
			int y = (int) GDK_POPUP_GET_POSITION_Y.invoke(seg(surf));
			return new int[] { x, y };
		} catch (Throwable t) {
			return null;
		}
	}


		// 返回 popover 控件的真实分配尺寸 [w, h](GTK 布局后);失败返回 null
	// 用于诊断 popover 是否被主窗口 client 区截断(文字被切)
	static int[] size(long popoverHandle) {
		int w = GTK4.gtk_widget_get_width(popoverHandle);
		int h = GTK4.gtk_widget_get_height(popoverHandle);
		if (w <= 0 || h <= 0)
			return null;
		return new int[] { w, h };
	}

}
