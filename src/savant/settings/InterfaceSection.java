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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import savant.controller.TrackController;
import savant.view.swing.Track;


/**
 * Settings panel which allows user to specify interval heights and other interface-related preferences.
 *
 * @author AndrewBrook
 */
public class InterfaceSection extends Section {

    private JFormattedTextField bamIntervalHeightField;
    private JFormattedTextField richIntervalHeightField;
    private JFormattedTextField otherIntervalHeightField;
    private JCheckBox disablePopupsCheck;

    @Override
    public String getTitle() {
        return "Interface Settings";
    }

    @Override
    public void lazyInitialize() {
        GridBagConstraints gbc = getFullRowConstraints();
        add(SettingsDialog.getHeader(getTitle()), gbc);
        add(getIntervalHeightsPanel(), gbc);

        disablePopupsCheck = new JCheckBox("Disable Informational Popups");
        disablePopupsCheck.setSelected(InterfaceSettings.isPopupsDisabled());
        disablePopupsCheck.addActionListener(enablingActionListener);
        gbc.weighty = 1.0;
        add(disablePopupsCheck, gbc);
    }


    @Override
    public void applyChanges() {
        if (bamIntervalHeightField != null) {
            InterfaceSettings.setBamIntervalHeight(Integer.parseInt(bamIntervalHeightField.getText().replaceAll(",", "")));
            InterfaceSettings.setRichIntervalHeight(Integer.parseInt(richIntervalHeightField.getText().replaceAll(",", "")));
            InterfaceSettings.setGenericIntervalHeight(Integer.parseInt(otherIntervalHeightField.getText().replaceAll(",", "")));
            InterfaceSettings.setPopupsDisabled(disablePopupsCheck.isSelected());

            try {
                PersistentSettings.getInstance().store();
            } catch (IOException iox) {
                LOG.error("Unable to save remote file settings.", iox);
            }

            // TODO: Modify existing interval heights.
            for (Track t: TrackController.getInstance().getTracks()) {
                t.getFrame().forceRedraw();
            }
        }
    }

    private JPanel getIntervalHeightsPanel() {

        bamIntervalHeightField = getFormattedTextField(InterfaceSettings.getBamIntervalHeight());
        richIntervalHeightField = getFormattedTextField(InterfaceSettings.getRichIntervalHeight());
        otherIntervalHeightField = getFormattedTextField(InterfaceSettings.getGenericIntervalHeight());

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
        result.addKeyListener(enablingKeyListener);

        return result;
    }
}
