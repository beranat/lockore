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
import madrat.storage.Storage;
import madrat.storage.StorageException;

/**
 * Encrypted containers list screen
 */
final class StoresListScreen extends LockoreForm {
    private final Command open_, details_, delete_, create_, settings_;

    public StoresListScreen() {
        super(I18N.get("Stores"), null, Command.EXIT, HelpIndexList.GET_STARTED);

        create_ = new Command(I18N.get("Create"),       Command.SCREEN, 1);
        settings_ = new Command(I18N.get("Settings"),   Command.SCREEN, 2);
        addCommand(settings_);

        open_ = new Command(I18N.get("Open"), Command.ITEM, 1);
        details_ = new Command(I18N.get("Details"), Command.ITEM, 2);
        delete_ = new Command(I18N.get("Delete"),  Command.ITEM, 3);

        setCommandListener(this);
        updateUI(null);
    }

    public void commandAction(Command c, Displayable d) {
        // 'Common' Actions, not for items
        if (settings_ == c) {
            new SettingsScreen(this).show();
        }
        else if (create_ == c) {
            Midlet.setStore(null);
            new StoreEditScreen(this).show();
        }
        else
            super.commandAction(c, d);
    }

    protected void commandAction(Command c, GenericItem i) throws Exception {
        Midlet.setStore(null);

        if (c == create_) {
            commandAction(create_, this);
            return;
        }

        final String rmsStore = (String)i.getUserInfo();
        if (null == rmsStore)
            return;

        final String name = i.getLabel();

        if (delete_ == c) {
            ConfirmAlert.showConfirmOkCancel(this, I18N.get("Delete store '{0}' and all data on it?", name), "Delete", 0, rmsStore);
            return;
        }

        Storage store = null;

        try {
            store = new Storage(rmsStore);

            if (details_ == c) {
                LockoreForm info = new LockoreForm(I18N.get("Info"), this, Command.OK, null);
                info.append(info.createTitle(name, store.getIcon(), null));
                info.appendInfoline("JSR-118 name", rmsStore, Midlet.ICON_SETTINGS);
                info.appendInfoline("Description", store.getDescription(), Midlet.ICON_TEXT);

                final long keylen = 8*store.getValue(Storage.F_CIPHER_KEYLEN, 0);

                info.appendInfoline("Cipher", store.getValue(Storage.F_CIPHER, "") + "-" + Long.toString(keylen), Midlet.ICON_LOCKED_STORE);
                info.appendInfoline("Hash",   store.getValue(Storage.F_HASH, "")+ "-" + Long.toString(keylen*2), Midlet.ICON_STORE);
                info.appendInfoline("PBKDF2 hash", store.getValue(Midlet.F_PBKDF2_HASH, null), Midlet.ICON_PASSWORD);

                int pbkdf2Count = (int)store.getValue(Midlet.F_PBKDF2_COUNT, 0);
                String hashTime = "";
                long hashPerf = Pbkdf2CountingScreen.getPerfomance();
                if (hashPerf > 0 && pbkdf2Count>0) {
                    hashTime = "/" + I18N.formatPeriod(pbkdf2Count/hashPerf);
                }
                info.appendInfoline("PBKDF2 count", I18N.formatSI(pbkdf2Count) + hashTime, Midlet.ICON_NUMBER);
                info.appendInfoline("Used space",I18N.formatBytes(store.getStorageSize()), Midlet.ICON_BINARY);
                info.appendInfoline("Free space", I18N.formatBytes(store.getStorageFree()), Midlet.ICON_STORE);

                info.show();
            } else if (open_ == c) {
                Midlet.setStore(store);
                store = null;
                Midlet.setCurrent(new PasswordScreen());
            }
            else
                super.commandAction(c, i);
        }
        finally {
            if (null != store)
                store.destroy();
        }
    }

    public void updateUI(Object unused) {
        deleteAll();

        final String[] list = javax.microedition.rms.RecordStore.listRecordStores();
        final int count = (null != list)?list.length:0;

        Vector v = new Vector(count);

        for (int i = 0; i < count; ++i) {
            final String storeName = list[i];

            if (!Midlet.isStore(storeName))
                continue;

            String userName     = storeName;
            String desc;
            GenericItem item = null;

            try {
                Storage store = new Storage(storeName);

                userName = store.getName();
                desc = store.getDescription();
                final int icon = store.getIcon();
                final String pbkdf2Hash = store.getValue(Midlet.F_PBKDF2_HASH, null);
                store.destroy();

                if (!Pbkdf2CountingScreen.HASH_NAME.equals(pbkdf2Hash))
                    throw new IllegalArgumentException(I18N.get("PBKDF2 '{0}' not supported", pbkdf2Hash));

                item = createStore(userName, desc, icon, open_, true, storeName);
            }
            catch (IllegalArgumentException ia) {
                desc = ia.getMessage();
            }
            catch (StorageException inVal) {
                desc = I18N.get("Unsupported format");
            } catch (Exception ex) {
                String text = ex.getMessage();
                if (null == text)
                    text = ex.toString();
                desc = I18N.get("Error {0}", text);
            }

            if (null == item)
                item = createStore(userName, desc, Midlet.ICON_ALERT, delete_, false, storeName);
            v.addElement(item);
        }

        if (0 == v.size()) {
            removeCommand(create_);
            append(createNewItem("Create store", "No stores available", create_));
            return;
        }

        addCommand(create_);
        QuickSort.sort(v, this);
        append(v);
    }

    private GenericItem createStore(String name, String desc, int icon, Command def, boolean isValid, String storeName) {
        GenericItem item = createItem(name, desc, icon, def);
        if (isValid) {
            item.addCommand(open_);
            item.addCommand(details_);
            item.addCommand(delete_);
        }
        item.setUserInfo(storeName);
        return item;
    }

    public void onConfirmed(int action, Object userinfo, boolean isAgreed) {
        Storage.wipeStore((String)userinfo);
        show(null);
    }

    public void show() {
        Midlet.setStore(null);
        super.show();
    }
}
