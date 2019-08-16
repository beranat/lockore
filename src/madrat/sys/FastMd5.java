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
 * Tuned MD5 implementation
 */
public class FastMd5 {

    private static final byte[] PADDING = {
        (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    protected int[] state_ = new int[4];      // 128 bits = 32 int
    protected byte[] buffer_ = new byte[64];
    protected int[] block_ = new int[16];

    protected int index_ = 0;
    protected long length_ = 0;   // hashed length in bytes

    public void reset() {
        state_[0] = 0x67452301;  state_[1] = 0xefcdab89;
        state_[2] = 0x98badcfe;  state_[3] = 0x10325476;

        for(int i = 0; i < buffer_.length; ++i)
            buffer_[i] = 0x55;
        for(int i = 0; i < block_.length; ++i)
            block_[i] = 0x55AA55AA;
        length_ = 0;
        index_ = 0;
    }

    public void update(byte[] input, int off, int len) {
        length_ += len;
        while (len > 0) {
            final int block = Math.min(buffer_.length - index_, len);
            System.arraycopy(input, off, buffer_, index_, block);
            index_ += block;
            off += block;
            len -= block;
            transform();
        }
    }

    public void digest(int[] digest) {
        long l = length_;

        int padLen = (index_ < 56) ? (56 - index_) : (120 - index_);
        update(PADDING, 0, padLen);
        encode((int) (l << 3), buffer_, 56);
        encode((int) (l >> (32 - 3)), buffer_, 60);
        index_ += 8;
        transform();
        if (digest != state_) {
            System.arraycopy(state_, 0, digest, 0, 4);
        }
    }
    public void digest(byte[] digest) {
        digest(state_);
        encode(state_, 16, digest);
    }

    private void transform() {
        if (index_ < buffer_.length) {
            return;
        }
        index_ = 0;
        decode(buffer_, 64, block_);
        transform(state_, block_);
    }

    protected static void encode(int value, byte[] dest, int ofs) {
        dest[ofs]     = (byte) (value & 0xff);
        dest[ofs + 1] = (byte) ((value >>> 8) & 0xff);
        dest[ofs + 2] = (byte) ((value >>> 16) & 0xff);
        dest[ofs + 3] = (byte) ((value >>> 24) & 0xff);
    }

    public static void encode(int[] src, int len, byte[] dest) {
        for (int i = 0, j = 0; j < len; ++i, j += 4) {
            encode(src[i], dest, j);
        }
    }

    public static void decode(byte[] src, int len, int[] dest) {
        for (int i = 0, j = 0; j < len; i++, j += 4) {
            dest[i] = (((int) src[j]) & 0xFF)
                    | (((int) (src[j + 1]) & 0xFF) << 8)
                    | (((int) (src[j + 2]) & 0xFF) << 16)
                    | (((int) (src[j + 3]) & 0xFF) << 24);
        }
    }

    public static void fast(int N, int[] hash, int[] block) {
        for (int i = N >> 1; i >0; --i) {
            hash[4] = 0x00000080;     hash[5] = 0x00000000;     hash[6] = 0x00000000;
            hash[7] = 0x00000000;     hash[8] = 0x00000000;     hash[9] = 0x00000000;
            hash[10] = 0x00000000;    hash[11] = 0x00000000;    hash[12] = 0x00000000;
            hash[13] = 0x00000000;    hash[14] = 0x00000080;    hash[15] = 0x00000000;
            block[0] = 0x67452301;    block[1] = 0xefcdab89;
            block[2] = 0x98badcfe;    block[3] = 0x10325476;
            FastMd5.transform(block, hash);

            block[4] = 0x00000080;    block[5] = 0x00000000;    block[6] = 0x00000000;
            block[7] = 0x00000000;    block[8] = 0x00000000;    block[9] = 0x00000000;
            block[10] = 0x00000000;   block[11] = 0x00000000;   block[12] = 0x00000000;
            block[13] = 0x00000000;   block[14] = 0x00000080;   block[15] = 0x00000000;
            hash[0] = 0x67452301;     hash[1] = 0xefcdab89;
            hash[2] = 0x98badcfe;     hash[3] = 0x10325476;
            FastMd5.transform(hash, block);
        }
        if (0 != (N&1))
            fast(hash, block);
    }

    public static void fast(int[] state, int[] block) {
        block[0] = state[0];   block[1] = state[1];
        block[2] = state[2];   block[3] = state[3];
        block[4] = 0x00000080;  block[5] = 0x00000000;
        block[6] = 0x00000000;  block[7] = 0x00000000;
        block[8] = 0x00000000;  block[9] = 0x00000000;
        block[10]= 0x00000000;  block[11]= 0x00000000;
        block[12]= 0x00000000;  block[13]= 0x00000000;
        block[14]= 0x00000080;  block[15]= 0x00000000;
        state[0] = 0x67452301;  state[1] = 0xefcdab89;
        state[2] = 0x98badcfe;  state[3] = 0x10325476;
        transform(state, block);
    }

    public static void transform(int[] state, int[] block) {
        int     a = state[0],
                b = state[1],
                c = state[2],
                d = state[3],
                x[] = block;

        // Round 1
        a += ((b & c) | (~b & d)) + x[0] + 0xd76aa478;        a = ((a << 7) | (a >>> 25)) + b;
        d += ((a & b) | (~a & c)) + x[1] + 0xe8c7b756;        d = ((d << 12) | (d >>> 20)) + a;
        c += ((d & a) | (~d & b)) + x[2] + 0x242070db;        c = ((c << 17) | (c >>> 15)) + d;
        b += ((c & d) | (~c & a)) + x[3] + 0xc1bdceee;        b = ((b << 22) | (b >>> 10)) + c;
        a += ((b & c) | (~b & d)) + x[4] + 0xf57c0faf;        a = ((a << 7) | (a >>> 25)) + b;
        d += ((a & b) | (~a & c)) + x[5] + 0x4787c62a;        d = ((d << 12) | (d >>> 20)) + a;
        c += ((d & a) | (~d & b)) + x[6] + 0xa8304613;        c = ((c << 17) | (c >>> 15)) + d;
        b += ((c & d) | (~c & a)) + x[7] + 0xfd469501;        b = ((b << 22) | (b >>> 10)) + c;
        a += ((b & c) | (~b & d)) + x[8] + 0x698098d8;        a = ((a << 7) | (a >>> 25)) + b;
        d += ((a & b) | (~a & c)) + x[9] + 0x8b44f7af;        d = ((d << 12) | (d >>> 20)) + a;
        c += ((d & a) | (~d & b)) + x[10] + 0xffff5bb1;        c = ((c << 17) | (c >>> 15)) + d;
        b += ((c & d) | (~c & a)) + x[11] + 0x895cd7be;        b = ((b << 22) | (b >>> 10)) + c;
        a += ((b & c) | (~b & d)) + x[12] + 0x6b901122;        a = ((a << 7) | (a >>> 25)) + b;
        d += ((a & b) | (~a & c)) + x[13] + 0xfd987193;        d = ((d << 12) | (d >>> 20)) + a;
        c += ((d & a) | (~d & b)) + x[14] + 0xa679438e;        c = ((c << 17) | (c >>> 15)) + d;
        b += ((c & d) | (~c & a)) + x[15] + 0x49b40821;        b = ((b << 22) | (b >>> 10)) + c;

        // Round 2
        a += ((b & d) | (c & ~d)) + x[1] + 0xf61e2562;        a = ((a << 5) | (a >>> 27)) + b;
        d += ((a & c) | (b & ~c)) + x[6] + 0xc040b340;        d = ((d << 9) | (d >>> 23)) + a;
        c += ((d & b) | (a & ~b)) + x[11] + 0x265e5a51;        c = ((c << 14) | (c >>> 18)) + d;
        b += ((c & a) | (d & ~a)) + x[0] + 0xe9b6c7aa;        b = ((b << 20) | (b >>> 12)) + c;
        a += ((b & d) | (c & ~d)) + x[5] + 0xd62f105d;        a = ((a << 5) | (a >>> 27)) + b;
        d += ((a & c) | (b & ~c)) + x[10] + 0x02441453;        d = ((d << 9) | (d >>> 23)) + a;
        c += ((d & b) | (a & ~b)) + x[15] + 0xd8a1e681;        c = ((c << 14) | (c >>> 18)) + d;
        b += ((c & a) | (d & ~a)) + x[4] + 0xe7d3fbc8;        b = ((b << 20) | (b >>> 12)) + c;
        a += ((b & d) | (c & ~d)) + x[9] + 0x21e1cde6;        a = ((a << 5) | (a >>> 27)) + b;
        d += ((a & c) | (b & ~c)) + x[14] + 0xc33707d6;        d = ((d << 9) | (d >>> 23)) + a;
        c += ((d & b) | (a & ~b)) + x[3] + 0xf4d50d87;        c = ((c << 14) | (c >>> 18)) + d;
        b += ((c & a) | (d & ~a)) + x[8] + 0x455a14ed;        b = ((b << 20) | (b >>> 12)) + c;
        a += ((b & d) | (c & ~d)) + x[13] + 0xa9e3e905;        a = ((a << 5) | (a >>> 27)) + b;
        d += ((a & c) | (b & ~c)) + x[2] + 0xfcefa3f8;        d = ((d << 9) | (d >>> 23)) + a;
        c += ((d & b) | (a & ~b)) + x[7] + 0x676f02d9;        c = ((c << 14) | (c >>> 18)) + d;
        b += ((c & a) | (d & ~a)) + x[12] + 0x8d2a4c8a;        b = ((b << 20) | (b >>> 12)) + c;

        // Round 3
        a += (b ^ c ^ d) + x[5] + 0xfffa3942;        a = ((a << 4) | (a >>> 28)) + b;
        d += (a ^ b ^ c) + x[8] + 0x8771f681;        d = ((d << 11) | (d >>> 21)) + a;
        c += (d ^ a ^ b) + x[11] + 0x6d9d6122;        c = ((c << 16) | (c >>> 16)) + d;
        b += (c ^ d ^ a) + x[14] + 0xfde5380c;        b = ((b << 23) | (b >>> 9)) + c;
        a += (b ^ c ^ d) + x[1] + 0xa4beea44;        a = ((a << 4) | (a >>> 28)) + b;
        d += (a ^ b ^ c) + x[4] + 0x4bdecfa9;        d = ((d << 11) | (d >>> 21)) + a;
        c += (d ^ a ^ b) + x[7] + 0xf6bb4b60;        c = ((c << 16) | (c >>> 16)) + d;
        b += (c ^ d ^ a) + x[10] + 0xbebfbc70;        b = ((b << 23) | (b >>> 9)) + c;
        a += (b ^ c ^ d) + x[13] + 0x289b7ec6;        a = ((a << 4) | (a >>> 28)) + b;
        d += (a ^ b ^ c) + x[0] + 0xeaa127fa;        d = ((d << 11) | (d >>> 21)) + a;
        c += (d ^ a ^ b) + x[3] + 0xd4ef3085;        c = ((c << 16) | (c >>> 16)) + d;
        b += (c ^ d ^ a) + x[6] + 0x04881d05;        b = ((b << 23) | (b >>> 9)) + c;
        a += (b ^ c ^ d) + x[9] + 0xd9d4d039;        a = ((a << 4) | (a >>> 28)) + b;
        d += (a ^ b ^ c) + x[12] + 0xe6db99e5;        d = ((d << 11) | (d >>> 21)) + a;
        c += (d ^ a ^ b) + x[15] + 0x1fa27cf8;        c = ((c << 16) | (c >>> 16)) + d;
        b += (c ^ d ^ a) + x[2] + 0xc4ac5665;        b = ((b << 23) | (b >>> 9)) + c;

        // Round 4
        a += (c ^ (b | ~d)) + x[0] + 0xf4292244;        a = ((a << 6) | (a >>> 26)) + b;
        d += (b ^ (a | ~c)) + x[7] + 0x432aff97;        d = ((d << 10) | (d >>> 22)) + a;
        c += (a ^ (d | ~b)) + x[14] + 0xab9423a7;        c = ((c << 15) | (c >>> 17)) + d;
        b += (d ^ (c | ~a)) + x[5] + 0xfc93a039;        b = ((b << 21) | (b >>> 11)) + c;
        a += (c ^ (b | ~d)) + x[12] + 0x655b59c3;        a = ((a << 6) | (a >>> 26)) + b;
        d += (b ^ (a | ~c)) + x[3] + 0x8f0ccc92;        d = ((d << 10) | (d >>> 22)) + a;
        c += (a ^ (d | ~b)) + x[10] + 0xffeff47d;        c = ((c << 15) | (c >>> 17)) + d;
        b += (d ^ (c | ~a)) + x[1] + 0x85845dd1;        b = ((b << 21) | (b >>> 11)) + c;
        a += (c ^ (b | ~d)) + x[8] + 0x6fa87e4f;        a = ((a << 6) | (a >>> 26)) + b;
        d += (b ^ (a | ~c)) + x[15] + 0xfe2ce6e0;        d = ((d << 10) | (d >>> 22)) + a;
        c += (a ^ (d | ~b)) + x[6] + 0xa3014314;        c = ((c << 15) | (c >>> 17)) + d;
        b += (d ^ (c | ~a)) + x[13] + 0x4e0811a1;        b = ((b << 21) | (b >>> 11)) + c;
        a += (c ^ (b | ~d)) + x[4] + 0xf7537e82;        a = ((a << 6) | (a >>> 26)) + b;
        d += (b ^ (a | ~c)) + x[11] + 0xbd3af235;        d = ((d << 10) | (d >>> 22)) + a;
        c += (a ^ (d | ~b)) + x[2] + 0x2ad7d2bb;        c = ((c << 15) | (c >>> 17)) + d;
        b += (d ^ (c | ~a)) + x[9] + 0xeb86d391;        b = ((b << 21) | (b >>> 11)) + c;

        state[0] += a;
        state[1] += b;
        state[2] += c;
        state[3] += d;
    }
}
