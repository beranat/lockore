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
 *
 * This class uses some code from NetBeans 8.2 Localization Support Class.
 */

package madrat.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import javax.microedition.io.ConnectionNotFoundException;

/**
 * Internationalization support class
 */
public final class I18N {
    private static final String NULL = "null";

    private static final String PLURAL_SUFFIX = "_N";

    private static final int    MAX_SUBSTITUTION_LEN = 5; // no more then MAX_SUBSTITUTION_LEN chars in {...}
    private static final String DEFAULT_LANG = "en";

    private static final String L18N_FILEPATH = "/{0}.lang";
    private static final String L18N_EXTRA_DEFFILENAME = "extra";
    private static final String L18N_EXTRA_FILENAME = L18N_EXTRA_DEFFILENAME + "_{0}";
    private static final String FIX_SEPARATORS = "-.";
    private static final char SEPARATOR = '_';

    private static final long FORMAT_BYTES_MIN = 970;   // 970 bytes -> 0.9Kbyte
    private static final String FORMAT_BYTES[] =  {
        "{0}Bytes_N",   "{0}KiBytes_N", "{0}MiBytes_N",
        "{0}GiBytes_N", "{0}TiBytes_N", "{0}PiBytes_N"};

    private static final long FORMAT_SI_MIN = 899;   // 90 items ~ 0.9K
    private static final String FORMAT_SI[] =  {
        "{0}_N",    "{0}Kilo_N", "{0}Mega_N",
        "{0}Giga_N","{0}Tera_N", "{0}Peta_N" };

    private static final String FORMAT_DATE = "{0}-{1}-{2}";

    private static Hashtable messages_ = null;
    private static Plural plural_ = null;

    private static boolean isLang(String locale, String[] locales) {
        if (null == locales)
            return false;

        for (int i =0; i < locales.length; ++i) {
            if (locales[i].equals(locale))
                return true;
        }
        return false;
    }

    private static boolean loadPlurals(String locale) {
        if (null == locale || isLang(locale, Plural.LANGUAGES)) {
            plural_ = new Plural();
            return true;
        }

        if (isLang(locale, F2_Plural1.LANGUAGES)) {
            plural_ = new F2_Plural1();
            return true;
        }

        if (isLang(locale, F2_Plural2.LANGUAGES)) {
            plural_ = new F2_Plural2();
            return true;
        }

        if (isLang(locale, F3_Plural.LANGUAGES)) {
            plural_ = new F3_Plural();
            return true;
        }

        return false;
    }

    public static void init() {
        init(System.getProperty("microedition.locale"));     // I18N
    }

    public static boolean init(String locale) {
        messages_ = new Hashtable();

        String locales[] = new String[3];
        int size = 0;

        if (null != locale && 0 < locale.length()) {
            //Fix "en-US" -> "en_US"
            for (int i = 0; i < FIX_SEPARATORS.length(); ++i) {
                locale = locale.replace(FIX_SEPARATORS.charAt(i), SEPARATOR);
            }

            locales[size++] = locale;

            // add "en" for en_US too
            final int ind = locale.indexOf(SEPARATOR);
            if (0 < ind && ind < locale.length()-1)
                locales[size++] = locale.substring(0, ind);
        }

        locales[size++] = DEFAULT_LANG;

        // load main
        boolean loaded = false;
        for (int i = 0; i < size && !loaded; ++i) {
            String l = locales[i];
            if (loadMessages(l)) {
                for (int j = i; j < size; ++j) {
                    String lp = locales[j];
                    if (loadPlurals(lp)) {
                        loaded = true;
                        break;
                    }
                }
                loadPlurals(null);
                loaded = true;
                break;
            }
        }

        if (!loaded) {
            messages_.clear();
            loadPlurals(null);
        }

        //load add-on file(s)
        for (int i = size; i >= 0; --i) {
            String name = (i<size)?format(L18N_EXTRA_FILENAME, locales[i]):L18N_EXTRA_DEFFILENAME;
            appendMessages(name);
        }
        return loaded;
    }

