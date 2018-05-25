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

import java.io.OutputStream;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import madrat.i18n.I18N;
import madrat.storage.Field;
import madrat.storage.Record;

/**
 * Show field's content (with special buttons)
 */
final class FieldViewAlert extends Alert implements CommandListener {
    private final LockoreForm parent_;
    private final Record record_;
    private final Field field_;

    private String uri_;
    private Command request_, spend_, save_;

    public FieldViewAlert(LockoreForm parent, Record r, Field f, String content) {
        super(f.getName(), content, null, AlertType.INFO);
        parent_ = parent;
        record_ = r;
        field_ = f;


        if (null == content || 0 == content.length()) {
            setString(I18N.get("(empty)"));
            return;
        }

        addCommand(Midlet.BACK);
        setCommandListener(this);

        setTimeout(FOREVER);

        final int format = f.getFormat();
        switch (f.getType()) {
            case Field.STRING: {
                switch (format) {
                    case Midlet.PHONE_FORMAT:
                        request_ = new Command(I18N.get("Call"), Command.ITEM, 2);
                        uri_ = LockoreForm.getPhoneUri(content);
                        break;
                    case Midlet.EMAIL_FORMAT:
                        request_  = new Command(I18N.get("Send"), Command.ITEM, 2);
                        uri_ = "mailto:" + content;
                        break;
                    case Midlet.URI_FORMAT:
                        request_ = new Command(I18N.get("Open"), Command.ITEM, 2);
                        uri_ = (LockoreForm.getUriSchema(content) != -1) ? content : ("https://" + content);
                        break;
                    case Midlet.OTP_FORMAT:
                        spend_ = new Command(I18N.get("Spend"), Command.ITEM, 2);
                        break;
                }
                break;
            }
            case Field.BINARY: {
                if (f.getFormat() != Midlet.TOTP_FORMAT && Midlet.isSupportFileApi()) {
                    save_ = new Command(I18N.get("Save"), Command.ITEM, 2);
                }
                break;
            }
        }

        if (null != request_)
            addCommand(request_);
        if (null != spend_)
            addCommand(spend_);
        if (null != save_)
            addCommand(save_);
    }

    public void commandAction(Command c, Displayable d) {
        try {
            if (request_ == c) {
                final Midlet m = Midlet.getInstance();
                if (m.platformRequest(uri_)) {
                    m.destroyApp(true);
                    return;
                }
            }
            else if (spend_ == c) {
                final String content = field_.getString();
                if (null != content) {
                    int index = content.indexOf('\n');
                    if (-1 != index)
                        field_.set(content.substring(index+1));
                    else {
                        field_.set(null);
                    }
                    Midlet.getStore().updateExisting(record_);
                }
                parent_.show(field_);
                return;
            }
            else if (save_ == c) {
                final String name = Midlet.getLocalDirName() + "/" + field_.getName();
                OutputStream os = Connector.openOutputStream(Midlet.getLocalDirUri() + field_.getName());

                if (null == os)
                    throw new IllegalAccessException(I18N.get("No access to {0}", name));

                try {
                    byte[] data = Midlet.alloc(field_.getBytes(null, 0));
                    field_.getBytes(data, 0);
                    os.write(data);
                }
                finally {
                    os.close();
                }
                Midlet.showAlert(AlertType.INFO, I18N.get("{0} saved", name), null, parent_);
                return;
            }
            parent_.show();
        }
        catch (ConnectionNotFoundException e) {
            Midlet.showAlert(new IllegalStateException(I18N.get("Feature not supported")),
                        parent_);
        }
        catch (Exception e) {
            Midlet.showAlert(e, parent_);
        }
    }
}
