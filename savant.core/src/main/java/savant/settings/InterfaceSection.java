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
package savant.settings;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.text.NumberFormat;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.border.Border;

import savant.controller.FrameController;
import savant.controller.TrackController;
import savant.view.swing.Frame;
import savant.view.tracks.Track;


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
    private JCheckBox disableLegendsCheck;
    private JCheckBox wheelZoomsCheck;

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
        disablePopupsCheck.setSelected(InterfaceSettings.arePopupsDisabled());
        disablePopupsCheck.addActionListener(enablingActionListener);
        add(disablePopupsCheck, gbc);

        disableLegendsCheck = new JCheckBox("Disable Track Legends");
        disableLegendsCheck.setSelected(InterfaceSettings.areLegendsDisabled());
        disableLegendsCheck.addActionListener(enablingActionListener);
        add(disableLegendsCheck, gbc);

        wheelZoomsCheck = new JCheckBox("Mouse Wheel Zooms (Instead of Scrolling)");
        wheelZoomsCheck.setSelected(InterfaceSettings.doesWheelZoom());
        wheelZoomsCheck.addActionListener(enablingActionListener);
        gbc.weighty = 1.0;
        add(wheelZoomsCheck, gbc);    }


    @Override
    public void applyChanges() {
        if (bamIntervalHeightField != null) {
            InterfaceSettings.setBamIntervalHeight(Integer.parseInt(bamIntervalHeightField.getText().replaceAll(",", "")));
            InterfaceSettings.setRichIntervalHeight(Integer.parseInt(richIntervalHeightField.getText().replaceAll(",", "")));
            InterfaceSettings.setGenericIntervalHeight(Integer.parseInt(otherIntervalHeightField.getText().replaceAll(",", "")));
            InterfaceSettings.setPopupsDisabled(disablePopupsCheck.isSelected());
            InterfaceSettings.setLegendsDisabled(disableLegendsCheck.isSelected());
            InterfaceSettings.setWheelZooms(wheelZoomsCheck.isSelected());

            try {
                PersistentSettings.getInstance().store();
            } catch (IOException iox) {
                LOG.error("Unable to save remote file settings.", iox);
            }

            // TODO: Modify existing interval heights.
            Frame f = FrameController.getInstance().getActiveFrame();
            if (f != null) {
                f.setLegendVisible(!InterfaceSettings.areLegendsDisabled());
            }
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
