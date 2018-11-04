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
import java.security.GeneralSecurityException;
import java.util.Random;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Ticker;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

import madrat.gui.Theme;
import madrat.i18n.I18N;
import madrat.sys.PasswordQuality;
import madrat.sys.ImageCache;
import madrat.storage.Field;
import madrat.storage.Record;
import madrat.storage.Storage;
import madrat.storage.StorageException;

/**
 * The Lockore application main class
 */
public final class Midlet extends javax.microedition.midlet.MIDlet implements CommandListener {
    private static final String DB_STORE_PREFIX =   "store";
    private static final String DB_SETTINGS =       ".conf";

    private static final String TOPPASSWD_FILE = "/bad_pass.txt";

    static final String F_HARDWARE_MD5       = "MD5-Hard";
    static final String F_HARDWARE_SHA1      = "SHA1-Hard";
    static final String F_SOFTWARE_SHA3      = "SHA3-Soft";

    static final String F_HARDWARE_AESECB      = "AES-ECB";
    static final String F_HARDWARE_AESCBC      = "AES-CBC";
    static final String F_HARDWARE_RANDOM      = "Random";

    private static final String F_THEME_NAME         = "Theme-Name";
    private static final String F_TIMEOFFSET_NAME    = "Time-Offset";
    private static final String F_THEME_COLORS       = "Theme-Colors";
    static final String JAD_THEME_COLORS     = F_THEME_COLORS;

    static final String F_PBKDF2_HASH = "PBKDF2-Hash";
    static final String F_PBKDF2_COUNT = "PBKDF2-Count";
    static final String F_PBKDF2_SALT   = "PBKDF2-Salt";

    static final Command CANCEL = new Command(I18N.get("Cancel"), Command.CANCEL, 1);
    static final Command BACK = new Command(I18N.get("Back"), Command.BACK, 1);
    static final Command OK = new Command(I18N.get("OK"), Command.OK, 1);
    static final Command EXIT = new Command(I18N.get("Exit"), Command.EXIT, 1);

    // Features
    private static final boolean RELEASE        = true;

    private static int      supportFileApi_     = -1;
    private static int      maxString_          = -1;

    private static String   localDirUri_        = null;
    private static String   localDirName_       = null;

    // Midlet
    private static Midlet    self_      = null; // is in destroyed state or not
    private Display          display_   = null; // is active state or not

    private Theme theme_ = null;
    private Storage settings_   = null;
    private PasswordQuality passQuality_ = null;
    private Storage store_      = null; // sdi

    synchronized static Midlet getInstance() {
        if (null == self_)
            throw new NullPointerException(I18N.get("Not started"));
        return self_;
    }

    public Midlet() {
        self_ = this;
    }

    static PasswordQuality getPasswordQuality() {
        if (null == self_)
            return null;

        if (null != self_.passQuality_)
            return self_.passQuality_;

        try {
            InputStream is = Runtime.getRuntime().getClass().getResourceAsStream(TOPPASSWD_FILE);
            if (null == is)
                throw new IOException(I18N.get("Password's dictionary {0} is not available", TOPPASSWD_FILE));
            self_.passQuality_ = new PasswordQuality(new InputStreamReader(is));
        }
        catch (IOException ex) {
            self_.passQuality_ = new PasswordQuality();
        }
        return self_.passQuality_;
    }

    protected synchronized void startApp() throws MIDletStateChangeException {

        // check destroyed
        if (null == self_)
            throw new MIDletStateChangeException(I18N.get("Destroyed"));

        // Check already active state
        if (null != display_)
            throw new MIDletStateChangeException(I18N.get("Already started"));
        display_ = Display.getDisplay(self_);

        // Initialization
        try {
            if (null == settings_)
                loadConfig();
        }
        catch(Exception e) {
            clearConfig();
            Midlet.setCurrent(new SplashScreen(null));
            return;
        }

        // Show if all ok
        showStoreListScreen();
    }

