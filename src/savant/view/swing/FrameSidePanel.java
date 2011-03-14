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

package savant.view.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *
 * @author Andrew Brook
 */
public class FrameSidePanel extends JPanel {

    private GridBagConstraints c;
    private int y = 0;
    private JPanel bottomFill;
    private boolean opaque = false;

    public FrameSidePanel(){
        this.setBackground(Color.red);
        this.setLayout(new GridBagLayout());
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.setOpaque(opaque);
        c = new GridBagConstraints();
        bottomFill = new JPanel();
        bottomFill.setBackground(Color.ORANGE);
        bottomFill.setOpaque(opaque);
        this.add(bottomFill);
    }

    public void addPanel(JComponent pan){
        JPanel fill1 = new JPanel();
        fill1.setBackground(Color.blue);
        fill1.setSize(5,5);
        fill1.setPreferredSize(new Dimension(5,5));
        fill1.setOpaque(opaque);
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = y;
        this.add(fill1, c);

        JPanel fill2 = new JPanel();
        fill2.setBackground(Color.blue);
        fill2.setSize(18,5);
        fill2.setPreferredSize(new Dimension(18,5));
        fill2.setOpaque(opaque);
        c.gridx = 2;
        c.gridy = y;
        this.add(fill2, c);

        //force pan to be full size
        JPanel contain1 = new JPanel();
        contain1.setOpaque(false);
        contain1.setLayout(new BorderLayout());
        contain1.setCursor(new Cursor(Cursor.HAND_CURSOR));
        contain1.add(pan, BorderLayout.EAST);

        c.weightx = 1.0;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = y+1;
        this.add(contain1, c);

        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = y+2;
        this.remove(bottomFill);
        this.add(bottomFill, c);

        y += 2;
    }

}
