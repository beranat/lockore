/*
 * Copyright (C) 2017 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>
 *
 * This file is part of Lockore application.
 *
 * Lockoree is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Lockore distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Lockore.
 * If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Enumeration;
import java.util.Hashtable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import madrat.i18n.I18N;

/**
 * Extended list - implements hash for 'menu item' -> value for unique list
 */
abstract class ListScreen extends List implements CommandListener {
    private final Hashtable indexes_;

    protected ListScreen(String caption, int id) {
        super(I18N.get(caption), List.IMPLICIT);
        indexes_ = new Hashtable();

        setSelectCommand(new Command(I18N.get("Select"), id, 1));
        setCommandListener(this);
    }

    public Object getSelected() {
        final int index = getSelectedIndex();
        if (-1 == index)
            return null;
        return indexes_.get(getString(index));
    }

    public void setSelected(Object obj) {
        if (null == obj || indexes_.size() <= 0)
            return;

        final Enumeration keys = indexes_.keys();
        while (keys.hasMoreElements()) {
            final String key = (String)keys.nextElement();
            if (!obj.equals(indexes_.get(key)))
                continue;

            final int len = super.size();
            for (int i = 0; i < len; ++i) {
                if (key.equals(getString(i))) {
                    super.setSelectedIndex(i, true);
                    break;
                }
            }
            return;
        }
    }

    public int append(String stringPart, int image, Object info) {
        return insert(-1, stringPart, Midlet.getIcon(image, -1), info);
    }

    public void delete(int elementNum) {
        final String key = getString(elementNum);
        super.delete(elementNum);
        indexes_.remove(key);
    }

    public void deleteAll() {
        super.deleteAll();
        indexes_.clear();
    }

    public int insert(int elementNum, String stringPart, Image imagePart, Object info) {
        if (indexes_.containsKey(stringPart))
            throw new IllegalArgumentException(stringPart);
        indexes_.put(stringPart, info);

        if (-1 == elementNum)
            return super.append(stringPart, imagePart);
        else
            super.insert(elementNum, stringPart, imagePart);

        return elementNum;
    }
}
