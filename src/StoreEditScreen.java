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

import java.util.Random;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Ticker;

import madrat.i18n.I18N;
import madrat.sys.PasswordQuality;
import madrat.sys.SecureRandom;
import madrat.storage.Field;
import madrat.storage.Storage;
import madrat.storage.StringField;
import madrat.gui.GenericItem;

/**
 * Add new or Edit current store
 */
final class StoreEditScreen extends LockoreForm implements ItemStateListener, TaskNotification {
    protected final Command edit_;
    protected final Ticker notice_;

    protected final GenericItem name_;

    protected final TextField   desc_;
    protected final TextField   passwd_;
    protected final TextField   passwd2_;
    protected final TextField   pbkdf2_count_;
    protected final int         pbkdf2_pref_;

    protected final Field nameField_;
    protected final PasswordQuality pq_;

    public StoreEditScreen(LockoreForm back) {
        super(I18N.get("New Store"), back, Command.CANCEL, HelpIndexList.STORES);
        final Storage store = getStoreSafe();

        pbkdf2_pref_ = Pbkdf2CountingScreen.getPerfomance();
        pq_ = Midlet.getInstance().getPasswordQuality();
        edit_ = new Command(I18N.get("Edit"), Command.ITEM, 1);

        notice_ = new Ticker("");

        setItemStateListener(this);

        String  name = I18N.get("Unnamed"),
                desc = I18N.get(""),
                password = "",
                apply = "Create",
                title = null;
        int icon = Midlet.ICON_STORE;
        int pbkdf2Count = Pbkdf2CountingScreen.ITERATION_COUNT;

        if (null == store) {
            Random r = new Random(System.currentTimeMillis());
            pbkdf2Count = Pbkdf2CountingScreen.ITERATION_COUNT + (Pbkdf2CountingScreen.ITERATION_COUNT*(r.nextInt() & 0xFF)/2048);
        }
        else {
            title = name = store.getName();
            apply = "Save";
            desc = store.getDescription();
            pbkdf2Count = (int)store.getValue(Midlet.F_PBKDF2_COUNT, pbkdf2Count);
            icon = store.getIcon();
        }

        if (null != title)
            setTitle(title);

        nameField_ = new StringField(false, name, icon, Midlet.DEFAULT_FORMAT, "Unnamed");

        name_ = createTitle(name, icon, edit_);
        append(name_);
        desc_ = new TextField(I18N.get("Description"), desc, 255, TextField.ANY);
        append(desc_);

        passwd_ = new TextField("", password, 255, TextField.ANY | TextField.PASSWORD);
        passwd2_ = new TextField(I18N.get("Repeat password"), password, 255, TextField.ANY | TextField.PASSWORD);
        append(passwd_);
        append(passwd2_);

        append("Cipher", Storage.CIPHER + "-" + Storage.CIPHER_KEYLEN*8);
        append("Hash", Storage.HASH + "-" + Storage.CIPHER_KEYLEN*16);
        append("PBKDF2 hash", Pbkdf2CountingScreen.HASH_NAME);

        pbkdf2_count_ = new TextField(I18N.get("PBKDF2 count"),
                Integer.toString(pbkdf2Count), 16, TextField.NUMERIC);
        pbkdf2_count_.setItemCommandListener(this);
        append(pbkdf2_count_);

        addCommand(new Command(I18N.get(apply), Command.OK, 1));
        addCommand(new Command(I18N.get("Icon"), I18N.get("Change icon"), Command.SCREEN, 1));

        updateUI(null);
    }

    protected void append(String name, String value) {
        append(new TextField(I18N.get(name), I18N.get(value),
                Math.max(64, value.length()), TextField.ANY | TextField.UNEDITABLE));
    }

    public void updateUI(Object hint) {
        itemStateChanged(passwd_);

        name_.setIcon(Midlet.getIconName(nameField_.getIcon()));
        name_.setTitle(nameField_.getName());
    }

    protected void commandAction(Command c, GenericItem i) throws Exception {
        if (i == name_) {
            Midlet.setCurrent(new ValueScreen(this, I18N.get("Store Name"), nameField_, ValueScreen.NAME));
        }
        else
            super.commandAction(c, i);
    }

