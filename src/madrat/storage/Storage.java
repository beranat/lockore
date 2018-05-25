/*
 * Copyright (C) 2017 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>
 *
 * This file is part of madRat's J2ME Storage (madrat.storage).
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

package madrat.storage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Random;
import java.util.Vector;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import madrat.sys.SecureRandom;
import madrat.sys.Sha3;

/**
 * Secured container for data based on RMS (JSR-37/JSR-118)
 */
public final class Storage {
    private static final int STORE_RID = 1;
    private static final int INVALID_RID = 0;
    private static final int VERSION = 1;

    public static final String F_CIPHER = ".cipher";
    public static final String   CIPHER = "AES";

    public static final String F_CIPHER_KEYLEN  = ".keylen";
    public static final int      CIPHER_KEYLEN  = 32;
    public static final int      CIPHER_IVLEN   = 16;

    public static final String F_HASH   = ".hash";
    public static final String   HASH   = "SHA3";

    private static final String F_CHECKSUM    = ".check"; // int 16bit

    private final RecordStore store_;
    private final Record meta_;
    private final Vector recordHeads_ = new Vector();
    private final Cipher cipher_;
    private Key key_;
    private AlgorithmParameterSpec param_;

    /**
     * Create a new store
     * @param storeName
     * @param name
     * @param desc
     * @param icon
     * @throws RecordStoreException
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static void createStore(String storeName, String name, String desc, int icon)
                    throws  RecordStoreException,
                            IOException,
                            GeneralSecurityException {
        try {
            final RecordStore store = RecordStore.openRecordStore(storeName, false);
            store.closeRecordStore();
            throw new IllegalArgumentException("Store is exists");
        } catch (RecordStoreNotFoundException e) {
        }

        Record meta = new Record(STORE_RID);

        meta.setName(name);
        meta.setDescription(desc);
        meta.setIcon(icon);

        meta.setFormat(VERSION);

        meta.set(F_CIPHER, CIPHER);
        meta.set(F_CIPHER_KEYLEN, CIPHER_KEYLEN);
        meta.set(F_HASH, HASH);

        RecordStore store = RecordStore.openRecordStore(storeName, true);
        try {
            store.addRecord(null, 0, 0); // reserve 1st record for meta info
            saveMeta(store, meta);
        } finally {
            store.closeRecordStore();
        }
    }

    /**
     * Delete existing store (may be locked)
     * @param storeName
     */
    public static void wipeStore(String storeName) {
        try {
            RecordStore.deleteRecordStore(storeName);
        } catch (RecordStoreException e) {
        }
    }

    /**
     * find head by recordID
     * @param r
     * @return
     * @throws LockedException
     */
    private int findRid(Record r) throws LockedException {
        checkLocked();

        final int rid = r.getRid();
        if (INVALID_RID != rid) {
            for (int i = recordHeads_.size() - 1; i >= 0; --i) {
                Record c = (Record)recordHeads_.elementAt(i);
                if (null != c && rid == c.getRid())
                    return i;
            }
        }
        return -1;
    }

    /**
     * Helper - find a max record size
     * @param store
     * @param e
     * @return size in bytes
     * @throws RecordStoreException
     */
    private static int maxRecordSize(RecordStore store, RecordEnumeration e) throws RecordStoreException {
        int len = 0;
        e.reset();
        while (e.hasNextElement()) {
            final int rid = e.nextRecordId();
            len = Math.max(len, store.getRecordSize(rid));
        }
        return len;
    }

    /**
     * Allocate memory, may call GC for compacting
     * @param length
     * @return
     */
    public static final byte[] alloc(int length) {
        try {
            return new byte[length];
        } catch (OutOfMemoryError e) {
            System.gc();
            return new byte[length];
        }
    }

    /**
     *  Save public (meta) info about store
     * @param store
     * @param meta
     * @throws NobodyException
     * @throws RecordStoreException
     * @throws IOException
     */
    private static void saveMeta(RecordStore store, Record meta) throws NobodyException, RecordStoreException, IOException  {
        java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(bs);
        meta.save(os);
        byte[] buffer = bs.toByteArray();
        store.setRecord(meta.getRid(), buffer, 0, buffer.length);
    }

