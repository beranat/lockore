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

import java.io.IOException;
import java.util.Hashtable;

/**
 * Simple password quality calculator
 */
public class PasswordQuality {
    public static final String SPECIAL = "~!@#$%^&*()-_=+[{]};:'\"\\,<.>/?";

    // SHORT or DICT
    public static final int WEAK = 12;
    // WEAK LEVEL,
    // acceptable only with special conditions, like
    //  3 trys and lock card down
    //  Next try can be after a minute and etc
    public static final int AVERAGE = 36;
    // AVERAGE LEVEL
    // acceptable for local access, like unlock desktop from keyboard and etc
    // or with two-factor auth
    public static final int GOOD = 61;
    // GOOD LEVEL
    // Fairy secure for internetworking communication
    public static final int STRONG = 129;
    // STRONG LEVEL
    // Recomended for important and financial information

    protected static final String TOPPASSWD_FILE = "/bad_pass.txt";
    protected static final int MIN_LENGTH = 4;
    protected static final int MIN_CHARS_FOR_MIX = 2;

    protected final Hashtable dict_;

    public PasswordQuality() {
        dict_ = new Hashtable();
    }

    public PasswordQuality(java.io.InputStreamReader is) throws IOException {
        this();

        while (null != is) {
            String line = madrat.i18n.I18N._readLine(is);
            if (null == line)
                break;

            line = line.trim();

            if (line.length() == 0)
                continue;

            final String word = line.toLowerCase();
            dict_.put(word, word);
        }
    }

    public final boolean isDict(String password) {
        return isDict(password, true);
    }

    public final boolean isDict(String password, boolean checkNumTailed) {
        password = password.toLowerCase();
        final String tail = (checkNumTailed)?tailNumbers(password):null;
        if (null == tail)
            return dict_.containsKey(password);

        final String head = password.substring(0, password.length() - tail.length());
        return dict_.containsKey(head);
    }

    protected static final String tailNumbers(String password) {
        StringBuffer s = new StringBuffer();
        for (int i = password.length()-1; i >=0; --i) {
            final char c = password.charAt(i);
            if (!Character.isDigit(c))
                break;
            s.append(c);
        }
        return (s.length() >0)?s.reverse().toString():null;
    }

    protected static final boolean isSeries(String password) {
        if (null == password || 0 == password.length())
            return false;
        //          N, N, N         - from 3+
        //          N, M, N, M      - from 5+
        //          N, M, K, N      - from 6+
        int mod = password.indexOf(password.charAt(0), 1);
        int minlen = mod*2 + ((mod*2 <= MIN_LENGTH)?1:0);
        if (mod > 0 && minlen <= password.length()) {
            boolean seq = true;
            for (int i = 0; i < password.length(); ++i) {
                if (password.charAt(i) != password.charAt(i % mod)) {
                    seq = false;
                    break;
                }
            }
            if (seq)
                return true;
        }

        if (password.length() < MIN_LENGTH)
            return false;

        if (!password.equals(tailNumbers(password)))
            return false;
        //          N, N+1,...
        //          N, N-1,...

        int diff = (password.charAt(1) - password.charAt(0));
        if (diff > MIN_LENGTH || diff < -MIN_LENGTH)
            diff += (diff>0)?-10:+10;
        for (int i = 2; i < password.length(); ++i) {
            int cdiff = (password.charAt(i) - password.charAt(i-1));
            if (cdiff > MIN_LENGTH || cdiff < -MIN_LENGTH)
                cdiff += (cdiff>0)?-10:+10;
            if (diff != cdiff)
                return false;
        }
        return true;
    }

    protected static final int log2(int i) {
        switch (i) {
            case 0: throw new ArithmeticException("log2(0)");
            case 1: return 0;    //
            case 10: return 332; //123
            case 16: return 400; //#$%
            case 26: return 470; //ABC xyz 123#$% ???
            case 32: return 500;
            case 36: return 517; //123ABC 123xyz 123???
            case 42: return 539; //ABC#$% xyz#$% #$%???
            case 52: return 570; //ABCxyz 123ABC#$% 123xyz#$% ABC??? xyz??? 123#$%???
            case 62: return 595; //123ABCxyz 123ABC??? 123xyz???
            case 68: return 609; //ABCxyz#$% ABC#$%??? xyz#$%???
            case 78: return 629; //123ABCxyz#$% ABCxyz??? 123ABC#$%??? 123xyz#$%???
            case 88: return 646; //123ABCxyz???
            case 94: return 655; //ABCxyz#$%???
            case 104: return 670; //123ABCxyz#$%???
        }

        int b = (0 != (i & (i-1)))?1:0;
        while (i > 1) {
            ++b;
            i >>= 1;
        }
        return b*100;
    }

    public final int getBits(String password) {

        if (null == password || 0 == password.length())
            return 0;

        if (isSeries(password) || isDict(password, true))
            return -1;

        int dig = 0, lchar = 0, uchar = 0, spec = 0, extra = 0;
        for (int i = 0; i < password.length(); ++i) {
            final char c = password.charAt(i);
            if (Character.isDigit(c))
                ++dig;
            else if (Character.isUpperCase(c))
                ++uchar;
            else if (Character.isLowerCase(c))
                ++lchar;
            else if (c <= '\u007F')
                ++spec;
            else
                ++extra;
        }

        int bits = 0;
        int acc  = 0, cnt = 0;

        if (dig < MIN_CHARS_FOR_MIX) bits += log2(10)*dig;
        else { cnt += 10; acc += dig; }
        dig = 0;

        if (uchar < MIN_CHARS_FOR_MIX) bits += log2(26)*uchar;
        else { cnt += 26; acc += uchar; }
        uchar = 0;

        if (lchar < MIN_CHARS_FOR_MIX) bits += log2(26)*lchar;
        else { cnt += 26; acc += lchar; }
        lchar = 0;

        if (spec < MIN_CHARS_FOR_MIX) bits += log2(SPECIAL.length())*spec;
        else { cnt += 16; acc += spec; }
        spec = 0;

        if (extra < MIN_CHARS_FOR_MIX) bits += log2(26)*extra;
        else { cnt += 26; acc += extra; }
        extra = 0;

        return (bits + log2(0==acc?1:cnt)*acc) / 100;
    }
}
