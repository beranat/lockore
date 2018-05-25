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

import madrat.sys.ImageCache;

/**
 * Two-lines item with description
 */
public class LongMenuItem extends GenericItem {

    private int currentWidth_ = -1;
    private String titleLine0_;
    private String titleLine1_;


    private String descLine_;
    private int    descX_;
    private int    descLayout_;

    public LongMenuItem(Theme theme, Form form, String title) {
        this(theme, form, title, "", null);
    }

    public LongMenuItem(Theme theme, Form form, String title, String desc, String icon) {
        super(theme, form, title, null);
        icon_ = ImageCache.getImage(icon, 2*fontTitle_.getHeight());
        height_ = 2*fontTitle_.getHeight() + fontDesc_.getHeight() + 4*BORDER;
        desc_ = desc;
    }

    public void setLabel(String label) {
        setTitle(label);
    }

    public void setTitle(String title) {
        if (!title_.equals(title)) {
            title_ = title;
            currentWidth_ = -1;
            invalidate();
        }
    }

    public void setDesc(String desc) {
        desc = (null != desc && 0 < desc.length())?desc:null;
        if (desc != desc_) {
            desc_ = desc;
            currentWidth_ = -1;
            invalidate();
        }
    }

    public void paint(Graphics g, int w, int h) {
        super.paint(g, w, h);

        if (w != currentWidth_)
            layout(w);

        int x = w;

        if (null != icon_) {
            x = x - icon_.getWidth() - BORDER;
            g.drawImage(icon_, x, BORDER + BORDER / 2 + fontTitle_.getHeight(), Graphics.LEFT | Graphics.VCENTER);
        }

        if (null != desc_) {
            g.setFont(fontDesc_);
            g.drawString(descLine_, descX_, 3 * BORDER + 2 * fontTitle_.getHeight(), descLayout_);
        }

        g.clipRect(0, 0, x, h);
        g.setFont(fontTitle_);

        g.drawString(titleLine0_, BORDER, BORDER, Graphics.TOP | Graphics.LEFT);
        if (null != titleLine1_)
            g.drawString(titleLine1_, BORDER,  2 * BORDER + fontTitle_.getHeight(), Graphics.TOP | Graphics.LEFT);
    }

    protected boolean isOkLayout(int w) {
        if (fontTitle_.stringWidth(titleLine0_) > w)
            return false;

        if (null == titleLine1_)
            return true;

        return fontTitle_.stringWidth(titleLine1_) <= w;
    }

    protected static final String LABEL_SEPARATORS = " @.-+/\\";

    protected void layout(int w) {
        currentWidth_ = w;

        // description with '...'
        if (null != desc_) {
            descLayout_ = Graphics.TOP | Graphics.LEFT;
            descX_ = BORDER;
            descLine_ = desc_.trim();
            final int descWidth = w - descX_;
            if (getLayout() == LAYOUT_RIGHT) {
                descX_ = descWidth;
                descLayout_ = Graphics.TOP | Graphics.RIGHT;
            }

            if (fontDesc_.stringWidth(descLine_) > descWidth) {
                for (int i = descLine_.length()-1; i > 3; --i ) {
                    descLine_ = descLine_.substring(0, i) + "...";
                    if (fontDesc_.stringWidth(descLine_) <= descWidth)
                        break;
                }
            }
        }
        else
            descLine_ = null;

        // split title into 2 lines
        final int width = w - 2*BORDER-((null != icon_)?(BORDER+icon_.getWidth()):0);

        titleLine0_ = title_;
        titleLine1_ = null;

        if (isOkLayout(width))
            return;

        for (int i =0; i < LABEL_SEPARATORS.length(); ++i) {
            final char separator = LABEL_SEPARATORS.charAt(i);

            for (int j = title_.lastIndexOf(separator);
                     j > 0; // 'space'message should be trimmed before
                     j = title_.lastIndexOf(separator, j-1)){
                titleLine0_ = title_.substring(0, j);
                titleLine1_ = title_.substring(j + (separator != ' '?0:1));
                if (isOkLayout(width))
                    return;
            }
        }

        //can not find separator, force separation in-word
        // fit max at 1st line
        for (int i = title_.length()-1; i > 1; --i ) {
            titleLine0_ = title_.substring(0, i);
            titleLine1_ = title_.substring(i+1);
            if (fontTitle_.stringWidth(titleLine0_) <= width)
                return;
        }

    }
}