    /**
     * Open, but not unlock storage
     * @param storeName - RMS-database name
     * @throws UnsupportedException - unexpected param(s)
     * @throws RecordStoreException
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public Storage(String storeName) throws UnsupportedException,
                                            RecordStoreException,
                                            IOException,
                                            GeneralSecurityException {
        RecordStore store = RecordStore.openRecordStore(storeName, false);
        try {
            byte[] bs = store.getRecord(STORE_RID);
            DataInputStream is = new DataInputStream(new ByteArrayInputStream(bs));

            meta_ = new Record(STORE_RID, false, is);

            final int version = meta_.getFormat();
            if (version != VERSION)
                throw new UnsupportedException("version", Integer.toString(version));

            // MD5, AES should work for CBC/ECB as J2ME platform
            // SHA3/keccak - special implementation
            final String cipher = meta_.get(F_CIPHER, "Undef");
            final long keylen = meta_.get(F_CIPHER_KEYLEN, 0);
            if (!CIPHER.equals(cipher) || CIPHER_KEYLEN != keylen)
                throw new UnsupportedException("cipher", cipher + "-" +Long.toString(keylen*8));

            final String hash = meta_.get(F_HASH, "Undef");
            if (!HASH.equals(hash))
                throw new UnsupportedException("hash", hash);

            store_ = store;
            store = null;
            cipher_ = Cipher.getInstance("AES/CBC/NoPadding");
        }
        finally {
            if (null != store)
                store.closeRecordStore();
        }
    }

    public boolean isLocked() {
        return (null == key_);
    }

    private void checkLocked() throws LockedException {
        if (null == store_ || isLocked())
            throw new LockedException();
    }

    public int getStorageSize() throws RecordStoreNotOpenException {
        if (null == store_)
            throw new RecordStoreNotOpenException();
        return store_.getSize();
    }

    public int getStorageFree() throws RecordStoreNotOpenException {
        if (null == store_)
            throw new RecordStoreNotOpenException();
        return store_.getSizeAvailable();
    }

    public void lock(byte[] key, byte[] salt)
                                    throws  InvalidKeyException,
                                            RecordStoreException,
                                            IOException,
                                            GeneralSecurityException {
        Field fCheck = meta_.find(F_CHECKSUM);
        if (null != fCheck)
            throw new java.security.InvalidKeyException("Already locked");

        int check = cipherParams(key, salt);
        meta_.set(F_CHECKSUM, check);
        save();
    }

    public void unlock(byte[] key, byte[] salt)
                                    throws  RecordStoreException,
                                            IOException,
                                            InvalidKeyException,
                                            SignatureException,
                                            GeneralSecurityException {
        recordHeads_.removeAllElements();
        key_ = null;
        param_ = null;

        Field fCheck = meta_.find(F_CHECKSUM);
        if (null == fCheck)
            throw new java.security.InvalidKeyException("Public store");

        final int check = cipherParams(key, salt);

        if (check != (int)fCheck.getLong())
            throw new java.security.SignatureException("Key signature is invalid");


        RecordEnumeration recEnum = null;
        try {
            recEnum = store_.enumerateRecords(null, null, true);

            byte[] buffer;
            int offset;
            byte[] temp = null;

            final int maxSize = maxRecordSize(store_, recEnum);
            try {
                offset = madrat.sys.Cipher.CIPER_BLOCK_RESERVE;
                buffer = alloc(maxSize + offset);
            } catch (OutOfMemoryError e) {
                offset = 0;
                buffer = alloc(maxSize + offset);
                temp = alloc(madrat.sys.Cipher.CIPER_BLOCK_RESERVE + madrat.sys.Cipher.CIPER_BLOCK);
            }

            for (recEnum.reset(); recEnum.hasNextElement(); ) {
                int rid = recEnum.nextRecordId();
                if (STORE_RID == rid)
                    continue;
                recordHeads_.addElement(loadRecord(rid, true, buffer, offset, temp));
            }
        } finally {
            if (null != recEnum) {
                recEnum.destroy();
            }
        }
    }


    private int cipherParams(byte[] passwd, byte[] salt) throws
                                                InvalidAlgorithmParameterException,
                                                ShortBufferException,
                                                InvalidKeyException,
                                                NoSuchAlgorithmException,
                                                NoSuchPaddingException,
                                                SignatureException {

        Sha3 sha3 = new Sha3(CIPHER_KEYLEN<<4); //  x2 (twice) x8 (bits)
        byte[] hash = sha3.getHMmac(passwd, salt);

        final int check =    (hash[CIPHER_KEYLEN+CIPHER_IVLEN-1] & 0xFF) |
                            ((hash[CIPHER_KEYLEN+CIPHER_IVLEN-2] & 0xFF) <<8);

        SecretKeySpec   key   = new SecretKeySpec(hash, 0, CIPHER_KEYLEN, CIPHER);
        IvParameterSpec param = new IvParameterSpec(hash, CIPHER_KEYLEN, CIPHER_IVLEN);

        Random r = new Random(System.currentTimeMillis());

        for (int i = 0; i < hash.length; ++i)
            hash[i] = (byte)r.nextInt();

        cipher_.init(Cipher.ENCRYPT_MODE, key, param);

        key_ = key;
        param_ = param;

        return check;
    }

    public Record newRecord() {
        return new Record(INVALID_RID);
    }

    private Record saveRecord(Record r) throws  LockedException,
                                                NobodyException,
                                                RecordStoreException,
                                                GeneralSecurityException,
                                                IOException {
        checkLocked();
        r.checkBody();

        // Calculate len
        long seed = SecureRandom.getInstance().getLong();
        Random rand = new Random(seed);

        BufferOutputStream bs = new BufferOutputStream(null);
        DataOutputStream os = new DataOutputStream(bs);

        r.save(os, rand, 256);

        final int length = bs.pos();
        final int maxSize = ((length + madrat.sys.Cipher.CIPER_BLOCK) / madrat.sys.Cipher.CIPER_BLOCK) * madrat.sys.Cipher.CIPER_BLOCK;

        byte[] buffer;
        int offset;
        byte[] temp = null;

        try {
            offset = madrat.sys.Cipher.CIPER_BLOCK_RESERVE;
            buffer = alloc(maxSize + offset);
        } catch (OutOfMemoryError e) {
            offset = 0;
            buffer = alloc(maxSize + offset);
            temp = alloc(madrat.sys.Cipher.CIPER_BLOCK_RESERVE + madrat.sys.Cipher.CIPER_BLOCK);
        }

        bs = new BufferOutputStream(buffer, offset);
        os = new DataOutputStream(bs);
        rand.setSeed(seed);
        r.save(os, rand, 256);

        Random padRand = new Random(SecureRandom.getInstance().getLong());
        int padding = bs.pos() % madrat.sys.Cipher.CIPER_BLOCK;
        if (padding > 0) {
            padding = madrat.sys.Cipher.CIPER_BLOCK - padding;
        }
        Record.savePad(os, padRand, padding);

        cipher_.init(Cipher.ENCRYPT_MODE, key_, param_);
        final int elen = madrat.sys.Cipher.cipher(cipher_, buffer, offset, bs.pos(), temp);

        if (INVALID_RID != r.getRid()) {
            store_.setRecord(r.getRid(), buffer, 0, elen);
        } else {
            r.setRid(store_.addRecord(buffer, 0, elen));
        }
        return loadRecord(r.getRid(), true, buffer, offset, temp);
    }

    private Record loadRecord(int rid, boolean isHead, byte[] buffer, int offset, byte[] temp)
                        throws  LockedException,
                                RecordStoreException,
                                InvalidAlgorithmParameterException,
                                InvalidKeyException,
                                ShortBufferException,
                                IllegalBlockSizeException,
                                BadPaddingException,
                                IOException {
        checkLocked();

        BufferInputStream bs = new BufferInputStream(buffer);
        DataInputStream is = new DataInputStream(bs);

        int d = store_.getRecordSize(rid);
        int elen = store_.getRecord(rid, buffer, offset);
        cipher_.init(Cipher.DECRYPT_MODE, key_, param_);

        final int dlen = madrat.sys.Cipher.cipher(cipher_, buffer, offset, elen, temp);
        bs.reinit(0, dlen);
        is.reset();
        return new Record(rid, isHead, is);
    }

    public Record cloneRecord(Record r, String name) throws
                                                        LockedException,
                                                        IOException,
                                                        GeneralSecurityException,
                                                        RecordStoreException {
        Record cloned = load(r);
        cloned.setRid(INVALID_RID);
        if (null != name)
            cloned.setName(name);
        return cloned;
    }

    /**
     * Insert new Record w/o RID
     * @param r - new item (created by cloneRecord or newRecord)
     * @throws IllegalArgumentException - item is already used in storage
     * @throws NobodyException - storage it not opened or unlocked
     * @throws GeneralSecurityException - decryption error
     * @throws RecordStoreException - RMS error
     * @throws IOException - parsing and load error
     */
    public void insertNew(Record r) throws  IllegalArgumentException,
                                            LockedException,
                                            NobodyException,
                                            GeneralSecurityException,
                                            RecordStoreException,
                                            IOException {
        if (INVALID_RID != r.getRid())
            throw new IllegalArgumentException("Attached");

        if (-1 != findRid(r))
            throw new IllegalArgumentException("Existed");

        Record head = saveRecord(r);
        recordHeads_.addElement(head);
    }

