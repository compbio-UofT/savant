/*
 *    Copyright 2010-2011 University of Toronto
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
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import com.jidesoft.dialog.AbstractDialogPage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Base class for all sections on the SettingsDialog.
 *
 * @author mfiume, tarkvara
 */
public abstract class Section extends AbstractDialogPage {
    protected final Log LOG = LogFactory.getLog(Section.class);

    /**
     * ActionListener which enables the Apply button when something has been typed.
     */
    protected ActionListener enablingActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            enableApplyButton();
        }
    };

    /**
     * KeyListener which enables the Apply button when something has been typed.
     */
    protected KeyAdapter enablingKeyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            enableApplyButton();
        }
    };


    public Section() {
        super("","",null);
        setLayout(new GridBagLayout());
    }

    public abstract void applyChanges();

    public void enableApplyButton() {
        fireButtonEvent(com.jidesoft.dialog.ButtonEvent.ENABLE_BUTTON, com.jidesoft.dialog.MultiplePageDialog.APPLY);
    }

    public void disableApplyButton() {
        fireButtonEvent(com.jidesoft.dialog.ButtonEvent.DISABLE_BUTTON, com.jidesoft.dialog.MultiplePageDialog.APPLY);
    }

    public void populate(){};

    public GridBagConstraints getFullRowConstraints() {
        return new GridBagConstraints(0, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0);
    }
}
