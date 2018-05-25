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

/**
 * Exception when unsupported field/data/type
 */
public class UnsupportedException extends StorageException {
    private static String formatError(String type, String value) {
        if (null == type && null == value)
            return "Storage unsupported exception";

        StringBuffer buf = new StringBuffer("Storage unsupported");

        if (null != type && 0 < type.length())
            type = "object";

        buf.append(' ');
        buf.append(type);

        if (null != value && 0 < value.length()) {
            buf.append(" '");
            buf.append(value);
            buf.append('\'');
        }

        return buf.toString();
    }
    public UnsupportedException(String type, String value) {
        super(formatError(type, value));
    }
}
