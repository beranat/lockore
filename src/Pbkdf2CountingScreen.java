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

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.StringItem;

import madrat.i18n.I18N;
import madrat.sys.SecureRandom;

/**
 * Calculate PBKDF2 for SHA1 with progress bar
 * because weak J2ME phones may calculate it very long time
 */
final class Pbkdf2CountingScreen extends Form implements Runnable, CommandListener {
    public static final String HASH_NAME = "SHA-1";
    public static final int    ITERATION_COUNT = 1280;  // weak systems about 1K it/s => 4 seconds

    protected static final int MIN_UPDATE_PERFOMANCE  =  200;  // min perfomance for update

    protected static final int H_LEN_BYTES  = 20;                   // 160 bit
    protected static final int PBKDF2_L = 2;                        // x2
    protected static final int DK_LEN_BYTES = PBKDF2_L*H_LEN_BYTES; // 320 bit
    protected static final int HMAC_LEN_BYTES = 64;                 // HMAC-SHA1

    protected static final int MIN_CALCULATION = 1000;

    protected final TaskNotification listener_;
    protected final int count_;
    protected MessageDigest hash_;

    protected final Gauge progressBar_;
    protected final StringItem  estimateLabel_;

    protected Thread thread_;
    protected boolean break_ = false;

    protected byte[] password_;
    protected String salt_;

    protected long start_;

    public static int getPerfomance() {
        final Midlet m = Midlet.getInstance();
        if (null == m)
            return 0;
        // SHA-1's Perf - 1/4
        return m.getHashPerf(Midlet.F_HARDWARE_SHA1) * 3 / (4 * 2 * PBKDF2_L);
    }

    public Pbkdf2CountingScreen(TaskNotification listener,
            byte[] password, String salt,
            int count) throws NoSuchAlgorithmException, DigestException {
        super(I18N.get("Counting"));

        listener_ = listener;
        count_ = count;
        salt_ = salt;
        hash_ = MessageDigest.getInstance(HASH_NAME);

        estimateLabel_ = new StringItem("", "");
        progressBar_ = new Gauge(null, false, 100, 1);

        append(new StringItem(I18N.get("Please wait"), "\n"));
        append(estimateLabel_);
        append(progressBar_);

        updateProgress(0, -1);
        addCommand(Midlet.CANCEL);

        setCommandListener(this);

        password_ = password;

        thread_ = new Thread(this);
        thread_.setPriority(Thread.MAX_PRIORITY - 3);
        thread_.start();
    }

    protected void updateProgress(int count, long start) {
        if (0 == count_)
            return;

        final int progress = (int)(count * 100L / count_);
        if (progress != progressBar_.getValue())
            progressBar_.setValue(progress);

        final int perf = getPerfomance();
        if (perf <= 0)
            return;
        final long remained = count_ - count;
        int remTime = (int) remained / perf;
        if (0 <= start) {
            final long usedTime = (System.currentTimeMillis() - start);
            remTime =  (int)((usedTime * remained / count_) +
                             (remained  * remTime*1000 / count_) + 999) / 1000;
        }

        final String estimateTime = I18N.formatPeriod(remTime);
        if (!estimateTime.equals(estimateLabel_.getText()))
            estimateLabel_.setText(estimateTime);
    }

    protected void r0(int i,
            byte[] key36, byte[] key5C,
            byte[] salt,
            byte[] ui,
            byte[] digest, int ofs, int len) throws DigestException {
        ui[0] = (byte)((i >> 24) & 0xFF);
        ui[1] = (byte)((i >> 16) & 0xFF);
        ui[2] = (byte)((i >>  8) & 0xFF);
        ui[3] = (byte)((i      ) & 0xFF);

        // HMAC (key, salt || ui) -> ui;
        {
            // R1: internal
            hash_.update(key36, 0, HMAC_LEN_BYTES);
            hash_.update(salt, 0, salt.length);
            hash_.update(ui, 0, 4);
            hash_.digest(ui, 0, ui.length);
            hash_.reset();

            // R2: external
            hash_.update(key5C, 0, HMAC_LEN_BYTES);
            hash_.update(ui, 0, H_LEN_BYTES);
            hash_.digest(ui, 0, ui.length);
            hash_.reset();
        }

        System.arraycopy(ui, 0, digest, ofs, len);
    }

