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


import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import madrat.i18n.I18N;
import madrat.storage.Field;

/**
 * All store's types list and notify then select one
 */
final class ChangeTypeList extends ListScreen {
    protected final TaskNotification notif_;
    protected final Field field_;
    protected final Object userinfo_;

    public ChangeTypeList(TaskNotification notif, Field field, Object userinfo) {
        super(I18N.get("Field Type"), Command.OK);
        notif_ = notif;
        field_ = field;
        userinfo_ = userinfo;

        addType("Text", Field.STRING, Midlet.DEFAULT_FORMAT);
        addType("Integer", Field.INTEGER, Midlet.DEFAULT_FORMAT);
        addType("Binary", Field.BINARY, Midlet.DEFAULT_FORMAT);

        addType("Date", Field.INTEGER,  Midlet.DATE_FORMAT);
        addType("URI",  Field.STRING,   Midlet.URI_FORMAT);
        addType("Phone", Field.STRING,  Midlet.PHONE_FORMAT);
        addType("Email", Field.STRING,  Midlet.EMAIL_FORMAT);

        addType("OTP", Field.STRING,  Midlet.OTP_FORMAT);

        addType("TOTP (RFC-6238)", Field.BINARY,  Midlet.TOTP_FORMAT);

        addCommand(Midlet.CANCEL);
    }

    protected final void addType(String name, int type, int format) {

        final long value = (((long)format)<<32) | (type & 0xFFFFFFFFL);

        final int index = append(I18N.get(name),
                                 Midlet.getTypeIcon(type, format, false),
                                 new Long(value));

        final int ctype = field_.getType();
        final int cformat = (format == Midlet.DEFAULT_FORMAT)?0:field_.getFormat();

        if (ctype == type && cformat == format)
            this.setSelectedIndex(index, true);
    }

    public void commandAction(Command c, Displayable d) {
        if (c.getCommandType() == Command.OK) {
            final Long type = (Long)getSelected();
            notif_.onSuccess(userinfo_, null, type.longValue(), null);
        }
        else
            notif_.onSuccess(null, null, Field.NONE, null);
    }
}
