/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
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

        setLayout(new BorderLayout());

        add(SettingsDialog.getHeader(getTitle()), BorderLayout.BEFORE_FIRST_LINE);


        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));


        //JPanel panel = new JPanel(new GridBagLayout());

        //ENABLE CACHING///////////////////////////////////

        
        //add(new JLabel("This is just a demo. This page is not implemented yet.", JLabel.CENTER), BorderLayout.CENTER);
        //this.add(new JLabel("empty section"));


        p.add(getSequencePanel());
        p.add(getIntervalPanel());
        p.add(getBAMPanel());
        //add(Box.createVerticalGlue());
        add(Box.createVerticalGlue());
        //add(new JButton("Test"));
        //add(Box.createVerticalGlue());

        add(p,BorderLayout.CENTER);
    }

    @Override
    public void applyChanges() {
        System.out.println("ResolutionSettingsSection: unsupported");
    }

    private JPanel getSequencePanel() {
        JPanel panel = new JPanel();
        //panel.setLayout(new FlowLayout());
        //panel.setPreferredSize(new Dimension(1,1));
        Border sequenceTitle = BorderFactory.createTitledBorder("Sequence Tracks (FASTA)");
        panel.setBorder(sequenceTitle);
        sequenceAmountField = getFormattedTextField();

        panel.add(new JLabel("Don't show sequence for ranges larger than"));
        panel.add(sequenceAmountField);
        panel.add(new JLabel("bp"));

        return panel;
    }

    private JPanel getIntervalPanel() {
        JPanel panel = new JPanel();
        //panel.setLayout(new FlowLayout());
        //panel.setPreferredSize(new Dimension(1,1));
        Border sequenceTitle = BorderFactory.createTitledBorder("Interval Tracks (BED, GFF, etc.)");
        panel.setBorder(sequenceTitle);

        intervalAmountField = getFormattedTextField();

        panel.add(new JLabel("Don't show intervals for ranges larger than"));
        panel.add(intervalAmountField);
        panel.add(new JLabel("bp"));

        return panel;
    }

     private JPanel getBAMPanel() {

                 bamDefaultModeAmountField = getFormattedTextField();
                         bamArcModeAmountField = getFormattedTextField();

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

   /*

   // Create a sequential group for the horizontal axis.

   GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

   // The sequential group in turn contains two parallel groups.
   // One parallel group contains the labels, the other the text fields.
   // Putting the labels in a parallel group along the horizontal axis
   // positions them at the same x location.
   //
   // Variable indentation is used to reinforce the level of grouping.

   ParallelGroup hpg1 = layout.createParallelGroup();
   ParallelGroup hpg2 = layout.createParallelGroup();
   hpg1.addComponent(new JLabel("Don't show mate arcs for ranges larger than "));
   hpg1.addComponent(new JLabel("Show coverage for ranges larger than "));
   hpg2.addComponent(bamDefaultModeAmountField);
   hpg2.addComponent(bamArcModeAmountField);


   hGroup.addGroup(hpg1);
   hGroup.addGroup(hpg2);
   layout.setHorizontalGroup(hGroup);

   // Create a sequential group for the vertical axis.
   GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

   ParallelGroup vpg1 = layout.createParallelGroup(Alignment.BASELINE);
   ParallelGroup vpg2 = layout.createParallelGroup(Alignment.BASELINE);
   vpg1.addComponent(new JLabel("Don't show mate arcs for ranges larger than "));
   vpg2.addComponent(new JLabel("Show coverage for ranges larger than "));
   vpg1.addComponent(bamDefaultModeAmountField);
   vpg2.addComponent(bamArcModeAmountField);

   vGroup.addGroup(vpg1);
   vGroup.addGroup(vpg2);
   layout.setVerticalGroup(vGroup);
    *
    */

   /*
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.ipadx = 5;
        c.ipady = 5;
        c.gridx = 0;
        c.gridy = 0;


        panel.add(new JLabel("Show coverage for ranges larger than "),c);
        c.gridx++;
        panel.add(bamDefaultModeAmountField,c);
        c.gridx++;
        panel.add(new JLabel(" bp"),c);
        c.gridy++;
        c.gridx = 0;



        panel.add(new JLabel("Don't show mate arcs for ranges larger than "),c);
        c.gridx++;
        panel.add(bamArcModeAmountField,c);
        c.gridx++;
        panel.add(new JLabel(" bp"),c);

        c.gridy++;
        c.gridx = 0;
    * 
    */

        return panel;
    }

    private JFormattedTextField getFormattedTextField() {

        JFormattedTextField result = new JFormattedTextField(NumberFormat.getNumberInstance());
        result.setValue(new Double(TrackResolutionSettings.getIntervalLowToHighThresh()));
        result.setColumns(10);
        result.setPreferredSize(new Dimension(100,18));
        result.setMaximumSize(new Dimension(100,18));
        result.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableApplyButton();
            }
        });
        return result;
    }
}
