/*
 *    Copyright 2011 University of Toronto
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

import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

/**
 * Component which allows vertical scrolling for data-sets which are too long for
 * a JScrollPane.
 *
 * @author tarkvara
 */
public class TallScrollingPanel extends JPanel {
    JScrollBar scroller;

    public TallScrollingPanel(JComponent view) {
        setLayout(new BorderLayout());

        add(view, BorderLayout.CENTER);

        scroller = new JScrollBar();
        add(scroller, BorderLayout.EAST);
    }
    
    public JScrollBar getScrollBar() {
        return scroller;
    }
}