    public void commandAction(Command c, Displayable d) {
        try {
            switch (c.getCommandType()) {
                case Command.SCREEN:
                    Midlet.setCurrent(new SelectImageList(this, nameField_));
                    return;
                case Command.OK:
                    int count = Integer.parseInt(pbkdf2_count_.getString());
                    if (count <= 0)
                        throw new RuntimeException(I18N.get("PBKDF2 count is not positive"));

                    final byte[] passwd = passwd_.getString().getBytes("UTF-8");
                    final boolean isEmptyPasswd = (null == passwd || 0 == passwd.length);

                    final Storage store = getStoreSafe();
                    if (null != store) {
                        final int oldCount = (int)store.getValue(Midlet.F_PBKDF2_COUNT, Pbkdf2CountingScreen.ITERATION_COUNT);
                        if (    isEmptyPasswd && count == oldCount) {
                            store.setName(nameField_.getName());
                            store.setDescription(desc_.getString());
                            store.setIcon(nameField_.getIcon());
                            store.save();
                            back_.show(null);
                            break;
                        }
                    }

                    if (isEmptyPasswd)
                        throw new RuntimeException(I18N.get("Password is empty"));

                    Midlet.setCurrent(
                        new Pbkdf2CountingScreen(this, passwd, SecureRandom.getInstance().getBitString(256), count));
                    return;
                default:
                    super.commandAction(c, d);
                    break;
            }
        }
        catch (Exception e) {
            Midlet.showAlert(e, this);
        }
    }

    public void onFailure(Object hash, Exception e) {
        Midlet.showAlert(e, this);
    }

    public void onSuccess(Object hash, String salt, long count, byte[] passwd) {
        if (null == hash) {
            Midlet.showAlert(AlertType.WARNING, I18N.get("Canceled by user"), null, this);
            return;
        }
        try {
            String storeName = Midlet.generateStoreName();

            Storage.createStore(storeName,
                    nameField_.getName(),
                    desc_.getString(),
                    nameField_.getIcon());

            Storage newStore = new Storage(storeName);
            newStore.setValue(Midlet.F_PBKDF2_HASH, Pbkdf2CountingScreen.HASH_NAME);
            newStore.setValue(Midlet.F_PBKDF2_COUNT, count);
            newStore.setValue(Midlet.F_PBKDF2_SALT, salt);
            newStore.lock(passwd, (byte[])hash);

            final Storage store = getStoreSafe();
            if (null == store) {
                newStore.destroy();
                back_.show(null);
            }
            else {
                Midlet.showAlert(new MoveStoreAlert(store, newStore), this);
            }
        }
        catch (Exception e) {
            Midlet.showAlert(e, this);
        }
    }

    protected final void updateTicker(String message) {
        final Ticker current = getTicker();
        if (null != message) {
            if (!message.equals(notice_.getString()))
                notice_.setString(message);
            if (current != notice_)
                    setTicker(notice_);
        } else {
            if (null != current)
                setTicker(null);
        }
    }

    public void itemStateChanged(Item item) {
        final String p1 = passwd_.getString();
        final String p2 = passwd2_.getString();

        if (item == passwd2_ && !p1.equals(p2)) {
            updateTicker(I18N.get("Passwords are mismatch"));
            return;
        }

        if ((item == passwd_ || item == passwd2_) && null != pq_) {
            final int bits = pq_.getBits(p1);
            final boolean isNewStore = (null == getStoreSafe());
            final boolean isUnchanged = (0 == bits && !isNewStore);

            if (item == passwd_) {
                final String label = (isUnchanged)
                                        ?I18N.get("Password (unchanged)")
                                        :I18N.getN("Password ({0} bits)_N", Math.max(bits, 0));
                passwd_.setLabel(label);
            }


            String update = "Trivial password";
            if (bits >= PasswordQuality.GOOD || isUnchanged)  // Recomended for important and financial information
                update = null;
            else if (bits >= PasswordQuality.AVERAGE)
                update = "Average password - for non-sensible data";
            else if (0 == bits && isNewStore)
                update = "Password is empty";

            updateTicker(I18N.get(update));
        }

        if (item == pbkdf2_count_) {
            final String count = pbkdf2_count_.getString();
            try {
                int cnt = Integer.parseInt(count);
                if (cnt <= 0)
                    updateTicker(I18N.get("PBKDF2 count is not positive"));
                else {
                    if (pbkdf2_pref_ > 0) {
                        String calcTime = I18N.formatPeriod(cnt/pbkdf2_pref_);
                        updateTicker(I18N.get("{0} computing", calcTime));
                    }
                }
            }
            catch (NumberFormatException e) {
                updateTicker(I18N.get("'{0}' is not number", I18N.get("PBKDF2 count")));
            }
        }
    }
}
