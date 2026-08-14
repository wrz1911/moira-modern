//
//Moira - A Chinese Astrology Charting Program
//Copyright (C) 2004-2015 At Home Projects
//
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
package org.athomeprojects.base;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

public class ImageControl {
	static public final int SMALL_SIZE = 0;

	static public final int MEDIUM_SIZE = 1;

	static public final int LARGE_SIZE = 2;

	static public final int CUSTOM_SIZE = 3;

	static public final int MIN_SIZE = 320;

	static public String[] IMAGE_EXTENSIONS = { "*.png", "*.jpg", "*.svg" };

	static public int getSelectionFromSize(int[] size) {
		// 按实际盘面尺寸匹配预设档位(大/中/小),供保存对话框默认选择
		int[] large_size = Resource.getIntArray("image_size_large");
		int[] medium_size = Resource.getIntArray("image_size_medium");
		int[] small_size = Resource.getIntArray("image_size_small");
		if (size[0] <= MIN_SIZE || size[1] <= MIN_SIZE)
			return MEDIUM_SIZE;
		else if (size[0] == large_size[0] && size[1] == large_size[1])
			return LARGE_SIZE;
		else if (size[0] == medium_size[0] && size[1] == medium_size[1])
			return MEDIUM_SIZE;
		else if (size[0] == small_size[0] && size[1] == small_size[1])
			return SMALL_SIZE;
		else
			return CUSTOM_SIZE;
	}

	static public int[] getSizeFromSelection(int sel, int[] size) {
		switch (sel) {
		case LARGE_SIZE:
			return Resource.getIntArray("image_size_large");
		case SMALL_SIZE:
			return Resource.getIntArray("image_size_small");
		case CUSTOM_SIZE: {
			int[] large_size = Resource.getIntArray("image_size_large");
			int[] medium_size = Resource.getIntArray("image_size_medium");
			int[] small_size = Resource.getIntArray("image_size_small");
			size[2] = (size[0] < medium_size[0]) ? small_size[2]
					: ((size[0] < large_size[0]) ? medium_size[2]
							: large_size[2]);
			size[3] = (size[0] < medium_size[0]) ? small_size[3]
					: ((size[0] < large_size[0]) ? medium_size[3]
							: large_size[3]);
			return size;
		}
		default:
			return Resource.getIntArray("image_size_medium");
		}
	}

	static public int[] getImageSize(int width, int height) {
		int[] size = new int[4];
		size[0] = width;
		size[1] = height;
		switch (getSelectionFromSize(size)) {
		case LARGE_SIZE:
			size = Resource.getIntArray("image_size_large");
			break;
		case SMALL_SIZE:
			size = Resource.getIntArray("image_size_small");
			break;
		case CUSTOM_SIZE: {
			int[] large_size = Resource.getIntArray("image_size_large");
			int[] medium_size = Resource.getIntArray("image_size_medium");
			int[] small_size = Resource.getIntArray("image_size_small");
			size[2] = (size[0] < medium_size[0]) ? small_size[2]
					: ((size[0] < large_size[0]) ? medium_size[2]
							: large_size[2]);
			size[3] = (size[0] < medium_size[0]) ? small_size[3]
					: ((size[0] < large_size[0]) ? medium_size[3]
							: large_size[3]);
			break;
		}
		default:
			size = Resource.getIntArray("image_size_medium");
			break;
		}
		return size;
	}

	static public BufferedImage captureImage(int[] image_desc) {
		boolean chart_only = Resource.getPrefInt("image_chart_only") != 0;
		if (chart_only) {
			image_desc[0] = image_desc[1] = Math.min(image_desc[0],
					image_desc[1]);
			image_desc[2] = image_desc[3] = Math.min(image_desc[2],
					image_desc[3]);
		}
		int image_width = image_desc[0] - 2 * image_desc[2];
		int image_height = image_desc[1] - 2 * image_desc[3];
		BufferedImage image = new BufferedImage(image_desc[0], image_desc[1],
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = image.createGraphics();
		DrawAWT.setFillColor(g2d, "chart_window_bg_color", false);
		g2d.fillRect(0, 0, image_desc[0], image_desc[1]);
		g2d.translate(image_desc[2], image_desc[3]);
		int width = Resource.DIAGRAM_WIDTH;
		int scaler = Resource.getInt("print_scaler");
		width *= scaler;
		double scale = (double) Math.min(image_width, image_height) / width;
		g2d.scale(scale, scale);
		int scaled_width = (int) (image_width / scale);
		int scaled_height = (int) (image_height / scale);
		// 导出保持标准布局:屏幕拖动的区域偏移清零,导出后恢复
		int[] saved_offset = ChartData.screen_offset;
		ChartData.screen_offset = ChartData.defaultRegionOffset();
		try {
			ChartData.getData().pageDiagram(g2d, "", scaler,
				new java.awt.Point(scaled_width, scaled_height),
				new java.awt.Point(width, width), false, true, false,
				chart_only, true, false,
				Resource.getPrefInt("image_vertical_text") != 0, false);
		} finally {
			ChartData.screen_offset = saved_offset;
		}
		g2d.dispose();
		return image;
	}

	// 与 captureImage 相同的绘制序列,但目标换成 JFreeSVG 的
	// SVGGraphics2D,输出矢量 SVG 文件(可无损缩放、方便分享)。
	// pageDiagram 只依赖 Graphics2D 接口,因此复用同一套绘制代码
	static public boolean captureSVG(File svg_file, int[] image_desc) {
		boolean chart_only = Resource.getPrefInt("image_chart_only") != 0;
		int width = image_desc[0];
		int height = image_desc[1];
		int margin_x = image_desc[2];
		int margin_y = image_desc[3];
		if (chart_only) {
			width = height = Math.min(width, height);
			margin_x = margin_y = Math.min(margin_x, margin_y);
		}
		int image_width = width - 2 * margin_x;
		int image_height = height - 2 * margin_y;
		SVGGraphics2D svg = new SVGGraphics2D(width, height);
		DrawAWT.setFillColor(svg, "chart_window_bg_color", false);
		svg.fillRect(0, 0, width, height);
		svg.translate(margin_x, margin_y);
		int diag_width = Resource.DIAGRAM_WIDTH;
		int scaler = Resource.getInt("print_scaler");
		diag_width *= scaler;
		double scale = (double) Math.min(image_width, image_height)
				/ diag_width;
		svg.scale(scale, scale);
		int scaled_width = (int) (image_width / scale);
		int scaled_height = (int) (image_height / scale);
		int[] saved_offset = ChartData.screen_offset;
		ChartData.screen_offset = ChartData.defaultRegionOffset();
		try {
			ChartData.getData().pageDiagram(svg, "", scaler,
				new java.awt.Point(scaled_width, scaled_height),
				new java.awt.Point(diag_width, diag_width), false, true, false,
				chart_only, true, false,
				Resource.getPrefInt("image_vertical_text") != 0, false);
		} finally {
			ChartData.screen_offset = saved_offset;
		}
		try {
			SVGUtils.writeToSVG(svg_file, svg.getSVGElement());
		} catch (IOException e) {
			return false;
		}
		svg.dispose();
		return true;
	}
}