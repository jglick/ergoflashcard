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

import java.awt.Component;
import java.awt.Frame;
import java.net.URL;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.help.JHelp;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

class Help {
    
    private Help() {}
    
    public static void showHelp(Frame parent) {
        // XXX there is probably a better way to do this (using DefaultHelpBroker etc.)... get spec and find out what
        URL url = Help.class.getResource("help/helpset.xml");
        assert url != null;
        Component jhelp;
        try {
            jhelp = new JHelp(new HelpSet(Help.class.getClassLoader(), url));
        } catch (HelpSetException e) {
            e.printStackTrace();
            return;
        }
        // XXX should find an existing dialog and front it
        JDialog dlg = new JDialog(parent, "Help", false);
        dlg.getContentPane().add(jhelp);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.pack();
        dlg.setVisible(true);
    }
    
}
