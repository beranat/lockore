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
import javax.crypto.ShortBufferException;

/**
 * SHA-3 (Secure Hash Algorithm 3) implementation for Java ME
 */
public final class Sha3 {
    private static final int NUMBER_OF_ROUNDS = 24;
    private static final int B = 1600;

    private static final long[] RC = {
        0x0000000000000001L, 0x0000000000008082L, 0x800000000000808aL,
        0x8000000080008000L, 0x000000000000808bL, 0x0000000080000001L,
        0x8000000080008081L, 0x8000000000008009L, 0x000000000000008aL,
        0x0000000000000088L, 0x0000000080008009L, 0x000000008000000aL,
        0x000000008000808bL, 0x800000000000008bL, 0x8000000000008089L,
        0x8000000000008003L, 0x8000000000008002L, 0x8000000000000080L,
        0x000000000000800aL, 0x800000008000000aL, 0x8000000080008081L,
        0x8000000000008080L, 0x0000000080000001L, 0x8000000080008008L
    };

    private static final int[] RHO = {
        0, 1, 62, 28, 27,
        36, 44, 6, 55, 20,
        3, 10, 43, 25, 39,
        41, 45, 15, 21, 8,
        18, 2, 61, 56, 14
    };

    private final int size_;
    private final int hmacBlockSize_;
    private final long[] state_;
    private final byte[] buffer_;

    private int index_ = 0;

    private long[] backup_ = null;


    public Sha3() throws InvalidAlgorithmParameterException, IllegalStateException {
        this(256);
    }

    public Sha3(int size) throws InvalidAlgorithmParameterException, IllegalStateException  {
        switch (size) {
            case 224:
                hmacBlockSize_ = 144;
                break;
            case 256:
                hmacBlockSize_ = 136;
                break;
            case 384:
                hmacBlockSize_ = 104;
                break;
            case 512:
                hmacBlockSize_ = 72;
                break;
            default:
                throw new InvalidAlgorithmParameterException("Bad size");
        }

        size_ = size / 8;
        state_ = new long[B / 64];
        buffer_ = new byte[B / 8 - 2 * size_];

        // optimization in round
        if (1600 != B) {
            throw new IllegalStateException("Only B=1600");
        }
        reset();
    }

    /**
     * hash size in bytes
     *
     * @return hash size in bytes
     */
    public int size() {
        return size_;
    }

    /**
     * Resets the hash's state for calculate new one.
     */
    public void reset() {
        for (int i = 0; i < state_.length; i++) {
            state_[i] = 0L;
        }
        clear(buffer_);
        index_ = 0;
    }

    /**
     * Updates the digest using the some part of input array.
     *
     * @param input - the inputed array
     * @param off - offset in array for 1st byte
     * @param len - number of bytes
     */
    public void update(byte[] input, int off, int len) {
        while (len > 0) {
            final int block = Math.min(buffer_.length - index_, len);
            System.arraycopy(input, off, buffer_, index_, block);

            index_ += block;
            off += block;
            len -= block;

            if (index_ == buffer_.length)
                processBuffer();
        }
    }

    /**
     * Completes the hash computation by performing final operations and returns
     * the result. Hash calculation can be continued with update.
     *
     * @param hash - output buffer for the computed digest
     * @param ofs - offset into the output buffer to begin storing the digest
     * @param len - number of bytes within hash allotted for the digest
     * @return the number of bytes placed into hash
     * @throws ShortBufferException - if not enough space in buffer for hash
     */
    public int digest(byte[] hash, int ofs, int len) throws ShortBufferException {
        if (len < size_) {
            throw new ShortBufferException("Hash buffer is too small");
        }

        if (null == backup_) {
            backup_ = new long[state_.length];
        }

        int index = index_;
        System.arraycopy(state_, 0, backup_, 0, backup_.length);

        doFinal(hash, ofs);

        System.arraycopy(backup_, 0, state_, 0, state_.length);
        index_ = index;

        for (int i = 0; i < backup_.length; i++) {
            backup_[i] = 0L;
        }

        return size_;
    }

    /**
     * Completes the hash computation by performing final operations, calculate
     * hash and clear all internal states.
     *
     * @param hash - output buffer for the computed digest
     * @param ofs - offset into the output buffer to begin storing the digest
     * @throws ShortBufferException - if not enough space in buffer for hash
     * @note see size() for getting hash's size
     * @note this function is more fast and need less memory then digest, but
     * there is no way to get intermediate hash and continue hashing.
     */
    public void finalize(byte[] hash, int ofs) throws ShortBufferException {
        doFinal(hash, ofs);
        reset();
    }

    /**
     * Calculate HMAC.
     *
     * @param password - password (private key)
     * @param message  - messages (may be 'null' ~ empty message)
     * @return HMac
     * @throws ShortBufferException - internal issue
     */
    public byte[] getHMmac(byte[] password, byte[] message) throws ShortBufferException {
        //final int blockSize = ((size() + 31) >> 5) << 6;
        final byte[] key = new byte[hmacBlockSize_];
        final byte[] result = new byte[size()];
        try {
            // KEY
            reset();
            if (password.length <= hmacBlockSize_) {
                System.arraycopy(password, 0, key, 0, password.length);
            } else {
                update(password, 0, password.length);
                finalize(key, 0);
            }

            // R1: internal
            for (int i =0; i < hmacBlockSize_; ++i)
                key[i] ^= 0x36;
            update(key, 0, hmacBlockSize_);
            if (null != message)
                update(message, 0, message.length);
            finalize(result, 0);

            // R2: external
            for (int i =0; i < hmacBlockSize_; ++i)
                key[i] ^= (0x36 ^ 0x5C);
            update(key, 0, hmacBlockSize_);
            update(result, 0, result.length);
            finalize(result, 0);
        }
        finally {
            clear(key);
        }
        return result;
    }


