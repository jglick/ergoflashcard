package quiz;

import java.io.*;
import java.util.*;

public final class DataLine {

  /** the section name this entry is in */
  public final String section;

  /** a list of sides present in the entry */
  public final String[] sides;

  private DataLine (String section, String[] sides) {
    this.section = section;
    this.sides = sides;
  }

  public String toString () {
    StringBuffer buf = new StringBuffer (section);
    for (int i = 0; i < sides.length; i++) {
      buf.append (':');
      buf.append (sides[i]);
    }
    return buf.toString ();
  }

  /** Read data from a quiz-format file.
   * Each line should have one entry.
   * Slashes are recognized and will produce separate entries.
   * Produces a list of entries.
   * @param f the file to read
   * @param enc the encoding to use, or null
   * @param the resulting entries
   */
  public static DataLine[] read (File f, String enc) throws IOException {
    InputStream is = new FileInputStream (f);
    try {
      Reader r;
      if (enc == null)
	r = new InputStreamReader (is);
      else
	r = new InputStreamReader (is, enc);
      BufferedReader br = new BufferedReader (r);
      String line;
      Vector entries = new Vector (250);
      String currSec = null;
      while ((line = br.readLine ()) != null) {
	if (line.startsWith ("#") || line.length () == 0) continue;
	// XXX handle some whitespace on the line
	if (line.indexOf (":") == -1) {
	  currSec = line;
	  continue;
	}
	if (currSec == null)
	  throw new IOException ("Must set a section before the first line");
	Vector fields = new Vector (4);
	// XXX handle backslashed metachars
	StringTokenizer colonTok =
	  new StringTokenizer (currSec + ":" + line, ":");
	int product = 1;
	while (colonTok.hasMoreTokens ()) {
	  String field = colonTok.nextToken ();
	  StringTokenizer slashTok = new StringTokenizer (field, "/");
	  Vector alts = new Vector (1);
	  while (slashTok.hasMoreTokens ()) {
	    // XXX backslashes would be better, this is temporary:
	    String alt = slashTok.nextToken ().replace ('|', ':');
	    alts.addElement (alt);
	  }
	  product *= alts.size ();
	  fields.addElement (alts);
	}
	if (fields.size () < 2 || product == 0)
	  throw new IOException ("Bad: " + line);
	for (int i = 0; i < product; i++) {
	  String section = null;
	  String[] sides = new String[fields.size () - 1];
	  int j = i;
	  for (int k = 0; k < fields.size (); k++) {
	    Vector alts = (Vector) fields.elementAt (k);
	    String alt = (String) alts.elementAt (j % alts.size ());
	    if (k == 0)
	      section = alt;
	    else
	      sides[k - 1] = alt;
	    j /= alts.size ();
	  }
	  if (j != 0) throw new Error ("miscalc'd alts");
	  entries.addElement (new DataLine (section, sides));
	}
      }
      DataLine[] toRet = new DataLine[entries.size ()];
      entries.copyInto (toRet);
      return toRet;
    } finally {
      is.close ();
    }
  }

}
