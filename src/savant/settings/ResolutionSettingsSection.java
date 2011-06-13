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
import javax.swing.Icon;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;


/**
 *
 * @author mfiume
 */
public class ResolutionSettingsSection extends Section {

    private JFormattedTextField sequenceAmountField;
    private JFormattedTextField intervalAmountField;
    private JFormattedTextField bamDefaultModeAmountField;
    private JFormattedTextField bamArcModeAmountField;
    private JFormattedTextField conservationAmountField;

    @Override
    public String getTitle() {
        return "Track Resolutions";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public void lazyInitialize() {
        GridBagConstraints gbc = getFullRowConstraints();
        add(SettingsDialog.getHeader(getTitle()), gbc);
        add(getSequencePanel(), gbc);
        add(getIntervalPanel(), gbc);
        add(getBAMPanel(), gbc);
        gbc.weighty = 1.0;
        add(getConservationPanel(), gbc);
    }

    @Override
    public void applyChanges() {
        if (bamArcModeAmountField != null) {
            TrackResolutionSettings.setBamArcModeLowToHighThresh(Integer.parseInt(bamArcModeAmountField.getText().replaceAll(",", "")));
            TrackResolutionSettings.setBamDefaultModeLowToHighThresh(Integer.parseInt(bamDefaultModeAmountField.getText().replaceAll(",", "")));
            TrackResolutionSettings.setIntervalLowToHighThresh(Integer.parseInt(intervalAmountField.getText().replaceAll(",", "")));
            TrackResolutionSettings.setSequenceLowToHighThresh(Integer.parseInt(sequenceAmountField.getText().replaceAll(",", "")));
            TrackResolutionSettings.setConservationLowToHighThresh(Integer.parseInt(conservationAmountField.getText().replaceAll(",", "")));

            try {
                PersistentSettings.getInstance().store();
            } catch (IOException iox) {
                LOG.error("Unable to save track resolution settings.", iox);
            }
        }
    }

    private JPanel getSequencePanel() {

        sequenceAmountField = getFormattedTextField(TrackResolutionSettings.getSequenceLowToHighThresh());

        JPanel panel = new JPanel(new GridBagLayout());
        //panel.setPreferredSize(new Dimension(1,1));
        Border sequenceTitle = BorderFactory.createTitledBorder("Sequence Tracks (FASTA)");
        panel.setBorder(sequenceTitle);

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        // Turn on automatically adding gaps between components
        layout.setAutoCreateGaps(true);

        // Turn on automatically creating gaps between components that touch
        // the edge of the container and the container.
        layout.setAutoCreateContainerGaps(true);

        JLabel label1 = new JLabel("Don't show sequence for ranges larger than ");
        JTextField tf1 = sequenceAmountField;
        JLabel label3 = new JLabel(" bp");

        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

        // The sequential group in turn contains two parallel groups.
        // One parallel group contains the labels, the other the text fields.
        // Putting the labels in a parallel group along the horizontal axis
        // positions them at the same x location.
        //
        // Variable indentation is used to reinforce the level of grouping.
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(label1));
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(tf1));
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(label3));
        layout.setHorizontalGroup(hGroup);

        // Create a sequential group for the vertical axis.
        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

        // The sequential group contains two parallel groups that align
        // the contents along the baseline. The first parallel group contains
        // the first label and text field, and the second parallel group contains
        // the second label and text field. By using a sequential group
        // the labels and text fields are positioned vertically after one another.
        vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).
                addComponent(label1).addComponent(tf1).addComponent(label3));
        layout.setVerticalGroup(vGroup);

        return panel;
    }

    private JPanel getIntervalPanel() {

        intervalAmountField = getFormattedTextField(TrackResolutionSettings.getIntervalLowToHighThresh());

        JPanel panel = new JPanel(new GridBagLayout());
        //panel.setPreferredSize(new Dimension(1,1));
        Border sequenceTitle = BorderFactory.createTitledBorder("Interval Tracks (BED, GFF, etc.)");
        panel.setBorder(sequenceTitle);

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        // Turn on automatically adding gaps between components
        layout.setAutoCreateGaps(true);

        // Turn on automatically creating gaps between components that touch
        // the edge of the container and the container.
        layout.setAutoCreateContainerGaps(true);

        JLabel label1 = new JLabel("Don't show intervals for ranges larger than ");
        JTextField tf1 = intervalAmountField;
        JLabel label3 = new JLabel(" bp");

        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

        // The sequential group in turn contains two parallel groups.
        // One parallel group contains the labels, the other the text fields.
        // Putting the labels in a parallel group along the horizontal axis
        // positions them at the same x location.
        //
        // Variable indentation is used to reinforce the level of grouping.
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(label1));
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(tf1));
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(label3));
        layout.setHorizontalGroup(hGroup);

        // Create a sequential group for the vertical axis.
        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

        // The sequential group contains two parallel groups that align
        // the contents along the baseline. The first parallel group contains
        // the first label and text field, and the second parallel group contains
        // the second label and text field. By using a sequential group
        // the labels and text fields are positioned vertically after one another.
        vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).
                addComponent(label1).addComponent(tf1).addComponent(label3));
        layout.setVerticalGroup(vGroup);

        return panel;
    }

    private JPanel getBAMPanel() {

        bamDefaultModeAmountField = getFormattedTextField(TrackResolutionSettings.getBamDefaultModeLowToHighThresh());
        bamArcModeAmountField = getFormattedTextField(TrackResolutionSettings.getBamArcModeLowToHighThresh());

        JPanel panel = new JPanel(new GridBagLayout());
        //panel.setPreferredSize(new Dimension(1,1));
        Border sequenceTitle = BorderFactory.createTitledBorder("Read alignment tracks (BAM)");
        panel.setBorder(sequenceTitle);

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        // Turn on automatically adding gaps between components
        layout.setAutoCreateGaps(true);

        // Turn on automatically creating gaps between components that touch
        // the edge of the container and the container.
        layout.setAutoCreateContainerGaps(true);

        JLabel label1 = new JLabel("Show coverage for ranges larger than ");
        JLabel label2 = new JLabel("Don't show mate arcs for ranges larger than ");
        JTextField tf1 = bamDefaultModeAmountField;
        JTextField tf2 = bamArcModeAmountField;
        JLabel label3 = new JLabel(" bp");
        JLabel label4 = new JLabel(" bp");

        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

        // The sequential group in turn contains two parallel groups.
        // One parallel group contains the labels, the other the text fields.
        // Putting the labels in a parallel group along the horizontal axis
        // positions them at the same x location.
        //
        // Variable indentation is used to reinforce the level of grouping.
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(label1).addComponent(label2));
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(tf1).addComponent(tf2));
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(label3).addComponent(label4));
        layout.setHorizontalGroup(hGroup);

        // Create a sequential group for the vertical axis.
        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

        // The sequential group contains two parallel groups that align
        // the contents along the baseline. The first parallel group contains
        // the first label and text field, and the second parallel group contains
        // the second label and text field. By using a sequential group
        // the labels and text fields are positioned vertically after one another.
        vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).
                addComponent(label1).addComponent(tf1).addComponent(label3));
        vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).
                addComponent(label2).addComponent(tf2).addComponent(label4));
        layout.setVerticalGroup(vGroup);

        return panel;
    }

    private JPanel getConservationPanel() {

        conservationAmountField = getFormattedTextField(TrackResolutionSettings.getConservationLowToHighThresh());

        JPanel panel = new JPanel(new GridBagLayout());
        //panel.setPreferredSize(new Dimension(1,1));
        Border sequenceTitle = BorderFactory.createTitledBorder("Unformatted Continuous Tracks (WIG, BigWig, etc.)");
        panel.setBorder(sequenceTitle);

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        // Turn on automatically adding gaps between components
        layout.setAutoCreateGaps(true);

        // Turn on automatically creating gaps between components that touch
        // the edge of the container and the container.
        layout.setAutoCreateContainerGaps(true);

        JLabel label1 = new JLabel("Don't show levels for ranges larger than ");
        JTextField tf1 = conservationAmountField;
        JLabel label3 = new JLabel(" bp");

        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

        // The sequential group in turn contains two parallel groups.
        // One parallel group contains the labels, the other the text fields.
        // Putting the labels in a parallel group along the horizontal axis
        // positions them at the same x location.
        //
        // Variable indentation is used to reinforce the level of grouping.
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(label1));
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(tf1));
        hGroup.addGroup(layout.createParallelGroup().
                addComponent(label3));
        layout.setHorizontalGroup(hGroup);

        // Create a sequential group for the vertical axis.
        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

        // The sequential group contains two parallel groups that align
        // the contents along the baseline. The first parallel group contains
        // the first label and text field, and the second parallel group contains
        // the second label and text field. By using a sequential group
        // the labels and text fields are positioned vertically after one another.
        vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).
                addComponent(label1).addComponent(tf1).addComponent(label3));
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
