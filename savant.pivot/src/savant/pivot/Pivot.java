/*
 *    Copyright 2010 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/*
 * DataTab.java
 * Created on Feb 25, 2010
 */

package savant.pivot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import org.java.plugin.Plugin;
import savant.plugin.PluginAdapter;

import javax.swing.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import savant.plugin.GUIPlugin;
import savant.swing.component.PathField;
import savant.view.swing.Savant;

public class Pivot extends Plugin implements GUIPlugin {

    static PathField f;

    public void init(JPanel tablePanel, PluginAdapter pluginAdapter) {
        f = new PathField(JFileChooser.OPEN_DIALOG);
        JButton b = new JButton("Open Excel File");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    readFile();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(Savant.getInstance(), ex.getMessage());
                }
            }
        });
        tablePanel.add(f);
        tablePanel.add(b);
    }

    /**
	 * creates an {@link HSSFWorkbook} the specified OS filename.
	 */
    private static HSSFWorkbook readFile() throws IOException {
        return new HSSFWorkbook(new FileInputStream(f.getPath()));
    }

    protected void doStart() throws Exception {

    }

    protected void doStop() throws Exception {

    }

    public String getTitle() {
        return "SQL DB Browser";
    }
    
}
