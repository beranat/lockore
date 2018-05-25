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

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import madrat.sys.ImageCache;

/**
 * One line item with description
 */
public abstract class GenericItem extends javax.microedition.lcdui.CustomItem {
    protected static final int BORDER = 2;

    protected final Theme theme_;
    protected final Form form_;

    protected Image icon_;

    private boolean isFocused_ = false;

    protected Font fontTitle_;
    protected Font fontDesc_;

    protected String title_;
    protected String desc_;

    protected int height_;

    protected Object user_;

    public GenericItem(Theme theme, Form form, String title, String icon) {
        super("");

        theme_ = theme;
        form_ = form;

        setIcon(icon);

        fontTitle_ = theme.getFont(Theme.FONT_TITLE);
        fontDesc_ = theme.getFont(Theme.FONT_TEXT);

        height_ = fontTitle_.getHeight() + 2 * BORDER;

        title_ = title;
        desc_ = null;

        user_ = null;
    }

    public final void setUserInfo(Object user) {
        user_ = user;
    }

    public final Object getUserInfo() {
        return user_;
    }

    public void setIcon(Image icon) {
        icon_ = icon;
        invalidate();
    }

    public void setIcon(String icon) {
        setIcon(ImageCache.getImage(icon, theme_.getListElement()));
    }

    public String getLabel() {
        return getTitle();
    }

    public void setLabel(String label) {
        setTitle(label);
    }

    public String getTitle() {
        return title_;
    }

    public void setTitle(String title) {
        if (null != title && title.length() > 0)
            title_ = title;
        else
            title_ = null;
        invalidate();
    }

    public String getDesc() {
        return desc_;
    }

    public void setDesc(String desc) {
        if (null != desc && desc.length() > 0)
            desc_ = desc;
        else
            desc_ = null;
        invalidate();
    }

    public void paint(Graphics g, int w, int h)  {

        int     indBackground   = Display.COLOR_BACKGROUND,
                indBorder       = Display.COLOR_BORDER,
                indForeground   = Display.COLOR_FOREGROUND;

        if (isFocused_) {
                indBackground   = Display.COLOR_HIGHLIGHTED_BACKGROUND;
                indBorder       = Display.COLOR_HIGHLIGHTED_BORDER;
                indForeground   = Display.COLOR_HIGHLIGHTED_FOREGROUND;
        }

        final int back = theme_.getColor(indBackground);
        if (0 != (back & 0xFF000000)) {
            g.setColor(back);
            g.fillRect(0, 0, w, h);
        }

        final int border = theme_.getColor(indBorder);
        if (0 != (border & 0xFF000000)) {
            g.setColor(border);
            g.drawRoundRect(0, 0, w-1, h-1, BORDER/2, BORDER/2);
        }

        g.setColor(theme_.getColor(indForeground));
        g.setFont(fontTitle_);
    }

    protected int getPrefContentHeight(int width) {
        return getMinContentHeight();
    }

    protected int getPrefContentWidth(int height) {
        return getMinContentWidth();
    }

    protected int getMinContentHeight() {
        return height_;
    }

    protected int getMinContentWidth() {
        return form_.getWidth();
    }

    public final boolean isFocused() {
        return isFocused_;
    }

    // The traverse() method is called by the system when traversal has entered
    // the item or has occurred within the item
    protected boolean traverse(int dir, int portWidth, int portHeight,
            int[] visRect_inout) {
        isFocused_ = true;
        return super.traverse(dir, portWidth, portHeight, visRect_inout);
    }

    protected void traverseOut() {
        if (isFocused_) {
            isFocused_ = false;
            repaint();
        }
    }

    public static final GenericItem getSelected(Form form) {
        for (int i =0; i < form.size(); ++i) {
            try {
                GenericItem item = (GenericItem) form.get(i);
                if (item.isFocused())
                    return item;
            }catch (Exception e) {
            }
        }
        return null;
    }
}
