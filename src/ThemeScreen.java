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
import java.util.Hashtable;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import madrat.gui.GenericItem;

import madrat.gui.Theme;
import madrat.i18n.I18N;

/**
 * Theme selector screen
 */
final class ThemeScreen extends LockoreForm {
    private static final String THEMES = "/themes.txt";
    private static final String SYSTEM_THEME = "System theme";
    private static final String JAD_THEME = "JAD theme";

    private final Midlet midlet_;
    private final Command next_;
    private final Command select_;
    private final Hashtable themes_;

    private final String current_;

    private String name_ = null;
    private String colors_ = null;

    private final int INIT = 0;
    private final int CHANGE = 1;

    protected final void addTheme(String name, String theme) {
        if (   null == name || 0 == name.length()
            || null == theme || 0 == theme.length())
            return;

        try {
            Theme p = Midlet.getTheme(theme);
            themes_.put(name, theme);
        } catch (RuntimeException e) {
        }
    }

    public ThemeScreen(LockoreForm back) {
        super(I18N.get("Themes"), back, Command.CANCEL, HelpIndexList.SETTINGS);
        midlet_ = Midlet.getInstance();
        next_ = new Command(I18N.get("Another"), I18N.get("Show another"), Command.ITEM, 1);
        select_ = new Command(I18N.get("Select"), Command.OK, 1);

        themes_ = new Hashtable();

        String t = midlet_.getThemeName();
        // Current Theme (may be overvritten)
        if (null != t) {
            current_ = t;
            addTheme(current_, midlet_.getThemeColors());
        } else
            current_ = SYSTEM_THEME;

        //Themes from file
        try {
            InputStream is = Runtime.getRuntime().getClass().getResourceAsStream(THEMES);
            java.io.InputStreamReader isr = new InputStreamReader(is);
            do {
                final String line = I18N._readLine(isr);
                if (line == null)
                    break;

                if (0 == line.length() || '#' == line.charAt(0))
                    continue;

                final int pos = line.indexOf('=');
                if (pos <= 0 || pos + 1 == line.length())
                    continue;

                addTheme(line.substring(0, pos).trim(),
                        line.substring(pos + 1).trim());
            } while (true);
        } catch (IOException e) {
        }

        // System Theme
        themes_.put(SYSTEM_THEME, "");

        // JAD Theme
        addTheme(JAD_THEME, midlet_.getAppProperty(Midlet.JAD_THEME_COLORS));

        setTheme(current_);
    }

    public void commandAction(Command c, Displayable d) {
        try {
            switch (c.getCommandType()) {
                case Command.ITEM:
                    Enumeration e = themes_.keys();
                    if (e.hasMoreElements()) {
                        final String first = (String) e.nextElement();
                        String next = first;
                        boolean isFound;
                        do {
                            isFound = next.equals(name_);
                            next = first;
                            if (!e.hasMoreElements())
                                break;
                            next = (String) e.nextElement();
                        } while (!isFound);
                        setTheme(next);
                    }
                    break;
                case Command.OK:
                    if (null == back_) {
                        onConfirmed(INIT, null, true);
                        break;
                    }
                    if (!name_.equals(current_)) {
                        ConfirmAlert.showConfirmOkCancel(this, I18N.get("Restart application for apply theme?"), "Restart", CHANGE, null);
                        return;
                    }
                case Command.CANCEL:
                    if (null != back_)
                        back_.show();
                    else
                        Midlet.showStoreListScreen();
                    break;
                default:
                    super.commandAction(c, d);
                    break;
            }
        } catch (Exception ex) {
            Midlet.fatal(ex, "reinit@ThemeScreen");
        }
    }

    public void updateUI(Object hint) {
        colors_ = (String) themes_.get(name_);
        if (null != colors_ && 0 == colors_.length())
            colors_ = null;

        final String locName = I18N.get(name_);

        final boolean isError = !midlet_.setTheme(colors_);

        deleteAll();

        final GenericItem item = createItem(name_, locName, Midlet.ICON_ART, next_);
        item.addCommand(select_);
        append(item);

        appendLorem("Lorem ipsum", "Eiusmod tempor incididunt et dolore magna", Midlet.ICON_STORE);
        appendLorem("Minim veniam", "Quis nostrud exercitation ullamco laboris", Midlet.ICON_TEXT);
        appendLorem("Duis aute", "Reprehenderit in voluptate velit esse", Midlet.ICON_DATE);

        if (isError) {
            Midlet.showAlert(
                    AlertType.WARNING,
                    I18N.get("Invalid Theme"),
                    I18N.get("'{0}' can not loaded", locName), this);
        }
    }

    protected void setTheme(String name) {
        if (null == name || 0 == name.length()) {
            name = SYSTEM_THEME;
        }
        name_ = name;
        updateUI(null);
    }

    protected void appendLorem(String name, String desc, int icon) {
        final GenericItem item = createItem(I18N.get(name), I18N.get(desc), icon, next_);
        item.addCommand(select_);
        append(item);
    }

    protected void onConfirmed(int action, Object userinfo, boolean isAgreed) throws Exception {
        midlet_.setThemeName(name_);
        midlet_.setThemeColors(colors_);
        midlet_.saveConfig();

        if (action == INIT) {
            Midlet.showStoreListScreen();
        }
        else
            midlet_.destroyApp(true);
    }
}
