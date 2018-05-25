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

import java.util.Date;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.DateField;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

import madrat.storage.IntegerField;

/**
 * Date edit screen (for Field.DATE type)
 */
final class DateScreen extends Form implements CommandListener {
    protected final LockoreForm     owner_;
    protected final IntegerField    item_;
    protected final DateField       field_;

    public DateScreen(LockoreForm owner, IntegerField item) {
        super(item.getName());
        owner_ = owner;
        item_ = item;

        field_ = new DateField(null, DateField.DATE);
        field_.setDate(new Date(item_.getLong()));
        append(field_);

        addCommand(Midlet.OK);
        addCommand(Midlet.CANCEL);

        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {

        switch (c.getCommandType()) {
            case Command.OK:
                item_.set(field_.getDate().getTime());
                owner_.show(item_);
                break;
            case Command.CANCEL:
                owner_.show();
        }
    }
}
