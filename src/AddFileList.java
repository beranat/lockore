/*
 * Copyright (C) 2017 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>
 *
 * This file is part of Lockore application.
 *
 * Lockoree is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Lockore distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Lockore.
 * If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import madrat.i18n.I18N;
import madrat.storage.BinaryField;
import madrat.storage.Field;
import madrat.storage.Record;
import madrat.storage.Storage;
import madrat.storage.StringField;

/**
 * File selecting class
 */
final class AddFileList extends ListScreen {
    private final LockoreForm   parent_;
    private final Record        record_;

    private static final String EXTENSIONS[][] = {
        // LINKS 'bmk' is a binary link
        { "lnk", "url", "desktop", "webloc", "torrent" },
        // TEXT (UTF/ASCII)
        { "txt", "text", "ascii", "asc", "log" },
        // KEYS
        { "key", "pub", "pfx", "p12" },
        // CERTS
        { "pem", "cer", "crt", "der", "pkcs", "p7b", "p7c", "p7", "csr" }
    };

    static final int[] ICONS = {
        Midlet.ICON_URI,
        Midlet.ICON_TEXT,
        Midlet.ICON_KEY,
        Midlet.ICON_CERTIFICATE
    };

    static int getExtensionIcon(String name) {
        final int dot = name.lastIndexOf('.');
        if (-1 == dot)
            return Midlet.ICON_BINARY;
        final String ext = name.substring(dot+1).toLowerCase();

        for (int i =0; i < EXTENSIONS.length; ++i) {
            final String[] exts = EXTENSIONS[i];
            for (int j =0; j < exts.length; ++j) {
                if (exts[j].equals(ext))
                    return ICONS[i];
            }
        }
        return Midlet.ICON_BINARY;
    }


    public AddFileList(LockoreForm parent, Record r) throws Exception {
        super(Midlet.getLocalDirName(), Command.OK);
        parent_ = parent;
        record_ = r;

        final String dirPath = Midlet.getLocalDirUri();
        if (null == dirPath)
            throw new IllegalAccessException(I18N.get("No access to files"));

        FileConnection dir = (FileConnection)Connector.open(dirPath, Connector.READ);
        try {
            if (!dir.exists() || !dir.isDirectory())
                throw new IllegalAccessException(I18N.get("No access to {0}", dir.getName()));

            final Enumeration list = dir.list();
            while (list.hasMoreElements()) {
                final String name = (String)list.nextElement();
                if (name.endsWith("/") || name.endsWith("\\"))
                    continue;
                final String uri = dirPath + name;
                append(name, getExtensionIcon(name), uri);
            }
        }
        finally {
            dir.close();
        }

        addCommand(Midlet.CANCEL);
    }

    static final String getName(String path)
    {
        if (null == path)
            return null;

        int tail = path.length()-1;
        if (tail < 1)
            return path;

        for(; 0 < tail; --tail) {
            final char c = path.charAt(tail);
            if ('/' != c && '\\' != c) {
                break;
            }
        }

        int head = Math.max(path.lastIndexOf('/', tail), path.lastIndexOf('\\', tail));
        return path.substring(head+1, tail+1);
    }

    public void commandAction(Command c, Displayable d) {
        String uri = (String)getSelected();
        if (null == uri || Command.CANCEL == c.getCommandType()) {
            parent_.show();
            return;
        }

        try {
            String name = getString(getSelectedIndex());

            InputStream is = Connector.openInputStream(uri);
            if (null == is)
                throw new IllegalAccessException(I18N.get("No access to {0}", name));

            Object hint;
            try {
                hint = (null == record_)?addNewRecord(name, is):addNewField(name, is);
            }
            finally {
                is.close();
            }
            parent_.show(hint);
        }
        catch (Exception e) {
            Midlet.showAlert(e, parent_);
        }
    }

    private Object addNewRecord(String name, InputStream is) throws Exception {
        Storage s = Midlet.getStore();
        Record r = s.newRecord();

        r.setName(name);

        InputStreamReader isr = new InputStreamReader(is, "UTF-8");

        while (null != is) {
            String line = madrat.i18n.I18N._readLine(isr);
            if (null == line)
                break;

            line = line.trim();
            if (line.length() == 0)
                continue;
            if ('#' == line.charAt(0))
                continue;


            int sep = line.indexOf(':');
            if (-1 == sep)
                sep = line.length()-1;

            String fv = line.substring(sep+1).trim();
            String fn = line.substring(0, Math.max(0, sep)).trim();
            if (fn.length() < 1)
                fn = I18N.get("Unnamed");
            r.append(new StringField(false, fn, Midlet.ICON_NULL, Midlet.DEFAULT_FORMAT, fv));
        }

        s.insertNew(r);
        return r;
    }

    private Object addNewField(String name, InputStream is) throws IOException {
        byte[] data = null;

        final long len = is.available();
        if (len > Integer.MAX_VALUE)
            throw new OutOfMemoryError(I18N.get("File too large ({0})", I18N.formatBytes(len)));

        if (len > 0) {
            data = Midlet.alloc((int)len);

            if (len != is.read(data))
                throw new IOException(I18N.get("Load error"));
        }

        Field f;
        int icon = Midlet.ICON_NULL;
        switch (getExtensionIcon(name)) {
            case Midlet.ICON_URI:
                icon = Midlet.ICON_URI;
            case Midlet.ICON_TEXT:
                try {
                    String text = new String(data, "UTF-8");
                    if (text.length() > Midlet.getMaxString())
                        throw new IndexOutOfBoundsException(I18N.get("Too long"));
                    f = new StringField(false, name, icon, Midlet.DEFAULT_FORMAT, text);
                    break;
                }
                catch (RuntimeException e){
                }
            default:
                f = new BinaryField(false, name, icon, Midlet.DEFAULT_FORMAT, data);
            break;
        }
        record_.append(f);
        return f;
    }
}
