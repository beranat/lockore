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

/**
 * Memory Input Buffer
 */
final class BufferInputStream extends java.io.InputStream {

    private final byte[] buffer_;
    private int offset_;
    private int index_;
    private int mark_;
    private int length_;

    public BufferInputStream(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    public BufferInputStream(byte[] buffer, int offset, int length) {
        if (null == buffer)
            throw new NullPointerException("NULL input stream buffer");
        buffer_ = buffer;
        reinit(offset, length);
    }

    public int read() throws IOException {
        if (index_ >= length_ )
            return -1;
        return buffer_[index_++] & 0xFF;
    }

    public boolean markSupported() {
        return true;
    }

    public int available() throws IOException {
        return (length_ - index_);
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (null == b)
            throw new NullPointerException("NULL read buffer");

        if (0 == len)
            return 0;

        if (index_ >= length_)
            return -1;

        final int r = Math.min(length_ - index_, len);
        System.arraycopy(buffer_, index_, b, off, r);
        index_ += r;
        return r;
    }

    public long skip(long n) throws IOException {
        if (n < 0)
            return 0;

        if (n > Integer.MAX_VALUE)
            throw new IOException("can not skip more then int bytes");

        final int l = Math.min((int)n, length_ - index_);
        index_ += l;    // length_ - index_ should protecte from overflow
        return l;
    }

    public void close() throws IOException {
        reinit(0, 0);
    }

    public void mark(int readlimit) {
        mark_ = index_;
    }

    public void reset() throws IOException {
        index_ = mark_;
    }

    public void reinit(int offset, int length) throws IllegalArgumentException {
        length += offset;
        if (length > buffer_.length)
            throw new IllegalArgumentException("Reinit buffer offset more then its length");

        offset_ = mark_ = index_ = offset_;
        length_ = length;
    }

    public byte[] buffer() {
        return buffer_;
    }

    public int pos() {
        return index_ - offset_;
    }
    public int size() {
        return length_ - offset_;
    }
}
