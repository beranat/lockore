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

import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * CPRNG based on AES/ECB cipher
 */
public class SecureRandom {
    protected static final int CIPHER_SIZE = 256;
    protected static final int RESEED_SIZE = 4096; // reseed every 4K bits

    protected static SecureRandom random_ = null;

    protected final Cipher cipher_; // CBC mode - state is in object
    protected final Sha3   hash_;

    protected final byte[] counter_;  // counter value
    protected final byte[] result_;

    protected int generated_ = 0;

    public SecureRandom() throws GeneralSecurityException {
        cipher_ = Cipher.getInstance("AES/ECB/NoPadding");
        hash_ = new Sha3(CIPHER_SIZE);
        counter_ = new byte[CIPHER_SIZE/8]; // size in BYTES
        counter_[0] = 1;
        result_ = new byte[CIPHER_SIZE/8];
        generated_ = 0;

        Convertion.ltob(Runtime.getRuntime().freeMemory(), result_, 0, 8);
        Convertion.ltob(System.currentTimeMillis(), result_, 8, 8);
        initSeed();
    }

    protected void initSeed() throws GeneralSecurityException {
        SecretKeySpec ks = new SecretKeySpec(result_, 0, result_.length, "AES");
        cipher_.init(Cipher.ENCRYPT_MODE, ks);
        generate();
        generated_ = 0;
    }

    public void reseed(byte[] seed) throws RuntimeException {
        try {
            generate();
            hash_.reset();
            if (null != seed)
                hash_.update(seed, 0, seed.length);
            hash_.update(result_, 0, result_.length);
            hash_.finalize(result_, 0);
            initSeed();
        }
        catch (GeneralSecurityException e) {
            throw new RuntimeException(e.getClass().getName() + "/" + e.getMessage());
        }
    }

    protected void generate() throws GeneralSecurityException {
        cipher_.doFinal(counter_, 0, counter_.length, result_, 0);

        for (int i = 0; i < counter_.length; ++i) {
            int v = (((int) counter_[i]) & 0xff) + 1;
            if (v < 0x100) {
                counter_[i] = (byte) v;
                break;
            }
            counter_[i] = 0;
        }
        generated_ += result_.length*8;
    }

    protected void next() {
        try {
            generate();
            if (generated_ > RESEED_SIZE)
                reseed(null);
        }
        catch (GeneralSecurityException e) {
            throw new RuntimeException(e.getClass().getName() + "/" + e.getMessage());
        }
    }

    public byte getByte() {
        next();
        return result_[0];
    }

    public int getUInt31() {
        return (int)(getLong() & 0x7FFFFFFFL);
    }

    public long getLong()  {
        next();
        return Convertion.btol(result_, 0, 8);
    }

    protected static void appendSet(StringBuffer buf, char b, char e) {
        for (char c = b; c <= e; ++c) {
            buf.append(c);
        }
    }

    public String getString(int length, String alpa) throws GeneralSecurityException {
        final int len = alpa.length();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < length; ++i) {
            final int ind = getUInt31() % len;
            buf.append(alpa.charAt(ind));
        }
        return buf.toString();
    }

    public void getBytes(byte[] buf, int ofs, int len) {
        int remain = len;
        while (remain > 0) {
            int block = Math.min(result_.length, remain);
            next();
            System.arraycopy(result_, 0, buf, ofs, block);
            ofs += block;
            remain -= block;
        }
    }

    public String getString(int len, boolean useLetters, String special) throws GeneralSecurityException {
        StringBuffer buf = new StringBuffer();
        appendSet(buf, '0', '9');
        if (useLetters) {
            appendSet(buf, 'a', 'z');
            appendSet(buf, 'A', 'Z');
        }
        if (null != special)
            buf.append(special);
        return getString(len, buf.toString());
    }

    public String getBitString(int nBits) throws GeneralSecurityException {
        // [32..127] = 96 chars ~ 6,58 bit / char
        StringBuffer buf = new StringBuffer();
        appendSet(buf, '\u0020', '\u007f');
        return getString((nBits *100 + 657)/ 658, buf.toString());
    }

    public static SecureRandom getInstance() throws GeneralSecurityException {
        if (null == random_)
            random_ = new SecureRandom();
        return random_;
    }
}
