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

package savant.util.swing;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import savant.view.icon.SavantIconFactory;


/**
 * Dorky little class which puts up a progress-bar.  Used when it is taking a while
 * to render a GraphPane load a track.
 *
 * @author tarkvara
 */
public class ProgressPanel extends JPanel {
    private JButton cancelButton;
    private JLabel message;

    /**
     * Create a new ProgressPanel.  The bar is indeterminate because we have no idea
     * how long a track will take to load.
     * @param cancellationListener if non-null, a Cancel button will be added which will fire this listener
     */
    public ProgressPanel(ActionListener cancellationListener) {
        super(new GridBagLayout());
        setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 0, 3, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        Dimension prefSize = bar.getPreferredSize();
        prefSize.width = 240;
        bar.setPreferredSize(prefSize);
        add(bar, gbc);

        if (cancellationListener != null) {
            cancelButton = new JButton(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.CLOSE_LIGHT));
            cancelButton.setBorderPainted(false);
            cancelButton.setRolloverIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.CLOSE_DARK));
            cancelButton.addActionListener(cancellationListener);
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 0.0;
            add(cancelButton, gbc);
        }

        message = new JLabel();
        gbc.insets = new Insets(3, 0, 3, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(message, gbc);

    }

    public void setMessage(String msg) {
        message.setText(msg);
    }
}
