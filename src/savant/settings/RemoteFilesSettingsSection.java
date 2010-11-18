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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.util.RemoteFileCache;

/**
 *
 * @author mfiume
 */
public class RemoteFilesSettingsSection extends Section {
    private static final Log LOG = LogFactory.getLog(PersistentSettings.class);

    private JTextField directoryInput;
    private String directoryPath;
    JCheckBox enableCaching_cb;

    @Override
    public String getSectionName() {
        return "Remote Files";
    }

    @Override
    public Icon getSectionIcon() {
        return null;
    }

    @Override
    public void lazyInitialize() {
        setLayout(new BorderLayout());
        add(SettingsDialog.getHeader(getTitle()), BorderLayout.BEFORE_FIRST_LINE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        //ENABLE CACHING///////////////////////////////////

        enableCaching_cb = new JCheckBox("Enable remote file caching ");
        enableCaching_cb.setSelected(BrowserSettings.getCachingEnabled());
        enableCaching_cb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableApplyButton();
            }
        });
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0;
        panel.add(enableCaching_cb, c);

        //add separator
        JPanel sep1 = new JPanel();
        sep1.setPreferredSize(new Dimension(10,10));
        sep1.setSize(new Dimension(10,10));
        c.gridy = 1;
        panel.add(sep1, c);

        //CACHE DIRECTORY//////////////////////////////////

        JLabel directoryLabel = new JLabel("Select the folder to store cached files: ");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0;
        panel.add(directoryLabel, c);

        directoryInput = new JTextField();
        directoryInput.setPreferredSize(new Dimension(20, 20));
        directoryInput.setSize(new Dimension(20, 20));
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 1.0;
        panel.add(directoryInput, c);

        JButton directoryBrowse = new JButton("Browse...");
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 0;
        panel.add(directoryBrowse, c);

        JPanel spacer1 = new JPanel();
        spacer1.setPreferredSize(new Dimension(20,20));
        c.fill = GridBagConstraints.VERTICAL;
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0;
        panel.add(spacer1, c);

        //initial directory
        directoryPath = DirectorySettings.getCacheDirectory();
        directoryInput.setText(directoryPath);

        //enable apply button if text changed
        directoryInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                enableApplyButton();
            }
        });

        //browse action
        directoryBrowse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fc.showOpenDialog(RemoteFilesSettingsSection.this);
                if (returnVal == -1){
                    return;
                }
                directoryInput.setText(fc.getSelectedFile().getAbsolutePath());
                enableApplyButton();
            }
        });

        //CLEAR CACHE///////////////////////////////////

        JButton clearButton = new JButton("Clear remote file cache");
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 9;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0;
        panel.add(clearButton, c);

        //clear action
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteFileCache.clearCache();
            }
        });

        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void applyChanges() {
        // Only save anything if this panel has gone through lazy initialization.
        if (directoryInput != null) {
            directoryPath = directoryInput.getText();
            DirectorySettings.setCacheDirectory(directoryPath);

            BrowserSettings.setCachingEnabled(this.enableCaching_cb.isSelected());
            
            try {
                PersistentSettings.getInstance().store();
            } catch (IOException iox) {
                LOG.error("Unable to save directory settings.", iox);
            }
        }
    }
}
