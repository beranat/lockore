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

import java.io.IOException;
import madrat.sys.Convertion;

/**
 * Binary data
 */
public class BinaryField extends Field {
    protected byte[] data_;

    public int getType() {
        return BINARY;
    }

    BinaryField(boolean isProtected, String name, int icon, int format, java.io.DataInputStream is)
            throws IOException, NegativeArraySizeException {
        this(isProtected, name, icon, format, (byte[])null);

        final int length = Stream.loadVarInt(is);
        if (length > 0) {
            data_ = Storage.alloc(length);
            is.read(data_);
        }

        clearModify();
    }

    public BinaryField(boolean isProtected, String name, int icon, int format, byte[] data) {
        super(isProtected, name, icon, format);
        data_ = data;
        clearModify();
    }

    public void save(java.io.DataOutputStream os) throws IOException {
        super.save(os);

        final int dlen = (null != data_)?data_.length:0;
        Stream.saveVarInt(os, dlen);
        if (dlen > 0)
            os.write(data_);
        clearModify();
    }

    public String getString() throws RuntimeException {
        return madrat.sys.Convertion.encodeHex(data_);
    }

    public long getLong() {
        if (null == data_ || 0 == data_.length)
            return 0;

        if (data_.length > 8)
            throw new NumberFormatException();

        return Convertion.btol(data_, 0, 8);
    }

    public int getBytes(byte[] d, int ofs) {
        if (null == data_ || 0 == data_.length)
            return 0;

        if (null != d)
            System.arraycopy(data_, 0, d, ofs, data_.length);

        return data_.length;
    }

    public void set(String hexString) throws RuntimeException {
        data_ = madrat.sys.Convertion.decodeHex(hexString);
        modify();
    }

    public void set(long val) {
        byte[] data = new byte[8];
        Convertion.ltob(val, data, 0, data.length);
        data_ = data;
        modify();
    }

    public void set(byte[] val) {
        if (null != val && 0 != val.length) {
            data_ = new byte[val.length];
            System.arraycopy(val, 0, data_, 0, data_.length);
        }
        else
            data_ = null;
        modify();
    }

    public void assign(byte[] val) {
        if (null != val && 0 == val.length)
            val = null;
        data_ = val;
        modify();
    }
}
