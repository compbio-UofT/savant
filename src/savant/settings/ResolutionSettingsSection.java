/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.settings;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

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

        setLayout(new BorderLayout());

        add(SettingsDialog.getHeader(getTitle()), BorderLayout.BEFORE_FIRST_LINE);


        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.add(getSequencePanel());
        p.add(getIntervalPanel());
        p.add(getBAMPanel());
        add(Box.createVerticalGlue());

        add(p, BorderLayout.CENTER);
    }

    @Override
    public void applyChanges() {
        if(this.bamArcModeAmountField == null) return; //make sure section has been initialized
        TrackResolutionSettings.setBAMArcModeLowToHighThresh((int) Double.parseDouble(this.bamArcModeAmountField.getText().replaceAll(",", "")));
        TrackResolutionSettings.setBAMDefaultModeLowToHighThresh((int) Double.parseDouble(this.bamDefaultModeAmountField.getText().replaceAll(",", "")));
        TrackResolutionSettings.setIntervalLowToHighThresh((int) Double.parseDouble(this.intervalAmountField.getText().replaceAll(",", "")));
        TrackResolutionSettings.setSequenceLowToHighThresh((int) Double.parseDouble(this.sequenceAmountField.getText().replaceAll(",", "")));
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

        bamDefaultModeAmountField = getFormattedTextField(TrackResolutionSettings.getBAMDefaultModeLowToHighThresh());
        bamArcModeAmountField = getFormattedTextField(TrackResolutionSettings.getBAMArcModeLowToHighThresh());

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
}
