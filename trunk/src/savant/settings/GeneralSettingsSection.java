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

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JCheckBox;

import savant.util.IOUtils;


/**
 *
 * @author mfiume
 */
public class GeneralSettingsSection extends Section {
    JCheckBox checkversion_cb;
    JCheckBox collectrstats_cb;
    private JCheckBox startpage_cb;

    @Override
    public String getTitle() {
        return "General Settings";
    }

    @Override
    public void lazyInitialize() {
        add(SettingsDialog.getHeader(getTitle()), getFullRowConstraints());

        checkversion_cb = new JCheckBox("Check version on startup");
        checkversion_cb.setSelected(BrowserSettings.getCheckVersionOnStartup());
        checkversion_cb.addActionListener(enablingActionListener);
        add(checkversion_cb, getFullRowConstraints());

        startpage_cb = new JCheckBox("Show Start Page");
        startpage_cb.setSelected(BrowserSettings.getShowStartPage());
        startpage_cb.addActionListener(enablingActionListener);
        add(startpage_cb, getFullRowConstraints());

        collectrstats_cb = new JCheckBox("Collect anonymous statistics about usage");
        collectrstats_cb.setSelected(BrowserSettings.getCollectAnonymousUsage());
        collectrstats_cb.addActionListener(enablingActionListener);
        add(collectrstats_cb, getFullRowConstraints());

        JButton clearTmpButt = new JButton("Clear Temporary Files");
        clearTmpButt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IOUtils.removeTmpFiles();
            }
        });
        GridBagConstraints gbc = getFullRowConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 1.0;
        add(clearTmpButt, gbc);
    }

    @Override
    public void applyChanges() {
        // Only save anything if this panel has gone through lazy initialization.
        if (checkversion_cb != null) {
            BrowserSettings.setCheckVersionOnStartup(this.checkversion_cb.isSelected());
            BrowserSettings.setShowStartPage(this.startpage_cb.isSelected());
            BrowserSettings.setCollectAnonymousUsage(this.collectrstats_cb.isSelected());
            
            try {
                PersistentSettings.getInstance().store();
            } catch (IOException iox) {
                LOG.error("Unable to save general settings.", iox);
            }
        }
    }
}
