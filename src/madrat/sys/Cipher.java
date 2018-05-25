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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

/**
 * Do en(de)crypt big data without infinity memory need
 */
public final class Cipher {
    // ASSERTION
    // CIPER_BLOCK < CIPER_BLOCK_RESERVE < 2*CIPER_BLOCK
    public static final int CIPER_BLOCK = 1024;  // encode/decode by 4K 'page'
    public static final int CIPER_BLOCK_RESERVE = CIPER_BLOCK + (CIPER_BLOCK>>1);

    private static int cipher(javax.crypto.Cipher cipher,
                                byte[] buffer, int offset, int len)
                                        throws  ShortBufferException,
                                                BadPaddingException,
                                                IllegalBlockSizeException {
        int dlen = 0;
        while (len > CIPER_BLOCK) {
            dlen += cipher.update(buffer, offset, CIPER_BLOCK, buffer, dlen);
            if (dlen > offset)
                throw new RuntimeException("Decoding block overried for decrypt_inplace");
            len -= CIPER_BLOCK;
            offset += CIPER_BLOCK;
        }

        dlen += cipher.doFinal(buffer, offset, len, buffer, dlen);
        return dlen;
    }

    // temp.length >= CIPER_BLOCK + CIPER_BLOCK_RESERVE
    //   or
    // offset >= CIPER_BLOCK_RESERVE
    // ASSERT: CIPER_BLOCK_RESERVE > CIPER_BLOCK
    public static int cipher(   javax.crypto.Cipher cipher,
                                byte[] buffer, int offset, int len,
                                byte[] temp)
                                    throws  InvalidAlgorithmParameterException,
                                            InvalidKeyException,
                                            ShortBufferException,
                                            BadPaddingException,
                                            IllegalBlockSizeException {
        int tempLen = 0;
        if (offset < CIPER_BLOCK_RESERVE) {
            if (null == temp)
                throw new IllegalArgumentException("Small chiper block w/o temporary buffer");

            if (len <= temp.length) {
                System.arraycopy(buffer, offset, temp, 0, len);
                return cipher.doFinal(temp, 0, len, buffer, 0);
            }
            if (temp.length < CIPER_BLOCK+CIPER_BLOCK_RESERVE)
                throw new ShortBufferException("Temp buffer is too short for BLOCK and RESERVE");

            tempLen = cipher.update(buffer, offset, (CIPER_BLOCK<<1), temp, 0);
            len -= (CIPER_BLOCK<<1);
            offset += (CIPER_BLOCK<<1);
        }

        int buffered = cipher(cipher, buffer, offset, len);

        if (tempLen > 0) {
            if (buffered + tempLen > buffer.length)
                throw new ShortBufferException("Short Temp buffer len");
            System.arraycopy(buffer, 0, buffer, tempLen, buffered);
            System.arraycopy(temp, 0, buffer, 0, tempLen);
        }

        return buffered + tempLen;
    }
}
