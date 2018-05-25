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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Vector;
import madrat.sys.SecureRandom;

/**
 * Record - named set of fields
 */
public final class Record extends StringField {
    private int rid_;
    private final Vector fields_; // null - nobody

    public Record(int rid) {
        super(true, false, "", DEFAULT_ICON, DEFAULT_FORMAT, "");
        fields_ = new Vector();
        setRid(rid);
    }

    public Record(int rid, boolean isNobody, DataInputStream is) throws IOException {
        super(true, false, "", DEFAULT_ICON, DEFAULT_FORMAT, "");
        fields_ = isNobody?null:new Vector();
        load(rid, is);
    }

    public final int getRid() {
        return rid_;
    }

    public final void setRid(int rid) {
        rid_ = rid;
    }

    public final void setDescription(String text) {
        super.set(text);
    }

    public final String getDescription() {
        return super.getString();
    }

    public boolean isNobody() {
        return (null == fields_);
    }

    final void checkBody() throws NobodyException {
        if (isNobody())
            throw new NobodyException();
    }

    public void append(Field f) {
        checkBody();
        modify();
        fields_.addElement(f);
    }

    public void remove(Field f) {
        checkBody();
        if (null != f) {
            modify();
            fields_.removeElement(f);
        }
    }

    public void remove(int index) {
        checkBody();
        modify();
        fields_.removeElementAt(index);
    }

    public Field at(int index) {
        checkBody();
        return (Field) fields_.elementAt(index);
    }

    public int size() {
        checkBody();
        return fields_.size();
    }

    public Field find(String name) {
        checkBody();

        java.util.Enumeration e = fields_.elements();
        while (e.hasMoreElements()) {
            Field f = (Field)e.nextElement();
            if (name.equals(f.getName()))
                return f;
        }
        return null;
    }

    public String get(String name, String def) {
        checkBody();

        Field f = find(name);
        if (null != f)
            return f.get(def);
        return def;
    }

    public long get(String name, long def) {
        checkBody();

        Field f = find(name);
        if (null != f)
            return f.get(def);
        return def;
    }

    public void set(String name, String value) {
        checkBody();

        Field f = find(name);
        try {
            if (null != f) {
                f.set(value);
                return;
            }
        }
        catch (RuntimeException e) {
            if (null != f)
                remove(f);
        }
        append(new StringField(false, name, DEFAULT_ICON, DEFAULT_FORMAT, value));
    }

    public void set(String name, long value) {
        checkBody();

        Field f = find(name);
        try {
            if (null != f) {
                f.set(Long.toString(value));
                return;
            }
        }
        catch (RuntimeException e) {
            if (null != f)
                remove(f);
        }
        append(new IntegerField(false, name, DEFAULT_ICON, DEFAULT_FORMAT, value));
    }

    public void load(int rid, DataInputStream is) throws IOException {
        rid_ = rid;
        if (!isNobody())
            fields_.removeAllElements();

        try
        {
            while (true) {
                Field f = Field.load(is);
                if (null == f)
                    continue;
                if (Field.META != f.getType()) {
                    if (!isNobody())
                        append(f);
                }
                else {
                        StringField s = (StringField)f;
                        super.setIcon(s.getIcon());
                        super.setFormat(s.getFormat());
                        super.setName(s.getName());
                        super.set(s.getString());
                        clearModify();
                }
            }
        }
        catch (EOFException eof) {
        }

        if (!isNobody()) {
            try {
                final int len = fields_.size();
                for (int i = len-1; i >= 0; --i) {
                    int v = SecureRandom.getInstance().getUInt31() % len;
                    final Object p = fields_.elementAt(v);
                    fields_.setElementAt(fields_.elementAt(i), v);
                    fields_.setElementAt(p, i);
                }
            }
            catch (GeneralSecurityException e) {
            }
        }
    }

    public boolean isModified() {
        if (isNobody())
            return false;

        if (super.isModified())
            return true;

        java.util.Enumeration e = fields_.elements();
        while (e.hasMoreElements()) {
            Field f = (Field)e.nextElement();
            if (f.isModified())
                return true;
        }
        return false;
    }

    public void clearModify() {
        super.clearModify();

        if (null != fields_) {
            java.util.Enumeration e = fields_.elements();
            while (e.hasMoreElements()) {
                Field f = (Field)e.nextElement();
                f.clearModify();
            }
        }
    }

    public void save(DataOutputStream os) throws IOException {
        checkBody();
        save(os, null, 0);
    }

    /**
     * Save current fields into stream
     * @param os
     * @param rand
     * @param saltLen
     * @throws IOException
     */
    public void save(DataOutputStream os,
                java.util.Random rand, int saltLen) throws IOException {
        checkBody();

        final int nElements = fields_.size();
        // |E0|E1|..En-1|M|
        final int nSaltPlaces = nElements + 2;
        int saltInd = (null != rand)?Math.abs(rand.nextInt()) % nSaltPlaces:0;

        for (int i = nElements-1; i >= 0; --i ) {
            if (0 == --saltInd)
                savePad(os, rand, saltLen);
            Field f = (Field)fields_.elementAt(i);
            f.save(os);
        }

        if (0 == --saltInd)
            savePad(os, rand, saltLen);

        super.save(os);
        clearModify();
    }
}
