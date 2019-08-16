/*
 * Copyright (C) 2017 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>
 *
 * This file is part of madRat's Internationalization (madrat.i18n).
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

package madrat.i18n;

/**
 * Implements 1-formed plural
 * XXX_N - for all forms
 * Families: Chinese, Japanese, Korean
 *
 *   NOTE: Yes, I know the academic way is implement  'interface Plural' and class 'F1_Plural', 
 * but for J2ME it takes more then 200 bytes in jar(!). */

public class Plural {
    public static final String[] LANGUAGES = {"zh", "jp", "ko"};

    public String get(long quantity) {
        return null;
    }
}
