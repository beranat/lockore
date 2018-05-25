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
 * Long integer field
 */
public class IntegerField extends Field {
    protected long value_;

    public int getType() {
        return INTEGER;
    }

    public IntegerField(boolean isProtected, String name, int icon, int format, java.io.DataInputStream is) throws IOException {
        this(isProtected, name, icon, format, is.readLong());
    }

    public IntegerField(boolean isProtected, String name, int icon, int format, long value) {
        super(isProtected, name, icon, format);
        value_ = value;
    }

    public void save(java.io.DataOutputStream os) throws IOException {
        super.save(os);
        os.writeLong(value_);
        clearModify();
    }

    public String getString() throws RuntimeException {
        return Long.toString(value_);
    }

    public void set(String val) throws RuntimeException {
        set(Long.parseLong(val));
    }

    public void set(long d) {
        if (value_ == d)
            return;
        value_ = d;
        modify();
    }

    public long getLong() {
        return value_;
    }

    public int getBytes(byte[] d, int ofs) throws RuntimeException {
        final int LONG_LEN = 8;

        if (null != d) {
            final int l = d.length - ofs;
            if ( l > LONG_LEN)
                throw new IndexOutOfBoundsException();
            Convertion.ltob(value_, d, ofs, l);
        }
        return LONG_LEN;
    }
}