    protected synchronized void destroyApp(boolean unconditional) {
        // Destroyed state
        setStore(null);
        display_ = null;
        theme_ = null;

        if (null != settings_)
            settings_.destroy();
        settings_ = null;
        self_ = null;

        notifyDestroyed();
    }

    protected synchronized void pauseApp() {
        // paused state
        display_ = null;
        setStore(null);
        notifyPaused();
    }

    // Configuration API
    void clearConfig() {
        if (null != settings_) {
            settings_.wipe();
            settings_ = null;
        }
    }

    int getHashPerf(String hash) {
        return (int)(settings_.getValue(hash, 0));
    }

    String getThemeName() {
        return settings_.getValue(F_THEME_NAME, null);
    }
    void setThemeName(String name) {
        settings_.setValue(F_THEME_NAME, name);
    }

    String getThemeColors() {
        return settings_.getValue(F_THEME_COLORS, null);
    }

    void setThemeColors(String colors) {
        settings_.setValue(F_THEME_COLORS, colors);
    }

    int getTimeOffset() {
        return (int)(settings_.getValue(F_TIMEOFFSET_NAME, 0));
    }

    void setTimeOffset(int v) {
        settings_.setValue(F_TIMEOFFSET_NAME, v);
    }

    void initConfig( int hardMd5, int hardSha1, int softSha3,
                            int hardAesEcb, int hardAesCbc, int hardRandom) throws RecordStoreException, IOException, GeneralSecurityException {
        Storage.wipeStore(DB_SETTINGS);
        Storage.createStore(DB_SETTINGS, DB_SETTINGS, null, Record.DEFAULT_ICON);
        loadConfig();
        settings_.setValue(F_HARDWARE_MD5,  hardMd5);
        settings_.setValue(F_HARDWARE_SHA1,  hardSha1);
        settings_.setValue(F_SOFTWARE_SHA3, softSha3);

        settings_.setValue(F_HARDWARE_AESECB,  hardAesEcb);
        settings_.setValue(F_HARDWARE_AESCBC,  hardAesCbc);
        settings_.setValue(F_HARDWARE_RANDOM, hardRandom);

        setTimeOffset(0);
        saveConfig();
    }

    private void loadConfig() throws RecordStoreException, IOException, StorageException, GeneralSecurityException {
        if (null != settings_)
            settings_.destroy();
        settings_ = new Storage(DB_SETTINGS);
    }

    void saveConfig() throws IOException, RecordStoreException, GeneralSecurityException {
        if (null != settings_)
            settings_.save();
    }

    static final int getMaxString() {
        if (-1 == maxString_) {
            TextField f;
            try {
                f = new TextField(null, null, 0x20000, TextField.ANY);  // 128K is enouth
            }
            catch (Exception e) {
                f = new TextField(null, null, 0xFF, TextField.ANY);
            }
            maxString_ = f.getMaxSize();
        }
        return maxString_;
    }

    static final String getLocalDirName() {
        if (null == localDirName_)
            getLocalDirUri();
        return localDirName_;
    }

    static final String getLocalDirUri() {
        if (null == localDirUri_) {
            checkLocalDir("fileconn.dir.received");
            checkLocalDir("fileconn.dir.graphics");
            checkLocalDir("fileconn.dir.recordings");
        }
        return localDirUri_;
    }

    static final String getPathName(String path)
    {
        if (null == path)
            return null;

        int tail = path.length()-1;
        if (tail < 1)
            return path;

        for(; 0 < tail; --tail) {
            final char c = path.charAt(tail);
            if ('/' != c && '\\' != c) {
                break;
            }
        }

        int head = Math.max(path.lastIndexOf('/', tail), path.lastIndexOf('\\', tail));
        return path.substring(head+1, tail+1);
    }

