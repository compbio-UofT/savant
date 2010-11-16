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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author mfiume
 */
public class GeneralSettingsSection extends Section {
    private static final Log LOG = LogFactory.getLog(PersistentSettings.class);

    JCheckBox checkversion_cb;
    JCheckBox collectrstats_cb;

    @Override
    public String getSectionName() {
        return "General Settings";
    }

    @Override
    public Icon getSectionIcon() {
        return null;
    }

    @Override
    public void lazyInitialize() {
        setLayout(new BorderLayout());

        add(SettingsDialog.getHeader(getTitle()), BorderLayout.BEFORE_FIRST_LINE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        checkversion_cb = new JCheckBox("Check version on startup");
        checkversion_cb.setSelected(BrowserSettings.checkVersionOnStartup);
        checkversion_cb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableApplyButton();
            }
        });
        panel.add(checkversion_cb);


        collectrstats_cb = new JCheckBox("Collect anonymous statistics about usage");
        collectrstats_cb.setSelected(BrowserSettings.collectAnonymousStats);
        collectrstats_cb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableApplyButton();
            }
        });
        panel.add(collectrstats_cb);

        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void applyChanges() {
        // Only save anything if this panel has gone through lazy initialization.
        if (checkversion_cb != null) {
            BrowserSettings.checkVersionOnStartup = this.checkversion_cb.isSelected();
            BrowserSettings.collectAnonymousStats = this.collectrstats_cb.isSelected();
            
            try {
                PersistentSettings.getInstance().store();
            } catch (IOException iox) {
                LOG.error("Unable to save general settings.", iox);
            }
        }
    }
}
