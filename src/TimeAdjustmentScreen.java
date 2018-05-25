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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import madrat.i18n.I18N;

/**
 * Setting for TOTP time adjust (from phone's to precision time)
 */
public class TimeAdjustmentScreen extends LockoreForm implements ItemStateListener, TaskNotification {
    private final Midlet midlet_;
    private final Calendar calendar_;
    private final String   timeFormat_;
    private final String[] time_;

    private final StringItem preview_;
    private final TextField offset_;

    private final Timer timer_;

    public TimeAdjustmentScreen(LockoreForm back) {
        super(I18N.get("Time Adjustment"), back, Command.CANCEL, HelpIndexList.SETTINGS);

        midlet_ = Midlet.getInstance();
        calendar_ = Calendar.getInstance();

        TimeZone gmt = TimeZone.getTimeZone("GMT");
        if (null == gmt)
            gmt = TimeZone.getTimeZone("UTC");

        if (null != gmt) {
                timeFormat_ = "{0}:{1}:{2} UTC";
                calendar_.setTimeZone(gmt);
        }
        else
            timeFormat_ = "{0}:{1}:{2}";
        time_ = new String[3];

        preview_ = new StringItem(I18N.get("Exact time"), null);
        append(preview_);

        // -99999 ~ 27h should be enouth
        offset_ = new TextField(I18N.get("Time offset (sec)"),
                Integer.toString(midlet_.getTimeOffset()), 6, TextField.NUMERIC);
        append(offset_);

        addCommand(Midlet.OK);

        updateUI(null);
        setItemStateListener(this);

        timer_ = new Timer();
        long initial = 1000 - (System.currentTimeMillis() % 1000);
        timer_.scheduleAtFixedRate(new UpdateTask(this, timer_), initial, 1000);
    }

    public void commandAction(Command c, Displayable d) {
        try {
            switch (c.getCommandType()) {
                case Command.OK:
                    midlet_.setTimeOffset(getOffset());
                    midlet_.saveConfig();
                    timer_.cancel();
                    back_.show(null);
                    break;
                case Command.CANCEL:
                    timer_.cancel();
                    back_.show();
                    break;
                default:
                    super.commandAction(c, d);
                    break;
            }
        } catch (NumberFormatException ex) {
            Midlet.showAlert(AlertType.ERROR, I18N.get("'{0}' is not number", ex.getMessage()),  null, this);
        } catch (Exception e) {
            Midlet.showAlert(e, this);
        }
    }

    protected int getOffset() throws NumberFormatException {
        final String offs = offset_.getString();
        return (null != offs && 0 < offs.length()) ? Integer.parseInt(offs) : 0;
    }

    protected void updateUI(Object hint) {
        final long ts = System.currentTimeMillis();

        try {
            Date d = new Date(ts + getOffset() * 1000L);
            calendar_.setTime(d);
            time_[0] = I18N.toStringZ(calendar_.get(Calendar.HOUR_OF_DAY), 2);
            time_[1] = I18N.toStringZ(calendar_.get(Calendar.MINUTE), 2);
            time_[2] = I18N.toStringZ(calendar_.get(Calendar.SECOND), 2);
            preview_.setText(I18N.format(timeFormat_, time_));
        } catch (NumberFormatException e) {
            preview_.setText(I18N.get("Invalid"));
        }
    }

    public void itemStateChanged(Item item) {
        if (item == offset_)
            updateUI(null);
    }

    public void onFailure(Object id, Exception e) {
    }

    public void onSuccess(Object id, String str, long value, byte[] data) {
        updateUI(null);
    }
}