    /**
     * Generates correct text form based on quantity for keyFormat,
     * like "1 byte", "2 bytes" or etc
     *
     * @param valFormat - key-based format like '{0} Bytes' + PLURAL_SUFFIX
     * @param quantity - quantity
     * @return correct form of message
     */
    public static String getN(String valFormat, long quantity) {
        if (null == plural_)
            init();

        int vl = valFormat.length() - PLURAL_SUFFIX.length();
        String format = valFormat;

        if (vl > 0 && valFormat.endsWith(PLURAL_SUFFIX)) {
            String plural = plural_.get(Math.abs(quantity));
            if (null != plural)
                format = valFormat.substring(0, vl) + plural;
        }
        else
            vl = 0;

        format = get(format, false);
        while (null == format) {
            format = get(valFormat, false);
            if (null != format)
                break;
            format = (vl > 0)?valFormat.substring(0, vl):valFormat;
        }

        return format(format, Long.toString(quantity));
    }

    /**
     * Finds a localized string in a message bundle.
     *
     * @param key key of the localized string to look for
     * @param fixNA - true - return key if no string
     *                false - return null if no string
     * @return the localized string. If key is not found, then key string is
     * returned
     */
    protected static String get(String key, boolean fixNA) {
        if (null == messages_)
            init();
        if (null == key)
            return null;

        String s = (String) messages_.get(key);
        return (null != s)?s: (fixNA?key:null);
    }

    public static String get(String key) {
        return get(key, true);
    }

    public static String get(String key, String[] args) {
        return format(get(key, true), args);
    }

    /**
     * Localize key-string and format message with argument.
     *
     * @param key key of the localized string to look for
     * @param arg array of parameters to use for formatting the message
     * @return the localized string. If key is not found
     */
    public static String get(String key, String arg) {
        return format(get(key, true), arg);
    }

    protected static String padZ(final int count, String s) {
        if (count <= 0)
            return s;

        switch (count) {
            case 1:
                return "0" + s;
            case 2:
                return "00" + s;
        }

        StringBuffer b = new StringBuffer(count);
        for (int i = count; i >0; --i)
            b.append('0');
        b.append(s);
        return b.toString();
    }

    public static String toStringZ(long value, int digits) {
        final String s = Long.toString(value);
        return padZ(digits - s.length(), s);
    }

    /**
     * Localize and convert bytes value into Kilo/Mega and etc
     *
     * @param value - byte value
     * @return Value byes in user-friendly form
     */
    public static String formatBytes(long value) {
        if (value < 0)
            return get(FORMAT_BYTES[0], "*.**");
        return formatValue(value, FORMAT_BYTES, 1024, FORMAT_BYTES_MIN);
    }

    public static final String formatPeriod(long period) {
        if (period < 60)
            return I18N.getN("{0}seconds_N", Math.max(1, period));

        period /= 60; // minutes
        if (period < 60)
            return I18N.getN("{0}minutes_N", period);

        period /= 60; // hours
        if (period < 24)
            return I18N.getN("{0}hours_N", period);

        period /= 24; // days
        return I18N.getN("{0}days_N", period);
    }

    public static String formatSI(long value) {
        return formatValue(value, FORMAT_SI, 1000, FORMAT_SI_MIN);
    }

    private static String formatValue(long value, String[] format, int base, long min) {
        final int maxInd = format.length - 1;
        final long sign = (value<0)?-1:1;

        int ind = 0;
        int dec = 0;
        if (value > Long.MAX_VALUE/10L || value < Long.MIN_VALUE/10L) {
            ind = 1;
            value /= base;
        }
        value = Math.abs(value);

        while (value > min && ind < maxInd) {
            ++ind;
            value = value * 10L / base;
            dec = (int)(value % 10);
            value = value / 10L;

            if (10 <= value)
                dec = 0;
        }

        value = value*sign;
        if (dec == 0)
            return getN(format[ind], value);
        else
            return get(format[ind], Long.toString(value)+"."+Integer.toString(dec));
    }

