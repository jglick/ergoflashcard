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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.StringTokenizer;
import java.util.Vector;

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
            buf.append(':');
            buf.append(sides[i]);
        }
        return buf.toString();
    }
    
    /** Read data from a quiz-format file.
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
            Vector entries = new Vector(250);
            String currSec = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.length() == 0) continue;
                // XXX handle some whitespace on the line
                if (line.indexOf(":") == -1) {
                    currSec = line;
                    continue;
                }
                if (currSec == null) {
                    throw new IOException("Must set a section before the first line");
                }
                Vector fields = new Vector(4);
                // XXX handle backslashed metachars
                StringTokenizer colonTok =
                        new StringTokenizer(currSec + ":" + line, ":");
                int product = 1;
                while (colonTok.hasMoreTokens()) {
                    String field = colonTok.nextToken();
                    StringTokenizer slashTok = new StringTokenizer(field, "/");
                    Vector alts = new Vector(1);
                    while (slashTok.hasMoreTokens()) {
                        // XXX backslashes would be better, this is temporary:
                        String alt = slashTok.nextToken().replace('|', ':');
                        alts.addElement(alt);
                    }
                    product *= alts.size();
                    fields.addElement(alts);
                }
                if (fields.size() < 2 || product == 0)
                    throw new IOException("Bad: " + line);
                for (int i = 0; i < product; i++) {
                    String section = null;
                    String[] sides = new String[fields.size() - 1];
                    int j = i;
                    for (int k = 0; k < fields.size(); k++) {
                        Vector alts = (Vector) fields.elementAt(k);
                        String alt = (String) alts.elementAt(j % alts.size());
                        if (k == 0) {
                            section = alt;
                        } else {
                            sides[k - 1] = alt;
                        }
                        j /= alts.size();
                    }
                    if (j != 0) throw new Error("miscalc'd alts");
                    entries.addElement(new DataLine(section, sides));
                }
            }
            DataLine[] toRet = new DataLine[entries.size()];
            entries.copyInto(toRet);
            return toRet;
        } finally {
            is.close();
        }
    }
    
}
