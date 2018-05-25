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
 * Field with UTF-8-encoded string
 */
public class StringField extends Field {

    private final boolean isMeta_;
    private String value_;

    public final int getType() {
        return (isMeta_)?META:STRING;
    }

    public StringField(boolean isProtected, String name, int icon, int format, java.io.DataInputStream is) throws IOException {
        this(false, isProtected, name, icon,  format, is);
    }

    public StringField(boolean isProtected, String name, int icon, int format, String value) {
        this(false, isProtected, name, icon, format, value);
    }

    protected StringField(boolean isMeta, boolean isProtected, String name, int icon, int format, java.io.DataInputStream is) throws IOException {
        this(isMeta, isProtected, name, icon,  format, Stream.loadVarString(is));
    }

    protected StringField(boolean isMeta, boolean isProtected, String name, int icon, int format, String value) {
        super(isProtected, name, icon, format);
        isMeta_ = isMeta;
        set(value);
        clearModify();
    }

    public void save(java.io.DataOutputStream os) throws IOException {
        super.save(os);
        Stream.saveVarString(os, value_);
    }

    public String getString() throws StorageException, IllegalArgumentException {
        return value_;
    }

    public void set(String val) throws StorageException, IllegalArgumentException {
        if (null == val)
            val = "";

        if (!val.equals(value_)) {
            modify();
            value_ = val;
        }
    }
}
