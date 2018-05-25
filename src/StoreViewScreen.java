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

import madrat.sys.QuickSort;
import madrat.i18n.I18N;
import madrat.gui.GenericItem;
import madrat.storage.Record;
import madrat.storage.Storage;

/**
 * Show store's records
 */
final class StoreViewScreen extends LockoreForm {

    protected final Command properties_, stores_, add_, addFile_, open_, delete_, duplicate_;

    public StoreViewScreen() {
        super("", null, Command.BACK, HelpIndexList.STORES);

        add_ = new Command(I18N.get("New"),
                           I18N.get("Add record"),          Command.SCREEN, 2);
        addCommand(add_);

        if (Midlet.isSupportFileApi()) {
            addFile_ = new Command(I18N.get("Add file"), I18N.get("Load from file"), Command.SCREEN, 2);
            addCommand(addFile_);
        }
        else
            addFile_ = null;


        properties_ = new Command(I18N.get("Props"), I18N.get("Store options"), Command.SCREEN, 3);
        addCommand(properties_);

        stores_ = new Command(I18N.get("Back"), I18N.get("Back to stores"),    Command.SCREEN, 3);
        addCommand(stores_);

        open_       = new Command(I18N.get("Open"),     Command.ITEM, 1);
        duplicate_  = new Command(I18N.get("Copy"),     I18N.get("Duplicate"),  Command.ITEM, 2);
        delete_     = new Command(I18N.get("Delete"),   Command.ITEM, 3);

        updateUI(null);
    }

    public void updateUI(Object hint) {
        final Storage store = Midlet.getStore();
        super.setTitle(store.getName());

        deleteAll();

        if (store.isLocked()) {
            append(createItem(
                    I18N.get("Locked"),
                    I18N.get("No access to records"),
                    Midlet.ICON_LOCKED_STORE, stores_));
            return;
        }

        final int count = store.size();
        if (0 == count) {
            append(createNewItem("Add record", "No records available", add_));
            return;
        }

        final Vector v = new Vector(count);
        for (int i = 0; i < count; ++i) {
            final Record r = store.get(i);
            v.addElement(createRecord(r));
        }
        QuickSort.sort(v, this);
        append(v);
    }

    private GenericItem createRecord(Record record) {

        final String name = record.getName();
        final String desc = record.getDescription();
        final int icon = record.getIcon();

        GenericItem item = createItem(name, desc, icon, open_);
        item.setUserInfo(record);
        item.addCommand(duplicate_);
        item.addCommand(delete_);
        return item;
    }

    public void commandAction(Command c, Displayable d) {
        try {
            if (stores_ == c) {
                Midlet.showStoreListScreen();
            } else if (properties_ == c) {
                new StoreEditScreen(this).show();
            } else if (add_ == c) {
                new RecordEditScreen(this, null).show();
            } else if (addFile_ == c) {
                Midlet.setCurrent(new AddFileList(this, null));
            }
            else
                super.commandAction(c, d);
        }
        catch (Exception e) {
            Midlet.showAlert(e, this);
        }
    }

    protected void commandAction(Command c, GenericItem i) throws Exception {
        if (c == add_) {
            commandAction(add_, this);
            return;
        }

        final Record record = (Record)i.getUserInfo();
        if (null == record)
            return;

        final String name = i.getLabel();

        if (open_ == c) {
            new RecordViewScreen(this, record).show();
        }
        else if (duplicate_ == c) {
            final Storage store = Midlet.getStore();
            Record nr = store.cloneRecord(record, I18N.get("{0}'s copy", record.getName()));
            store.insertNew(nr);
            append(createRecord(nr));
        }
        else if (delete_ == c) {
            ConfirmAlert.showConfirmOkCancel(this, I18N.get("Delete '{0}'?", name), "Yes", 0, record);
        }
        else
            super.commandAction(c, i);
    }

    public void onConfirmed(int action, Object userinfo, boolean isAgreed) throws Exception {
        Record record = (Record)userinfo;
        final Storage store = Midlet.getStore();
        store.remove(record);
        show(null);
    }
}
