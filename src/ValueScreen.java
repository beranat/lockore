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

import java.util.Timer;
import javax.microedition.lcdui.AlertType;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Ticker;

import madrat.i18n.I18N;
import madrat.storage.BinaryField;
import madrat.storage.Field;
import madrat.sys.PasswordQuality;

/**
 * Edit Field's name and content as a string
 */
final class ValueScreen extends TextBox implements CommandListener, TaskNotification  {
    public static final int NAME = 0;   // Name of Field
    public static final int DESC = 1;   // Description (can not be random)
    public static final int DATA = 2;   // Any allowed value

    private static final int    TICKER_RATE = 750;

    private final LockoreForm owner_;
    private final Field       item_;
    private final boolean     isValue_;

    private String binaryMode_;

    private final PasswordQuality   pq_;

    protected final Timer       timer_;

    private Command base64_;
    private Command base32_;
    private Command base16_;

    public final void insert(String str, int pos) {
        if (null == str || str.length() == 0)
            return;

        //check content limit(s)
        if (getMaxSize() < str.length() + size())
            throw new IndexOutOfBoundsException(I18N.get("Max size ({0}char) exceeded", I18N.formatSI(getMaxSize())));

        try {
            super.insert(str, pos);
            return;
        }
        catch (IllegalArgumentException e) {
        }

        final String content = getString();

        try {
            final int contentLen = (content != null)? content.length() : 0;
            final int remainSize = getMaxSize() - contentLen;

            if (remainSize > 0) {
                char[] buf = str.toCharArray();
                super.insert(buf, 0, remainSize, pos);
            }
            return;
        }
        catch (IllegalArgumentException e) {
        }

        throw new IllegalArgumentException(content + " + " + str);
    }

    public ValueScreen(LockoreForm owner, String title, Field item, int mode) {
        super((null != title)?title:item.getName(), null, Midlet.getMaxString(), TextField.ANY);
        owner_ = owner;
        item_ = item;
        isValue_ = (mode != NAME);

        boolean isProtected = false;
        int type = Field.STRING;
        final int format = (mode == DATA)?item_.getFormat():0;

        if (isValue_) {
            isProtected = item_.isProtected();
            type = item_.getType();
        }

        int extraConstraints = isProtected?(TextField.SENSITIVE+TextField.NON_PREDICTIVE):0;
        boolean showRandom = (mode == DATA);
        boolean showStrength = isProtected && type == Field.STRING;
        String content = isValue_?item_.getString():item_.getName();

        switch (type) {
            case Field.META:
                setMaxSize(255);
                break;
            case Field.STRING:
                switch (format) {
                case Midlet.PHONE_FORMAT:
                    setMaxSize(64);
                    setInitialInputMode("IS_LATIN_DIGITS");
                    extraConstraints |= TextField.NON_PREDICTIVE;
                    showRandom = false;
                    break;
                case Midlet.URI_FORMAT:
                case Midlet.EMAIL_FORMAT:
                    setInitialInputMode("MIDP_LOWERCASE_LATIN");
                    showRandom = false;
                    extraConstraints |= TextField.NON_PREDICTIVE;
                    break;
                case Midlet.OTP_FORMAT:
                    setInitialInputMode("IS_LATIN_DIGITS");
                    showStrength = false;
                    extraConstraints |= TextField.NON_PREDICTIVE;
                    break;
                }
                break;
            case Field.INTEGER:
                setMaxSize(32);
                setInitialInputMode("IS_LATIN_DIGITS");
                break;
            case Field.BINARY:
                setInitialInputMode("MIDP_UPPERCASE_LATIN");
                extraConstraints |= TextField.SENSITIVE | TextField.NON_PREDICTIVE;
                showStrength = false;
                final byte[] b = Midlet.alloc(item_.getBytes(null, 0));
                item_.getBytes(b, 0);
                binaryMode_ = madrat.sys.Convertion.CHARSET_B16;
                content = madrat.sys.Convertion.encode(b, binaryMode_);
                base16_ = new Command(I18N.get("To Hex"), Command.ITEM, 2);
                base32_ = new Command(I18N.get("To Base-32"), Command.ITEM, 3);
                base64_ = new Command(I18N.get("To Base-64"), Command.ITEM, 3);
                addCommand(base32_);
                addCommand(base64_);
                break;
            default:
                throw new IllegalArgumentException(I18N.get("Unsupported type {0}", Integer.toString(type)));
        }

        if (showRandom) {
            addCommand(new Command( I18N.get("Random"), I18N.get("Generate word"), Command.SCREEN, 1));
        }

        if (showRandom && showStrength) {
            pq_ = Midlet.getPasswordQuality();

            setTicker(new Ticker(""));
            timer_ = new Timer();
            timer_.scheduleAtFixedRate(new UpdateTask(this, timer_), TICKER_RATE*2, TICKER_RATE);
        } else {
            timer_ = null;
            pq_ = null;
        }

        // NOTE: some phones limits NUMBER/DECIMAL with ~ 11 digits
        setConstraints(TextField.ANY | extraConstraints);
        insert(content, 0);

        addCommand(Midlet.OK);
        addCommand(Midlet.CANCEL);
        setCommandListener(this);
    }

