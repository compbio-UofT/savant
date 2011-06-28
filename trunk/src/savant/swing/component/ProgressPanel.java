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

package savant.swing.component;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

/**
 * Dorky little class which puts up a progress-bar.  Used when it is taking a while
 * to render a GraphPane load a track.
 *
 * @author tarkvara
 */
public class ProgressPanel extends JPanel {
    private JLabel message;

    public ProgressPanel() {
        super(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setOpaque(false);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        message = new JLabel();
        add(bar, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
    }

    public void setMessage(String msg) {
        message.setText(msg);
    }
}
