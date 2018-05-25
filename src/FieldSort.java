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

import madrat.sys.IComparator;
import madrat.sys.QuickSort;
import madrat.gui.GenericItem;
import madrat.storage.Field;

/**
 * Sort class for madrat.storage.Field
 *  - Unprotected strings
 *  - Protected strings
 *  - Numbers and Dates
 */
final class FieldSort implements IComparator {

    public boolean isLess(Object o1, Object o2) {
        final GenericItem i1 = (GenericItem)o1;
        final GenericItem i2 = (GenericItem)o2;

        if (null == i1)
            return i2 != null;

        if (null == i2)
            return false;

        final Field f1 = (Field)i1.getUserInfo();
        final Field f2 = (Field)i2.getUserInfo();

        final boolean isString1 = (Field.STRING == f1.getType());
        final boolean isString2 = (Field.STRING == f2.getType());
        if (isString1 != isString2)
            return isString1;

        if (isString1) {
            final boolean isProt1 = f1.isProtected();
            final boolean isProt2 = f2.isProtected();
            if (isProt1 != isProt2)
                return isProt2;
        }

        return QuickSort.lessString(f1.getName(),  f2.getName());
    }
}
