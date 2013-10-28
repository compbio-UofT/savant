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
    private JProgressBar bar;

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

        bar = new JProgressBar();
        bar.setIndeterminate(true);
        Dimension prefSize = bar.getPreferredSize();
        prefSize.width = 240;
        bar.setPreferredSize(prefSize);
        add(bar, gbc);

        if (cancellationListener != null) {
            cancelButton = new JButton(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.CLOSE_LIGHT));
            cancelButton.setContentAreaFilled(false);
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
    
    public void setFraction(double fract) {
        if (fract < 0.0) {
            bar.setIndeterminate(true);
        } else {
            bar.setIndeterminate(false);
            bar.setValue((int)Math.round(fract * 100.0));
        }
    }
}