    public static String formatDate(Date value) {
        String[] d = new String[3];
        if (null != value) {
            Calendar c = Calendar.getInstance();
            c.setTime(value);
            d[0] = toStringZ(c.get(Calendar.YEAR), 4);
            d[1] = toStringZ(c.get(Calendar.MONTH) - Calendar.JANUARY + 1, 2);
            d[2] = toStringZ(c.get(Calendar.DAY_OF_MONTH), 2);
        }
        else {
            d[0] = "****";
            d[1] = d[2] = "**";
        }
        return I18N.get(FORMAT_DATE, d);
    }

    /**
     * String formatter for a string
     * @param format - format string with markers '{0}'
     * @param arg - argument
     * @return format with replaced all '{0}' to argument
     */
    public static String format(String format, String arg) {
        return format(format, new String[] { arg });
    }

    /**
     * String formatter
     * @param format - format string with markers '{N}' (N - integer)
     * @param args - array of arguments
     * @return format string where all {N} will be replaces with args[N]
     */
    public static String format(String format, String[] args) {
        final int len = format.length();
        char[] f = new char[len];
        format.getChars(0, len, f, 0);

        StringBuffer result = new StringBuffer();

        int index = 0;
        do {
            int start  = format.indexOf('{', index);

            if (0 > start || start >= len)
                start = len;

            result.append(f, index, start-index);

            if (start < len-1) {
                start = start+1;

                if ('{' == f[start]) {
                    result.append('{');
                    ++start;
                } else {
                    int end = format.indexOf('}', start);
                    if (0 > end) {
                        end = len;
                    }

                    final int l = (start + MAX_SUBSTITUTION_LEN > end)?end:start+MAX_SUBSTITUTION_LEN;

                    int  arg = 0;
                    for (int j = start; j < l; ++j) {
                        arg = arg*10 + f[j] - '0';
                    }

                    if (arg >=0 && arg < args.length) {
                        result.append((null!=args[arg])?args[arg]:NULL);
                    }
                    start = end + 1;
                }
            }
            index = start;
        } while (index < len);
        return result.toString();
    }

    /**
     * Characters separating keys and values
     */
    private static final String KEY_VALUE_SEPARATORS = "=: \t\r\n\f";
    /**
     * Characters strictly separating keys and values
     */
    private static final String STRICT_KEY_VALUE_SEPARTORS = "=:";
    /**
     * white space characters understood by the support (these can be in the
     * message file)
     */
    private static final String WHITESPACE_CHARS = " \t\r\n\f";

    /**
     * Loads messages from input stream to hash table.
     *
     * @param inStream stream from which the messages are read
     * @throws IOException if there is any problem with reading the messages
     */
    private static boolean loadMessages(String locale) {
        messages_.clear();
        if (null == locale || 0 >= locale.length())
            locale = DEFAULT_LANG;
        return appendMessages(locale);
    }