    private static void checkLocalDir(String prop) {
        if (null != localDirUri_ || !isSupportFileApi())
            return;

        localDirUri_ = System.getProperty(prop);
        if (null == localDirUri_)
            return;

        localDirName_ = getPathName(System.getProperty(prop + ".name"));

        if (null == localDirName_)
            localDirName_ = getPathName(localDirUri_);
    }

    static final boolean isSupportFileApi() {
        if (-1 == supportFileApi_)
            supportFileApi_ = (null != System.getProperty("microedition.io.file.FileConnection.version"))?1:0;
        return (0 != supportFileApi_);
    }

    static boolean isStore(String name) {
        return (null != name && name.startsWith(DB_STORE_PREFIX));
    }

    static String generateStoreName() {
        Random r = new Random(System.currentTimeMillis());

        for (int i =0; i < 1024; ++i) {
            final String storeName = DB_STORE_PREFIX + Integer.toHexString((r.nextInt() & 0xFFFF));
            try {
                Storage s = new Storage(storeName);
            } catch (RecordStoreNotFoundException ex) {
                return storeName;
            } catch (Exception ex) {
            }
        }
        throw new RuntimeException(I18N.get("No free stores"));
    }

    static Theme getTheme(String theme) {
        return new Theme(getInstance().display_, theme);
    }

    boolean setTheme(String theme) {
        try {
            theme_ = getTheme(theme);
            return true;
        } catch (Exception e) {
            theme_ = new Theme(null);
            return false;
        }
    }

    final Theme getTheme() {
        if (null == theme_) {
            String theme = null;
            try {
                if (null != settings_)
                    theme = settings_.getValue(F_THEME_COLORS, null);
            }
            catch (Exception e) {
            }
            setTheme(theme);
        }
        return theme_;
    }

    static synchronized void fatal(Exception e, String location) {
        e.printStackTrace();

        if (null == self_)
            return;

        setStore(null);

        if (null == self_.display_)
            return;

        final Display d = self_.display_;
        self_.display_ = null;

        final Form f = new Form(I18N.get("Fatal Error"));
        final Font font = Font.getFont(Font.FONT_STATIC_TEXT);
        final int face = font.getFace();
        final Font headerFont = Font.getFont(face, Font.STYLE_BOLD, Font.SIZE_LARGE);
        final Font details = Font.getFont(face, Font.STYLE_PLAIN, Font.SIZE_SMALL);

        final int size = headerFont.getHeight();

        f.append(new ImageItem(null, getIcon(ICON_ALERT, size), Item.LAYOUT_CENTER, "[!]"));

        String message = e.getMessage();
        if (null == message || 0 == message.length()) {
            message = e.getClass().getName();
            message = message.substring(message.lastIndexOf('.')+1);
        }
        final StringItem header = new StringItem(null, message);
        header.setFont(headerFont);
        header.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER);
        f.append(header);

        final StringItem type = new StringItem(I18N.get("Class"), e.getClass().getName() + " @ " + location);
        type.setFont(details);
        type.setLayout(Item.LAYOUT_LEFT);
        f.append(type);

        final StringItem desc = new StringItem(I18N.get("Details"), e.toString());
        desc.setFont(details);
        desc.setLayout(Item.LAYOUT_LEFT);
        f.append(desc);

        f.addCommand(Midlet.EXIT);
        f.setCommandListener(self_);

