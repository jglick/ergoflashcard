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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class DataLine {
    
    /** the section name this entry is in */
    public final String section;
    
    /** a list of sides present in the entry */
    public final String[] sides;
    
    private DataLine(String section, String[] sides) {
        this.section = section;
        this.sides = sides;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer(section);
        for (int i = 0; i < sides.length; i++) {
            buf.append(" - ");
            buf.append(sides[i]);
        }
        return buf.toString();
    }
    
    /** Read data from a ergoflashcard-format file.
     * Each line should have one entry.
     * Slashes are recognized and will produce separate entries.
     * Produces a list of entries.
     * @param f the file to read
     * @param the resulting entries
     */
    public static DataLine[] read(File f) throws IOException {
        InputStream is = new FileInputStream(f);
        try {
            Reader r = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(r);
            String line;
            List entries = new ArrayList(250);
            String currSec = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.length() == 0) continue;
                if (line.indexOf(" - ") == -1) {
                    currSec = line;
                    continue;
                }
                if (currSec == null)
                    throw new IOException("Must set a section before the first line");
                List fields = new ArrayList(4);
                List sectionAlts = split(currSec, " / ");
                fields.add(sectionAlts);
                int product = sectionAlts.size();
                Iterator it = split(line, " - ").iterator();
                while (it.hasNext()) {
                    List sideAlts = split((String)it.next(), " / ");
                    fields.add(sideAlts);
                    product *= sideAlts.size();
                }
                if (fields.size() < 2 || product == 0)
                    throw new IOException("Bad: " + line);
                for (int i = 0; i < product; i++) {
                    String section = null;
                    String[] sides = new String[fields.size() - 1];
                    int j = i;
                    for (int k = 0; k < fields.size(); k++) {
                        List alts = (List)fields.get(k);
                        String alt = ((String)alts.get(j % alts.size())).trim();
                        if (k == 0)
                            section = alt;
                        else
                            sides[k - 1] = alt;
                        j /= alts.size();
                    }
                    if (j != 0) throw new Error("miscalc'd alts");
                    entries.add(new DataLine(section, sides));
                }
            }
            return (DataLine[])entries.toArray(new DataLine[entries.size()]);
        } finally {
            is.close();
        }
    }
    
    private static List split(String text, String sep) {
        int i = text.indexOf(sep);
        if (i == -1) {
            return Collections.singletonList(text);
        }
        List l = new ArrayList(2);
        l.add(text.substring(0, i));
        int j;
        while ((j = text.indexOf(sep, i + sep.length())) != -1) {
            l.add(text.substring(i + sep.length(), j));
            i = j;
        }
        l.add(text.substring(i + sep.length()));
        return l;
    }
    
}
