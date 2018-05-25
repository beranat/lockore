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

import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;

import madrat.i18n.I18N;
import madrat.storage.Field;

/**
 * Enumerate images and shows it for selecting
 */
final class SelectImageList extends ListScreen {
    protected final Midlet midlet_;
    protected final int height_;
    protected final LockoreForm owner_;
    protected final Field     value_;

    public SelectImageList(LockoreForm owner, Field value) {
        super(I18N.get("Images"), Command.OK);
        midlet_ = Midlet.getInstance();
        owner_ = owner;
        value_ = value;

        height_ = Math.min(  midlet_.getTheme().getAlertElement(),
                             midlet_.getTheme().getImageHeight());

        addIcons(Midlet.ICON_NULL + 1, Midlet.ICON_INTERNAL_INDEX);
        addIcons(Midlet.ICON_INTERNAL_INDEX, Midlet.ICON_LIMIT_INDEX);

        final int iconValue = (null != value_)?value_.getIcon():Midlet.ICON_NULL;
        if (iconValue > Midlet.ICON_NULL) {
            final Integer selected = new Integer(iconValue);
            setSelected(selected);
        }

        addCommand(Midlet.CANCEL);
        addCommand(new Command(I18N.get("None"),    Command.STOP, 1));
        addCommand(new Command(I18N.get("Preview"), Command.ITEM, 1));
        addCommand(new Command(I18N.get("Scroll"),  Command.SCREEN, 1));
    }

    private void addIcons(int start, int end) {
        final int element = midlet_.getTheme().getListElement();
        final Vector names = new Vector();
        final Hashtable ids = new Hashtable();

        for (int i = start; i < end; ++i) {
            final Image icon = Midlet.getIcon(i, -1);
            if (null != icon) {
                final String name = I18N.get(Midlet.getIconName(i));
                final Integer id = new Integer(i);
                names.addElement(name);
                ids.put(name, id);
            }
        }

        madrat.sys.QuickSort.sort(names, new madrat.sys.QuickSort());
        for (int i = 0; i < names.size(); ++i) {
            final String name = (String)names.elementAt(i);
            final Integer id = (Integer)ids.get(name);
            append(name, id.intValue(), id);
        }
    }

    public void commandAction(Command c, Displayable d) {
        final int type = c.getCommandType();
        if (Command.CANCEL == type) {
            owner_.show();
            return;
        }

        Integer id = (Integer)getSelected();
        if (null == id)
            return;

        switch (type) {
            case Command.SCREEN:
                final int s = size();
                if (s > 1)
                    setSelectedIndex((getSelectedIndex() + s / 4 + 1 ) % s, true);
                break;
            case Command.ITEM:
                    Alert preview =
                            new Alert(I18N.get("Preview"),
                                    getString(getSelectedIndex()),
                                    Midlet.getIcon(id.intValue(), height_), AlertType.INFO);
                    Midlet.showAlert(preview, this);
                break;
            case Command.STOP:
                id = new Integer(Midlet.ICON_NULL);
            case Command.OK:
                value_.setIcon(id.intValue());
            default:
                owner_.show(value_);
                break;
        }
    }
}