        d.setCurrent(f);
    }

    static void showAlert(Exception e, Displayable next) {
        e.printStackTrace();

        String message = e.getMessage();
        String ticker = e.toString();

        if (null != message && RELEASE)
            ticker = null;
        showAlert(AlertType.ERROR, message, ticker, next);
    }

    static void showAlert(AlertType type, String message, String ticker, Displayable next) {
        String title = "Error";
        if (type.equals(AlertType.INFO))
            title = "Information";
        else if (type.equals(AlertType.ALARM))
            title = "Alarm";
        else if (type.equals(AlertType.CONFIRMATION))
            title = "Confirmation";
        else if (type.equals(AlertType.WARNING))
            title = "Warning";
        title = I18N.get(title);

        if (null == message || 0 == message.length()) {
            message = ticker;
            ticker = null;
        }

        if (null == message || 0 == message.length())
            message = title;

        if (null != ticker && 0 != ticker.length() && message.equals(ticker))
            ticker = null;

        Alert alert = new Alert(title, message, null, type);
        alert.setTimeout(Alert.FOREVER);
        if (null != ticker)
            alert.setTicker(new Ticker(ticker));
        showAlert(alert, next);
    }

    static void showAlert(Alert alert, Displayable next) {
        if (null != self_ && null != self_.display_)
            self_.display_.setCurrent(alert, next);
    }

    static void setCurrentItem(Item i) {
        if (null != self_ && null != self_.display_)
            self_.display_.setCurrentItem(i);
    }

    static Storage getStore() throws NullPointerException {
        final Storage s = getInstance().store_;
        if (null == s)
            throw new NullPointerException(I18N.get("Storage is not available"));
        return s;
    }

    static void setStore(Storage store) {
        try {
            final Midlet self = getInstance();

            if (null != self.store_)
                self.store_.destroy();

            self.store_ = store;
        }
        catch(Exception e) {
            if (null != store)
                store.destroy();
        }
    }

    static void showStoreListScreen() throws RuntimeException {
        if (null != self_ && null != self_.display_)
            new StoresListScreen().show();
    }

    static void setCurrent(Displayable n) {
        if (null == self_)
            return;
        if (null != self_.display_)
            self_.display_.setCurrent(n);
    }
    //* ICONS:
    //* 0(ICON_NULL)                                - NULL/NONE
    //* 1..ICON_INTERNAL_INDEX-1                    - User's icons
    //* ICON_INTERNAL_INDEX..ICON_EXTRA_INDEX-1     - Extra (optional) icons
    //* ICON_EXTRA_INDEX..ICON_LIMIT_INDEX-1        - GUI (required) that are available for user as icons
    //* ICON_LIMIT_INDEX..                          - GUI (required) icons, can not be used for store/record/field icon

    //* Icon resources
    static final int ICON_ALERT              = 259;
    static final int ICON_LOCKED_STORE       = 258;
    static final int ICON_ADD_NEW            = 257;
    static final int ICON_APPLICATION        = 256;

    // Can be used as icons
    static final int ICON_LIMIT_INDEX        = 256;

    // Mandatory icons - needed by application
    // Types and common graphic
    static final int ICON_OTP                = 255;
    static final int ICON_EMAIL              = 254;
    static final int ICON_URI                = 253;
    static final int ICON_PHONE              = 252;
    static final int ICON_BINARY             = 251;
    static final int ICON_DATE               = 250;
    static final int ICON_PASSWORD           = 249;
    static final int ICON_NUMBER             = 248;
    static final int ICON_TEXT               = 247;
    static final int ICON_STORE              = 246;
    static final int ICON_SETTINGS           = 245;
    static final int ICON_ART                = 244;
    static final int ICON_KEY                = 243;
    static final int ICON_CERTIFICATE        = 242;
    static final int ICON_SIGNATURE          = 241;
    static final int ICON_CHIP               = 240;
    static final int ICON_RANDOM             = 239;
    static final int ICON_ID                 = 238;

    static final int ICON_EXTRA_INDEX        = 230;
    static final int ICON_INTERNAL_INDEX     = 128;

    private static final String[] ICON_EXTRA_NAMES = {
        "admin",    "airplane", "anonymous","basket",   "bluedoc",
        "bulb",     "bus",
        "car",      "card",     "cloud",    "coin",
        "devel",
        "floppy",   "forum",
        "garage",   "gift",     "gnu",
        "home",     "hsm",
        "laptop",   "lightning",
        "medkit",   "messanger","music",
        "office",
        "palm",     "paw",      "printer",
        "rain",     "reddoc",
        "satelite", "server",   "shield",   "snow",     "sun",      "sword",
        "thermometer","ticket", "transmitter", "tux",   "tv",
        "user"
    };

    static final int ICON_NULL               = Record.DEFAULT_ICON;

    // Image resources
    static final int getTypeIcon(int type, int format, boolean isProtected) {

        switch (type) {
            case Field.BINARY:
                return (format == TOTP_FORMAT)?ICON_OTP:ICON_BINARY;
            case Field.STRING:
                switch (format) {
                    case URI_FORMAT:
                        return ICON_URI;
                    case PHONE_FORMAT:
                        return ICON_PHONE;
                    case EMAIL_FORMAT:
                        return ICON_EMAIL;
                    case OTP_FORMAT:
                        return ICON_OTP;
                }
                return isProtected?ICON_PASSWORD:ICON_TEXT;
            case Field.INTEGER:
                if (format == DATE_FORMAT)
                    return ICON_DATE;
            case Field.DECIMAL:
                return isProtected?ICON_PASSWORD:ICON_NUMBER;
            default:
                return ICON_ALERT;
        }
    }

    static final int DEFAULT_FORMAT = 0; // All

    static final int OTP_FORMAT      = 1;   // String
    static final int URI_FORMAT      = 2;   // String
    static final int PHONE_FORMAT    = 3;   // String
    static final int EMAIL_FORMAT    = 4;   // String
    static final int DATE_FORMAT     = 1;   // Integer
    static final int TOTP_FORMAT     = 1;   // Binary

    static final String getIconName(int icon) {
        switch (icon) {
            case ICON_NULL:             return null;

            // Types's icons
            case ICON_OTP:              return "otp";
            case ICON_EMAIL:            return "email";
            case ICON_URI:              return "uri";
            case ICON_PHONE:            return "phone";
            case ICON_BINARY:           return "binary";
            case ICON_DATE:             return "date";
            case ICON_PASSWORD:         return "password";
            case ICON_NUMBER:           return "number";
            case ICON_TEXT:             return "text";

            // System/special icons
            case ICON_ALERT:            return "alert";
            case ICON_LOCKED_STORE:     return "locked";
            case ICON_ADD_NEW:          return "add";
            case ICON_APPLICATION:      return "app";
            case ICON_STORE:            return "store";
            case ICON_ART:              return "art";
            case ICON_SETTINGS:         return "settings";
            case ICON_KEY:              return "key";
            case ICON_CERTIFICATE:      return "certificate";
            case ICON_SIGNATURE:        return "signature";
            case ICON_CHIP:             return "chip";
            case ICON_RANDOM:           return "random";
            case ICON_ID:               return "identity";

            default:
                final int iconId = icon&0xFF;
                if (iconId <= ICON_EXTRA_INDEX && iconId > ICON_EXTRA_INDEX - ICON_EXTRA_NAMES.length)
                    return ICON_EXTRA_NAMES[ICON_EXTRA_INDEX-iconId];
                return I18N.format("icon_{0}", Integer.toString(iconId));
        }
    }

    static Image getIcon(int icon, int size) {
        if (null == self_)
            return null;

        if (ICON_NULL == icon)
            return null;

        if (size <= 0) {
            final Theme theme = self_.getTheme();
            if (null == theme)
                return null;
            size = theme.getListElement();
        }
        return ImageCache.getImage(getIconName(icon), size, size, ImageCache.BILINEAR_MODE);
    }

    public void commandAction(Command c, Displayable d) {
        setStore(null);
        destroyApp(null == self_);
    }

    static final byte[] alloc(int length) {
        try {
            return new byte[length];
        } catch (OutOfMemoryError e) {
            System.gc();
            return new byte[length];
        }
    }

    static final boolean equals(byte[] b1, int o1, int l1, byte[] b2, int o2, int l2) {
        if (l1 != l2)
            return false;
        for (int i = 0; i < l1; ++i) {
            if (b1[o1++] != b2[o2++])
                return false;
        }
        return true;
    }
}
