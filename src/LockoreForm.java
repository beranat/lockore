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

import java.security.MessageDigest;
import java.util.Date;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;

import madrat.i18n.I18N;
import madrat.sys.IComparator;
import madrat.gui.GenericItem;
import madrat.gui.LongMenuItem;
import madrat.gui.ShortMenuItem;
import madrat.storage.Storage;
import madrat.storage.Field;

/**
 * Base class for all complex forms - support creating items, back feature
 */
class LockoreForm extends Form implements CommandListener, ItemCommandListener, IComparator {
    protected final LockoreForm back_;
    protected final Command backCommand_;
    protected final String helpIndex_;

    protected static final Storage getStoreSafe() {
        try {
            return Midlet.getStore();
        }
        catch (Exception e) {
            return null;
        }
    }

    public LockoreForm(String title, LockoreForm back, int backCmd, String helpIndex) {
        super(title);
        back_ = back;

        backCmd = (null == back)?Command.EXIT:backCmd;
        backCommand_ = new Command(I18N.get(getCommandText(backCmd)), backCmd, 1);
        addCommand(backCommand_);

        helpIndex_ = helpIndex;
        if (null != helpIndex)
            addCommand(new Command(I18N.get("Help"), Command.HELP, 2));

        setCommandListener(this);
    }

    static protected String getCommandText(int cmd) {
        switch (cmd) {
            case Command.CANCEL:
                return "Cancel";
            case Command.EXIT:
                return "Exit";
            case Command.STOP:
                return "Stop";
            default:
            case Command.BACK:
                return "Back";
        }
    }

    protected void updateUI(Object hint) {
    };

    public void show() {
        Midlet.setCurrent(this);
    }

    public void show(Object hint) {
        updateUI(hint);
        show();
        final GenericItem item = findItem(hint);
        if (null != item)
            Midlet.setCurrentItem(item);
    }

    protected void commandAction(Command c, GenericItem item) throws Exception {
        commandAction(c, this);
    }

    public final void commandAction(Command c, Item item) {
        try {
            if (item instanceof GenericItem)
                commandAction(c, (GenericItem)item);
        }
        catch (Exception e) {
            Midlet.showAlert(e, this);
        }
    }

    public void commandAction(Command c, Displayable d) {
        final int type = c.getCommandType();

        if (type == Command.HELP) {
            HelpIndexList.showHelp(this, helpIndex_);
            return;
        }

        if (c == backCommand_)  {
            if (type == Command.EXIT || null == back_)
                Midlet.getInstance().commandAction(c, d);
            else
                back_.show();
            return;
        }
    }

    protected static String hidePhone(String phone) {
        // XXXX-XXX-XX12
        final int len = phone.length();
        if (len > 5)
            return I18N.get("+****-***-**{0}", phone.substring(len-2));
        return I18N.get("****");
    }

    protected static String hideEmail(String email) {
        //  name: < 5 => *****
        //        >=  => m***t@domain.com
        final int at = email.indexOf('@');
        return I18N.get("******@{0}", (at != -1)?email.substring(at+1):hideUri("email.com"));
    }

    public static int getUriSchema(String uri) {
        final int at = uri.indexOf(':');
        if (at != -1 &&
                uri.length() > at + 2 &&
                uri.charAt(at+1) == '/' &&
                uri.charAt(at+2) == '/' )
                return at;
        return -1;
    }

