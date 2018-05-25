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
import madrat.sys.SecureRandom;
import madrat.storage.Field;
import madrat.sys.Convertion;
import madrat.sys.PasswordQuality;

/**
 * Passwords generation screen
 */
final class GeneratePassList extends ListScreen {
    private static final int LETTERS_BIT = 0x10000;
    private static final int SPEC_BIT    = 0x20000;
    private static final int HEX_BIT     = 0x40000;
    private static final int LENGTH_MASK = 0x0FFFF;

    private final TaskNotification notif_;

    private final Command append_;

    public GeneratePassList(TaskNotification notif, int type) {
        super(I18N.get("Strength"), Command.OK);

        notif_ = notif;

        addCommand(Midlet.CANCEL);
        append_ = new Command(I18N.get("Append"), Command.OK, 2);
        addCommand(append_);

        // numbers = 3.32 b/c
        // num+let = 5.95 b/s
        // all     = 6.52 b/s
        append("Pin (4 digits)",  Midlet.ICON_PHONE,     4);
        append("Code (8 digits)", Midlet.ICON_NUMBER,   8);

        switch (type) {
            default:
            case Field.INTEGER:
                append("Moderate (80 bits)",Midlet.ICON_PASSWORD, 24);
                break;
            case Field.STRING:
                append("Simple (8 chars)",      Midlet.ICON_TEXT,            8 | LETTERS_BIT);
                append("Moderate (14 chars)",   Midlet.ICON_PASSWORD,       14 | LETTERS_BIT);
                append("Good (128 bits)",       Midlet.ICON_KEY,            22 | LETTERS_BIT | SPEC_BIT);
                append("Strong (256 bits)",     Midlet.ICON_STORE,          48 | LETTERS_BIT | SPEC_BIT);
                break;
            case Field.BINARY:
                deleteAll(); // no pin/code
                append("Code (32 bits)", Midlet.ICON_NUMBER, 8 | HEX_BIT);          // 4 bit x 8 items
                append("Moderate (80 bits)", Midlet.ICON_PASSWORD, 20 | HEX_BIT);
                append("Good (128 bits)", Midlet.ICON_KEY, 32 | HEX_BIT);
                append("Strong (256 bits)", Midlet.ICON_STORE, 64 | HEX_BIT);
                break;
        }
    }

    protected final void append(String name, int icon, int value) {
        super.append(I18N.get(name), icon, new Integer(value));
    }

    public static final int CANCEL  = 0;
    public static final int SET     = 1;
    public static final int APPEND  = 2;

    public void commandAction(Command c, Displayable d) {
        try {
            final int mask = ((Integer)super.getSelected()).intValue();

            String pass = null;
            byte[] data = null;
            int mode = CANCEL;

            switch (c.getCommandType()) {
                case Command.OK:
                    mode = (c == append_)?APPEND:SET;
                    final SecureRandom rand = SecureRandom.getInstance();
                    final int length = mask & LENGTH_MASK;

                    if (0 != (mask & HEX_BIT)) {
                        data  = Midlet.alloc(length/2);
                        rand.getBytes(data, 0, data.length);
                        pass = Convertion.encodeHex(data);
                    }
                    else {
                        pass = rand.getString(length,
                                0 != (mask & LETTERS_BIT),
                                0 != (mask & SPEC_BIT)?PasswordQuality.SPECIAL:null);
                    }
                    break;
                default:
                case Command.CANCEL:
                    break;
            }
            notif_.onSuccess(null, pass, mode, data);
        }
        catch (Exception e) {
            notif_.onFailure(null, e);
        }
    }
}