    /**
     * Update existing Record
     * @param r - existing record (load)
     * @throws IllegalArgumentException - item is already used in storage
     * @throws NobodyException - storage it not opened or unlocked
     * @throws GeneralSecurityException - decryption error
     * @throws RecordStoreException - RMS error
     * @throws IOException - parsing and load error
     */
    public void updateExisting(Record r) throws
                                            IllegalArgumentException,
                                            NobodyException,
                                            LockedException,
                                            GeneralSecurityException,
                                            RecordStoreException,
                                            IOException {
        checkLocked();

        final int index = findRid(r);
        if (-1 == index)
            throw new IllegalArgumentException("Unexisted");

        Record head = saveRecord(r);
        recordHeads_.setElementAt(head, index);
    }

    public void remove(Record r) throws IllegalArgumentException, RecordStoreException {
        final int index = findRid(r);
        if (-1 == index)
            throw new IllegalArgumentException();

        remove(index);
    }

    public void remove(int index) throws RecordStoreException {
        checkLocked();

        if (index < 0 || index >= recordHeads_.size())
            throw new IllegalArgumentException();

        Record r = (Record)recordHeads_.elementAt(index);
        recordHeads_.removeElementAt(index);
        final int rid = r.getRid();
        if (INVALID_RID != rid) {
            store_.deleteRecord(rid);
        }
    }

