/*
 * Copyright (C) 2017 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>
 *
 * This file is part of madRat's J2ME helpers (madrat.sys).
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

package madrat.sys;

/**
 * Encoding/Decoding for HEX, Base32 and Base64
 */
public final class Convertion {
    private static final char   PAD     = '=';

    /**
     * Base16 (HEX) charset for encode/decode
     */
    public static final String CHARSET_B16  = "0123456789ABCDEF";

    /**
     * Base32 charset for encode/decode
     */
    public static final String CHARSET_B32  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    /**
     * Base64 charset for encode/decode
     */
    public static final String CHARSET_B64  = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    // returns 0x00 MM S B
    //  B - count of bits
    //  S - block size
    private static int getBaseInfo(String alpha) {
        switch (alpha.length()) {
            case 16:
                return 0x24;
            case 32:
                return 0x85;
            case 64:
                return 0x46;
        }
        throw new IllegalStateException(alpha);
    }

    /**
     * Hex Encode
     */
    public static String encodeHex(byte[] src) {
        if (null == src || 0 == src.length)
            return null;
        final StringBuffer r = new StringBuffer();
        for (int i =0; i < src.length; ++i) {
            final int v = src[i] & 0xFF;
            if (v < 0x10)
                r.append('0');
            r.append(Integer.toHexString(v));
        }
        return r.toString();
    }

    /**
     * Hex Decode
     * @throws IllegalArgumentException - Padding or source string size issue
     * @throws NumberFormatException - Unexpected character in encoded string
     */
    public static byte[] decodeHex(String src) throws IndexOutOfBoundsException, NumberFormatException {
        if (null == src || 0 == src.length())
            return null;

        final int lSrc = src.length();

        if (0 != (lSrc & 0x1))
            throw new IllegalArgumentException(src);

        byte[] result = new byte[lSrc>>1];
        for (int i = 0, j= 0; i < result.length; ++i, j += 2) {
            result[i] = (byte)Integer.parseInt(src.substring(j, j+2), 16);
        }
        return result;
    }

    /**
     * Encode bytes array into printable string
     *
     * @param src - source bytes
     * @param charset - encoding target @see CHARSET_B16, CHARSET_B32 and CHARSET_B64
     * @return Encoded string
     * @throws IllegalStateException - invalid/unsupported charset
     * @note charset should be 16-, 32-, or 64-chars
     */
    public static String encode(byte[] src, String charset) throws IllegalStateException {
        final int baseInfo = getBaseInfo(charset);

        if (null == src || 0 == src.length)
            return null;

        final int base = baseInfo & 0xF;
        final int quantity = (baseInfo>>4) & 0xF;
        final int mask = charset.length()-1;

        final StringBuffer r = new StringBuffer();

        int count = 0;
        int index = 0;
        int data  = 0;

        while (index < src.length || count >= base) {
            if (count < base && index < src.length) {
                data = (data<<8) | (src[index] & 0xFF);
                ++index;
                count += 8;
            }
            //consume
            while (count >= base) {
                count -= base;
                final int v = (data>>>count) & mask;
                r.append(charset.charAt(v));
            }
        }

        if (0 != count) {
            final int v = (data << (base-count) ) & mask;
            r.append(charset.charAt(v));

            final int block = quantity*base;
            final int last = (src.length << 3) % block;
            final int free = block - last;
            final int pads = free / base;

            for (int i =0; i < pads; ++i)
                r.append(PAD);
        }

        return r.toString();
    }

    /**
     * Decode from printable string into bytes array
     * @param src - encoded string
     * @param charset - source charset @see CHARSET_B16, CHARSET_B32 and CHARSET_B64
     * @param mustPadded - Base32/Base64 must be padded
     * @return decoded array of bytes
     * @throws IllegalStateException - invalid/unsupported charset
     * @throws IllegalArgumentException - Padding or source string size issue
     * @throws NumberFormatException - Unexpected character in encoded string
     */
    public static byte[] decode(String src, String charset, boolean mustPadded)
            throws  IllegalStateException,
                    IllegalArgumentException,
                    NumberFormatException {
        final int baseInfo = getBaseInfo(charset);

        if (null == src || 0 == src.length())
            return null;

        final int base = baseInfo & 0xF;
        final int quantity = (baseInfo>>4) & 0xF;
        final int mask = charset.length()-1;

        final int lSrc = src.length();
        int pads = 0;
        for (int j = lSrc-1, i = 0; 0 <= j && i < quantity; i++, --j) {
            if (PAD != src.charAt(j))
                break;
            ++pads;
        }

        final int len = lSrc - pads;
        byte[] result = new byte[(len * base) >>> 3];

        if (mustPadded) {
            if (0 != lSrc % quantity)
                throw new IllegalArgumentException(src);

            final int block = quantity*base;
            final int npads = ((block - (result.length << 3) % block) % block / base);
            if (npads != pads)
                throw new IllegalArgumentException(src);
        }

        int data = 0;
        int count = 0;
        int index = 0;

        for (int iSrc = 0; iSrc < len; ++iSrc) {
            int code = charset.indexOf(src.charAt(iSrc));
            if (code == -1)
                throw new NumberFormatException(src);

            data = (data << base ) | code;
            count += base;

            if (count >= 8) {
                count -= 8;
                result[index++] = (byte)(data >>> count);
            }
        }

        return result;
    }


    /**
     * Byte[] to Long conversion
     * @param arr - source array
     * @param ofs - 1st byte offset in array
     * @param len - max count of bytes, that can be used to converion
     * @return long value
     * @note Little-endian format, so int value may be restored as len=4
     */
    public static final long btol(byte[] arr, int ofs, int len) {
        if (null == arr)
            return 0;

        long v = 0;
        len = Math.min(len, 8);
        for(int i = 0; i < len && ofs < arr.length; ++i, ++ofs) {
            v |= (arr[ofs] & 0xFFL) << (i*8);
        }
        return v;
    }

    /**
    * Long to Byte[] conversion
    * @param value - source value
    * @param arr - destination array
    * @param ofs - 1st byte offset in array
    * @param len - max count of bytes, that can be used
    * @return next offset
    * @note Little-endian format, so int value may be stored as len=4
    */
    public static final int ltob(long value, byte[] arr, int ofs, int len) {
        for(int i = 0; i < len && ofs < arr.length; ++i, ++ofs) {
            arr[ofs] = (byte) (value & 0xFF);
            value >>>= 8;
        }
        return ofs;
    }
}
