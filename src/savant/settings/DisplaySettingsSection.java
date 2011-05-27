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

package savant.settings;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import savant.controller.TrackController;
import savant.view.swing.Track;
import savant.view.swing.TrackRenderer;

/**
 *
 * @author AndrewBrook
 */
public class DisplaySettingsSection extends Section {

    private JFormattedTextField bamIntervalHeightField;
    private JFormattedTextField richIntervalHeightField;
    private JFormattedTextField otherIntervalHeightField;

    @Override
    public String getSectionName() {
        return "Display Settings";
    }

    @Override
    public Icon getSectionIcon() {
        return null;
    }

    @Override
    public void lazyInitialize() {
        setLayout(new BorderLayout());

        add(SettingsDialog.getHeader(getTitle()), BorderLayout.BEFORE_FIRST_LINE);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.add(getIntervalHeightsPanel());

        add(Box.createVerticalGlue());

        add(p, BorderLayout.CENTER);
    }

    private JPanel getIntervalHeightsPanel() {

        bamIntervalHeightField = getFormattedTextField(DisplaySettings.getBamIntervalHeight());
        richIntervalHeightField = getFormattedTextField(DisplaySettings.getRichIntervalHeight());
        otherIntervalHeightField = getFormattedTextField(DisplaySettings.getGenericIntervalHeight());

        JPanel panel = new JPanel(new GridBagLayout());
        Border sequenceTitle = BorderFactory.createTitledBorder("Interval Heights");
        panel.setBorder(sequenceTitle);

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        // Turn on automatically adding gaps between components
        layout.setAutoCreateGaps(true);

        // Turn on automatically creating gaps between components that touch
        // the edge of the container and the container.
        layout.setAutoCreateContainerGaps(true);

        JLabel label1 = new JLabel("BAM Interval Height ");
        JLabel label2 = new JLabel("BED Interval Height ");
        JLabel label3 = new JLabel("Generic Interval Height ");
        JTextField tf1 = bamIntervalHeightField;
        JTextField tf2 = richIntervalHeightField;
        JTextField tf3 = otherIntervalHeightField;
        JLabel label4 = new JLabel(" pixels");
        JLabel label5 = new JLabel(" pixels");
        JLabel label6 = new JLabel(" pixels");

        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

        hGroup.addGroup(layout.createParallelGroup().
                addComponent(label1).addComponent(label2).addComponent(label3));
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(tf1).addComponent(tf2).addComponent(tf3));
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(label4).addComponent(label5).addComponent(label6));
        layout.setHorizontalGroup(hGroup);

        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

        vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).
                addComponent(label1).addComponent(tf1).addComponent(label4));
        vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).
                addComponent(label2).addComponent(tf2).addComponent(label5));
        vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).
                addComponent(label3).addComponent(tf3).addComponent(label6));
        layout.setVerticalGroup(vGroup);

        return panel;
    }

    private JFormattedTextField getFormattedTextField(int amount) {

        JFormattedTextField result = new JFormattedTextField(NumberFormat.getNumberInstance());
        result.setValue(new Double(amount));
        result.setColumns(10);
        result.setPreferredSize(new Dimension(100, 18));
        result.setMaximumSize(new Dimension(100, 18));
        result.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                enableApplyButton();
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        return result;
    }

    @Override
    public void applyChanges() {
        if(this.bamIntervalHeightField == null) return; //make sure section has been initialized
        DisplaySettings.setBamIntervalHeight((int) Double.parseDouble(this.bamIntervalHeightField.getText().replaceAll(",", "")));
        DisplaySettings.setRichIntervalHeight((int) Double.parseDouble(this.richIntervalHeightField.getText().replaceAll(",", "")));
        DisplaySettings.setGenericIntervalHeight((int) Double.parseDouble(this.otherIntervalHeightField.getText().replaceAll(",", "")));
    }

}
