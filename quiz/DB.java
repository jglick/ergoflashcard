/*
 * Copyright 2004 Jesse N. Glick
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quiz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.Vector;

public class DB implements Runnable {
    
    // Five minutes:
    public static final int FLUSH_DELAY = 1000 * 60 * 5;
    
    private File db;
    private RandomAccessFile raf;
    private Vector entries; // Vector<Entry>
    private Hashtable finder; // Hashtable<Entry,Integer>
    private int count;
    private boolean clean;
    private boolean closed;
    
    /** Create a database bound to a file.
     * The file may be null, in which case it is in-memory only.
     * @param db the file to bind to, or null
     */
    public DB(File db) {
        this.db = db;
        raf = null;
        entries = null;
        finder = null;
        clean = true;
        closed = false;
        if (db != null) {
            new Thread(this, "Autoflushing: " + toString()).start();
        }
    }
    
    public void run() {
        while (! closed) {
            try {
                Thread.sleep(FLUSH_DELAY);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                return;
            }
            flush();
        }
    }
    
    public synchronized void close() {
        if (closed) return;
        flush();
        closed = true;
    }
    
    public synchronized void flush() {
        if (db == null) return;
        if (! ensureRaf()) return;
        if (clean) return;
        //System.err.println ("flushing...");
        try {
            raf.close();
            raf = null;
            raf = new RandomAccessFile(db, "rw");
            clean = true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            raf = null;
            entries = null;
            finder = null;
        }
    }
    
    private synchronized boolean ensureRaf() {
        if (closed) return false;
        if (raf == null) {
            try {
                raf = new RandomAccessFile(db, "rw");
                return true;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return false;
            }
        } else {
            return true;
        }
    }
    
    private synchronized boolean ensureEntries() {
        if (closed) return false;
        if (entries == null) {
            if (db == null) {
                entries = new Vector(100);
                finder = new Hashtable(100);
                count = 0;
                return true;
            }
            if (! ensureRaf()) return false;
            try {
                raf.seek(0);
                long fLength = raf.length();
                if (fLength % Entry.ENTRY_LENGTH != 0)
                    throw new Error("weird file size");
                count = (int) (fLength / Entry.ENTRY_LENGTH);
                int cap = (count < 100) ? 100 : ((int) (count * 1.5));
                Vector v = new Vector(cap);
                Hashtable h = new Hashtable(cap);
                for (int i = 0; i < count; i++) {
                    Entry e = Entry.read(raf);
                    v.addElement(e);
                    h.put(e, new Integer(i));
                }
                entries = v;
                finder = h;
                return true;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return false;
            }
        } else {
            return true;
        }
    }
    
    public synchronized Entry findEntry(byte[] key) {
        Entry def = new Entry(key);
        if (! ensureEntries()) return def;
        Integer i = (Integer) finder.get(def);
        if (i == null) {
            return def;
        } else {
            return (Entry) entries.elementAt(i.intValue());
        }
    }
    
    public Entry findEntry(String[] in, String[][] out) {
        return findEntry(Entry.computeKey(in, out));
    }
    
    
    public synchronized void updateEntry(Entry e) {
        if (! ensureEntries()) return;
        Integer i = (Integer) finder.get(e);
        int pos;
        if (i == null) {
            pos = count++;
            entries.addElement(e);
            finder.put(e, new Integer(pos));
        } else {
            pos = i.intValue();
            entries.setElementAt(e, pos);
        }
        if (db == null) return;
        if (! ensureRaf()) return;
        if (clean) {
            clean = false;
            File bak = new File(db.getParent(), db.getName() + ".bak");
            byte[] buf = new byte[4096];
            try {
                InputStream is = new FileInputStream(db);
                try {
                    OutputStream os = new FileOutputStream(bak);
                    try {
                        int count;
                        while ((count = is.read(buf)) != -1)
                            os.write(buf, 0, count);
                    } finally {
                        os.close();
                    }
                } finally {
                    is.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        try {
            raf.seek(pos * Entry.ENTRY_LENGTH);
            e.write(raf);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            raf = null;
            entries = null;
            finder = null;
        }
    }
    
    public static final class Entry {
        
        // acc. to SHA-1
        public final static int KEY_LENGTH = 20;
        // eight for the date, four for performance
        private final static int EXTRA_LENGTH = 8 + 4;
        public final static int ENTRY_LENGTH = KEY_LENGTH + EXTRA_LENGTH;
        
        private static MessageDigest md;
        static {
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException nsae) {
                nsae.printStackTrace();
            }
        }
        
        /** key for this question */
        public final byte[] key;
        /** last time this question was asked, as millisec since epoch */
        public final long lastMod;
        /** current performance between 0.0 and 1.0 */
        public final float perf;
        
        public Entry(byte[] key) {
            this.key = key;
            lastMod = Long.MIN_VALUE;
            perf = 0.0f;
        }
        
        public Entry(byte[] key, long lastMod, float perf) {
            this.key = key;
            this.lastMod = lastMod;
            this.perf = perf;
        }
        
        public Entry(String[] in, String[][] out) {
            this.key = computeKey(in, out);
            lastMod = Long.MIN_VALUE;
            perf = 0.0f;
        }
        
        public Entry(String[] in, String[][] out, long lastMod, float perf) {
            this.key = computeKey(in, out);
            this.lastMod = lastMod;
            this.perf = perf;
        }
        
        public static Entry read(RandomAccessFile raf) throws IOException {
            byte[] key = new byte[KEY_LENGTH];
            raf.readFully(key);
            long d = raf.readLong();
            float f = raf.readFloat();
            return new Entry(key, d, f);
        }
        
        public void write(RandomAccessFile raf) throws IOException {
            raf.write(key);
            raf.writeLong(lastMod);
            raf.writeFloat(perf);
        }
        
        private int hashCodeCache = 0;
        public int hashCode() {
            if (hashCodeCache == 0) {
                int x = 0;
                for (int i = 0; i < Entry.KEY_LENGTH; i++)
                    x ^= (key[i] << (8 * (i % 4)));
                hashCodeCache = x;
            }
            return hashCodeCache;
        }
        
        public boolean equals(Object o) {
            if (o == null || ! (o instanceof Entry)) return false;
            Entry e = (Entry) o;
            if (hashCode() != e.hashCode()) return false;
            byte[] other = e.key;
            for (int i = 0; i < Entry.KEY_LENGTH; i++)
                if (key[i] != other[i])
                    return false;
            return true;
        }
        
        /** Compute a key based on quiz strings.
         * @param in list of displayed strings
         * @param out list of possible answers, each of which is a list of displayed strings
         * @param a byte-array key to look up this question in the DB with
         */
        public static byte[] computeKey(String[] in, String[][] out) {
            try {
                synchronized (md) {
                    md.reset();
                    update(md, in.length);
                    for (int i = 0; i < in.length; i++) {
                        update(md, in[i].length());
                        md.update(in[i].getBytes("UTF-8"));
                    }
                    update(md, out.length);
                    for (int i = 0; i < out.length; i++) {
                        update(md, out[i].length);
                        for (int j = 0; j < out[i].length; j++) {
                            update(md, out[i][j].length());
                            md.update(out[i][j].getBytes("UTF-8"));
                        }
                    }
                    byte[] toRet = md.digest();
                    if (toRet.length != KEY_LENGTH)
                        throw new Error("wrong key length");
                    return toRet;
                }
            } catch (UnsupportedEncodingException uee) {
                uee.printStackTrace();
                throw new Error(uee.toString());
            }
        }
        private static void update(MessageDigest md, int x) {
            md.update((byte) ((x & 0xFF000000) >> 24));
            md.update((byte) ((x & 0x00FF0000) >> 16));
            md.update((byte) ((x & 0x0000FF00) >> 8));
            md.update((byte) (x & 0x000000FF));
        }
        
    }
    
}
