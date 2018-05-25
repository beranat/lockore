/*
 * Copyright (C) 2017 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>
 *
 * This file is part of madRat's J2ME helpers (madrat.sys).
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

package madrat.sys;

import java.util.Vector;

/**
 * Do Quick Sorting
 */
public final class QuickSort implements IComparator {
    public static final void sort(Vector v, IComparator comparator) {
        if (null == v || 1 >= v.size())
            return;
        sort(v, comparator, 0, v.size()-1);
    }

    private static void sort(Vector v, IComparator c, int left, int right) {
        Object pivot = v.elementAt((left + right) >> 1);

        int i = left, j = right;
        while (i <= j) {
            while (c.isLess(v.elementAt(i), pivot))
                    ++i;
            while (c.isLess(pivot, v.elementAt(j)))
                    --j;

            if (i <= j) {
                Object tmp = v.elementAt(i);
                v.setElementAt(v.elementAt(j), i);
                v.setElementAt(tmp, j);
                  ++i; --j;
            }
      }

      if (left < j)
            sort(v, c, left, j);
      if (i < right)
            sort(v, c, i, right);
    }

    public static final boolean lessString(String n1, String n2) {
        if (null == n1)
            return (null != n2);

        if (null == n2)
            return false;
        return n1.compareTo(n2) < 0;
    }

    public final boolean isLess(Object o1, Object o2) {
        return lessString((String)o1, (String)o2);
    }
}
