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

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

public class Config implements Cloneable {
    
    // Percentage fudge added to score to make deferment work.
    private static final float DEF_SCORE_FUDGE = 0.10f;
    // Millisecond fudge added to last-seen to make deferment work.
    private static final long DEF_TIME_FUDGE = 100000L;
    // Maximum millisecond randomized fudge added to last-seen to make deferment work.
    private static final long DEF_TIME_RANDOM_FUDGE = 30000L;
    // Trace percentage used when updating scores; 1 = no trace, 0 = no update.
    private static final float DEF_TRACE_FUDGE = 0.6f;
    // Minimum milliseconds alotted penalty-free for answering.
    private static final long DEF_SCORE_GRACE_PERIOD = 5000L;
    // Milliseconds after grace period after which raw score will be halved.
    private static final long DEF_SCORE_HALF_LIFE = 5000L;
    // Maximum number of characters in question + answer not affecting grace period.
    // Each separate side in the question and in each answer counts as an additional 10 chars.
    // So 60 means really a 20-char question plus 20-char answer with no additional sides.
    private static final int DEF_SCORE_GRACE_CHARS = 60;
    // Number of additional milliseconds added to grace period for each additional character.
    private static final long DEF_SCORE_PERIOD_PER_CHAR = 50L;
    // Milliseconds between displaying score and starting next question.
    private static final int DEF_INTERMISSION = 1000;
    // Name of font used to display quiz text.
    private static final String DEF_FONT_FAMILY = "Serif";
    // Style of font used to display quiz text.
    private static final int DEF_FONT_STYLE = Font.PLAIN;
    // Point size of font used to display quiz text.
    private static final int DEF_FONT_SIZE = 24;
    
    public File[] dataFiles;
    public String[] sections;
    public int[] inSides;
    public int[] outSides;
    public String[] sideNames;
    public String name;
    public float scoreFudge;
    public long timeFudge;
    public long timeRandomFudge;
    public float traceFudge;
    public long scoreGracePeriod;
    public long scoreHalfLife;
    public int scoreGraceChars;
    public long scorePeriodPerChar;
    public File dbFile;
    public int intermission;
    public String fontFamily;
    public int fontStyle;
    public int fontSize;
    public Locale locale;
    
    public File origin;
    
    public Config() {
        dataFiles = new File[0];
        sections = new String[0];
        inSides = new int[0];
        outSides = new int[0];
        sideNames = new String[0];
        name = "<unconfigured>";
        scoreFudge = DEF_SCORE_FUDGE;
        timeFudge = DEF_TIME_FUDGE;
        timeRandomFudge = DEF_TIME_RANDOM_FUDGE;
        traceFudge = DEF_TRACE_FUDGE;
        scoreGracePeriod = DEF_SCORE_GRACE_PERIOD;
        scoreHalfLife = DEF_SCORE_HALF_LIFE;
        scoreGraceChars = DEF_SCORE_GRACE_CHARS;
        scorePeriodPerChar = DEF_SCORE_PERIOD_PER_CHAR;
        dbFile = null;
        intermission = DEF_INTERMISSION;
        fontFamily = DEF_FONT_FAMILY;
        fontStyle = DEF_FONT_STYLE;
        fontSize = DEF_FONT_SIZE;
        locale = Locale.getDefault();
        
        origin = null;
    }
    
    public Config cloneConfig() {
        try {
            return (Config) clone();
        } catch (CloneNotSupportedException cnse) {
            cnse.printStackTrace();
            return this;
        }
    }
    
    public void save() throws IOException {
        if (origin != null) write(origin);
    }
    
    public void write(File f) throws IOException {
        Properties p = new Properties();
        p.put("dataFiles", join(dataFiles, f));
        p.put("sections", join(sections));
        p.put("inSides", join(inSides));
        p.put("outSides", join(outSides));
        p.put("sideNames", join(sideNames));
        p.put("name", name);
        if (scoreFudge != DEF_SCORE_FUDGE)
            p.put("scoreFudge", Float.toString(scoreFudge));
        if (timeFudge != DEF_TIME_FUDGE)
            p.put("timeFudge", Long.toString(timeFudge));
        if (timeRandomFudge != DEF_TIME_RANDOM_FUDGE)
            p.put("timeRandomFudge", Long.toString(timeRandomFudge));
        if (traceFudge != DEF_TRACE_FUDGE)
            p.put("traceFudge", Float.toString(traceFudge));
        if (scoreGracePeriod != DEF_SCORE_GRACE_PERIOD)
            p.put("scoreGracePeriod", Long.toString(scoreGracePeriod));
        if (scoreHalfLife != DEF_SCORE_HALF_LIFE)
            p.put("scoreHalfLife", Long.toString(scoreHalfLife));
        if (scoreGraceChars != DEF_SCORE_GRACE_CHARS)
            p.put("scoreGraceChars", Integer.toString(scoreGraceChars));
        if (scorePeriodPerChar != DEF_SCORE_PERIOD_PER_CHAR)
            p.put("scorePeriodPerChar", Long.toString(scorePeriodPerChar));
        if (dbFile != null) p.put("dbFile", relativize(dbFile, f));
        if (intermission != DEF_INTERMISSION)
            p.put("intermission", Integer.toString(intermission));
        if (! fontFamily.equals(DEF_FONT_FAMILY))
            p.put("fontFamily", fontFamily);
        if (fontStyle != DEF_FONT_STYLE)
            p.put("fontStyle", Integer.toString(fontStyle));
        if (fontSize != DEF_FONT_SIZE)
            p.put("fontSize", Integer.toString(fontSize));
        if (! locale.equals(Locale.getDefault()))
            p.put("locale", locale.getLanguage() + "_" + locale.getCountry());
        OutputStream os = new FileOutputStream(f);
        try {
            p.store(os, "Quiz Configuration -*- Java-Properties -*-");
        } finally {
            os.close();
        }
        origin = f;
    }
    
