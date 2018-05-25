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
 * Entity item with description and icon
 */
public class EntityItem extends GenericItem {

    public EntityItem(Theme theme, Form form, String title) {
        this(theme, form, title, "");
    }

    public EntityItem(Theme theme, Form form, String title, String desc) {
        this(theme, form, title, desc, null);
    }

    public EntityItem(Theme theme, Form form, String title, String desc, String icon) {
        super(theme, form, title, icon);
        height_ = fontTitle_.getHeight() + fontDesc_.getHeight() + 3 * BORDER;
        desc_ = desc;
    }

    public void paint(Graphics g, int w, int h) {
        super.paint(g, w, h);

        int text_x = BORDER;

        if (null != icon_) {
            text_x += BORDER + icon_.getWidth();
            g.drawImage(icon_, BORDER, BORDER + fontTitle_.getHeight() / 2, Graphics.LEFT | Graphics.VCENTER);
            g.clipRect(text_x, 0, w - text_x, h);
        }

        g.drawString(title_, text_x, BORDER, Graphics.TOP | Graphics.LEFT);
        if (null != desc_) {
            g.setFont(fontDesc_);
            g.drawString(desc_, text_x, 2 * BORDER + fontTitle_.getHeight(), Graphics.TOP | Graphics.LEFT);
        }
    }
}
