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
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.Collator;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;

public class Main extends JFrame {
    
    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java net.sf.ergoflashcard.Main [<configname>]");
            System.exit(1);
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        Main main = new Main();
        if (args.length == 1) {
            Config cfg = new Config();
            try {
                cfg.read(new File(args[0]));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            main.initConfig(cfg);
        }
        main.show();
        main.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent ev) {
                System.exit(0);
            }
        });
    }
    
    private static final String CONFIG_EXTENSION = "ergoflashcardcfg";
    
    private static final FileFilter CONFIG_FILTER = new FileFilter() {
        public String getDescription() {
            return "Config files (*." + CONFIG_EXTENSION + ")";
        }
        public boolean accept(File f) {
            return f.getName().endsWith("." + CONFIG_EXTENSION) || f.isDirectory();
        }
    };
    
    private DB db;
    private Questions qs;
    private Config cfg;
    private DataLine[] data;
    private String[] availableSections;
    private Collator coll;
    
    private File chooserDir;
    
    public Main() {
        super("ErgoFlashCard");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
                shutdown();
            }
        });
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            chooserDir = new File(userDir);
        }
        updateMenus();
        pack();
        repaint();
    }
    
    private void shutdown() {
        qs = null;
        if (db != null) {
            db.close();
            db = null;
        }
        cfg = null;
        dispose();
    }
    
    private void initConfig(Config nue) {
        db = new DB(nue.dbFile);
        readLines(nue);
        qs = new Questions(data, db, nue);
        initLayout(nue);
        commonConfig(nue);
    }
    
    private synchronized void setConfig(Config nue) {
        if (cfg == null) {
            initConfig(nue);
            return;
        }
        try {
            nue.save();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        if (! cfg.dbFile.equals(nue.dbFile)) {
            db.close();
            db = new DB(nue.dbFile);
        }
        boolean sameFiles;
        if (cfg.dataFiles.length != nue.dataFiles.length) {
            sameFiles = false;
        } else {
            sameFiles = true;
            for (int i = 0; i < cfg.dataFiles.length; i++) {
                if (! cfg.dataFiles[i].equals(nue.dataFiles[i])) {
                    sameFiles = false;
                    break;
                }
            }
        }
        if (! sameFiles) {
            readLines(nue);
            qs = new Questions(data, db, nue);
        } else {
            qs.refreshSections(nue);
        }
        commonConfig(nue);
    }
    
    private void setConfigLater(final Config nue) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setConfig(nue);
            }
        });
    }
    
    private void commonConfig(Config nue) {
        setTitle("ErgoFlashCard [" + nue.name + "]");
        cfg = nue;
        coll = Collator.getInstance(cfg.locale);
        updateMenus();
        gotoThree();
    }
    
    private void refreshData() {
        readLines(cfg);
        qs = new Questions(data, db, cfg);
        updateMenus();
        gotoThree();
    }
    
    private void refreshDataLater() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                refreshData();
            }
        }, "refresh data");
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();
    }
    
    private synchronized void readLines(final Config cfg) {
        final JDialog dlg = new JDialog(this, "Reading...");
        BorderLayout layout = new BorderLayout();
        layout.setVgap(5);
        dlg.getContentPane().setLayout(layout);
        final JProgressBar prog = new JProgressBar();
        prog.setStringPainted(true);
        dlg.getContentPane().add(prog, BorderLayout.CENTER);
        dlg.getContentPane().add(new JLabel("Reading data files, please wait..."), BorderLayout.SOUTH);
        dlg.pack();
        dlg.show();
        try {
            Vector resV = new Vector(1000);
            Hashtable secs = new Hashtable(100);
            for (int i = 0; i < cfg.dataFiles.length; i++) {
                final int _i = i;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        prog.setValue(_i * 100 / cfg.dataFiles.length);
                        prog.setString(cfg.dataFiles[_i].getName());
                    }
                });
                DataLine[] lines = DataLine.read(cfg.dataFiles[i]);
                for (int j = 0; j < lines.length; j++) {
                    resV.addElement(lines[j]);
                    secs.put(lines[j].section, Boolean.TRUE);
                }
            }
            data = new DataLine[resV.size()];
            resV.copyInto(data);
            availableSections = new String[secs.size()];
            int pos = 0;
            Enumeration e = secs.keys();
            while (e.hasMoreElements())
                availableSections[pos++] = (String) e.nextElement();
            bubblesort(availableSections, LOCALE_SORTER);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                dlg.dispose();
            }
        });
    }
    
    interface Sorter {
        int compare(Object o1, Object o2);
    }
    static final Sorter STRING_SORTER = new Sorter() {
        public int compare(Object o1, Object o2) {
            if (o1 == o2) return 0;
            return ((String) o1).compareTo((String) o2);
        }
    };
    final Sorter LOCALE_SORTER = new Sorter() {
        public int compare(Object o1, Object o2) {
            if (o1 == o2) return 0;
            String s1 = (String) o1;
            String s2 = (String) o2;
            if (coll != null) {
                return coll.compare(s1, s2);
            } else {
                return s1.compareTo(s2);
            }
        }
    };
    static void bubblesort(Object[] objs, Sorter sorter) {
        // Now actually quicksort:
        quicksort(objs, 0, objs.length, sorter, null);
    }
    interface Reporter {
        void report(int pos);
    }
    static void bubblesort(Object[] objs, Sorter sorter, Reporter reporter) {
        if (reporter != null) reporter.report(0);
        quicksort(objs, 0, objs.length, sorter, reporter);
    }
    
    private static void quicksort(Object[] objs, int start, int end,
            Sorter sorter, Reporter reporter) {
        //System.err.println ("quicksort: start=" + start + " end=" + end);
        int len = end - start;
        if (len < 2) {
            // XXX this is not really right since it does not take into
            // account time spent splitting into partitions before the
            // recursion
            if (reporter != null) reporter.report(end);
            return;
        }
        double ran = Math.random();
        int splitPoint = start + (int) (ran * len);
        Object split = objs[splitPoint];
        //System.err.println ("splitPoint=" + splitPoint + " split=" + split + " ran=" + ran);
        int a = start, b = end;
        // XXX would be nicer to collect equal items in a special middle section
        // and not sort them recursively at all, but this should work anyway:
        boolean doFirst = false;
        while (a < b) {
            Object test = objs[a];
            //System.err.println ("a=" + a + " b=" + b + " test=" + test);
            int c = sorter.compare(test, split);
            if (c > 0) {
                //System.err.println ("swap right");
                b--;
                if (a != b) {
                    Object temp = test;
                    objs[a] = objs[b];
                    objs[b] = temp;
                }
            } else {
                //System.err.println ("advance");
                a++;
                if (! doFirst && c < 0) doFirst = true;
            }
        }
        //System.err.println ("done splitting: start=" + start + " end=" + end + " a=" + a + " b=" + b);
        if (doFirst) quicksort(objs, start, a, sorter, reporter);
        quicksort(objs, a, end, sorter, reporter);
        //System.err.println ("done quicksort: start=" + start + " end=" + end);
    }
    
    private JLabel totalScore;
    private JLabel thisScore;
    private Container inSides;
    private Container outSides;
    private Container sections;
    private JLabel timeTaken;
    private JLabel lastSeen;
    private JLabel totalQuestions;
    // 0 - asked; 1 - answered; 2 - intermission
    private int state;
    private long startTime;
    private long endTime;
    
    private Questions.Question currQuestion;
    
    private JButton answer;
    private JButton correct;
    private JButton incorrect;
    private JButton pause;
    private JLabel answerKeys;
    private JLabel correctKeys;
    private JLabel incorrectKeys;
    private JLabel pauseKeys;
    
    private Font principalFont;
    private Color[] principalColors;
    
    private void initLayout(Config cfg) {
        answer = new JButton("Answer");
        answer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                gotoOne();
            }
        });
        answer.setEnabled(false);
        ActionListener someAnswer = new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                if (ev.getSource() == correct) {
                    gotoTwo(1.0f);
                } else if (ev.getSource() == incorrect) {
                    gotoTwo(0.0f);
                } else {
                    throw new Error();
                }
            }
        };
        answerKeys = new JLabel();
        correct = new JButton("Correct");
        correct.addActionListener(someAnswer);
        correct.setEnabled(false);
        correctKeys = new JLabel();
        incorrect = new JButton("Incorrect");
        incorrect.addActionListener(someAnswer);
        incorrect.setEnabled(false);
        incorrectKeys = new JLabel();
        pause = new JButton("Pause");
        pause.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                gotoThree();
            }
        });
        pause.setEnabled(false);
        pauseKeys = new JLabel();
        principalFont = new Font(cfg.fontFamily, cfg.fontStyle, cfg.fontSize);
        // XXX add to config
        principalColors = new Color[] { Color.red, Color.black };
        totalScore = new JLabel();
        // XXX should be separate config for these
        totalScore.setForeground(principalColors[0]);
        thisScore = new JLabel();
        thisScore.setForeground(principalColors[0]);
        // XXX these two should have some way of displaying long lines,
        // e.g. a JTextArea with line wrapping enabled (?):
        inSides = /*Box.createHorizontalBox ()*/new JPanel();
        // Apparently only the Swing component revalidates itself
        // correctly, Box does not (always) since it is an AWT container.
        // This appeared as the tail end of an old question not being erased
        // when a new one was displayed. For answers & sections, these are
        // cleared between runs and this seems to take care of it.
        // JPanel + BoxLayout seems to be OK.
        inSides.setLayout(new BoxLayout(inSides, BoxLayout.Y_AXIS));
        outSides = Box.createVerticalBox();
        sections = Box.createVerticalBox();
        timeTaken = new JLabel();
        lastSeen = new JLabel();
        totalQuestions = new JLabel();
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.anchor = left.EAST;
        left.weighty = 1;
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.fill = right.HORIZONTAL;
        right.weightx = 1;
        //right.weighty = 0;
        right.anchor = right.WEST;
        left.insets = right.insets = new Insets(3, 3, 3, 3);
        left.ipadx = right.ipadx = 2;
        left.ipady = right.ipady = 2;
        //Container pane = getContentPane ();
        JPanel pane = new JPanel();
        getContentPane().add(new JScrollPane(pane), BorderLayout.CENTER);
        pane.setLayout(layout);
        left.gridy = right.gridy = 0;
        left.ipady = 10;
        pane.add(new JLabel("Question:"), left);
        left.ipady = 2;
        pane.add(inSides, right);
        left.gridy = right.gridy = 1;
        pane.add(answer, left);
        pane.add(answerKeys, right);
        left.gridy = right.gridy = 2;
        left.ipady = 10;
        pane.add(new JLabel("Answer:"), left);
        left.ipady = 2;
        // XXX better would be to actually wrap excessively long answers...
        // but not clear how to do this. JTextArea would be easy enough
        // but would not permit coloring (easily). JEditorPane would not set the
        // font very reliably due to a Swing bug.
        //pane.add (new JScrollPane (outSides), right);
        // Simply too ugly, forget it:
        pane.add(outSides, right);
        left.gridy = right.gridy = 3;
        pane.add(correct, left);
        pane.add(correctKeys, right);
        left.gridy = right.gridy = 4;
        pane.add(incorrect, left);
        pane.add(incorrectKeys, right);
        left.gridy = right.gridy = 5;
        pane.add(new JLabel("Section(s):"), left);
        pane.add(sections, right);
        left.gridy = right.gridy = 6;
        pane.add(new JLabel("Time taken / par:"), left);
        pane.add(timeTaken, right);
        left.gridy = right.gridy = 7;
        pane.add(new JLabel("Score (this q.):"), left);
        pane.add(thisScore, right);
        left.gridy = right.gridy = 8;
        pane.add(new JLabel("Last seen (this q.):"), left);
        pane.add(lastSeen, right);
        left.gridy = right.gridy = 9;
        pane.add(new JLabel("Total score:"), left);
        pane.add(totalScore, right);
        left.gridy = right.gridy = 10;
        pane.add(new JLabel("Total questions:"), left);
        pane.add(totalQuestions, right);
        left.gridy = right.gridy = 11;
        pane.add(pause, left);
        pane.add(pauseKeys, right);
        pane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                shutdown();
            }
        }, KeyStroke.getKeyStroke('q'), JComponent.WHEN_FOCUSED);
        pane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                if (state != 3) {
                    gotoThree();
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }, KeyStroke.getKeyStroke('p'), JComponent.WHEN_FOCUSED);
        ActionListener general = new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                String cmd = ev.getActionCommand();
                if (currQuestion == null && state < 3) {
                    System.err.println("No question?!");
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                switch (state) {
                    case 0:
                        gotoOne();
                        break;
                    case 1:
                        if (cmd.equals("n")) {
                                gotoTwo(0.0f);
                        } else if (cmd.equals("y")) {
                                gotoTwo(1.0f);
                        } else if (cmd.length() == 1 && cmd.charAt(0) >= '0' && cmd.charAt(0) <= '9') {
                            int level = Integer.parseInt(cmd);
                            gotoTwo(0.1f * level);
                        } else {
                            System.err.println("Weird command: " + cmd);
                            Toolkit.getDefaultToolkit().beep();
                        }
                        break;
                    case 2:
                        Toolkit.getDefaultToolkit().beep();
                        break;
                    case 3:
                        updateTotalQuestions();
                        gotoZero();
                        break;
                }
            }
        };
        pane.registerKeyboardAction(general, "n", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_FOCUSED);
        pane.registerKeyboardAction(general, "y", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
        pane.registerKeyboardAction(general, "y", KeyStroke.getKeyStroke('y'), JComponent.WHEN_FOCUSED);
        pane.registerKeyboardAction(general, "n", KeyStroke.getKeyStroke('n'), JComponent.WHEN_FOCUSED);
        for (char c = '1'; c <= '9'; c++) {
            pane.registerKeyboardAction(general, String.valueOf(c), KeyStroke.getKeyStroke(c), JComponent.WHEN_FOCUSED);
        }
        updateMenus();
        pack();
        repaint();
    }
    
    public Dimension getPreferredSize() {
        Dimension def = super.getPreferredSize();
        if (cfg == null) {
            return def;
        } else {
            return new Dimension((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.9), (int) (def.height * 1.5));
        }
    }
    
    private void updateTotalQuestions() {
        if (! qs.isPrepared()) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    qs.calcQuestions();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            updateTotalQuestions();
                        }
                    });
                }
            }, "update total questions delayed");
            t.setPriority(Thread.NORM_PRIORITY);
            t.start();
            return;
        }
        int unseen = qs.getCountUnseen();
        String text = Integer.toString(qs.getCount());
        if (unseen > 0)
            text += " (" + unseen + " unseen)";
        totalQuestions.setText(text);
    }
    
    private static final DateFormat PRETTY_TIME_FORMAT =
            new SimpleDateFormat("EEE, MMM F, yyyy");
    private static String prettyTime(long time) {
        if (time == 0 || time == Long.MIN_VALUE) {
            return "(never)";
        }
        long curr = System.currentTimeMillis();
        long diff = curr - time;
        if (diff < 0 || diff > 1000L * 60 * 60 * 24 * 31) {
            return PRETTY_TIME_FORMAT.format(new Date(time));
        } else if (diff < 1000L * 60) {
            return "" + (diff / 1000) + " seconds ago";
        } else if (diff < 1000L * 60 * 60) {
            return "" + (diff / 1000 / 60) + " minutes ago";
        } else if (diff < 1000L * 60 * 60 * 24) {
            return "" + (diff / 1000 / 60 / 60) + " hours ago";
        } else {
            return "" + (diff / 1000 / 60 / 60 / 24) + " days ago";
        }
    }
    
    class SectionItem extends JCheckBoxMenuItem implements ActionListener {
        private boolean incl;
        private String sec;
        SectionItem(String section) {
            super(section);
            this.sec = section;
            incl = false;
            for (int i = 0; i < cfg.sections.length; i++) {
                if (cfg.sections[i].equals(section)) {
                    incl = true;
                    break;
                }
            }
            setState(incl);
            addActionListener(this);
        }
        public void actionPerformed(ActionEvent ev) {
            String[] newsecs;
            if (incl) {
                // Remove it.
                newsecs = new String[cfg.sections.length - 1];
                int pos = 0;
                for (int i = 0; i < cfg.sections.length; i++) {
                    if (! cfg.sections[i].equals(sec)) {
                        newsecs[pos++] = cfg.sections[i];
                    }
                }
            } else {
                // Add it.
                newsecs = new String[cfg.sections.length + 1];
                System.arraycopy(cfg.sections, 0, newsecs, 0, cfg.sections.length);
                newsecs[cfg.sections.length] = sec;
            }
            Config nue = cfg.cloneConfig();
            nue.sections = newsecs;
            setConfigLater(nue);
        }
    }
    class DirectionItem extends JRadioButtonMenuItem implements ActionListener {
        int side;
        int how; // 0 = ignore 1 = ins 2 = outs
        DirectionItem(int side, int how) {
            super((new String[] {"Ignore", "Asked", "Answered"})[how]);
            setMnemonic((new int[] { KeyEvent.VK_I, KeyEvent.VK_K, KeyEvent.VK_W })[how]);
            this.side = side;
            this.how = how;
            int realHow = 0;
            for (int i = 0; i < cfg.inSides.length; i++)
                if (side == cfg.inSides[i])
                    realHow = 1;
            for (int i = 0; i < cfg.outSides.length; i++)
                if (side == cfg.outSides[i])
                    realHow = 2;
            boolean sel = (how == realHow);
            setSelected(sel);
            boolean enabled = (cfg.inSides.length != 1 || side != cfg.inSides[0]);
            setEnabled(enabled);
            if (enabled && ! sel) addActionListener(this);
        }
        public void actionPerformed(ActionEvent ev) {
            Config nue = cfg.cloneConfig();
            Hashtable currIns = new Hashtable(Math.max(1, cfg.inSides.length));
            for (int i = 0; i < cfg.inSides.length; i++)
                currIns.put(new Integer(cfg.inSides[i]), Boolean.TRUE);
            Hashtable currOuts = new Hashtable(Math.max(1, cfg.outSides.length));
            for (int i = 0; i < cfg.outSides.length; i++)
                currOuts.put(new Integer(cfg.outSides[i]), Boolean.TRUE);
            Vector insV = new Vector(cfg.sideNames.length);
            Vector outsV = new Vector(cfg.sideNames.length);
            for (int i = 0; i < cfg.sideNames.length; i++) {
                Integer I = new Integer(i);
                if (i == side) {
                    if (how == 1) {
                        insV.addElement(I);
                    } else if (how == 2) {
                        outsV.addElement(I);
                    }
                } else {
                    if (currIns.get(I) != null) {
                        insV.addElement(I);
                    } else if (currOuts.get(I) != null) {
                        outsV.addElement(I);
                    }
                }
            }
            nue.inSides = new int[insV.size()];
            for (int i = 0; i < insV.size(); i++) {
                nue.inSides[i] = ((Integer) insV.elementAt(i)).intValue();
            }
            nue.outSides = new int[outsV.size()];
            for (int i = 0; i < outsV.size(); i++) {
                nue.outSides[i] = ((Integer) outsV.elementAt(i)).intValue();
            }
            qs = new Questions(data, db, nue);
            setConfigLater(nue);
        }
    }
    class ViewPerfItem extends JMenuItem implements ActionListener {
        private int sort;
        ViewPerfItem(int sort) {
            super((new String[] {"by Section", "by Asked", "by Answered", "by Last Visit", "by Score"})[sort], (new int[] { KeyEvent.VK_S, KeyEvent.VK_K, KeyEvent.VK_W, KeyEvent.VK_V, KeyEvent.VK_R })[sort]);
            this.sort = sort;
            addActionListener(this);
        }
        public void actionPerformed(ActionEvent ev) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    final JDialog dlg = new JDialog(Main.this, "Analyzing...");
                    BorderLayout layout = new BorderLayout();
                    layout.setVgap(5);
                    dlg.getContentPane().setLayout(layout);
                    final JProgressBar prog = new JProgressBar();
                    final int[] lastVal = new int[] { 0 };
                    prog.setValue(0);
                    dlg.getContentPane().add(prog, BorderLayout.CENTER);
                    dlg.getContentPane().add
                            (new JLabel("Analyzing performance, please wait..."),
                            BorderLayout.SOUTH);
                    dlg.pack();
                    dlg.show();
                    final Questions.Question[] qqs = new Questions.Question[qs.getCount()];
                    for (int i = 0; i < qqs.length; i++)
                        qqs[i] = qs.getQuestion(i);
                    bubblesort(qqs, new Sorter() {
                        public int compare(Object o1, Object o2) {
                            Questions.Question q1 = (Questions.Question) o1;
                            Questions.Question q2 = (Questions.Question) o2;
                            switch (sort) {
                                case 0:
                                    return coll.compare(q1.sections[0], q2.sections[0]);
                                case 1:
                                    return coll.compare(q1.in[0], q2.in[0]);
                                case 2:
                                    return coll.compare(q1.out[0][0], q2.out[0][0]);
                                case 3:
                                    long t1 = q1.getTime();
                                    long t2 = q2.getTime();
                                    if (t1 < t2) {
                                        return -1;
                                    } else if (t1 == t2) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                case 4:
                                    float p1 = q1.getPerformance();
                                    float p2 = q2.getPerformance();
                                    if (p1 < p2) {
                                        return -1;
                                    } else if (p1 == p2) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                default:
                                    throw new Error();
                            }
                        }
                    }, new Reporter() {
                        public void report(int pos) {
                            final int val = pos * 50 / qqs.length;
                            if (val > lastVal[0]) {
                                lastVal[0] = val;
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        prog.setValue(val);
                                    }
                                });
                            }
                        }
                    });
                    String[][] data = new String[qqs.length][];
                    for (int i = 0; i < qqs.length; i++) {
                        final int val = 50 + i * 50 / qqs.length;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                prog.setValue(val);
                            }
                        });
                        data[i] = new String[] {
                            join(qqs[i].sections),
                                    join(qqs[i].in),
                                    join(qqs[i].out),
                                    prettyTime(qqs[i].getTime()),
                                    scoreFormat.format(100.0 * qqs[i].getPerformance())
                        };
                    }
                    JTable tab = new JTable(data, new String[] {
                        "Section(s)",
                                "Asked",
                                "Answered",
                                "Last Seen",
                                "Score"
                    });
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            dlg.dispose();
                        }
                    });
                    JDialog dialog = new JDialog(Main.this, "Performance View");
                    dialog.getContentPane().add(new JScrollPane(tab));
                    dialog.pack();
                    dialog.show();
                }
            }, "calculating performance table");
            t.setPriority(Thread.NORM_PRIORITY);
            t.start();
        }
    }
    
    private void doOpenConfig() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Open Config ...");
            chooser.addChoosableFileFilter(CONFIG_FILTER);
            chooser.setFileFilter(CONFIG_FILTER);
            if (chooserDir != null) chooser.setCurrentDirectory(chooserDir);
            if (chooser.showOpenDialog(Main.this) == JFileChooser.APPROVE_OPTION) {
                Config nue;
                if (cfg == null) {
                    nue = new Config();
                } else {
                    nue = cfg.cloneConfig();
                }
                nue.read(chooser.getSelectedFile());
                setConfigLater(nue);
            }
            chooserDir = chooser.getCurrentDirectory();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    private void doEditConfig() {
        final JDialog dlg = new JDialog(Main.this, "Edit Configuration") {
            public Dimension getPreferredSize() {
                Dimension def = super.getPreferredSize();
                return new Dimension((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.9), def.height);
            }
        };
        dlg.setModal(true);
        Container cont = dlg.getContentPane();
        cont.setLayout(new BorderLayout());
        final JEditorPane pane = new JEditorPane();
        pane.setContentType("text/plain");
        pane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        try {
            Reader rd = new InputStreamReader(new FileInputStream(cfg.origin), "ISO-8859-1");
            pane.getEditorKit().read(rd, pane.getDocument(), 0);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        } catch (BadLocationException ble) {
            ble.printStackTrace();
            return;
        }
        cont.add(new JScrollPane(pane), BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("Save and Apply");
        okButton.setMnemonic(KeyEvent.VK_S);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    Writer wr = new OutputStreamWriter(new FileOutputStream(cfg.origin), "ISO-8859-1");
                    pane.write(wr);
                    wr.close();
                    Config nue = cfg.cloneConfig();
                    nue.read(cfg.origin);
                    setConfigLater(nue);
                    dlg.dispose();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        });
        buttons.add(okButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                dlg.dispose();
            }
        });
        buttons.add(cancelButton);
        cont.add(buttons, BorderLayout.SOUTH);
        dlg.pack();
        dlg.show();
    }
    
    private void doSaveAs() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Config As ...");
            chooser.addChoosableFileFilter(CONFIG_FILTER);
            chooser.setFileFilter(CONFIG_FILTER);
            if (chooserDir != null) chooser.setCurrentDirectory(chooserDir);
            if (cfg.origin != null) chooser.setSelectedFile(cfg.origin);
            if (chooser.showSaveDialog(Main.this) == JFileChooser.APPROVE_OPTION) {
                cfg.write(chooser.getSelectedFile());
            }
            chooserDir = chooser.getCurrentDirectory();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    private void updateMenus() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        JMenuItem open = new JMenuItem("Open ...", KeyEvent.VK_O);
        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                doOpenConfig();
            }
        });
        file.add(open);
        if (cfg != null) {
            if (cfg.origin != null) {
                JMenuItem edit = new JMenuItem("Edit Config ...", KeyEvent.VK_E);
                edit.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                        doEditConfig();
                    }
                });
                file.add(edit);
            }
            /*
            JMenuItem save = new JMenuItem("Save", KeyEvent.VK_S);
            save.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    try {
                        cfg.save();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            });
            file.add(save);
             */
            JMenuItem saveAs = new JMenuItem("Save As ...", KeyEvent.VK_A);
            saveAs.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    doSaveAs();
                }
            });
            file.add(saveAs);
            JMenuItem reload = new JMenuItem("Reload Data Files", KeyEvent.VK_R);
            reload.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    refreshDataLater();
                }
            });
            file.add(reload);
        }
        if (qs != null) {
            JMenu perf = new JMenu("View Performance");
            perf.setMnemonic(KeyEvent.VK_V);
            for (int i = 0; i < 5; i++)
                perf.add(new ViewPerfItem(i));
            file.add(perf);
        }
        JMenuItem exit = new JMenuItem("Exit", KeyEvent.VK_X);
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                shutdown();
            }
        });
        file.add(exit);
        bar.add(file);
        if (cfg != null && data != null) {
            JMenu sections = new JMenu("Sections");
            sections.setMnemonic(KeyEvent.VK_S);
            if (availableSections.length < 20) {
                for (int i = 0; i < availableSections.length; i++)
                    sections.add(new SectionItem(availableSections[i]));
            } else {
                int g = 0;
                int s = 0;
                JMenu menu = null;
                for (int i = 0; i < availableSections.length; i++) {
                    if (s == 15) {
                        sections.add(menu);
                        menu = null;
                    }
                    if (menu == null) {
                        menu = new JMenu("Group #" + ++g);
                        s = 0;
                    }
                    menu.add(new SectionItem(availableSections[i]));
                    s++;
                }
                if (menu != null && s > 0) sections.add(menu);
            }
            bar.add(sections);
        }
        if (cfg != null) {
            JMenu dirs = new JMenu("Direction");
            dirs.setMnemonic(KeyEvent.VK_D);
            for (int i = 0; i < cfg.sideNames.length; i++) {
                JMenu sub = new JMenu(cfg.sideNames[i]);
                sub.add(new DirectionItem(i, 1));
                sub.add(new DirectionItem(i, 2));
                sub.add(new DirectionItem(i, 0));
                dirs.add(sub);
            }
            bar.add(dirs);
        }
        setJMenuBar(bar);
        pack();
        repaint();
    }
    
    private static String join(String[] things) {
        return join(things, " - ");
    }
    
    private static String join(String[] things, String sep) {
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < things.length; i++) {
            if (i > 0) res.append(sep);
            res.append(things[i]);
        }
        return res.toString();
    }
    
    private static String join(String[][] things) {
        return join(things, " ; ", " - ");
    }
    
    private static String join(String[][] things, String sep1, String sep2) {
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < things.length; i++) {
            if (i > 0) res.append(sep1);
            for (int j = 0; j < things[i].length; j++) {
                if (j > 0) res.append(sep2);
                res.append(things[i][j]);
            }
        }
        return res.toString();
    }
    
    private static DecimalFormat scoreFormat = new DecimalFormat("0.0");
    private static DecimalFormat timeFormat = new DecimalFormat("0.0");
    
    // STATES:
    // 0 - showing question
    // 1 - showing answer
    // 2 - showing updated score
    // 3 - paused
    // TRANSITIONS:
    // 0 -> 1: user presses some key
    // 1 -> 2: user rates him/herself
    // 2 -> 0: a second elapses
    // 0..2 -> 3: pause
    // 3 -> 0: unpause
    
    private void gotoZero() {
        if (cfg == null) return;
        if (! qs.isPrepared()) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    qs.calcQuestions();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            gotoZero();
                        }
                    });
                }
            }, "goto zero delayed");
            t.setPriority(Thread.NORM_PRIORITY);
            t.start();
            return;
        }
        state = 0;
        thisScore.setText("");
        outSides.removeAll();
        sections.removeAll();
        timeTaken.setText("");
        lastSeen.setText("");
        correct.setEnabled(false);
        correctKeys.setText("");
        incorrect.setEnabled(false);
        incorrectKeys.setText("");
        pause.setEnabled(true);
        pauseKeys.setText("(keys: P)");
        currQuestion = qs.getNextQuestion();
        if (currQuestion != null) {
            inSides.removeAll();
            for (int i = 0; i < currQuestion.in.length; i++) {
                if (i > 0) inSides.add(Box.createHorizontalStrut(20));
                JLabel lab = new JLabel(currQuestion.in[i]);
                lab.setFont(principalFont);
                lab.setForeground(principalColors[i % principalColors.length]);
                inSides.add(lab);
            }
            totalScore.setText(scoreFormat.format(100.0 *
                    qs.getAveragePerformance()));
            answer.setEnabled(true);
            answerKeys.setText("(any key: Enter / Space / Y / N)");
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    startTime = System.currentTimeMillis();
                }
            });
        } else {
            inSides.removeAll();
            inSides.add(new JLabel("(no questions configured)"));
            totalScore.setText("");
            answer.setEnabled(false);
            answerKeys.setText("");
        }
    }
    
    private void gotoOne() {
        if (cfg == null) return;
        state = 1;
        endTime = System.currentTimeMillis();
        thisScore.setText
                (scoreFormat.format(100.0 * currQuestion.getPerformance()));
        outSides.removeAll();
        for (int i = 0; i < currQuestion.out.length; i++) {
            Box answer = Box.createHorizontalBox();
            for (int j = 0; j < currQuestion.out[i].length; j++) {
                if (j > 0) answer.add(Box.createHorizontalStrut(20));
                JLabel lab = new JLabel(currQuestion.out[i][j]);
                lab.setFont(principalFont);
                lab.setForeground(principalColors[j % principalColors.length]);
                lab.setHorizontalAlignment(SwingConstants.LEFT);
                answer.add(lab);
            }
            answer.add(Box.createHorizontalGlue());
            outSides.add(answer);
        }
        sections.removeAll();
        for (int i = 0; i < currQuestion.sections.length; i++) {
            JLabel label = new JLabel(currQuestion.sections[i]);
            sections.add(label);
        }
        timeTaken.setText(timeFormat.format(.001 * (endTime - startTime)) + " / " +
                timeFormat.format(.001 * currQuestion.getGracePeriod()));
        lastSeen.setText(currQuestion.isEverBeenSeen() ?
            prettyTime(currQuestion.getTime()) :
            "(first encounter)");
        answer.setEnabled(false);
        answerKeys.setText("");
        correct.setEnabled(true);
        correctKeys.setText("(keys: Y / Enter; partial: 1 ... 9)");
        incorrect.setEnabled(true);
        incorrectKeys.setText("(keys: N / Spc)");
    }
    
    private static int gcCounter = 0;
    private void gotoTwo(float perf) {
        if (cfg == null) return;
        state = 2;
        currQuestion.updatePerformance(endTime - startTime, perf);
        totalScore.setText(scoreFormat.format(100.0 * qs.getAveragePerformance()));
        thisScore.setText(scoreFormat.format(100.0 * currQuestion.getPerformance()));
        answer.setEnabled(false);
        correct.setEnabled(false);
        incorrect.setEnabled(false);
        updateTotalQuestions();
        Timer timer = new Timer(cfg.intermission, new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                if (state == 2)
                    gotoZero();
            }
        });
        timer.setRepeats(false);
        timer.start();
        if (++gcCounter % 10 == 0) {
            //System.err.println ("Will GC...");
            System.gc();
        }
    }
    
    private void gotoThree() {
        state = 3;
        totalQuestions.setText(cfg.sections.length == 0 ? "0" : "(Undetermined)");
        thisScore.setText("");
        outSides.removeAll();
        sections.removeAll();
        timeTaken.setText("");
        lastSeen.setText("");
        answer.setEnabled(false);
        answerKeys.setText("");
        correct.setEnabled(false);
        correctKeys.setText("");
        incorrect.setEnabled(false);
        incorrectKeys.setText("");
        pause.setEnabled(false);
        pauseKeys.setText("");
        totalScore.setText("");
        inSides.removeAll();
        inSides.add(new JLabel("(paused - hit any key: Enter / Space / Y / N to start)"));
        // XXX why is this needed?
        pack();
        repaint();
    }
    
}
