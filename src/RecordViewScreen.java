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

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import madrat.i18n.I18N;
import madrat.sys.QuickSort;
import madrat.storage.Field;
import madrat.storage.Record;
import madrat.storage.Storage;
import madrat.gui.GenericItem;

/**
 * Shows record
 */
final class RecordViewScreen extends LockoreForm {
    protected Record record_;
    protected final Command view_,
                            edit_;

    public RecordViewScreen(LockoreForm owner, Record record) {
        super(record.getName(), owner, Command.BACK, HelpIndexList.RECORDS);
        view_ = new Command(I18N.get("View"), Command.ITEM, 1);
        edit_ = new Command(I18N.get("Edit"), Command.ITEM, 3);

        reloadRecord(record);
        updateUI(null);
    }

    protected final void reloadRecord(Record r) {
        final Storage store = Midlet.getStore();
        try {
            System.gc();
            record_ = store.load(r);
        } catch (Exception e) {
            throw new RuntimeException(I18N.get("Load error"));
        }
    }

    public void updateUI(Object hint) {
        if (hint != null && hint instanceof Record) {
            reloadRecord((Record)hint);
            if (null != back_)
                back_.updateUI(hint);
        }

        super.setTitle(record_.getName());

        deleteAll();

        final int count = record_.size();
        if (0 == count) {
            removeCommand(edit_);
            append(createNewItem("No fields", "Edit record for adding fields", edit_));
            return;
        }

        addCommand(edit_);

        Vector v = new Vector(count);
        for (int i = 0; i < count; ++i) {
            v.addElement(createField(record_.at(i), view_));
        }
        QuickSort.sort(v, new FieldSort());
        append(v);
    }

    public void commandAction(Command c, Displayable d) {
        if (edit_ == c) {
            new RecordEditScreen(this, record_).show();
        } else {
            super.commandAction(c, d);
        }
    }

    protected void commandAction(Command c, GenericItem i) throws Exception {
        //for no items
        if (c == edit_) {
            commandAction(edit_, this);
            return;
        }

        final Field f = (Field) i.getUserInfo();
        if (null == f)
            return;

        if (view_ == c) {
            FieldViewAlert view = new FieldViewAlert(this, record_, f, formatField(f, true));
            Midlet.showAlert(view, this);
        }
        else
            super.commandAction(c, i);
    }
}