    /**
     * Appends messages from input stream to hash table.
     *
     * @param inStream stream from which the messages are read
     * @throws IOException if there is any problem with reading the messages
     */
    private static boolean appendMessages(String locale) {
        if (null == locale || 0 == locale.length())
            return false;

        try {
            final Class c = Runtime.getRuntime().getClass();
            InputStream is = c.getResourceAsStream(format(L18N_FILEPATH, locale));

            if (null == is)
                throw new  ConnectionNotFoundException("No l10n file");

            InputStreamReader in = new InputStreamReader(is, "UTF-8");

            while (true) {
                // Get next line
                String line = _readLine(in);
                if (line == null) {
                    break;
                }

                if (line.length() > 0) {

                    // Find start of key
                    int len = line.length();
                    int keyStart;
                    for (keyStart = 0; keyStart < len; keyStart++) {
                        if (WHITESPACE_CHARS.indexOf(line.charAt(keyStart)) == -1) {
                            break;
                        }
                    }

                    // Blank lines are ignored
                    if (keyStart == len) {
                        continue;
                    }

                    // Continue lines that end in slashes if they are not comments
                    char firstChar = line.charAt(keyStart);
                    if ((firstChar != '#') && (firstChar != '!')) {
                        while (_continueLine(line)) {
                            String nextLine = _readLine(in);
                            if (nextLine == null) {
                                nextLine = "";
                            }
                            String loppedLine = line.substring(0, len - 1);
                            // Advance beyond whitespace on new line
                            int startIndex;
                            for (startIndex = 0; startIndex < nextLine.length(); startIndex++) {
                                if (WHITESPACE_CHARS.indexOf(nextLine.charAt(startIndex)) == -1) {
                                    break;
                                }
                            }
                            nextLine = nextLine.substring(startIndex, nextLine.length());
                            line = loppedLine + nextLine;
                            len = line.length();
                        }

                        // Find separation between key and value
                        int separatorIndex;
                        for (separatorIndex = keyStart; separatorIndex < len; separatorIndex++) {
                            char currentChar = line.charAt(separatorIndex);
                            if (currentChar == '\\') {
                                separatorIndex++;
                            } else if (KEY_VALUE_SEPARATORS.indexOf(currentChar) != -1) {
                                break;
                            }
                        }

                        // Skip over whitespace after key if any
                        int valueIndex;
                        for (valueIndex = separatorIndex; valueIndex < len; valueIndex++) {
                            if (WHITESPACE_CHARS.indexOf(line.charAt(valueIndex)) == -1) {
                                break;
                            }
                        }

                        // Skip over one non whitespace key value separators if any
                        if (valueIndex < len) {
                            if (STRICT_KEY_VALUE_SEPARTORS.indexOf(line.charAt(valueIndex)) != -1) {
                                valueIndex++;
                            }
                        }

                        // Skip over white space after other separators if any
                        while (valueIndex < len) {
                            if (WHITESPACE_CHARS.indexOf(line.charAt(valueIndex)) == -1) {
                                break;
                            }
                            valueIndex++;
                        }
                        String key = line.substring(keyStart, separatorIndex);
                        String value = (separatorIndex < len) ? line.substring(valueIndex, len) : "";

                        // Convert then store key and value
                        key = _convertString(key);
                        value = _convertString(value);
                        if (null == value)
                            value = key;
                        messages_.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * reads a single line from InputStreamReader
     *
     * @param in InputStreamReader used to read the line
     * @throws IOException if there is any problem with reading
     * @return the read line
     */
    public static String _readLine(InputStreamReader in) throws IOException {
        StringBuffer strBuf = new StringBuffer("");
        int i;
        while ((i = in.read()) != -1) {
            if ((char) i == '\r' || (char) i == '\n') {
                return strBuf.toString();
            }
            strBuf.append((char) i);
        }
        return strBuf.length() > 0 ? strBuf.toString() : null;
    }

    /**
     * determines whether the line of the supplied string continues on the next
     * line
     *
     * @param line a line of String
     * @return true if the string contines on the next line, false otherwise
     */
    protected static boolean _continueLine(String line) {
        int slashCount = 0;
        int index = line.length() - 1;
        while ((index >= 0) && (line.charAt(index--) == '\\')) {
            slashCount++;
        }
        return (slashCount % 2 == 1);
    }

    /**
     * Decodes a String which uses unicode characters in \\uXXXX format.
     *
     * @param theString String with \\uXXXX characters
     * @return resolved string
     */
    private static String _convertString(String theString) {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len);

        for (int x = 0; x < len;) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = theString.charAt(x++);
                        switch (aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                return null;
                        }
                    }
                    outBuffer.append((char) value);
                } else {
                    switch (aChar) {
                        case 't':
                            aChar = '\t'; break;
                        case 'r':
                            aChar = '\r'; break;
                        case 'n':
                            aChar = '\n'; break;
                        case 'f':
                            aChar = '\f'; break;
                    }
                    outBuffer.append(aChar);
                }
            } else {
                outBuffer.append(aChar);
            }
        }
        return outBuffer.toString();
    }
}