    private static byte[] decode(String s, String charset) {
        if (madrat.sys.Convertion.CHARSET_B16.equals(charset))
            s = s.toUpperCase();
        return madrat.sys.Convertion.decode(s, charset, true);
    }

    private void switchTo(Command c) {
        String value = getString();

        byte[] content = decode(value, binaryMode_);

        addCommand(base16_);
        addCommand(base32_);
        addCommand(base64_);
        removeCommand(c);

        if (c == base16_) {
            binaryMode_ = madrat.sys.Convertion.CHARSET_B16;
        } else if (c == base32_) {
            binaryMode_ = madrat.sys.Convertion.CHARSET_B32;
        } else if (c == base64_) {
            binaryMode_ = madrat.sys.Convertion.CHARSET_B64;
        }

        value = madrat.sys.Convertion.encode(content, binaryMode_);
        setString(value);
    }

    public void commandAction(Command c, Displayable d) {
        final String value = getString();

        try {
            if (c == base16_ || c == base32_ || c == base64_) {
                switchTo(c);
                return;
            }

            switch (c.getCommandType()) {
                case Command.SCREEN:
                    Midlet.setCurrent(new GeneratePassList(this, item_.getType()));
                    return;
                case Command.OK:
                    if (!isValue_) {
                        if (0 == value.length())
                            throw new Exception(I18N.get("Name is empty"));
                        item_.setName(value);
                    }
                    else {
                        if (Field.BINARY == item_.getType()) {
                            BinaryField item = (BinaryField)item_;
                            byte[] content = decode(value, binaryMode_);
                            item.set(content);
                        }
                        else
                            item_.set(value);
                    }
                case Command.CANCEL:
                    if (null != timer_)
                        timer_.cancel();
                    owner_.show(item_);
            }
        }
        catch (NumberFormatException ex) {
            Midlet.showAlert(AlertType.ERROR,I18N.get("'{0}' is not number", value),  null, this);
        }
        catch (IllegalArgumentException ex) {
            Midlet.showAlert(AlertType.ERROR, I18N.get("Invalid data '{0}'", ex.getMessage()),  null, this);
        }
        catch (Exception e) {
            Midlet.showAlert(e, this);
        }
    }

    public void onFailure(Object id, Exception e) {
        Midlet.showAlert(e, this);
    }

    public void onSuccess(Object id, String str, long mode, byte[] data) {
        if (null != timer_ && timer_ == id) {
            final Ticker t = getTicker();
            if (null != t) {
                final String val = getString();
                final int bits = pq_.getBits(val);
                String label = "";

                if (0 != bits) {
                    if (bits < PasswordQuality.WEAK)
                        label = I18N.get("Trivial password");
                    else
                        label = I18N.get("{0} bits strength", Integer.toString(Math.max(bits, 0)));
                }

                final String old = t.getString();
                if (null == old || !old.equals(label))
                    t.setString(label);
                return;
            }
        }

        try {
            if (null != binaryMode_ && madrat.sys.Convertion.CHARSET_B16 != binaryMode_) {
                try {
                    switchTo(base16_);
                }
                catch(Exception e) {
                }
            }

            switch ((int)mode) {
                case GeneratePassList.CANCEL:
                default:
                    break;
                case GeneratePassList.SET:
                    setString(null);
                case GeneratePassList.APPEND:
                    insert(str, getCaretPosition());

            }
            Midlet.setCurrent(this);
        }
        catch (Exception e) {
            Midlet.showAlert(e, this);
        }
    }
}
