/*
 * Copyright (C) 2017-2018 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>
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

import java.security.MessageDigest;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.NoSuchPaddingException;

import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Font;

import madrat.sys.ImageCache;
import madrat.i18n.I18N;
import madrat.sys.Sha3;
import madrat.sys.SecureRandom;

/**
 * Splash screen with progress bar (%) and message text Tests all necessary
 * crypto-features at 1st start
 */
final class SplashScreen extends javax.microedition.lcdui.Canvas
        implements Runnable {

    private static final int TEST_TIME = 1400;
    private static final int WAIT_TIME = 1100;

    private static final int ITERATIONS = 256;
    private static final int ITERATIONS_LIMIT = (Integer.MAX_VALUE >>> 8) - ITERATIONS;

    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;

    private static final int OFFSET = 5;

    private static final int PROGRESS_COLOR = 0xFF808080;
    private static final int PROGRESS_X_FACTOR = 10; // progress bar is less then width in
    // OFFSET*2+width/X_PROGRESS;

    // if there is small area for icon - use other offsets, not OFFSET
    private static final int SMALL_ICON = 64;
    private static final int SMALL_ICON_OFFSET = 2;

    private static final byte[] MD5_RESULT = {
        (byte) 0x37, (byte) 0xEF, (byte) 0xF0, (byte) 0x18,
        (byte) 0x66, (byte) 0xBA, (byte) 0x3F, (byte) 0x53,
        (byte) 0x84, (byte) 0x21, (byte) 0xB3, (byte) 0x0B,
        (byte) 0x7C, (byte) 0xBE, (byte) 0xFC, (byte) 0xAC};

    private static final byte[] SHA1_RESULT = {
        (byte) 0xE6, (byte) 0x43, (byte) 0x4B, (byte) 0xC4,
        (byte) 0x01, (byte) 0xF9, (byte) 0x86, (byte) 0x03,
        (byte) 0xD7, (byte) 0xED, (byte) 0xA5, (byte) 0x04,
        (byte) 0x79, (byte) 0x0C, (byte) 0x98, (byte) 0xC6,
        (byte) 0x73, (byte) 0x85, (byte) 0xD5, (byte) 0x35};

    private static final byte[] SHA3_RESULT = {
        (byte) 0x98, (byte) 0x9c, (byte) 0x19, (byte) 0x95,
        (byte) 0xda, (byte) 0x9d, (byte) 0x2d, (byte) 0x34,
        (byte) 0x1f, (byte) 0x99, (byte) 0x3c, (byte) 0x2e,
        (byte) 0x2c, (byte) 0xa6, (byte) 0x95, (byte) 0xf3,
        (byte) 0x47, (byte) 0x70, (byte) 0x75, (byte) 0x06,
        (byte) 0x1b, (byte) 0xfb, (byte) 0xd2, (byte) 0xcd,
        (byte) 0xf0, (byte) 0xbe, (byte) 0x75, (byte) 0xcf,
        (byte) 0x7b, (byte) 0xa9, (byte) 0x9f, (byte) 0xbe,
        (byte) 0x33, (byte) 0xd8, (byte) 0xd2, (byte) 0xc4,
        (byte) 0xdc, (byte) 0xc3, (byte) 0x1f, (byte) 0xa8,
        (byte) 0x99, (byte) 0x17, (byte) 0x78, (byte) 0x6b,
        (byte) 0x88, (byte) 0x3e, (byte) 0x6c, (byte) 0x9d,
        (byte) 0x5b, (byte) 0x02, (byte) 0xed, (byte) 0x81,
        (byte) 0xb7, (byte) 0x48, (byte) 0x3a, (byte) 0x4c,
        (byte) 0xb3, (byte) 0xea, (byte) 0x98, (byte) 0x67,
        (byte) 0x15, (byte) 0x88, (byte) 0xf7, (byte) 0x45};

    private static final byte[] AES_ECB = {
        (byte)0x68, (byte)0x96, (byte)0xe5, (byte)0xd3,
        (byte) 0x16, (byte) 0x0c, (byte) 0x2e, (byte) 0x53,
        (byte) 0xee, (byte) 0x69, (byte) 0x78, (byte) 0x1f,
        (byte) 0xc2, (byte) 0xfd, (byte) 0x91, (byte) 0x71};

    private static final byte[] AES_CBC = {
        (byte)0x8d, (byte)0x27, (byte)0xb4, (byte)0xf3,
        (byte)0x1d, (byte)0xdf, (byte)0x22, (byte)0xde,
        (byte)0x94, (byte)0xf7, (byte)0x4f, (byte)0x8a,
        (byte)0x48, (byte)0xff, (byte)0x1b, (byte)0x8e};

    private final Displayable back_;
    private final String appIcon_;
    private final String copyright_;
    private final String reserved_;
    private final Thread thread_;

    private String message_ = null;
    private int percent_ = 0;

    private int progress_y_ = 0, progress_h_ = -1;

    protected void keyPressed(int keyCode) {
        if (null != back_)
            Midlet.setCurrent(back_);
    }

    public SplashScreen(Displayable back) {
        back_ = back;
        setFullScreenMode(true);

        final Midlet midlet = Midlet.getInstance();
        appIcon_ = Midlet.getIconName(Midlet.ICON_APPLICATION);
        reserved_ = I18N.get("All rights reserved.");
        copyright_ = I18N.get("(C) 2018 {0}", I18N.get("AuthorsList"));

        if (null != back_) {
            thread_ = null;
            update(I18N.get("Version {0}", midlet.getAppProperty("MIDlet-Version")), 0);
        } else {
            thread_ = new Thread(this);
            update(I18N.get("Initialization"), 0);
            thread_.start();
        }
    }

    protected void paint(Graphics g) {
        int h = getHeight();

        // Backround
        g.setColor(WHITE);
        g.fillRect(0, 0, getWidth(), h);

        final int w = getWidth() - 2 * OFFSET, cx = w / 2 + OFFSET;

        // Copyright messages (footer)
        {
            g.setColor(BLACK);
            g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL));
            final int fH = g.getFont().getHeight();
            g.drawString(reserved_, cx, h, Graphics.HCENTER | Graphics.BOTTOM);
            h -= fH;
            g.drawString(copyright_, cx, h, Graphics.HCENTER | Graphics.BOTTOM);
            h -= fH;
        }

        // Progress bar and text
        {
            g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
            final int fH = g.getFont().getHeight();

            final int po = w / PROGRESS_X_FACTOR;
            final int pw = w - (po << 1);

            progress_y_ = h - OFFSET - 2 * fH;
            progress_h_ = fH;

            if (null != thread_) {
                g.setColor(PROGRESS_COLOR);
                g.fillRect(OFFSET + po, progress_y_, percent_ * pw / 100, fH);
                g.setColor(BLACK);
                g.drawRect(OFFSET + po, progress_y_, pw, fH);
            }

            progress_y_ -= OFFSET + fH;
            progress_h_ += OFFSET + fH;

            if (null != message_) {
                g.drawString(message_, cx, progress_y_, Graphics.TOP | Graphics.HCENTER);
            }

            h = progress_y_;
        }

        // app's Icon
        {
            int geometry = Math.min(w, h) - 2 * SMALL_ICON_OFFSET;
            if (geometry > SMALL_ICON) {
                geometry = (geometry + 2 * SMALL_ICON_OFFSET) * 2 / 3 - 2 * OFFSET;
            }

            if (geometry > 0) {
                Image img = ImageCache.getImage(appIcon_, geometry, geometry, ImageCache.NEAREST_MODE);
                g.drawImage(img, cx, h / 2, Graphics.HCENTER | Graphics.VCENTER);
            }
        }
    }

    /**
     * Update progress message and bar at once
     *
     * @param message - text message
     * @param percent - progress bar percent
     * @note percent will be truncated into [0, 100]
     */
    public void update(String message, int percent) {
        percent_ = Math.max(Math.min(percent, 100), 0);
        message_ = message;
        if (progress_h_ > 0) {
            repaint(0, progress_y_, getWidth(), progress_h_);
        } else {
            repaint();
        }
    }

    private static int long2int(long v) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(v, Integer.MIN_VALUE));
    }

    public void run() {
        try {
            byte[] data = new byte[128];    // 1Kbit
            byte[] crypt = Midlet.alloc(data.length + 128);
            byte[] crypt2 = Midlet.alloc(crypt.length);

            int hardMd5  = 0;
            int hardSha1 = 0;
            int softSha3 = 0;

            int hardAesEcb = 0;
            int hardAesCbc = 0;
            int hardRandom = 0;

            Thread.sleep(WAIT_TIME);
            update(I18N.get("only at 1st start"), 7);
            Thread.sleep(WAIT_TIME);

            // Hardware MD5
            {
                final String feature = I18N.get("MD5");
                update(feature, 14);

                MessageDigest hash = MessageDigest.getInstance("MD5");
                for (int i = 0; i < data.length; ++i) {
                    data[i] = (byte) i;
                }
                hash.reset();
                hash.update(data, 0, data.length);
                int lenMd5 = hash.digest(data, 0, data.length);

                if (!Midlet.equals(data, 0, lenMd5, MD5_RESULT, 0, MD5_RESULT.length))
                    throw new java.lang.SecurityException(feature);

                long count = 0;
                long now = 0;

                final long timeout = System.currentTimeMillis() + TEST_TIME;

                do {
                    for (int i = ITERATIONS; i > 0; i -= 2) {
                        hash.reset();
                        hash.update(data, 0, data.length);
                        hash.digest(data, 0, data.length);
                        hash.reset();
                        hash.update(data, 0, data.length);
                        hash.digest(data, 0, data.length);
                    }
                    count += ITERATIONS;
                    now = System.currentTimeMillis();
                } while ((timeout > now) && (count < ITERATIONS_LIMIT));

                hardMd5 = long2int(count * 1000 / (now - timeout + TEST_TIME + 1));

                final long delay = timeout - now;
                if (delay > 0) {
                    Thread.sleep(delay);
                }
            }

            // SHA-1
            {
                final String feature = I18N.get("SHA-1");
                update(feature, 28);

                MessageDigest hash = MessageDigest.getInstance("SHA-1");
                for (int i = 0; i < data.length; ++i) {
                    data[i] = (byte) i;
                }
                hash.reset();
                hash.update(data, 0, data.length);
                int lenMd5 = hash.digest(data, 0, data.length);

                if (!Midlet.equals(data, 0, lenMd5, SHA1_RESULT, 0, SHA1_RESULT.length))
                    throw new java.lang.SecurityException(feature);

                long count = 0;
                long now = 0;

                final long timeout = System.currentTimeMillis() + TEST_TIME;

                do {
                    for (int i = ITERATIONS; i > 0; i -= 2) {
                        hash.reset();
                        hash.update(data, 0, data.length);
                        hash.digest(data, 0, data.length);
                        hash.reset();
                        hash.update(data, 0, data.length);
                        hash.digest(data, 0, data.length);
                    }
                    count += ITERATIONS;
                    now = System.currentTimeMillis();
                } while ((timeout > now) && (count < ITERATIONS_LIMIT));

                hardSha1 = long2int(count * 1000 / (now - timeout + TEST_TIME + 1));

                final long delay = timeout - now;
                if (delay > 0) {
                    Thread.sleep(delay);
                }
            }

            // SHA-3
            {
                final String feature = I18N.get("SHA-3");
                update(feature, 42);

                Sha3 sha3 = new Sha3(512);
                for (int i = 0; i < data.length; ++i) {
                    data[i] = (byte) i;
                }

                sha3.reset();
                sha3.update(data, 0, data.length);
                int lenSha3 = sha3.digest(data, 0, data.length);

                if (!Midlet.equals(data, 0, lenSha3, SHA3_RESULT, 0, SHA3_RESULT.length))
                    throw new java.lang.SecurityException(feature);

                long count = 0;
                long now = 0;

                final long timeout = System.currentTimeMillis() + TEST_TIME;

                sha3.reset();
                do {
                    for (int i = ITERATIONS; i > 0; i -= 2) {
                        sha3.update(data, 0, data.length);
                        sha3.finalize(data, 0);
                        sha3.update(data, 0, data.length);
                        sha3.finalize(data, 0);
                    }
                    count += ITERATIONS;
                    now = System.currentTimeMillis();
                } while ((timeout > now) && (count < ITERATIONS_LIMIT));

                softSha3 = long2int(count * 1000 / (now - timeout + TEST_TIME + 1));

                final long delay = timeout - now;
                if (delay > 0) {
                    Thread.sleep(delay);
                }
            }

            // AES Encryption
            update(I18N.get("AES"), 56);
            for (int i = 0; i < data.length; ++i) {
                data[i] = (byte) (i ^ 0x55);
            }
            SecretKeySpec key = new SecretKeySpec(data, 0, 32, "AES");  // AES-256
            IvParameterSpec param = new IvParameterSpec(data, 0, 16);   // IV=16

            // AES/ECB/NoPadding
            {
                update(I18N.get("AES-ECB"), 60);
                Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, key);
                final int lenAesEcb = cipher.doFinal(MD5_RESULT, 0, MD5_RESULT.length, crypt, 0);

                if (!Midlet.equals(crypt, 0, lenAesEcb, AES_ECB, 0, AES_ECB.length))
                    throw new java.lang.SecurityException(I18N.get("AES/ECB encryption"));

                cipher.init(Cipher.DECRYPT_MODE, key);
                final int lenResult = cipher.doFinal(crypt, 0, lenAesEcb, data, 0);

                if (!Midlet.equals(data, 0, lenResult, MD5_RESULT, 0, MD5_RESULT.length))
                    throw new java.lang.SecurityException(I18N.get("AES/ECB decryption"));

                long count = 0;
                long now = 0;
                int len = lenAesEcb;

                cipher.init(Cipher.ENCRYPT_MODE, key);
                final long timeout = System.currentTimeMillis() + TEST_TIME;
                do {
                    for (int i = ITERATIONS; i > 0; i -= 2) {
                        len = cipher.doFinal(crypt, 0, len, crypt2, 0);
                        count += len;
                        len = cipher.doFinal(crypt2, 0, len, crypt, 0);
                        count += len;
                    }
                    now = System.currentTimeMillis();
                } while ((timeout > now) && (count < ITERATIONS_LIMIT));

                hardAesEcb = long2int(count * 1000 / (now - timeout + TEST_TIME + 1));

                final long delay = timeout - now;
                if (delay > 0) {
                    Thread.sleep(delay);
                }
            }

            //AES/CBC/NoPadding
            {
                update(I18N.get("AES-CBC"), 73);
                Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");

                cipher.init(Cipher.ENCRYPT_MODE, key, param);
                final int lenAesCbc = cipher.doFinal(MD5_RESULT, 0, MD5_RESULT.length, crypt, 0);
                if (!Midlet.equals(crypt, 0, lenAesCbc, AES_CBC, 0, AES_CBC.length))
                    throw new java.lang.SecurityException(I18N.get("AES/CBC encryption"));

                cipher.init(Cipher.DECRYPT_MODE, key, param);
                final int lenResult = cipher.doFinal(crypt, 0, lenAesCbc, data, 0);
                if (!Midlet.equals(data, 0, lenResult, MD5_RESULT, 0, MD5_RESULT.length))
                    throw new java.lang.SecurityException(I18N.get("AES/CBC decryption"));

                long count = 0;
                long now = 0;
                int len = lenAesCbc;

                cipher.init(Cipher.ENCRYPT_MODE, key);
                final long timeout = System.currentTimeMillis() + TEST_TIME;
                do {
                    for (int i = ITERATIONS; i > 0; i -= 2) {
                        len = cipher.doFinal(crypt, 0, len, crypt2, 0);
                        count += len;
                        len = cipher.doFinal(crypt2, 0, len, crypt, 0);
                        count += len;
                    }
                    now = System.currentTimeMillis();
                } while ((timeout > now) && (count < ITERATIONS_LIMIT));

                hardAesCbc = long2int(count * 1000 / (now - timeout + TEST_TIME + 1));

                final long delay = timeout - now;
                if (delay > 0) {
                    Thread.sleep(delay);
                }
            }

            //SecureRandom
            {
                final String feature = I18N.get("Secure Random");
                update(feature, 85);

                SecureRandom rng = SecureRandom.getInstance();
                int value = 0;
                int count = 0;

                final long timeout = System.currentTimeMillis() + TEST_TIME;
                long now = 0;
                do {
                    for (int i = ITERATIONS; i > 0; i -= 2) {
                        value += (rng.getByte() & 0xFF);
                        value += (rng.getByte() & 0xFF);
                    }
                    count += ITERATIONS;
                    now = System.currentTimeMillis();
                } while ((timeout > now) && (count < ITERATIONS_LIMIT));

                hardRandom = long2int(count * 1000 / (now - timeout + TEST_TIME + 1));

                final int avg = value / count;
                if (avg < 63 || avg > 192)
                    throw new java.lang.ArithmeticException(feature);

                final long delay = timeout - now;
                if (delay > 0) {
                    Thread.sleep(delay);
                }
            }

            update(I18N.get("Completed"), 100);
            Thread.sleep(WAIT_TIME);
            Midlet.getInstance().initConfig(hardMd5, hardSha1, softSha3, hardAesEcb, hardAesCbc, hardRandom);
            new ThemeScreen(null).show();
        } catch (ArithmeticException ex) {
            fatal("'{0}' has failed the mathematical check", ex);
        } catch (java.lang.SecurityException ex) {
            fatal("Algorithm '{0}' has been not implemented", ex);
        } catch (NoSuchAlgorithmException ex) {
            fatal("Algorithm '{0}' has been not supported", ex);
        } catch (InvalidKeyException ex) {
            fatal("Key '{0}' has been not supported", ex);
        } catch (InvalidAlgorithmParameterException ex) {
            fatal("Param for {0} has been not supported", ex);
        } catch (NoSuchPaddingException ex) {
            Exception err = new NoSuchPaddingException(
                        I18N.get("Padding '{0}' for {1} has been not supported",
                                new String[]{ex.getMessage(), message_}));
            Midlet.fatal(err, "SplashScreen/"+message_);
        } catch (Exception ex) {
            Midlet.fatal(ex, "SplashScreen/"+message_);
        }
    }
    private void fatal(String format, Exception e) {
        String arg = e.getMessage();
        arg = (null != arg) ? arg : message_;
        arg = I18N.get(format, arg);
        Midlet.fatal(new IllegalStateException(arg), "SplashScreen/"+message_);
    }
}
