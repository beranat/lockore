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

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Gauge;

import madrat.i18n.I18N;
import madrat.storage.Record;
import madrat.storage.Storage;

/**
 * Re-encrypt store with others params
 */
final class MoveStoreAlert extends Alert implements Runnable, CommandListener {
    protected final Gauge indicator_;
    protected final Thread thread_;
    protected Storage from_;
    protected Storage to_;
    protected boolean break_ = false;

    public MoveStoreAlert(Storage from, Storage to) {
        super(I18N.get("Copying"), I18N.get("Please wait"), null, AlertType.INFO);

        from_ = from;
        to_ = to;
        setTimeout(FOREVER);
        indicator_ = new Gauge(null, false, Math.max(1, from.size()), 0);
        setIndicator(indicator_);
        addCommand(Midlet.CANCEL);

        setCommandListener(this);

        thread_ = new Thread(this);
        thread_.start();
    }

    public void run() {
        try {
            Thread.sleep(250L);

            final int count = from_.size();
            if (count > 0) {
                final int diff = from_.getStorageSize() - to_.getStorageFree();
                if (diff>0)
                    throw new RuntimeException(I18N.get("Not enough space (additionally {0})", I18N.formatBytes(diff)));

                for (int i = 0; i < count; ++i) {
                    indicator_.setValue(i);

                    Record r = from_.get(i);
                    to_.insertNew(from_.cloneRecord(r, null));

                    if (break_)
                        throw new InterruptedException();
                }
            }
            to_.save();
            stopUI(null);
        } catch (InterruptedException ex) {
            stopUI(I18N.get("Interrupted by user"));
        } catch (Exception ex) {
            ex.printStackTrace();
            stopUI(ex.getMessage());
        }
    }

    protected void stopUI(String msg) {
        removeCommand(Midlet.CANCEL);
        setIndicator(null);

        final boolean isSuccess = (null == msg);
        int timeout = FOREVER;
        AlertType type = AlertType.ERROR;

        if (isSuccess) {
            msg = I18N.get("Completed");
            setType(AlertType.INFO);
            timeout = getDefaultTimeout();
            type = AlertType.ALARM;
        }

        setType(type);
        setTimeout(timeout);
        releaseStores(isSuccess);
        Midlet.setStore(null);
        setString(msg);
    }

    public void commandAction(Command c, Displayable d) {
        if (c.getCommandType() == Command.CANCEL && thread_.isAlive()) {
            break_ = true;
            try {
                thread_.join();
            } catch (InterruptedException ex) {
            }
            releaseStores(false);
            return ;
        }
        Midlet.showStoreListScreen();
    }

    private void releaseStores(boolean keepTo) {
        if (null == from_ || null == to_)
            return;

        Storage close = from_;
        Storage wipe = to_;

        if (keepTo) {
            close = to_;
            wipe = from_;
        }

        if (null != close)
            close.destroy();

        if (null != wipe)
            wipe.wipe();
    }
}
