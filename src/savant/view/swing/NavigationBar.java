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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import javax.swing.*;

import savant.api.util.DialogUtils;
import savant.controller.LocationController;
import savant.controller.TrackController;
import savant.controller.event.GenomeChangedEvent;
import savant.controller.event.LocationChangedEvent;
import savant.controller.event.LocationChangedListener;
import savant.settings.BrowserSettings;
import savant.util.Bookmark;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.icon.SavantIconFactory;

/**
 * Contains the various widgets for providing easy range navigation.
 *
 * @author tarkvara
 */
public class NavigationBar extends JToolBar {

    private LocationController locationController = LocationController.getInstance();

    /** Range text-box */
    JTextField rangeField;

    /** Length being displayed */
    private JLabel lengthLabel;

    NavigationBar() {

        setFloatable(false);

        String buttonStyle = "segmentedTextured";

        Dimension iconDimension = MiscUtils.MAC ? new Dimension(50, 23) : new Dimension(27, 27);
        String shortcutMod = MiscUtils.MAC ? "Cmd" : "Ctrl";

        add(getRigidPadding());

        JLabel rangeText = new JLabel("Location: ");
        add(rangeText);

        int tfwidth = 240;
        int labwidth = 100;
        int tfheight = 22;
        rangeField = new JTextField() {
            @Override
            public void processKeyEvent(KeyEvent evt) {
                switch (evt.getKeyCode()) {
                    case KeyEvent.VK_ENTER:
                        setRangeFromTextBox();
                        break;
                    case KeyEvent.VK_LEFT:
                        if (evt.getModifiers() == KeyEvent.SHIFT_MASK) {
                            locationController.shiftRangeLeft();
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (evt.getModifiers() == KeyEvent.SHIFT_MASK) {
                            locationController.shiftRangeRight();
                        }
                        break;
                    case KeyEvent.VK_UP:
                        if (evt.getModifiers() == KeyEvent.SHIFT_MASK) {
                            locationController.zoomIn();
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        if (evt.getModifiers() == KeyEvent.SHIFT_MASK) {
                            locationController.zoomOut();
                        }
                        break;
                    case KeyEvent.VK_HOME:
                        locationController.shiftRangeFarLeft();
                        break;
                    case KeyEvent.VK_END:
                        locationController.shiftRangeFarRight();
                        break;
                    default:
                        return;
                }
                evt.consume();
            }
        };
        add(rangeField);
        rangeField.setToolTipText("Current display range");
        rangeField.setHorizontalAlignment(JTextField.CENTER);
        rangeField.setPreferredSize(new Dimension(tfwidth, tfheight));
        rangeField.setMaximumSize(new Dimension(tfwidth, tfheight));
        rangeField.setMinimumSize(new Dimension(tfwidth, tfheight));

        add(getRigidPadding());

        JButton goButton = (JButton)add(new JButton("  Go  "));
        goButton.putClientProperty("JButton.buttonType", buttonStyle);
        goButton.putClientProperty("JButton.segmentPosition", "only");
        goButton.setToolTipText("Go to specified range (Enter)");
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRangeFromTextBox();
            }
        });

        add(getRigidPadding());

        JLabel l = new JLabel("Length: ");
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
                    locationController.undoLocationChange();
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
                    locationController.redoLocationChange();
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
                locationController.zoomIn();
            }
        });

        JButton zoomOut = (JButton)add(new JButton(""));
        zoomOut.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.ZOOMOUT));
        zoomOut.setToolTipText("Zoom out (Shift+Down)");
        zoomOut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                locationController.zoomOut();
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
                locationController.shiftRangeFarLeft();
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
                locationController.shiftRangeLeft();
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
                locationController.shiftRangeRight();
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
                locationController.shiftRangeFarRight();
            }
        });

        add(getRigidPadding());

        locationController.addLocationChangedListener(new LocationChangedListener() {

            @Override
            public void genomeChanged(GenomeChangedEvent event) { }

            @Override
            public void locationChanged(LocationChangedEvent event) {
                updateLocation(event.getReference(), event.getRange());
            }
        });

    }

    private static Component getRigidPadding() {
        return Box.createRigidArea(new Dimension(BrowserSettings.padding, BrowserSettings.padding));
    }

    /**
     * Set the current range from the Zoom track bar.
     */
    private void setRangeFromTextBox() {

        String text = rangeField.getText();
        try {
            for (Track t: TrackController.getInstance().getTracks()) {
                Bookmark mark = t.lookup(text);
                if (mark != null) {
                    LocationController.getInstance().setLocation(mark.getReference(), mark.getRange().getFrom() - 1, mark.getRange().getTo() + 1);
                    return;
                }
            }
            // No lookup found, so try to parse it as a range string.
            Bookmark mark = new Bookmark(text);
            LocationController.getInstance().setLocation(mark.getReference(), (Range)mark.getRange());
        } catch (Exception x) {
            DialogUtils.displayMessage(String.format("Unabled to parse \"%s\" as a range.", text));
        }
    }

    private void updateLocation(String ref, Range r) {
        rangeField.setText(String.format("%s:%,d-%,d", ref, r.getFrom(), r.getTo()));
        lengthLabel.setText(String.format("%,d", r.getLength()));
        rangeField.requestFocusInWindow();
        rangeField.selectAll();
    }
}
