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
import javax.microedition.lcdui.Item;

import madrat.gui.GenericItem;
import madrat.i18n.I18N;

/**
 * Main settings menu list
 */
final class SettingsScreen extends LockoreForm {
    protected final Command clear_;
    protected final Command choose_;

    protected final GenericItem theme_;
    protected final GenericItem timeoffset_;
    protected final GenericItem sysinfo_;

    public SettingsScreen(LockoreForm back) {
        super(I18N.get("Settings"), back, Command.BACK, HelpIndexList.SETTINGS);

        choose_ = new Command(I18N.get("Select"), Command.ITEM, 1);
        clear_ = new Command(I18N.get("Reset"), I18N.get("Reset settings"), Command.SCREEN, 1);
        addCommand(clear_);

        theme_ = createLongItem(I18N.get("Theme"), null, Midlet.ICON_ART, choose_, Item.LAYOUT_RIGHT);
        append(theme_);

        timeoffset_= createLongItem(I18N.get("Time adjustment"), null, Midlet.ICON_NUMBER, choose_, Item.LAYOUT_RIGHT);
        append(timeoffset_);

        sysinfo_= createLongItem(I18N.get("System info"), null, Midlet.ICON_SETTINGS, choose_, Item.LAYOUT_RIGHT);
        append(sysinfo_);

        updateUI(null);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == clear_) {
            ConfirmAlert.showConfirmOkCancel(this, I18N.get("Reset settings (data will remain unchanged)?"), "Reset", 0, null);
            return;
        }
        else
            super.commandAction(c, d);
    }

    protected void commandAction(Command c, GenericItem item) throws Exception {
        if (theme_ == item)
            new ThemeScreen(this).show();
        else if (sysinfo_ == item) {
            LockoreForm info = new LockoreForm(I18N.get("System Info"), this, Command.OK, null);

            final Midlet m = Midlet.getInstance();
            info.appendInfoline("Platform", System.getProperty("microedition.platform"), Midlet.ICON_SETTINGS);
            info.appendInfoline("Locale", System.getProperty("microedition.locale"), Midlet.ICON_URI);
            info.appendInfoline("Filesystem", System.getProperty("microedition.io.file.FileConnection.version"), Midlet.ICON_STORE);
            info.appendInfoline("Text limit", I18N.formatBytes(Midlet.getMaxString()), Midlet.ICON_TEXT);
            info.appendInfoline("MD5",
                    I18N.get("{0}hash/s", I18N.formatSI(m.getHashPerf(Midlet.F_HARDWARE_MD5))), Midlet.ICON_NUMBER);
            info.appendInfoline("SHA-1",
                    I18N.get("{0}hash/s", I18N.formatSI(m.getHashPerf(Midlet.F_HARDWARE_SHA1))), Midlet.ICON_PASSWORD);
            info.appendInfoline("SHA-3",
                    I18N.get("{0}hash/s", I18N.formatSI(m.getHashPerf(Midlet.F_SOFTWARE_SHA3))), Midlet.ICON_CERTIFICATE);
            info.appendInfoline("AES-ECB",
                    I18N.get("{0}/s", I18N.formatBytes(m.getHashPerf(Midlet.F_HARDWARE_AESECB))), Midlet.ICON_KEY);
            info.appendInfoline("AES-CBC",
                    I18N.get("{0}/s", I18N.formatBytes(m.getHashPerf(Midlet.F_HARDWARE_AESCBC))), Midlet.ICON_STORE);
            info.appendInfoline("Secure Random",
                    I18N.get("{0}/s", I18N.formatBytes(m.getHashPerf(Midlet.F_HARDWARE_RANDOM))), Midlet.ICON_RANDOM);

            info.show();
        }
        else if (timeoffset_ == item) {
            new TimeAdjustmentScreen(this).show();
        }
        else
            super.commandAction(c, item);
    }

    public void updateUI(Object hint) {
        final Midlet m = Midlet.getInstance();
        if (null == m)
            return;

        final String theme = m.getThemeName();
        theme_.setDesc(I18N.get(null!=theme?theme:"Undefined"));

        long offset = m.getTimeOffset();

        if (0 == offset) {
            timeoffset_.setDesc(I18N.get("Not needed"));
        }
        else {
            String sign = "";

            if (offset <= -1) {
                sign = "-";
                offset = -offset;
            }
            timeoffset_.setDesc(sign + I18N.formatPeriod(offset));
        }
    }

    public void onConfirmed(int action, Object userinfo, boolean isAgreed) {
        final Midlet m = Midlet.getInstance();
        m.clearConfig();
        m.destroyApp(true);
    }
}
