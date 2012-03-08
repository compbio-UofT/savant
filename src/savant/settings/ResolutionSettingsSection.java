/*
 *    Copyright 2011-2012 University of Toronto
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
import java.awt.Insets;
import java.io.IOException;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import savant.controller.FrameController;
import savant.view.swing.Frame;


/**
 *
 * @author mfiume
 */
public class ResolutionSettingsSection extends Section {

    private JFormattedTextField sequenceThresholdField;
    private JFormattedTextField intervalThresholdField;
    private JFormattedTextField bamThresholdField;
    private JFormattedTextField bamArcModeThresholdField;
    private JFormattedTextField continuousThresholdField;
    private JFormattedTextField variantThresholdField;
    private JFormattedTextField ldMaxLociField;

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
        add(getContinuousPanel(), gbc);
        gbc.weighty = 1.0;
        add(getVariantPanel(), gbc);
    }

    @Override
    public void applyChanges() {
        if (bamArcModeThresholdField != null) {
            ResolutionSettings.setBAMArcModeLowToHighThreshold(Integer.parseInt(bamArcModeThresholdField.getText().replaceAll(",", "")));
            ResolutionSettings.setBAMLowToHighThreshold(Integer.parseInt(bamThresholdField.getText().replaceAll(",", "")));
            ResolutionSettings.setIntervalLowToHighThreshold(Integer.parseInt(intervalThresholdField.getText().replaceAll(",", "")));
            ResolutionSettings.setSequenceLowToHighThreshold(Integer.parseInt(sequenceThresholdField.getText().replaceAll(",", "")));
            ResolutionSettings.setContinuousLowToHighThreshold(Integer.parseInt(continuousThresholdField.getText().replaceAll(",", "")));
            ResolutionSettings.setVariantLowToHighThreshold(Integer.parseInt(variantThresholdField.getText().replaceAll(",", "")));
            ResolutionSettings.setLDMaxLoci(Integer.parseInt(ldMaxLociField.getText().replaceAll(",", "")));
            
            //redraw all tracks
            for(Frame f : FrameController.getInstance().getFrames()){
                f.forceRedraw();
            }

            try {
                PersistentSettings.getInstance().store();
            } catch (IOException iox) {
                LOG.error("Unable to save track resolution settings.", iox);
            }
        }
    }

    private JPanel getSequencePanel() {

        sequenceThresholdField = getFormattedTextField(ResolutionSettings.getSequenceLowToHighThreshold());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Sequence Tracks (FASTA)"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        panel.add(new JLabel("Don't show sequence for ranges larger than"), gbc);
        panel.add(sequenceThresholdField, gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        panel.add(new JLabel("bp"), gbc);

        return panel;
    }

    private JPanel getIntervalPanel() {

        intervalThresholdField = getFormattedTextField(ResolutionSettings.getIntervalLowToHighThreshold());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Interval Tracks (BED, GFF, etc.)"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        panel.add(new JLabel("Don't show intervals for ranges larger than"), gbc);
        panel.add(intervalThresholdField, gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        panel.add(new JLabel("bp"), gbc);

        return panel;
    }

    private JPanel getBAMPanel() {

        bamThresholdField = getFormattedTextField(ResolutionSettings.getBAMLowToHighThreshold());
        bamArcModeThresholdField = getFormattedTextField(ResolutionSettings.getBAMArcModeLowToHighThreshold());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Read alignment tracks (BAM)"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Show coverage for ranges larger than"), gbc);
        panel.add(bamThresholdField, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        panel.add(new JLabel("bp"), gbc);
        
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Don't show mate arcs for ranges larger than"), gbc);
        panel.add(bamArcModeThresholdField, gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        panel.add(new JLabel("bp"), gbc);

        return panel;
    }

    private JPanel getContinuousPanel() {

        continuousThresholdField = getFormattedTextField(ResolutionSettings.getContinuousLowToHighThreshold());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Unformatted Continuous Tracks (WIG, BigWig, etc. from external datasources)"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        
        panel.add(new JLabel("Don't show levels for ranges larger than"), gbc);
        panel.add(continuousThresholdField, gbc);
        gbc.weightx = 1.0;
        panel.add(new JLabel("bp"), gbc);

        return panel;
    }

    private JPanel getVariantPanel() {

        variantThresholdField = getFormattedTextField(ResolutionSettings.getVariantLowToHighThreshold());
        ldMaxLociField = getFormattedTextField(ResolutionSettings.getLDMaxLoci());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Variant Tracks (VCF)"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Don't show variants for ranges larger than"), gbc);
        panel.add(variantThresholdField, gbc);
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(new JLabel("bp"), gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Don't generate LD plots for more than"), gbc);
        panel.add(ldMaxLociField, gbc);
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(new JLabel("loci"), gbc);
        return panel;
    }

    private JFormattedTextField getFormattedTextField(int amount) {

        JFormattedTextField result = new JFormattedTextField(NumberFormat.getNumberInstance());
        result.setValue(new Integer(amount));
        result.setColumns(10);
        result.setPreferredSize(new Dimension(90, 18));
        result.setMinimumSize(new Dimension(90, 18));
        result.setMaximumSize(new Dimension(90, 18));
        result.addKeyListener(enablingKeyListener);

        return result;
    }
}
