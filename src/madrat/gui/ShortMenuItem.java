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

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;

/**
 * Short item only text and icon (without description)
 */
public class ShortMenuItem extends GenericItem {

    public ShortMenuItem(Theme theme, Form form, String title, String icon) {
        super(theme, form, title, icon);
        height_ = fontTitle_.getHeight()*2;
        desc_ = title;
    }

    public void setDesc(String desc) {
        setTitle(desc);
    }

    public String getDesc() {
        return getTitle();
    }

    public void paint(Graphics g, int w, int h) {
        super.paint(g, w, h);

        final int height = fontTitle_.getHeight();
        final int b2 = BORDER<<1;
        int x = w;
        if (null != icon_) {
            x = w - (BORDER<<2) - icon_.getWidth();
            g.drawImage(icon_, x+b2, height, Graphics.LEFT | Graphics.VCENTER);
            g.clipRect(0, 0, x, h);
        }

        int l;
        switch (getLayout() & (LAYOUT_CENTER | LAYOUT_LEFT | LAYOUT_RIGHT)) {
            case LAYOUT_CENTER:
                x /= 2;
                l = Graphics.HCENTER | Graphics.TOP;
                break;
            case LAYOUT_RIGHT:
                l = Graphics.RIGHT | Graphics.TOP;
                break;
            default:
                x = b2;
                l = Graphics.LEFT | Graphics.TOP;
                break;
        }
        g.drawString(title_, x, height/2, l);
    }
}
