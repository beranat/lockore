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
 * Implements 3 formed plural
 * XXX_1 - Singular (Ends 1)
 * XXX_2 - Small values (Ends 2-4)
 * XXX_N - Plural
 * Languages: Belarusian, Bosnian, Croatian, Serbian, Russian, Ukrainian
 */
public class F3_Plural extends Plural {
    public static final String[] LANGUAGES = {"by", "ba", "hr", "rs", "ru", "ua"};

    public String get(long quantity) {
        return  (quantity %10==1 && quantity%100!=11
                    ? "_1"
                    : quantity%10>=2 && quantity%10<=4 && (quantity%100<10 || quantity%100>=20) ? "_2" : null);
    }
}
