/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.settings;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

import savant.util.RemoteFileCache;

/**
 *
 * @author mfiume
 */
public class RemoteFilesSettingsSection extends Section {

    private JTextField directoryInput;
    private JTextField buffSizeInput;
    private String buffSize;
    private File cacheDir;
    JCheckBox enableCaching_cb;

    @Override
    public String getTitle() {
        return "Remote Files";
    }

    @Override
    public void lazyInitialize() {
        GridBagConstraints gbc = getFullRowConstraints();
        add(SettingsDialog.getHeader(getTitle()), gbc);

        //ENABLE CACHING///////////////////////////////////
        enableCaching_cb = new JCheckBox("Enable remote file caching ");
        enableCaching_cb.setSelected(BrowserSettings.getCachingEnabled());
        enableCaching_cb.addActionListener(enablingActionListener);
        add(enableCaching_cb, gbc);

        //CACHE DIRECTORY//////////////////////////////////

        JLabel directoryLabel = new JLabel("Select the folder to store cached files: ");
        add(directoryLabel, gbc);

        directoryInput = new JTextField();
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets.bottom = 12;
        add(directoryInput, gbc);

        JButton directoryBrowse = new JButton("Browse...");
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        add(directoryBrowse, gbc);

        //initial directory
        cacheDir = DirectorySettings.getCacheDirectory();
        directoryInput.setText(cacheDir.getAbsolutePath());

        //enable apply button if text changed
        directoryInput.addKeyListener(enablingKeyListener);

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

        //BUFFER SIZE//////////////////////////////////

        JLabel buffSizeLabel = new JLabel("Select buffer size (bytes): ");
        gbc = getFullRowConstraints();
        add(buffSizeLabel, gbc);

        buffSizeInput = new JTextField();
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets.bottom = 12;
        add(buffSizeInput, gbc);

        JButton defaultSizeButton = new JButton("Default");
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        add(defaultSizeButton, gbc);

        //initial buffer size
        buffSize = String.valueOf(BrowserSettings.getRemoteBufferSize());
        buffSizeInput.setText(buffSize);

        //enable apply button if text changed
        buffSizeInput.addKeyListener(enablingKeyListener);

        //default button action
        defaultSizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buffSizeInput.setText(String.valueOf(BrowserSettings.DEFAULT_BUFFER_SIZE));
                enableApplyButton();
            }
        });

        JButton clearButton = new JButton("Clear remote file cache");
        gbc = getFullRowConstraints();
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        add(clearButton, gbc);

        //clear action
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteFileCache.clearCache();
            }
        });
    }

    @Override
    public void applyChanges() {
        // Only save anything if this panel has gone through lazy initialization.
        if (directoryInput != null) {
            cacheDir = new File(directoryInput.getText());
            DirectorySettings.setCacheDirectory(cacheDir);

            buffSize = buffSizeInput.getText();
            int newVal;
            try {
                newVal = Integer.parseInt(buffSize);
            } catch(NumberFormatException e){
                newVal = BrowserSettings.DEFAULT_BUFFER_SIZE;
            }
            newVal = Math.max(newVal, 1024);
            buffSizeInput.setText(String.valueOf(newVal));
            BrowserSettings.setRemoteBufferSize(newVal);

            BrowserSettings.setCachingEnabled(this.enableCaching_cb.isSelected());
            
            try {
                PersistentSettings.getInstance().store();
            } catch (IOException iox) {
                LOG.error("Unable to save remote file settings.", iox);
            }
        }
    }
}
