/*
 * Copyright (C) 2017 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>
 *
 * This file is part of madRat's J2ME Storage (madrat.storage).
 *
 * This package is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This package distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with package.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package madrat.storage;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

/**
 * Base class for all field types and common operations
 */
public abstract class Field {
    protected static final int PROTECTED = 0x80;

    protected static final int TYPE_MASK = (~PROTECTED) & 0xFF;

    protected static final int RESERVED = 0x80;// RESERVED
    public static final int NONE     = 0x00;// 'NONE'

    protected static final int PAD_1    = 0x81;// 'PAD' + BYTE(LEN) + RANDOM

    public static final int META    = 0x01; // 'META' Record ID
    public static final int STRING  = 0x02; // UTF-8
    public static final int INTEGER = 0x03; // Signed long (8 bytes)
    public static final int BINARY  = 0x04; // Binary object
    public static final int DECIMAL = 0x05; // double (8bytes), IEE-754, not implemented

    public static final int DEFAULT_ICON   = 0;
    public static final int DEFAULT_FORMAT = 0;

    private boolean   isProtected_;
    private String    name_;
    private int       icon_;
    private int       format_;
    private boolean   isModified_;

    public abstract int getType();

    public final boolean isProtected() {
        return isProtected_;
    }

    public final void setProtection(boolean isProt) {
        if (isProtected_ == isProt)
            return;
        isProtected_ = isProt;
        modify();
    }

    public final String getName() {
        return name_;
    }

    public final void setName(String n) {
        if (name_.equals(n))
            return;
        name_ = (null !=n)?n:"";
        modify();
    }

    public final int getIcon() {
        return icon_;
    }

    public final void setIcon(int i) {
        if (icon_ == i)
            return;
        icon_ = i;
        modify();
    }

    public final int getFormat() {
        return format_;
    }

    public final void setFormat(int format) {
        if (format_ == format)
            return;
        format_ = format;
        modify();
    }

    protected Field(boolean isProtected, String name, int icon, int format) {
        isProtected_ = isProtected;
        name_ = (null!=name)?name:"";
        icon_ = icon;
        format_ = format;
        isModified_ = false;
    }

    public static String getString(Random rand, int minLen, int maxLen) {
        int length = (maxLen <= minLen)
                                ?minLen
                                :minLen + (Math.abs(rand.nextInt()) % (maxLen - minLen));

        StringBuffer s = new StringBuffer();
        while(length --> 0) {
            int v = (rand.nextInt() & 0x3F) - 2; // 6 bit
            char c;
            if (v < 0)                      //-1, -2
                c = (-1==v)?'?':'@';
            else if (v < 10)                //0...9
                    c = (char) ('0' + v);
            else { //10...62
                    v -= 10;
                    c = (char)((v % 26) + (0 == (v / 26)?'a':'A'));
            }
            s.append(c);
        }
        return s.toString();
    }

    public void save(java.io.DataOutputStream os) throws IOException {
        final byte type = (byte)(getType() | (isProtected_?PROTECTED:0));
        saveHeader(os, type, name_, icon_, format_);
        isModified_ = false;
    }

    public static void skipPad(java.io.DataInputStream is) throws IOException {
        final int length = is.readUnsignedByte();
        if (length == -1)
            throw new EOFException("No enouth data for skip");

        is.skip(length);

        if (0 >= is.available())
            throw new EOFException("Skipped all data");
    }

    public static void savePad(java.io.DataOutputStream os, Random rand, int length) throws IOException {
        if (0 >= length)
            return;

        if (length > 257) {
            final int blen = (rand.nextInt() & 0xFF) + 2;
            savePad(os, rand, blen);
            savePad(os, rand, length - blen);
            return ;
        }

        // 0 < length <= 257
        if (1 == length) {
            os.write(NONE);
            return;
        }

        // 1 < length <= 257 => 0 <= length-2 <= 255
        length -= 2;
        os.write(PAD_1);
        os.write(length);
        for (int i = length; i > 0; --i) {
            os.write(rand.nextInt());
        }
    }

    protected static final void saveHeader(java.io.DataOutputStream os, byte t, String n, int i, int f) throws IOException {
        os.writeByte(t);
        Stream.saveVarString(os, n);
        Stream.saveVarInt(os, i);
        Stream.saveVarInt(os, f);
    }

    public static final Field load(java.io.DataInputStream is) throws IOException {
        final int typeByte = is.readUnsignedByte();

        if (RESERVED == typeByte)
            throw new UnsupportedException("Field-type", "Reserved");

        if (NONE == typeByte)
            return null;

        if (PAD_1 == typeByte) {
            skipPad(is);
            return null;
        }

        final String name = Stream.loadVarString(is);
        final int icon = Stream.loadVarInt(is);
        final int format = Stream.loadVarInt(is);

        if (META == typeByte) {
            StringField f = new StringField(true, false, name, icon, format, is);
            return f;
        }

        final boolean isProtected = (typeByte & PROTECTED) != 0;
        final int type = (typeByte & TYPE_MASK);

        switch (type) {
            case STRING:
                return new StringField(isProtected, name, icon, format, is);
            case INTEGER:   // may be '0012', '-12'
                return new IntegerField(isProtected, name, icon, format, is);
            case BINARY:
                return new BinaryField(isProtected, name, icon, format, is);
            case DECIMAL:
            default:
                throw new UnsupportedException("Field-type", Integer.toString(type));
        }
    }

    public void clearModify() {
        isModified_ = false;
    }

    public void modify() {
        isModified_ = true;
    }

    public boolean isModified() {
        return isModified_;
    }

    public final long get(long def) {
        try {
            return getLong();
        }
        catch (RuntimeException e) {
            return def;
        }
    }

    public final String get(String def) {
        try {
            return getString();
        }
        catch (RuntimeException e) {
                return def;
        }
    }

    public long getLong() throws RuntimeException {
        final String data = getString();
        if (null == data || data.length() == 0)
            return 0;
        return Long.parseLong(data);
    }

    /**
     * Convert into binary
     * @param d - result array (null - means only return length)
     * @param ofs - offset in resulted array
     * @return - used length
     */
    public int getBytes(byte[] d, int ofs) {
        final String s = getString();
        byte[] src;

        try {
            src = s.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            src = s.getBytes();
        }

        if (null != d)
            System.arraycopy(src, 0, d, ofs, src.length);

        return src.length;
    }

    public abstract String getString() throws RuntimeException;

    public abstract void set(String val) throws RuntimeException;
}