    protected void rk(
            byte[] key36, byte[] key5C,
            byte[] ui,
            byte[] digest, int ofs, int len) throws DigestException {
        // HMAC (key, ui) -> ui;
        {
            // R1: internal
            hash_.update(key36, 0, HMAC_LEN_BYTES);
            hash_.update(ui, 0, ui.length);
            hash_.digest(ui, 0, ui.length);
            hash_.reset();

            // R2: external
            hash_.update(key5C, 0, HMAC_LEN_BYTES);
            hash_.update(ui, 0, H_LEN_BYTES);
            hash_.digest(ui, 0, ui.length);
            hash_.reset();
        }

        for (int k = 0; k < len; ++k)
            digest[k+ofs] ^= ui[k];
    }

    public void run() {

        final byte[] digest = Midlet.alloc(DK_LEN_BYTES);
        final byte[] key36 = Midlet.alloc(HMAC_LEN_BYTES);
        final byte[] key5C = Midlet.alloc(HMAC_LEN_BYTES);

        final byte[] u0     = Midlet.alloc(H_LEN_BYTES);
        final byte[] u1     = Midlet.alloc(H_LEN_BYTES);

        try {
            hash_.reset();
            final byte[] salt   = salt_.getBytes("UTF-8");

            if (password_.length <= HMAC_LEN_BYTES)
                System.arraycopy(password_, 0, key36, 0, password_.length);
            else {
                hash_.update(password_, 0, password_.length);
                hash_.digest(key36, 0, key36.length);
                hash_.reset();
            }

            for (int i = 0; i < HMAC_LEN_BYTES; ++i) {
                final byte b = key36[i];
                key36[i] = (byte)(b ^ 0x36);
                key5C[i] = (byte)(b ^ 0x5C);
            }

            long start = System.currentTimeMillis();
            r0(1, key36, key5C, salt, u0, digest, 0,              H_LEN_BYTES);
            r0(2, key36, key5C, salt, u1, digest, H_LEN_BYTES,    H_LEN_BYTES);

            //r[1] .. r[count]
            int count = count_ - 1;

            final int updateIterations = Math.max(getPerfomance()/2, MIN_UPDATE_PERFOMANCE);
            final int numSets = (count + updateIterations - 1) / updateIterations;

            for (int set = 0; set < numSets; ++set) {
                final int numIt = Math.min(updateIterations, count);
                for (int it = 0; it < numIt; ++it) {
                    rk(key36, key5C, u0, digest, 0,             H_LEN_BYTES);
                    rk(key36, key5C, u1, digest, H_LEN_BYTES,   H_LEN_BYTES);
                }
                count -= numIt;
                updateProgress(count_ - count, start);
                if (break_)
                    throw new InterruptedException();
            }

            wipe(key36);
            wipe(key5C);
            wipe(u0);
            wipe(u1);

            //sleep and calculate
            final SecureRandom r = SecureRandom.getInstance();
            final int specifiy = r.getUInt31() & 0x3F;
            for (int i =0; i < specifiy; ++i) {
                if (0 == (r.getUInt31() & 0x1F))
                    break;
            }

            updateProgress(count_, start);
            final long minTime = MIN_CALCULATION - (System.currentTimeMillis() - start);
            if (minTime > 0)
                Thread.sleep(minTime);

            listener_.onSuccess(digest, salt_, count_, password_);
        } catch (InterruptedException ex) {
            listener_.onSuccess(null, null, 0, null);
        } catch (Exception ex) {
            listener_.onFailure(null, ex);
        }

        wipe(password_);    password_ = null;
        wipe(key36);
        wipe(key5C);
        wipe(u0);            wipe(u1);
        wipe(digest);
        removeCommand(Midlet.CANCEL);
    }

    public void commandAction(Command c, Displayable d) {
        if (c.getCommandType() == Command.CANCEL && thread_.isAlive()) {
            break_ = true;
            try {
                thread_.join();
            }
            catch (InterruptedException ex) {
            }
            wipe(password_);    password_ = null;
        }
    }

    private static void wipe(byte[] data) {
        if (null == data)
            return;

        Random r = new Random(System.currentTimeMillis());

        if (null != data) {
            for (int i = 0; i < data.length; ++i)
                data[i] = (byte)r.nextInt();
            data = null;
        }
    }
}