    public void read(File f) throws IOException {
        Properties p = new Properties();
        InputStream is = new FileInputStream(f);
        try {
            p.load(is);
        } finally {
            is.close();
        }
        origin = f;
        try {
            if (p.containsKey("dataFiles"))
                dataFiles = readFileArray(p.getProperty("dataFiles"), f);
            if (p.containsKey("sections"))
                sections = readStringArray(p.getProperty("sections"));
            if (p.containsKey("inSides"))
                inSides = readIntArray(p.getProperty("inSides"));
            if (p.containsKey("outSides"))
                outSides = readIntArray(p.getProperty("outSides"));
            if (p.containsKey("sideNames"))
                sideNames = readStringArray(p.getProperty("sideNames"));
            if (p.containsKey("name"))
                name = p.getProperty("name");
            if (p.containsKey("scoreFudge"))
                scoreFudge = Float.valueOf(p.getProperty("scoreFudge")).floatValue();
            if (p.containsKey("timeFudge"))
                timeFudge = Long.valueOf(p.getProperty("timeFudge")).longValue();
            if (p.containsKey("timeRandomFudge"))
                timeRandomFudge = Long.valueOf(p.getProperty("timeRandomFudge")).longValue();
            if (p.containsKey("traceFudge"))
                traceFudge = Float.valueOf(p.getProperty("traceFudge")).floatValue();
            if (p.containsKey("scoreGracePeriod"))
                scoreGracePeriod = Long.valueOf(p.getProperty("scoreGracePeriod")).longValue();
            if (p.containsKey("scoreHalfLife"))
                scoreHalfLife = Long.valueOf(p.getProperty("scoreHalfLife")).longValue();
            if (p.containsKey("scoreGraceChars"))
                scoreGraceChars = Integer.parseInt(p.getProperty("scoreGraceChars"));
            if (p.containsKey("scorePeriodPerChar"))
                scorePeriodPerChar = Long.valueOf(p.getProperty("scorePeriodPerChar")).longValue();
            if (p.containsKey("dbFile"))
                dbFile = relateFile(f, p.getProperty("dbFile"));
            if (p.containsKey("intermission"))
                intermission = Integer.parseInt(p.getProperty("intermission"));
            if (p.containsKey("fontFamily"))
                fontFamily = p.getProperty("fontFamily");
            if (p.containsKey("fontStyle"))
                fontStyle = Integer.parseInt(p.getProperty("fontStyle"));
            if (p.containsKey("fontSize"))
                fontSize = Integer.parseInt(p.getProperty("fontSize"));
            if (p.containsKey("locale")) {
                StringTokenizer tok = new StringTokenizer(p.getProperty("locale"), "_");
                locale = new Locale(tok.nextToken(), tok.nextToken());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.toString());
        }
    }
    
    private static File relateFile(File orig, String path) {
        File test = new File(path);
        if (test.isAbsolute()) {
            return test; 
        } else {
            return new File(orig.getParent(), path);
        }
    }
    private static String relativize(File target, File orig) {
        // XXX could be more aggressive and look for partial path matches
        if (! target.isAbsolute()) target = new File(target.getAbsolutePath());
        if (! orig.isAbsolute()) orig = new File(orig.getAbsolutePath());
        File targetC, origC;
        try {
            targetC = target.getCanonicalFile();
            origC = orig.getCanonicalFile();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            targetC = target;
            origC = orig;
        }
        if (targetC.getParent().equals(origC.getParent())) {
            return target.getName();
        } else if (targetC.getParentFile().equals(origC.getParentFile().getParentFile())) {
            return ".." + File.separator + target.getName();
        } else {
            return target.getPath();
        }
    }
    private static String join(Object[] objs) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < objs.length; i++) {
            if (i > 0) buf.append('|');
            buf.append(objs[i].toString());
        }
        return buf.toString();
    }
    private static String join(int[] nums) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < nums.length; i++) {
            if (i > 0) buf.append('|');
            buf.append(Integer.toString(nums[i]));
        }
        return buf.toString();
    }
    private static String join(File[] files, File orig) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < files.length; i++) {
            if (i > 0) buf.append('|');
            buf.append(relativize(files[i], orig));
        }
        return buf.toString();
    }
    private static File[] readFileArray(String text, File base) {
        Vector v = new Vector();
        StringTokenizer tok = new StringTokenizer(text, "|");
        while (tok.hasMoreTokens())
            v.addElement(relateFile(base, tok.nextToken()));
        File[] ret = new File[v.size()];
        v.copyInto(ret);
        return ret;
    }
    private static String[] readStringArray(String text) {
        Vector v = new Vector();
        StringTokenizer tok = new StringTokenizer(text, "|");
        while (tok.hasMoreTokens())
            v.addElement(tok.nextToken());
        String[] ret = new String[v.size()];
        v.copyInto(ret);
        return ret;
    }
    private static int[] readIntArray(String text) throws NumberFormatException {
        Vector v = new Vector();
        StringTokenizer tok = new StringTokenizer(text, "|");
        while (tok.hasMoreTokens())
            v.addElement(new Integer(Integer.parseInt(tok.nextToken())));
        int[] ret = new int[v.size()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = ((Integer) v.elementAt(i)).intValue();
        return ret;
    }
    
}
