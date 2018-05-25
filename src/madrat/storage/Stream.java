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
import java.io.InputStreamReader;

/**
 * Helpers for reading/writing simple types
 */
public final class Stream {
    private Stream() {
    }

    public static int loadVarInt(java.io.InputStream is) throws IOException {
        int v = 0;
        boolean next = true;

        for (int ind = 0; ind < 32 && next; ind += 7) {

            int b = is.read();
            if (b == -1)
                throw new EOFException();
            next = (0 != (b &0x80));
            v |= (b&0x7F)<<ind;
        }

        if (0 == (v & 0x1))
            return (v>>>1);
        return ~(v>>>1);
    }

    public static int saveVarInt(java.io.OutputStream os, int value) throws IOException {
        if (value >= 0)
            value <<= 1;
        else
            value = ((~value)<<1) | 1;

        int len = 0;
        do {
            int v = value & 0x7F;
            value >>>= 7;
            if (value > 0)
                v |= 0x80;
            os.write(v);
            ++len;
        } while (value > 0);
        return len;
    }

    public static String loadVarString(java.io.InputStream in) throws IOException {
        final int len = loadVarInt(in);
        if (len < 0)
            throw new IndexOutOfBoundsException();

        InputStreamReader js = new InputStreamReader(in, "UTF-8");
        StringBuffer buf = new StringBuffer(len);
        for (int i = len; i >0; --i) {
            final int f = js.read();
            if (f == -1)
                throw new EOFException();
            buf.append((char)f);
        }
        String str = buf.toString();
        return str;
    }

    public static int saveVarString(java.io.OutputStream os, String data) throws IOException {
        final int strlen = (null != data)?data.length():0;

        int len = saveVarInt(os, strlen);
        if (strlen > 0) {
            final byte[] bin = data.getBytes("UTF-8");
            os.write(bin);
            len += bin.length;
        }
        return len;
    }
}
