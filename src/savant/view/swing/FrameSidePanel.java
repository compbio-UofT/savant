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
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *
 * @author Andrew Brook
 * 
 * Good luck to anyone who tries to decipher this mess...
 * The result of hours of wrestling with Swing layouts. 
 */
public class FrameSidePanel extends JPanel {

    private GridBagConstraints c;
    private int y = 0;
    private JPanel bottomFill;
    private JPanel bottomFill1;
    private boolean opaque = false;
    private int containerHeight = 200;
    private boolean showPanel = false;
    private boolean firstPanel = true;
    private JPanel extraPanel;

    private ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    private ArrayList<JComponent> tempHidden = new ArrayList<JComponent>();

    public FrameSidePanel(){
        this.setBackground(Color.red);
        this.setLayout(new GridBagLayout());
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.setOpaque(opaque);
        c = new GridBagConstraints();
        bottomFill = new JPanel();
        bottomFill.setMinimumSize(new Dimension(0,0));
        bottomFill.setBackground(Color.ORANGE);
        bottomFill.setOpaque(opaque);
        this.add(bottomFill);

        JPanel fill2 = new JPanel();
        fill2.setBackground(Color.blue);
        fill2.setSize(18,5);
        fill2.setPreferredSize(new Dimension(18,5));
        fill2.setOpaque(opaque);
        c.gridx = 2;
        c.gridy = 1;
        this.add(fill2, c);
    }

    public void addPanel(JComponent pan){

        JPanel toAddTo = this;
        if(!firstPanel){
            toAddTo = extraPanel;
        }

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
        toAddTo.add(fill1, c);

        JPanel fill2 = new JPanel();
        fill2.setBackground(Color.blue);
        fill2.setSize(18,5);
        fill2.setPreferredSize(new Dimension(18,5));
        fill2.setOpaque(opaque);
        c.gridx = 2;
        c.gridy = y;
        toAddTo.add(fill2, c);

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
        toAddTo.add(contain1, c);

        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = y+2;
        this.remove(bottomFill);
        this.add(bottomFill, c);
        if(!firstPanel){
            extraPanel.remove(bottomFill1);
            extraPanel.add(bottomFill1, c);
        }

        y += 2;

        if(firstPanel){
            componentList.add(fill1);
            componentList.add(fill2);
            componentList.add(pan);
            tempHidden.add(fill1);
            tempHidden.add(fill2);
            if(pan.isVisible())
                tempHidden.add(pan);
        }
        setShowPanel(showPanel);

        if(firstPanel){
            extraPanel = new JPanel();
            extraPanel.setLayout(new GridBagLayout());
            extraPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            extraPanel.setOpaque(opaque);
            c.gridx = 0;
            c.gridwidth = 3;
            this.add(extraPanel, c);
            bottomFill1 = new JPanel();
            bottomFill1.setMinimumSize(new Dimension(0,0));
            bottomFill1.setBackground(Color.ORANGE);
            bottomFill1.setOpaque(opaque);
            extraPanel.add(bottomFill1);
            c.gridx = 1;
            c.gridwidth = 1;
            y++;
            firstPanel = false;
            componentList.add(extraPanel);
            tempHidden.add(extraPanel);
        }

    }

    public void setContainerHeight(int height){
        containerHeight = height;
        setShowPanel(showPanel);
    }

    public void setShowPanel(boolean show){
        showPanel = show;
        int necessaryHeight = getNecessaryHeight();
        if (!show || containerHeight < necessaryHeight) {
            for (JComponent comp : componentList) {
                if (comp instanceof FrameCommandBar) continue;
                if (comp.isVisible()) {
                    tempHidden.add(comp);
                    comp.setVisible(false);
                }
            }
        } else {
            for (JComponent comp: tempHidden) {
                comp.setVisible(true);
            }
            tempHidden.clear();
        }
    }

    public int getNecessaryHeight(){
        int necessaryHeight = 0;
        for(JComponent comp : componentList){
            if(comp.isVisible())
                necessaryHeight += comp.getPreferredSize().height;
        }
        for(JComponent comp : tempHidden){
            if(!comp.isVisible())
                necessaryHeight += comp.getPreferredSize().height;
        }
        
        return necessaryHeight + 25;
    }

}