    protected void doFinal(byte[] hash, int ofs) throws ShortBufferException {
        if (hash.length - ofs < size_) {
            throw new ShortBufferException("No enouth space in finalization buffer");
        }

        addPadding();
        processBuffer();

        final int l64 = size_ >> 3;
        final int ltail = size_ & 0x7;
        for (int i = 0; i < l64; ++i) {
            // Call
            // ofs = ltob(state_[i], hash, ofs);
            // Raises perfomance loss about 30%
            final long v = state_[i];
            hash[ofs++] = (byte) (v & 0xFF);
            hash[ofs++] = (byte) (v >>> 8);
            hash[ofs++] = (byte) (v >>> 16);
            hash[ofs++] = (byte) (v >>> 24);
            hash[ofs++] = (byte) (v >>> 32);
            hash[ofs++] = (byte) (v >>> 40);
            hash[ofs++] = (byte) (v >>> 48);
            hash[ofs++] = (byte) (v >>> 56);
        }

        if (0 != ltail)
            Convertion.ltob(state_[l64], hash, ofs, ltail);
    }

    private void addPadding() {
        if (index_ + 1 == buffer_.length) {
            buffer_[index_] = (byte) 0x86;
        } else {
            buffer_[index_] = (byte) 0x06;
            for (int i = index_ + 1; i < buffer_.length - 1; i++) {
                buffer_[i] = 0;
            }
            buffer_[buffer_.length - 1] = (byte) 0x80;
        }
    }

    private void processBuffer() {
        for (int i = 0; i < buffer_.length; i += 8) {
            final long v = ((((long) buffer_[i]) & 0xFF))
                    | ((((long) buffer_[i + 1]) & 0xFF) << 8)
                    | ((((long) buffer_[i + 2]) & 0xFF) << 16)
                    | ((((long) buffer_[i + 3]) & 0xFF) << 24)
                    | ((((long) buffer_[i + 4]) & 0xFF) << 32)
                    | ((((long) buffer_[i + 5]) & 0xFF) << 40)
                    | ((((long) buffer_[i + 6]) & 0xFF) << 48)
                    | ((((long) buffer_[i + 7]) & 0xFF) << 56);
            state_[i >>> 3] ^= v;
        }

        for (int n = 0; n < NUMBER_OF_ROUNDS; n++) {
            round(n);
        }
        index_ = 0;
    }

    private void round(int n) {
        // keccak_theta
        long C0 = state_[0] ^ state_[5] ^ state_[10] ^ state_[15] ^ state_[20];
        long C1 = state_[1] ^ state_[6] ^ state_[11] ^ state_[16] ^ state_[21];
        long C2 = state_[2] ^ state_[7] ^ state_[12] ^ state_[17] ^ state_[22];
        long C3 = state_[3] ^ state_[8] ^ state_[13] ^ state_[18] ^ state_[23];
        long C4 = state_[4] ^ state_[9] ^ state_[14] ^ state_[19] ^ state_[24];

        long D0 = rot64(C1, 1) ^ C4;
        long D1 = rot64(C2, 1) ^ C0;
        long D2 = rot64(C3, 1) ^ C1;
        long D3 = rot64(C4, 1) ^ C2;
        long D4 = rot64(C0, 1) ^ C3;

        for (int x = 0; x < state_.length; x += 5) {
            state_[x] ^= D0;
            state_[x + 1] ^= D1;
            state_[x + 2] ^= D2;
            state_[x + 3] ^= D3;
            state_[x + 4] ^= D4;
        }

        // Keccak rho
        for (int i = 0; i < state_.length; ++i) {
            state_[i] = rot64(state_[i], RHO[i]);
        }

        //keccak_pi
        {
            final long A1 = state_[1];
            state_[1] = state_[6];
            state_[6] = state_[9];
            state_[9] = state_[22];
            state_[22] = state_[14];
            state_[14] = state_[20];
            state_[20] = state_[2];
            state_[2] = state_[12];
            state_[12] = state_[13];
            state_[13] = state_[19];
            state_[19] = state_[23];
            state_[23] = state_[15];
            state_[15] = state_[4];
            state_[4] = state_[24];
            state_[24] = state_[21];
            state_[21] = state_[8];
            state_[8] = state_[16];
            state_[16] = state_[5];
            state_[5] = state_[3];
            state_[3] = state_[18];
            state_[18] = state_[17];
            state_[17] = state_[11];
            state_[11] = state_[7];
            state_[7] = state_[10];
            state_[10] = A1;
        }

        //keccak_chi
        for (int i = 0; i < state_.length; i += 5) {
            final long A0 = state_[0 + i];
            final long A1 = state_[1 + i];

            state_[0 + i] ^= ~A1 & state_[2 + i];
            state_[1 + i] ^= ~state_[2 + i] & state_[3 + i];
            state_[2 + i] ^= ~state_[3 + i] & state_[4 + i];
            state_[3 + i] ^= ~state_[4 + i] & A0;
            state_[4 + i] ^= ~A0 & A1;
        }

        //keccak iota
        state_[0] ^= RC[n];
    }

    public static final long rot64(long w, int r) {
        return (w << r) | (w >>> (64 - r));
    }

    public static final void clear(byte[] arr) {
        for (int i =0; i < arr.length; ++i)
            arr[i] = 0;
    }
}

