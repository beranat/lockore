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

import java.security.SignatureException;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;

import madrat.i18n.I18N;
import madrat.storage.Storage;
import madrat.sys.SecureRandom;
import madrat.sys.Sha3;

/**
 * 'Enter password' for store
 *   + record user's typing for random seed noise
 */
final class PasswordScreen extends Form implements
        CommandListener, ItemCommandListener, ItemStateListener,
        TaskNotification {
    protected final TextField field_;
    protected final byte[] seed_;
    protected int index_;

    public PasswordScreen() {
        super(I18N.get("Password"));
        index_ = 0;
        seed_ = new byte[32]; // 256 bit

        addCommand(Midlet.CANCEL);
        addCommand(Midlet.EXIT);

        append(new StringItem(I18N.get("Enter password"), null));
        append(new Spacer(1, 1));
        append(field_ = new TextField(null, null, 512,
                TextField.PASSWORD | TextField.ANY | TextField.SENSITIVE | TextField.NON_PREDICTIVE));
        field_.setDefaultCommand(Midlet.OK);
        field_.setItemCommandListener(this);
        setItemStateListener(this);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        // Random re-seed
        try {
            itemStateChanged(null);
            final SecureRandom r = SecureRandom.getInstance();
            for (int i =0; i < index_; ++i)
                r.getLong();

            Sha3 hash = new Sha3(256);
            hash.update(seed_, 0, seed_.length);
            hash.finalize(seed_, 0);

            r.reseed(seed_);
        }
        catch(Exception e) {
            Midlet.fatal(e, "rng@PasswordScreen");
            return;
        }

        try {
            switch (c.getCommandType()) {
                case Command.OK:
                    final Storage store = Midlet.getStore();
                    final byte[] passwd = field_.getString().getBytes("UTF-8");
                    final int count = (int)store.getValue(Midlet.F_PBKDF2_COUNT,
                                        Pbkdf2CountingScreen.ITERATION_COUNT);
                    final String salt = store.getValue(Midlet.F_PBKDF2_SALT, null);

                    Midlet.setCurrent(
                        new Pbkdf2CountingScreen(this, passwd, salt, count));
                    field_.setString(null);
                    break;
                default:
                    Midlet.showStoreListScreen();
                    break;
                case Command.EXIT:
                    Midlet.getInstance().commandAction(c, d);
                    break;
            }
        } catch (Exception e) {
            Midlet.showAlert(e, this);
        }
    }

    public void commandAction(Command c, Item item) {
        commandAction(c, this);
    }

    public void itemStateChanged(Item item) {
        if (null != seed_) {
            final long now8 = System.currentTimeMillis();
            final int now4 = (int)((now8 >>> 32) ^ now8);
            final int now2 = (int)((now4 >>> 16) ^ now4);
            seed_[index_] = (byte)(now2 & 0xFF);
            index_ = (index_+1) % seed_.length;
            seed_[index_] = (byte)((now2 >>8) & 0xFF);
            index_ = (index_+1) % seed_.length;
        }
    }

    public void onFailure(Object hash, Exception err) {
        Midlet.showAlert(err, this);
    }

    public void onSuccess(Object hash, String unused0, long count, byte[] password) {
        if (null == hash) {
            Midlet.showStoreListScreen();
            return;
        }
        try {
            final Storage store = Midlet.getStore();
            store.unlock(password, (byte[])hash);
            new StoreViewScreen().show();
        }
        catch (SignatureException e) {
            onFailure(hash, new SignatureException(I18N.get("Invalid password")));
        }
        catch (Exception e) {
            onFailure(hash, e);
        }
    }
}
