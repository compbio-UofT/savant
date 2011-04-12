/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.view.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.settings.BrowserSettings;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.icon.SavantIconFactory;

/**
 *
 * @author tarkvara
 */
public class NavigationBar extends JToolBar {
    private static final Log LOG = LogFactory.getLog(NavigationBar.class);

    private RangeController rangeController = RangeController.getInstance();

    /** reference drop-down menu */
    private JComboBox referenceDropdown;

    /** From and To text-boxes */
    private JTextField fromField, toField;

    /** Length being displayed */
    private JLabel lengthLabel;

    private KeyAdapter rangeTextBoxKeyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent evt) {
            if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                setRangeFromTextBoxes();
            }
        }
    };


    NavigationBar() {

        this.setFloatable(false);

        String buttonStyle = "segmentedTextured";

        //p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

        Dimension comboboxDimension = new Dimension(200, 23);
        Dimension iconDimension = MiscUtils.MAC ? new Dimension(50, 23) : new Dimension(27, 27);
        String shortcutMod = MiscUtils.MAC ? "Cmd" : "Ctrl";

        add(getRigidPadding());

        JLabel refLabel = new JLabel();
        refLabel.setText("Reference: ");
        refLabel.setToolTipText("Reference sequence");
        add(refLabel);

        referenceDropdown = new JComboBox();
        referenceDropdown.setPreferredSize(comboboxDimension);
        referenceDropdown.setMinimumSize(comboboxDimension);
        referenceDropdown.setMaximumSize(comboboxDimension);
        referenceDropdown.setToolTipText("Reference sequence");
        referenceDropdown.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                // the first item is a header and not allowed to be selected
                if (referenceDropdown.getItemCount() <= 1) {
                    return;
                }

                int index = referenceDropdown.getSelectedIndex();
                String ref = (String) referenceDropdown.getItemAt(index);
                if (ref.contains("[")) {
                    int size = referenceDropdown.getItemCount();
                    for (int i = 1; i < size; i++) {
                        int newindex = (index + i) % size;
                        String newref = (String) referenceDropdown.getItemAt(newindex);
                        if (!((String) referenceDropdown.getItemAt(newindex)).contains("[")) {
                            index = newindex;
                            referenceDropdown.setSelectedIndex(index);
                            break;
                        }
                    }
                }
                switchReference(index);
            }

            private void switchReference(int index) {

                String ref = (String) referenceDropdown.getItemAt(index);

                if (!ReferenceController.getInstance().getReferenceNames().contains(ref)) {
                    if (!Savant.showNonGenomicReferenceDialog) {
                        return;
                    }
                    //Custom button text
                    Object[] options = {"OK",
                        "Don't show again"};
                    int n = JOptionPane.showOptionDialog(Savant.getInstance(),
                            "This reference is nongenomic (i.e. it appears in a loaded track but it is not found in the loaded genome)",
                            "Non-Genomic Reference",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            options,
                            options[0]);

                    if (n == 1) {
                        Savant.showNonGenomicReferenceDialog = false;
                    } else if (n == 0) {
                        return;
                    }
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Actually changing reference to " + ref);
                }
                ReferenceController.getInstance().setReference(ref);
            }
        });
        add(referenceDropdown);

        add(getRigidPadding());

        JLabel fromtext = new JLabel();
        fromtext.setText("From: ");
        fromtext.setToolTipText("Start position of range");
        add(fromtext);
        //p.add(this.getRigidPadding());

        int tfwidth = 100;
        int labwidth = 100;
        int tfheight = 22;
        fromField = (JTextField)add(new JTextField());
        fromField.setToolTipText("Start position of range");
        fromField.setHorizontalAlignment(JTextField.CENTER);
        fromField.setPreferredSize(new Dimension(tfwidth, tfheight));
        fromField.setMaximumSize(new Dimension(tfwidth, tfheight));
        fromField.setMinimumSize(new Dimension(tfwidth, tfheight));

        fromField.addKeyListener(rangeTextBoxKeyAdapter);

        add(getRigidPadding());

        JLabel toLabel = new JLabel();
        toLabel.setToolTipText("End position of range");
        toLabel.setText("To: ");
        add(toLabel);
        //p.add(this.getRigidPadding());

        toField = (JTextField)add(new JTextField());
        toField.setToolTipText("End position of range");
        toField.setHorizontalAlignment(JTextField.CENTER);
        toField.setPreferredSize(new Dimension(tfwidth, tfheight));
        toField.setMaximumSize(new Dimension(tfwidth, tfheight));
        toField.setMinimumSize(new Dimension(tfwidth, tfheight));

        toField.addKeyListener(rangeTextBoxKeyAdapter);

        add(getRigidPadding());

        JButton goButton = (JButton)add(new JButton("  Go  "));
        goButton.putClientProperty("JButton.buttonType", buttonStyle);
        goButton.putClientProperty("JButton.segmentPosition", "only");
        goButton.setToolTipText("Go to specified range (Enter)");
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRangeFromTextBoxes();
            }
        });

        add(getRigidPadding());

        JLabel l = new JLabel();
        l.setText("Length: ");
        l.setToolTipText("Length of the current range");
        add(l);

        lengthLabel = (JLabel)add(new JLabel());
        lengthLabel.setToolTipText("Length of the current range");
        lengthLabel.setPreferredSize(new Dimension(labwidth, tfheight));
        lengthLabel.setMaximumSize(new Dimension(labwidth, tfheight));
        lengthLabel.setMinimumSize(new Dimension(labwidth, tfheight));

        add(Box.createGlue());

        double screenwidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();

        JButton afterGo = null;
        if (screenwidth > 800) {
            JButton undoButton = (JButton)add(new JButton(""));
            afterGo = undoButton;
            undoButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.UNDO));
            undoButton.setToolTipText("Undo range change (" + shortcutMod + "+Z)");
            undoButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rangeController.undoRangeChange();
                }
            });
            undoButton.putClientProperty("JButton.buttonType", buttonStyle);
            undoButton.putClientProperty("JButton.segmentPosition", "first");
            undoButton.setPreferredSize(iconDimension);
            undoButton.setMinimumSize(iconDimension);
            undoButton.setMaximumSize(iconDimension);

            JButton redo = (JButton)add(new JButton(""));
            redo.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.REDO));
            redo.setToolTipText("Redo range change (" + shortcutMod + "+Y)");
            redo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rangeController.redoRangeChange();
                }
            });
            redo.putClientProperty("JButton.buttonType", buttonStyle);
            redo.putClientProperty("JButton.segmentPosition", "last");
            redo.setPreferredSize(iconDimension);
            redo.setMinimumSize(iconDimension);
            redo.setMaximumSize(iconDimension);
        }

        add(getRigidPadding());
        add(getRigidPadding());

        JButton zoomInButton = (JButton)add(new JButton());
        if (afterGo == null) {
            afterGo = zoomInButton;
        }
        zoomInButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.ZOOMIN));
        zoomInButton.putClientProperty("JButton.buttonType", buttonStyle);
        zoomInButton.putClientProperty("JButton.segmentPosition", "first");
        zoomInButton.setPreferredSize(iconDimension);
        zoomInButton.setMinimumSize(iconDimension);
        zoomInButton.setMaximumSize(iconDimension);
        zoomInButton.setToolTipText("Zoom in (Shift+Up)");
        zoomInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rangeController.zoomIn();
            }
        });

        JButton zoomOut = (JButton)add(new JButton(""));
        zoomOut.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.ZOOMOUT));
        zoomOut.setToolTipText("Zoom out (Shift+Down)");
        zoomOut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rangeController.zoomOut();
            }
        });
        zoomOut.putClientProperty("JButton.buttonType", buttonStyle);
        zoomOut.putClientProperty("JButton.segmentPosition", "last");
        zoomOut.setPreferredSize(iconDimension);
        zoomOut.setMinimumSize(iconDimension);
        zoomOut.setMaximumSize(iconDimension);

        add(getRigidPadding());
        add(getRigidPadding());

        JButton shiftFarLeft = (JButton)add(new JButton());
        shiftFarLeft.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SHIFT_FARLEFT));
        shiftFarLeft.putClientProperty("JButton.buttonType", buttonStyle);
        shiftFarLeft.putClientProperty("JButton.segmentPosition", "first");
        shiftFarLeft.setToolTipText("Move to the beginning of the genome (Home)");
        shiftFarLeft.setPreferredSize(iconDimension);
        shiftFarLeft.setMinimumSize(iconDimension);
        shiftFarLeft.setMaximumSize(iconDimension);
        shiftFarLeft.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rangeController.shiftRangeFarLeft();
            }
        });

        JButton shiftLeft = (JButton)add(new JButton());
        shiftLeft.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SHIFT_LEFT));
        shiftLeft.putClientProperty("JButton.buttonType", buttonStyle);
        shiftLeft.putClientProperty("JButton.segmentPosition", "middle");
        shiftLeft.setToolTipText("Move left (Shift+Left)");
        shiftLeft.setPreferredSize(iconDimension);
        shiftLeft.setMinimumSize(iconDimension);
        shiftLeft.setMaximumSize(iconDimension);
        shiftLeft.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rangeController.shiftRangeLeft();
            }
        });

        JButton shiftRight = (JButton)add(new JButton());
        shiftRight.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SHIFT_RIGHT));
        shiftRight.putClientProperty("JButton.buttonType", buttonStyle);
        shiftRight.putClientProperty("JButton.segmentPosition", "middle");
        shiftRight.setToolTipText("Move right (Shift+Right)");
        shiftRight.setPreferredSize(iconDimension);
        shiftRight.setMinimumSize(iconDimension);
        shiftRight.setMaximumSize(iconDimension);
        shiftRight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rangeController.shiftRangeRight();
            }
        });

        JButton shiftFarRight = (JButton)add(new JButton());
        shiftFarRight.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SHIFT_FARRIGHT));
        shiftFarRight.putClientProperty("JButton.buttonType", buttonStyle);
        shiftFarRight.putClientProperty("JButton.segmentPosition", "last");
        shiftFarRight.setToolTipText("Move to the end of the genome (End)");
        shiftFarRight.setPreferredSize(iconDimension);
        shiftFarRight.setMinimumSize(iconDimension);
        shiftFarRight.setMaximumSize(iconDimension);
        shiftFarRight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rangeController.shiftRangeFarRight();
            }
        });

        add(getRigidPadding());

        // Don't know why our tab order got wonky, but it did some time between 1.4.1 and 1.4.2
        fromField.setNextFocusableComponent(toField);
        toField.setNextFocusableComponent(goButton);
        goButton.setNextFocusableComponent(afterGo);
        shiftFarRight.setNextFocusableComponent(referenceDropdown);

    }

    private static Component getRigidPadding() {
        return Box.createRigidArea(new Dimension(BrowserSettings.padding, BrowserSettings.padding));
    }

        /**
     * Set the current range from the Zoom track bar.
     */
    void setRangeFromTextBoxes() {

        String fromtext = fromField.getText().trim();
        String totext = toField.getText().trim();

        int from, to;

        if (fromtext.startsWith("-")) {
            fromtext = MiscUtils.removeChar(fromtext, '-');
            int diff = MiscUtils.stringToInt(MiscUtils.removeChar(fromtext, ','));
            to = MiscUtils.stringToInt(MiscUtils.removeChar(toField.getText(), ','));
            from = to - diff + 1;
        } else if (totext.startsWith("+")) {
            totext = MiscUtils.removeChar(totext, '+');
            int diff = MiscUtils.stringToInt(MiscUtils.removeChar(totext, ','));
            from = MiscUtils.stringToInt(MiscUtils.removeChar(fromField.getText(), ','));
            to = from + diff - 1;
        } else {
            from = MiscUtils.stringToInt(MiscUtils.removeChar(fromField.getText(), ','));
            to = MiscUtils.stringToInt(MiscUtils.removeChar(toField.getText(), ','));
        }

        if (from <= 0) {
            JOptionPane.showMessageDialog(this, "Invalid start value.");
            fromField.requestFocus();
            return;
        }

        if (to <= 0) {
            JOptionPane.showMessageDialog(this, "Invalid end value.");
            toField.requestFocus();
            return;
        }

        if (from > to) {
            //MessageBox.Show("INVALID RANGE");
            JOptionPane.showMessageDialog(this, "Invalid range.");
            toField.requestFocus();
            return;
        }

        rangeController.setRange(from, to);
    }

    void setReferences(List refs) {
        String curRef = (String)referenceDropdown.getSelectedItem();
        referenceDropdown.removeAllItems();

        int maxWidth = 0;
        for (Object s : refs) {
            String ref = s.toString();
            maxWidth = Math.max(maxWidth, ref.length());
            referenceDropdown.addItem(ref);
        }
        maxWidth = Math.max(200, maxWidth * 8 + 20);
        Dimension dim = new Dimension(maxWidth, 23);
        referenceDropdown.setPreferredSize(dim);
        referenceDropdown.setMinimumSize(dim);
        referenceDropdown.setMaximumSize(dim);
        referenceDropdown.invalidate();

        //this.referenceDropdown.addItem("[ NON-GENOMIC (" + nongenomicrefnames.size() + ") ]");
        List<String> nonGenomicRefs = MiscUtils.set2List(ReferenceController.getInstance().getNonGenomicReferenceNames());
        if (nonGenomicRefs.size() > 0) {
            for (String s : nonGenomicRefs) {
                referenceDropdown.addItem(s);
            }
        }

        if (curRef != null) {
            referenceDropdown.setSelectedItem(curRef);
        }
    }

    void setSelectedReference(String value) {
        referenceDropdown.setSelectedItem(value);
    }

    void setSelectedReference(int index) {
        referenceDropdown.setSelectedIndex(index);
    }

    /**
     * Set range description.
     *  - Change the from and to textboxes.
     * @param range
     */
    void setRangeDescription(Range range) {
        fromField.setText(MiscUtils.numToString(range.getFrom()));
        toField.setText(MiscUtils.numToString(range.getTo()));
        lengthLabel.setText(MiscUtils.numToString(range.getLength()));
    }
}
