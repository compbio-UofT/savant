/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 *
 * @author mfiume
 */
public class ResolutionSettingsSection extends Section {

    private JFormattedTextField sequenceAmountField;
    private JFormattedTextField intervalAmountField;
    private JFormattedTextField bamDefaultModeAmountField;
    private JFormattedTextField bamArcModeAmountField;

    @Override
    public String getSectionName() {
        return "Track Resolutions";
    }

    @Override
    public Icon getSectionIcon() {
        return null;
    }

    @Override
    public void lazyInitialize() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(SettingsDialog.getHeader(getTitle()));

        //JPanel panel = new JPanel(new GridBagLayout());

        //ENABLE CACHING///////////////////////////////////

        
        //add(new JLabel("This is just a demo. This page is not implemented yet.", JLabel.CENTER), BorderLayout.CENTER);
        //this.add(new JLabel("empty section"));

        add(getSequencePanel());
        add(getIntervalPanel());
        add(getBAMPanel());
        add(Box.createVerticalGlue());
    }

    @Override
    public void applyChanges() {
        System.out.println("ResolutionSettingsSection: unsupported");
    }

    private JPanel getSequencePanel() {
        JPanel panel = new JPanel();
        Border sequenceTitle = BorderFactory.createTitledBorder("Sequence Tracks (FASTA)");
        panel.setBorder(sequenceTitle);

        NumberFormat amountFormat = NumberFormat.getNumberInstance();
        sequenceAmountField = new JFormattedTextField(amountFormat);
        sequenceAmountField.setValue(new Double(TrackResolutionSettings.getSequenceLowToHighThresh()));
        sequenceAmountField.setColumns(10);
        sequenceAmountField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableApplyButton();
            }
        });

        panel.add(new JLabel("Don't show sequence for ranges larger than"));
        panel.add(sequenceAmountField);
        panel.add(new JLabel("bp"));

        return panel;
    }

    private JPanel getIntervalPanel() {
        JPanel panel = new JPanel();
        Border sequenceTitle = BorderFactory.createTitledBorder("Interval Tracks (BED, GFF, etc.)");
        panel.setBorder(sequenceTitle);

        NumberFormat amountFormat = NumberFormat.getNumberInstance();
        intervalAmountField = new JFormattedTextField(amountFormat);
        intervalAmountField.setValue(new Double(TrackResolutionSettings.getIntervalLowToHighThresh()));
        intervalAmountField.setColumns(10);
        intervalAmountField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableApplyButton();
            }
        });

        panel.add(new JLabel("Don't show intervals for ranges larger than"));
        panel.add(intervalAmountField);
        panel.add(new JLabel("bp"));

        return panel;
    }

     private JPanel getBAMPanel() {

        JPanel panel = new JPanel(new GridBagLayout());
        Border sequenceTitle = BorderFactory.createTitledBorder("Read alignment tracks (BAM)");
        panel.setBorder(sequenceTitle);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipadx = 5;
        c.ipady = 5;
        c.gridx = 0;
        c.gridy = 0;

        NumberFormat amountFormat = NumberFormat.getNumberInstance();
        bamDefaultModeAmountField = new JFormattedTextField(amountFormat);
        bamDefaultModeAmountField.setValue(new Double(TrackResolutionSettings.getIntervalLowToHighThresh()));
        bamDefaultModeAmountField.setColumns(10);
        bamDefaultModeAmountField.setPreferredSize(new Dimension(100,23));
        bamDefaultModeAmountField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableApplyButton();
            }
        });

        panel.add(new JLabel("Show coverage for ranges larger than"),c);
        c.gridx++;
        panel.add(bamDefaultModeAmountField,c);
        c.gridx++;
        panel.add(new JLabel("bp"),c);
        c.gridy++;
        c.gridx = 0;

        bamArcModeAmountField = new JFormattedTextField(amountFormat);
        bamArcModeAmountField.setValue(new Double(TrackResolutionSettings.getIntervalLowToHighThresh()));
        bamArcModeAmountField.setColumns(10);
        bamArcModeAmountField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableApplyButton();
            }
        });

        panel.add(new JLabel("Don't show mate arcs for ranges larger than"),c);
        c.gridx++;
        panel.add(bamArcModeAmountField,c);
        c.gridx++;
        panel.add(new JLabel("bp"),c);

        c.gridy++;
        c.gridx = 0;

        return panel;
    }
}
