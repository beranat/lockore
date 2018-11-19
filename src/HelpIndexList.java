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

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import madrat.i18n.I18N;

/**
 *  Main help screen - it shows screen and enumerates content
 */
final class HelpIndexList extends ListScreen {
    protected final Displayable parent_;

    public static final String GET_STARTED  = "Get Started";
    public static final String STORES       = "Stores";
    public static final String RECORDS      = "Records";
    public static final String FIELDS       = "Fields";
    public static final String TYPES        = "Types";
    public static final String SETTINGS     = "Settings";
    public static final String LICENSE      = "License";
    public static final String ABOUT        = "About";


    protected static final String HELP_POSTFIX = "_CONTENT";

    public static void showHelp(Displayable parent, String ref) {
        showHelp(parent, ref, false);
    }

    protected static void showHelp(Displayable parent, String ref, boolean fromIndex) {
        Displayable next = null;
        do {
            if (null == ref)
                break;

            if (ref.equals(ABOUT)) {
                next = new SplashScreen(parent);
                break;
            }

            final String index = ref + HELP_POSTFIX;
            String content = I18N.get(index);

            if (index.equals(content)) {
                if (GET_STARTED.equals(ref))
                    break;
                content = Midlet.getInstance().getAppProperty("MIDlet-Description");
            }

            next = new HelpScreen(parent, I18N.get(ref), content, fromIndex);
        } while (false);

        if (null == next && !fromIndex)
            next = new HelpIndexList(parent);

        if (null != next)
            Midlet.setCurrent(next);
    }

    protected HelpIndexList(Displayable parent) {
        super(I18N.get("Help"), Command.OK);

        parent_ = parent;
        addCommand(Midlet.BACK);

        addIndex(GET_STARTED,   Midlet.ICON_NUMBER);
        addIndex(STORES,        Midlet.ICON_STORE);
        addIndex(RECORDS,       Midlet.ICON_KEY);
        addIndex(FIELDS,        Midlet.ICON_TEXT);
        addIndex(TYPES,         Midlet.ICON_BINARY);
        addIndex(SETTINGS,      Midlet.ICON_SETTINGS);
        addIndex(LICENSE,       Midlet.ICON_CERTIFICATE);
        addIndex(ABOUT,         Midlet.ICON_APPLICATION);
    }

    protected final void addIndex(String name, int icon) {
        append(I18N.get(name), icon, name);
    }

    public void commandAction(Command c, Displayable d) {
        switch (c.getCommandType()) {
            case Command.OK:
                String page = (String)getSelected();
                showHelp(this, page, true);
                return;
            case Command.BACK:
                Midlet.setCurrent(parent_);
                break;
        }
    }
}
