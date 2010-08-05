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

package savant.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import savant.model.data.interval.BAMIndexCache;

/**
 *
 * @author mfiume
 */
public class TemporaryFilesSettingsSection extends Section {

    private JTextField formatInput;
    private JTextField pluginsInput;
    private String formatPath;
    private String pluginsPath;

    @Override
    public String getSectionName() {
        return "Temporary Files";
    }

    public Icon getSectionIcon() {
        return null;
    }

    private TemporaryFilesSettingsSection getThis(){
        return this;
    }

    @Override
    public void lazyInitialize() {

        setLayout(new BorderLayout());
        add(SettingsDialog.getHeader(getTitle()), BorderLayout.BEFORE_FIRST_LINE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        //FORMATTING//////////////////////////////////

        JLabel formatLabel = new JLabel("Select the folder to store temporary formatted files: ");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0;
        panel.add(formatLabel, c);

        formatInput = new JTextField();
        formatInput.setPreferredSize(new Dimension(20, 20));
        formatInput.setSize(new Dimension(20, 20));
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 1.0;
        panel.add(formatInput, c);

        JButton formatBrowse = new JButton("Browse...");
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0;
        panel.add(formatBrowse, c);

        JPanel spacer1 = new JPanel();
        spacer1.setPreferredSize(new Dimension(20,20));
        c.fill = GridBagConstraints.VERTICAL;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0;
        panel.add(spacer1, c);

        //initial directory
        formatPath = DirectorySettings.getFormatDirectory();
        formatInput.setText(formatPath);

        //enable apply button if text changed
        formatInput.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
                enableApplyButton();
            }
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
        });

        //browse action
        formatBrowse.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {
                final JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fc.showOpenDialog(getThis());
                if(returnVal == -1){
                    return;
                }
                formatInput.setText(fc.getSelectedFile().getAbsolutePath());
                enableApplyButton();
            }
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });

        
        //PLUGINS/////////////////////////////////////

        JLabel pluginsLabel = new JLabel("Select the folder to store plugin files: ");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0;
        panel.add(pluginsLabel, c);

        pluginsInput = new JTextField();
        pluginsInput.setPreferredSize(new Dimension(20, 20));
        pluginsInput.setSize(new Dimension(20, 20));
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 1.0;
        panel.add(pluginsInput, c);

        JButton pluginsBrowse = new JButton("Browse...");
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 0;
        panel.add(pluginsBrowse, c);

        JPanel spacer2 = new JPanel();
        spacer2.setPreferredSize(new Dimension(20,20));
        c.fill = GridBagConstraints.VERTICAL;
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0;
        panel.add(spacer2, c);

        //initial directory
        pluginsPath = DirectorySettings.getPluginsDirectory();
        pluginsInput.setText(pluginsPath);

        //enable apply button if text changed
        pluginsInput.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
                enableApplyButton();
            }
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
        });

        //browse action
        pluginsBrowse.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {
                final JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fc.showOpenDialog(getThis());
                if(returnVal == -1){
                    return;
                }
                pluginsInput.setText(fc.getSelectedFile().getAbsolutePath());
                enableApplyButton();
            }
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });

        //CLEAR CACHE///////////////////////////////////

        JButton clearButton = new JButton("Clear remote BAM index cache");
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0;
        panel.add(clearButton, c);

        //clear action
        clearButton.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {
                BAMIndexCache.getInstance().clearCache();
            }
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });

        this.add(panel, BorderLayout.CENTER);
    }

    public void applyChanges() {

        formatPath = formatInput.getText();
        DirectorySettings.setFormatDirectory(formatPath);

        pluginsPath = pluginsInput.getText();
        DirectorySettings.setPluginsDirectory(pluginsPath);

    }
}