    public final static String getPhoneUri(String content) {
        // based on RTF 2806 2.2, but simplified to
        // + 0-9 ; = a-z % x * #
        StringBuffer buf = new StringBuffer("tel:");
        final int len = content.length();
        for (int ind = 0; ind < len; ++ind) {
            final char c = content.charAt(ind);

            if (((c >= '0') && (c <= '9'))
                    || (c == '+') || (c == '*') || (c == '#')
                    || ((c >= 'a') && (c <= 'z'))
                    || (c == ';') || (c == '%')) {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    protected static String hideUri(String uri) {
        final int at = getUriSchema(uri);
        int len = uri.length();

        String prefix = "";
        if (at != -1) {
            prefix = uri.substring(0, at+3);
            uri = uri.substring(at+3);
            len = uri.length();
        }

        String[] arr = new String[2];
        arr[0] = prefix;
        if (len <= 5)
            arr[1] = "s";
        else
            arr[1] = uri.substring(0, uri.startsWith("www.")?5:1);
        return I18N.get("{0}{1}*******.***", arr);
    }

    protected static String getRFC6238(Field f) {
        final int hashLen = 20;
        final int blockLen = 64;
        final int period = 30;

        long ts = (System.currentTimeMillis()/1000L + Midlet.getInstance().getTimeOffset()) / period;

        String result;
        byte[] secret = null;

        try {
            final MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            secret = Midlet.alloc(f.getBytes(null, 0));
            f.getBytes(secret, 0);

            //HMAC-(secret, (long)time/30)
            byte[] b = new byte[1];

            //R1: internal hash H( K ^ 0x36 || ts/30)
            for (int i = 0; i < secret.length; ++i) {
                b[0] = (byte)(secret[i] ^ 0x36);
                sha1.update(b, 0, 1);
            }

            b[0] = (byte)(0x36);
            for (int i = secret.length; i < blockLen; ++i)
                sha1.update(b, 0, 1);


            for (int i = 0; i < 8; ++i) {
                b[0] = (byte)(ts >>> 56);
                sha1.update(b, 0, 1);
                ts <<= 8;
            }

            byte[] hash = new byte[hashLen];
            int len = sha1.digest(hash, 0, hash.length);
            sha1.reset();

            //R2: H( K ^ 0x5C || hash)
            for (int i = 0; i < secret.length; ++i) {
                b[0] = (byte)(secret[i] ^ 0x5C);
                sha1.update(b, 0, 1);
            }
            b[0] = (byte)(0x5C);
            for (int i = secret.length; i < blockLen; ++i)
                sha1.update(b, 0, 1);

            sha1.update(hash, 0, len);

            len = sha1.digest(hash, 0, hash.length);
            sha1.reset();

            final int offset = (hash[len-1] & 0xF);

            int password = 0;
            for (int i = 0; i < 4; ++i)
                password = (password << 8) | (hash[offset+i] & 0xFF);

            password = (password & 0x7FFFFFFF) % 1000000;
            result = I18N.toStringZ(password, 6);
        }
        catch (Exception e) {
            result = e.getMessage();
        }

        if (null != secret) {
            for (int i =0; i < secret.length; ++i)
                secret[i] = 0;
        }
        return result;
    }

    protected static String getLimitedString(Field f) {
        final String s = f.getString();
        final int len = (null != s)?s.length():0;
        if (len > Midlet.getMaxString())
            return I18N.get("Length {0}chars", I18N.formatSI(len));
        return s;
    }

    protected static String formatField(Field f, boolean showContent) {

        final boolean isUnprotected = !f.isProtected() || showContent;
        final int format = f.getFormat();

        switch (f.getType()) {
            case Field.BINARY: {
                if (Midlet.TOTP_FORMAT == format) {
                    if (showContent)
                        return getRFC6238(f);
                    else if (!isUnprotected)
                        return I18N.get("********");
                }

                final int len = f.getBytes(null, 0);
                if (showContent && len *2 <= Midlet.getMaxString())
                    return getLimitedString(f);
                return I18N.get("Length {0}", I18N.formatBytes(isUnprotected?len:-1));
            }
            case Field.INTEGER:
                if (Midlet.DATE_FORMAT == format)
                    return (isUnprotected)?I18N.formatDate(new Date(f.getLong())):I18N.formatDate(null);
                break;

            case Field.STRING: {
                    final String data = getLimitedString(f);
                    if (isUnprotected) {
                        if (Midlet.OTP_FORMAT == format) {
                            final int index = data.indexOf("\n");
                            if (-1 != index)
                                return data.substring(0, index);
                        }
                        return data;
                    }

                    switch (format) {
                        case Midlet.EMAIL_FORMAT:
                            return hideEmail(data);
                        case Midlet.PHONE_FORMAT:
                            return hidePhone(data);
                        case Midlet.URI_FORMAT:
                            return hideUri(data);
                    }
                    break;
            }
        }
        return (isUnprotected)?getLimitedString(f):I18N.get("********");
    }

    public void delete(GenericItem item) {
        if (null == item)
            return;

        final int num = size();
        for (int ind =0; ind < num; ++ind) {
            if (item.equals(get(ind))) {
                delete(ind);
                return;
            }
        }
    }

    public GenericItem findItem(Object userInfo) {
        for (int i =0; i < super.size(); ++i) {
            Item it = super.get(i);
            if (it instanceof GenericItem) {
                GenericItem item = (GenericItem)it;
                if (item.getUserInfo() == userInfo)
                    return item;
            }
        }
        return null;
    }

    protected void onConfirmed(int action, Object userinfo, boolean isAgreed) throws Exception {
        back_.show(null);
    }

    // gui helpers
    protected GenericItem createField(Field f, Command def) {
        int icon = f.getIcon();
        if (icon == Midlet.ICON_NULL)
            icon = Midlet.getTypeIcon(f.getType(), f.getFormat(), f.isProtected());
        GenericItem field = createItem(f.getName(), formatField(f, false), icon, def);
        field.setUserInfo(f);
        return field;
    }

    public GenericItem createItem(String name, String desc, int icon, Command def) {
        return createLongItem(name, desc, icon, def, Item.LAYOUT_DEFAULT);
    }

    public GenericItem createTitle(String name, int icon, Command def) {
        GenericItem mi = new ShortMenuItem(Midlet.getInstance().getTheme(), this, name, Midlet.getIconName(icon));
        mi.setLayout(Item.LAYOUT_CENTER);
        if (null != def)
            mi.setDefaultCommand(def);
        mi.setItemCommandListener(this);
        return mi;
    }

    public GenericItem createLongItem(String name, String desc, int icon, Command def, int layout) {
        LongMenuItem mi = new LongMenuItem(Midlet.getInstance().getTheme(), this, name, desc, Midlet.getIconName(icon));
        mi.setLayout(layout);
        if (null != def)
            mi.setDefaultCommand(def);
        mi.setItemCommandListener(this);
        return mi;
    }

    public GenericItem createNewItem(String enName, String enDesc, Command add) {
        return createItem(I18N.get(enName), I18N.get(enDesc), Midlet.ICON_ADD_NEW, add);
    }

    protected void append(Vector v) {
        if (null == v)
            return;
        final int len = v.size();
        for(int i = 0; i < len; ++i) {
            append((Item)v.elementAt(i));
        }
    }

    public boolean isLess(Object o1, Object o2) {
        if (null == o1)
            return o2 != null;

        if (null == o2)
            return false;

        final Item i1 = (Item)o1;
        final Item i2 = (Item)o2;

        return madrat.sys.QuickSort.lessString(i1.getLabel(), i2.getLabel());
    }

    public void appendInfoline(String name, String value, int icon) {
        value = (null != value && 0 != value.length())?value:I18N.get("(empty)");
        append(
                createLongItem(
                        I18N.get(name), value, icon, null, Item.LAYOUT_RIGHT));
    }
}
