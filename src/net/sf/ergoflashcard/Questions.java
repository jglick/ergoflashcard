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

package net.sf.ergoflashcard;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class Questions {
    
    private DataLine[] lines;
    private Question[] allQuestions, questions;
    
    // Also for use by Question instances:
    DB db;
    Config cfg;
    float performanceSum;
    int countUnseen;
    
    public Questions(DataLine[] lines, DB db, Config cfg) {
        this.lines = lines;
        this.db = db;
        this.cfg = cfg;
        allQuestions = questions = null;
    }
    
    public int getCount() {
        calcQuestions();
        return questions.length;
    }
    
    public int getCountUnseen() {
        calcQuestions();
        return countUnseen;
    }
    
    public float getAveragePerformance() {
        calcQuestions();
        int count = getCount();
        if (count == 0) {
            return 0.0f; 
        } else {
            return performanceSum / getCount();
        }
    }
    
    public Question getQuestion(int index) {
        calcQuestions();
        return questions[index];
    }
    
    class Aggregate {
        final List<String> sections = new ArrayList<String>(1);
        final String[] inSides;
        final List<String[]> outSides = new ArrayList<String[]>(1);
        Aggregate(String[] is) {
            inSides = is;
            //System.err.println ("New agg: " + this);
        }
        public boolean equals(Object o) {
            if (o == null || ! (o instanceof Aggregate)) {
                return false;
            }
            String[] test = ((Aggregate) o).inSides;
            if (test.length != inSides.length) {
                return false;
            }
            for (int i = 0; i < test.length; i++) {
                if (!test[i].equals(inSides[i])) {
                    return false;
                }
            }
            return true;
        }
        public int hashCode() {
            int x = 0;
            for (int i = 0; i < inSides.length; i++) {
                x ^= inSides[i].hashCode();
            }
            return x;
        }
        public String toString() {
            StringBuffer buf = new StringBuffer("Agg");
            for (int i = 0; i < inSides.length; i++) {
                buf.append('/');
                buf.append(inSides[i]);
            }
            return buf.toString();
        }
        void addSection(String sec) {
            //System.err.println ("Maybe add sec to agg " + this + ": " + sec);
            if (! sections.contains(sec)) {
                //System.err.println ("\t(adding)");
                sections.add(sec);
            }
        }
        void addOutSides(String[] sides) {
            //System.err.print ("Maybe add outSides to agg " + this + ":");
            //for (int i = 0; i < sides.length; i++)
            //System.err.print (" " + sides[i]);
            //System.err.println ();
            for (String[] oSides : outSides) {
                if (Arrays.equals(oSides, sides)) {
                    return;
                }
            }
            //System.err.println ("\t(adding)");
            outSides.add(sides);
        }
        Question toQuestion() {
            String[] secs = sections.toArray(new String[sections.size()]);
            if (secs.length > 1) {
                Arrays.sort(secs, Main.STRING_SORTER);
            }
            String[][] outs = outSides.toArray(new String[outSides.size()][]);
            if (outs.length > 1) {
                Arrays.sort(outs, new Comparator<String[]>() {
                    public int compare(String[] s1, String[] s2) {
                        for (int i = 0; ; i++) {
                            boolean t1 = (i < s1.length);
                            boolean t2 = (i < s2.length);
                            if (t1 && t2) {
                                int c = s1[i].compareTo(s2[i]);
                                if (c != 0) return c;
                            } else if (t1 && ! t2) {
                                return 1;
                            } else if (! t1 && t2) {
                                return -1;
                            } else /* ! t1 && ! t2 */ {
                                return 0;
                            }
                        }
                    }
                });
            }
            return new Question(secs, inSides, outs);
        }
    }
    
    private static String[] extractSides(String[] sides, int[] indices) {
        List<String> result = new ArrayList<String>(Math.max(1, indices.length));
        for (int index : indices) {
            if (index < sides.length) {
                result.add(sides[index]);
            }
        }
        return result.toArray(new String[result.size()]);
    }
    
    public boolean isPrepared() {
        return questions != null;
    }
    
    public void calcQuestions() {
        if (questions != null) return;
        synchronized (this) {
            if (allQuestions == null) {
                final JDialog dlg = new JDialog((Frame) null, "Analyzing...");
                BorderLayout layout = new BorderLayout();
                layout.setVgap(5);
                dlg.getContentPane().setLayout(layout);
                final JProgressBar prog = new JProgressBar();
                dlg.getContentPane().add(prog, BorderLayout.CENTER);
                dlg.getContentPane().add(new JLabel("Analyzing questions, please wait..."), BorderLayout.SOUTH);
                dlg.pack();
                dlg.setVisible(true);
                Map<Aggregate,Aggregate> aggs = new HashMap<Aggregate,Aggregate>(Math.max(1, lines.length));
                
                for (int i = 0; i < lines.length; i++) {
                    // 50% loading lines...
                    final int val = i * 50 / lines.length;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            prog.setValue(val);
                        }
                    });
                    DataLine line = lines[i];
                    String[] inSides = extractSides(line.sides, cfg.inSides);
                    String[] outSides = extractSides(line.sides, cfg.outSides);
                    Aggregate agg = new Aggregate(inSides);
                    Aggregate existing = aggs.get(agg);
                    if (existing == null) {
                        aggs.put(agg, agg);
                    } else {
                        agg = existing;
                    }
                    agg.addSection(line.section);
                    agg.addOutSides(outSides);
                }
                
                Question[] qs = new Question[aggs.size()];
                Iterator<Aggregate> e = aggs.keySet().iterator();
                for (int i = 0; i < qs.length; i++) {
                    // ...50% creating questions.
                    final int val = 50 + i * 50 / qs.length;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            prog.setValue(val);
                        }
                    });
                    assert e.hasNext();
                    qs[i] = e.next().toQuestion();
                }
                assert !e.hasNext(); // right?
                allQuestions = qs;
                lines = null; // clear memory (maybe)
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        dlg.dispose();
                    }
                });
            }
            if (questions == null) {
                refreshSectionsForQuestions();
            }
        }
    }
    
    private synchronized void refreshSectionsForQuestions() {
        Set<String> secs = new HashSet<String>(Arrays.asList(cfg.sections));
        List<Question> result = new ArrayList<Question>(Math.min(1, allQuestions.length));
        float pSum = 0.0f;
        countUnseen = 0;
        for (Question q : allQuestions) {
            for (String sec :  q.sections) {
                if (secs.contains(sec)) {
                    pSum += q.getPerformance();
                    result.add(q);
                    if (!q.isEverBeenSeen()) {
                        countUnseen++;
                    }
                    break;
                }
            }
        }
        questions = result.toArray(new Question[result.size()]);
        performanceSum = pSum;
    }
    
    public synchronized void refreshSections(Config cfg) {
        questions = null;
        this.cfg = cfg;
    }
    
    public int getNextQuestionIndex() {
        float best = Float.MAX_VALUE;
        int chosen = 0;
        int count = getCount();
        if (count == 0) return -1;
        for (int i = 0; i < count; i++) {
            float need = getQuestion(i).getNeedToDefer();
            if (need < best) {
                best = need;
                chosen = i;
            }
        }
        return chosen;
    }
    
    public Question getNextQuestion() {
        if (getCount() == 0) {
            return null; 
        } else {
            return getQuestion(getNextQuestionIndex());
        }
    }
    
    public final class Question {
        /** section(s) represented in this question */
        public final String[] sections;
        /** displayed sides */
        public final String[] in;
        /** answer sides--list of sets of answers */
        public final String[][] out;
        Question(String[] sections, String[] in, String[][] out) {
            this.sections = sections;
            this.in = in;
            this.out = out;
        }
        private byte[] dbKey = null;
        public synchronized byte[] getDBKey() {
            if (dbKey == null) {
                dbKey = DB.Entry.computeKey(in, out);
            }
            return dbKey;
        }
        private long gracePeriod = -1L;
        public synchronized long getGracePeriod() {
            if (gracePeriod == -1) {
                int len = 0;
                for (String inSide : in) {
                    len += 10 + inSide.length();
                }
                for (String[] outSides: out) {
                    for (String outSide : outSides) {
                        len += 10 + outSide.length();
                    }
                }
                if (len <= cfg.scoreGraceChars) {
                    gracePeriod = cfg.scoreGracePeriod;
                } else {
                    gracePeriod = cfg.scoreGracePeriod + cfg.scorePeriodPerChar * (len - cfg.scoreGraceChars);
                }
            }
            return gracePeriod;
        }
        private DB.Entry getEntry() {
            return db.findEntry(getDBKey());
        }
        private float perf = -1.0f;
        public float getPerformance() {
            if (perf < -0.5f) {
                perf = getEntry().perf;
            }
            return perf;
        }
        public synchronized void setPerformance(float nue) {
            if (nue < 0.0f || nue > 1.0f)
                throw new IllegalArgumentException();
            DB.Entry e = getEntry();
            time = System.currentTimeMillis();
            perf = nue;
            DB.Entry e2 = new DB.Entry(e.key, time, perf);
            db.updateEntry(e2);
            synchronized (Questions.this) {
                performanceSum += nue - e.perf;
            }
        }
        private long time = -1L;
        public long getTime() {
            if (time == -1L)
                time = getEntry().lastMod;
            return time;
        }
        public boolean isEverBeenSeen() {
            return getTime() > 0L;
        }
        /** need to defer showing this card..the lower, the sooner it will be shown */
        public float getNeedToDefer() {
            return (getPerformance() + cfg.scoreFudge) /
                    (cfg.timeFudge + System.currentTimeMillis() - getTime() +
                        (float) (Math.random() * cfg.timeRandomFudge));
        }
        public synchronized void updatePerformance(long delay, float correctness) {
            float baseRating;
            if (delay < getGracePeriod()) {
                baseRating = 1.0f;
            } else {
                baseRating = ((float) cfg.scoreHalfLife) / (delay - getGracePeriod() + cfg.scoreHalfLife);
            }
            float rating = baseRating * correctness;
            float orig = getPerformance();
            float nue = cfg.traceFudge * rating + (1.0f - cfg.traceFudge) * orig;
            // XXX not the prettiest place to do this, but it will suffice
            if (!isEverBeenSeen()) {
                countUnseen--;
            }
            setPerformance(nue);
            //System.err.println ("delay=" + delay + " correctness=" + correctness + " cfg.scoreGracePeriod=" + cfg.scoreGracePeriod + " cfg.scoreHalfLife=" + cfg.scoreHalfLife + " baseRating=" + baseRating + " rating=" + rating + " orig=" + orig + " nue=" + nue);
        }
    }
    
}