    public int size() throws LockedException {
        checkLocked();
        return recordHeads_.size();
    }

    public Record get(int index) throws LockedException {
        checkLocked();

        if (index < 0 || index >= recordHeads_.size())
            throw new IllegalArgumentException();
        return (Record) recordHeads_.elementAt(index);
    }

    public Record load(int index) throws
                                    LockedException,
                                    RecordStoreException,
                                    InvalidAlgorithmParameterException,
                                    InvalidKeyException,
                                    ShortBufferException,
                                    IllegalBlockSizeException,
                                    BadPaddingException,
                                    IOException {
        checkLocked();
        return load((Record)recordHeads_.elementAt(index));
    }

    public Record load(Record r) throws
                                    LockedException,
                                    RecordStoreException,
                                    InvalidAlgorithmParameterException,
                                    InvalidKeyException,
                                    ShortBufferException,
                                    IllegalBlockSizeException,
                                    BadPaddingException,
                                    IOException {
        checkLocked();

        final int rid = r.getRid();

        if (INVALID_RID == rid)
            throw new IllegalArgumentException();

        int recSize = store_.getRecordSize(rid);
        byte[] buffer;
        int offset;
        byte[] temp = null;

        try {
            offset = madrat.sys.Cipher.CIPER_BLOCK_RESERVE;
            buffer = alloc(recSize + offset);
        } catch (OutOfMemoryError e) {
            offset = 0;
            buffer = alloc(recSize + offset);
            temp = alloc(madrat.sys.Cipher.CIPER_BLOCK_RESERVE + madrat.sys.Cipher.CIPER_BLOCK);
        }
        return loadRecord(rid, false, buffer, offset, temp);
    }


    public void wipe() {
        if (null == store_)
            return;

        try {
            final String name = store_.getName();
            destroy();
            wipeStore(name);
        } catch (RecordStoreNotOpenException e) {
        }
    }

    public void destroy() {
        key_ = null;
        param_ = null;

        try {
            if (null != cipher_) {
                cipher_.init(Cipher.ENCRYPT_MODE, null, null);
            }
        } catch (Exception ex) {
        }

        try {
            store_.closeRecordStore();
        } catch (RecordStoreException ex) {
        }
        System.gc();
    }

    protected static void checkName(String name) throws IllegalArgumentException {
        if (null == name
                || name.length() == 0
                || name.charAt(0) == '.') {
            throw new IllegalArgumentException();
        }
    }

    public String getValue(String name, String def) {
        return meta_.get(name, def);
    }

    public void setValue(String name, String value) throws IllegalArgumentException {
        checkName(name);
        meta_.set(name, value);
    }

    public long getValue(String name, long def) {
        return meta_.get(name, def);
    }

    public void setValue(String name, long value) throws IllegalArgumentException {
        checkName(name);
        meta_.set(name, value);
    }

    public boolean isModified() {
        return meta_.isModified();
    }

    public void save() throws RecordStoreException, IOException, GeneralSecurityException {
        saveMeta(store_, meta_);
    }

    public String getName() {
        return meta_.getName();
    }

    public void setName(String n) {
        meta_.setName(n);
    }

    public int getIcon() {
        return meta_.getIcon();
    }

    public void setIcon(int i) {
        meta_.setIcon(i);
    }

    public void setDescription(String text) {
        meta_.set(text);
    }

    public String getDescription() {
        return meta_.getString();
    }

    public String toString() {
        return store_.toString();
    }
}
