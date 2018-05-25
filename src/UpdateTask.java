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

import java.util.TimerTask;

/**
 * Task for updating GUI items in-time
 */
public class UpdateTask extends TimerTask {
    private final TaskNotification    notify_;
    private final Object id_;

    public UpdateTask(TaskNotification notify, Object id) {
        notify_ = notify;
        id_ = id;
    }

    public void run() {
        notify_.onSuccess(id_, null, 0, null);
    }
}
