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

import madrat.i18n.I18N;

/**
 * Confirmation screen with callback for LockoreForm
 */
final class ConfirmAlert extends Alert implements CommandListener {
    private final LockoreForm   parent_;
    private final int           userInt_;
    private final Object        userObject_;

    private final Command       backType_;

    /**
     * Confirmation Alert, support 2 modes
     * 1. 2 state (enAgreed 'OK'/ Cancel)
     * 2. 3 state (enAgreed 'OK'/enNotAgree 'Cancel/No' and 'Back')
     */
    private ConfirmAlert(LockoreForm parent, String desc,
            String enAgreed, String enNotAgree, int userInt, Object userObject) {
        super(I18N.get("Confirmation"), desc, null, AlertType.WARNING);
        parent_ = parent;
        userInt_ = userInt;
        userObject_ = userObject;
        setTimeout(FOREVER);

        addCommand(new Command(I18N.get(enAgreed), Command.OK, 1));

        if (null != enNotAgree) {
            addCommand(new Command(I18N.get(enNotAgree), Command.CANCEL, 1));
            backType_ = new Command(I18N.get("Back"), Command.BACK, 1);
        }
        else
            backType_ = Midlet.CANCEL;

        addCommand(backType_);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        try {
            if (backType_ == c)
                parent_.show();
            else
                parent_.onConfirmed(userInt_, userObject_, Command.OK == c.getCommandType());
        }
        catch (Exception e) {
            Midlet.showAlert(e, this);
        }
    }

    static void showConfirmOkCancel(LockoreForm parent, String desc, String okEn, int userInt, Object userObject) {
        Midlet.showAlert(new ConfirmAlert(parent, desc, okEn, null, userInt, userObject), parent);
    }

    static void showConfirmYesNoBack(LockoreForm parent, String desc, int userInt, Object userObject) {
        Midlet.showAlert(new ConfirmAlert(parent, desc, "Yes", "No", userInt, userObject), parent);
    }

}
