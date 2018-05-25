/*
 * Copyright (C) 2017 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>
 *
 * This file is part of madRat's GUI package (madrat.gui).
 *
 * This package is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This package distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with package.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package madrat.gui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Graphic elements unification and management.
 *
 * NOTE: This layer between javax.microedition.lcdui.Display and GUI-items allows to
 * correct graphics items, when J2ME colors model (colors and highlight colors) is conflicted
 * with Phone's themes.
 *
 * Ex: Phone theme has highlight image, but for J2ME the background highlight color is the same
 * as foreground color.
 */
public class Theme {

    public static final int FONT_TITLE = 1;
    public static final int FONT_TEXT = 2;
    public static final int FONT_STATIC = 3;

    protected static final int UNDEFINED_COLOR = 0xFF808080;
    protected final boolean isSystemColors_;

    protected int background_;
    protected int foreground_;
    protected int hlBackground_;
    protected int hlForeground_;
    protected int border_;
    protected int hlBorder_;

    protected int defaultColor_;

    protected final Display display_;

    public Theme(Display display) {
        this(display, null);
    }

    //String format
    //background, foreground, hl-background, hl-foreground, border, hl-border, default
    public Theme(Display display, String theme) throws RuntimeException {
        display_ = display;
        if (null != theme) {
            isSystemColors_ = false;

            int colors[] = new int[7];
            colors[0] = display_.getColor(Display.COLOR_BACKGROUND);
            colors[1] = display_.getColor(Display.COLOR_FOREGROUND);
            colors[2] = display_.getColor(Display.COLOR_HIGHLIGHTED_BACKGROUND);
            colors[3] = display_.getColor(Display.COLOR_HIGHLIGHTED_FOREGROUND);
            colors[4] = display_.getColor(Display.COLOR_BORDER);
            colors[5] = display_.getColor(Display.COLOR_HIGHLIGHTED_BORDER);
            colors[6] = UNDEFINED_COLOR;

            for (int i = 0; null != theme && i < colors.length; ++i) {

                int pos = theme.indexOf(',');

                String color;
                if (-1 != pos) {
                    color = theme.substring(0, pos);
                    theme = theme.substring(pos + 1);
                } else {
                    color = theme;
                    theme = null;
                }

                final String c = color.trim();
                if (c.equals(""))
                    continue;
                colors[i] = (int)Long.parseLong(c, 16);
            }
            background_ = colors[0];
            foreground_ = colors[1];
            hlBackground_ = colors[2];
            hlForeground_ = colors[3];
            border_ = colors[4];
            hlBorder_ = colors[5];
            defaultColor_ = colors[6];
        } else {
            isSystemColors_ = true;
        }
    }

    public int getColor(int color) {
        if (isSystemColors_)
            return display_.getColor(color) | 0xFF000000;

        switch (color) {
            case Display.COLOR_BACKGROUND:
                return background_;
            case Display.COLOR_FOREGROUND:
                return foreground_;
            case Display.COLOR_HIGHLIGHTED_FOREGROUND:
                return hlForeground_;
            case Display.COLOR_HIGHLIGHTED_BACKGROUND:
                return hlBackground_;
            case Display.COLOR_BORDER:
                return border_;
            case Display.COLOR_HIGHLIGHTED_BORDER:
                return hlBorder_;
            default:
                return defaultColor_;
        }
    }

    public Font getFont(int font) {
        switch (font) {
            case FONT_STATIC:
                return Font.getFont(Font.FONT_STATIC_TEXT);
            case FONT_TITLE:
                return Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            case FONT_TEXT:
                return Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            default:
                return Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        }
    }

    public final int getAlertElement() {
        return Math.min(
               display_.getBestImageHeight(Display.ALERT),
               display_.getBestImageWidth(Display.ALERT));
    }

    public final int getListElement() {
        return Math.min(
               display_.getBestImageHeight(Display.LIST_ELEMENT),
               display_.getBestImageWidth(Display.LIST_ELEMENT));
    }

    public final int getImageHeight() {
        Canvas dummyCanvas = new Canvas() {
            protected void paint(Graphics g) {
            }
        };
        return Math.min(dummyCanvas.getWidth() / 4, dummyCanvas.getHeight()/ 4);
    }
}
