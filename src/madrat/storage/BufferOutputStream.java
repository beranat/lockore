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
 * Memory Ouput buffer
 */
final class BufferOutputStream extends java.io.OutputStream {
    private final byte[] buffer_;
    private int offset_;
    private int index_;

    public BufferOutputStream(byte[] buffer) {
        buffer_ = buffer;
        reset();
    }

    public BufferOutputStream(byte[] buffer, int offset) {
        buffer_ = buffer;
        init(offset);
    }

    public void write(int b) throws IOException {
        if (null != buffer_) {
            buffer_[index_] = (byte)b;
        }
        ++index_;
    }

    public void write(byte[] b) throws IOException {
        if (null != buffer_)
            System.arraycopy(b, 0, buffer_, index_, b.length);
        index_ += b.length;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (null != buffer_)
            System.arraycopy(b, off, buffer_, index_, len);
        index_ += len;
    }

    public void reset() {
        index_ = offset_;
    }

    public void init(int offset) {
        index_ = offset_ = offset;
    }

    public int pos() {
        return index_ - offset_;
    }
}
