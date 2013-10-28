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
package savant.view.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.apache.commons.httpclient.NameValuePair;
import org.ut.biolab.savant.analytics.savantanalytics.AnalyticsAgent;

import savant.api.adapter.BookmarkAdapter;
import savant.api.event.LocationChangedEvent;
import savant.api.util.DialogUtils;
import savant.api.util.Listener;
import savant.controller.GenomeController;
import savant.controller.LocationController;
import savant.controller.TrackController;
import savant.api.event.GenomeChangedEvent;
import savant.util.Bookmark;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.icon.SavantIconFactory;
import savant.view.tracks.Track;

/**
 * Contains the various widgets for providing easy range navigation.
 *
 * @author tarkvara
 */
public class NavigationBar extends JPanel {

    private static final Dimension LOCATION_SIZE = new Dimension(270, 22);
    private static final Dimension LENGTH_SIZE = new Dimension(100, 22);
    private static final Dimension ICON_SIZE = MiscUtils.MAC ? new Dimension(50, 23) : new Dimension(27, 27);
    private LocationController locationController = LocationController.getInstance();
    /**
     * Range text-box
     */
    JComboBox locationField;
    /**
     * Length being displayed
     */
    private JLabel lengthLabel;
    /**
     * Last string used when popping up the combo-box. Saves us from having to
     * regenerate the menu. Initially set to non-null so that first call to
     * populateCombo() will actually do something.
     */
    private String lastPoppedUp = "INVALID";
    /**
     * Flag to prevent action-events from being fired when we're populating the
     * menu.
     */
    private boolean currentlyPopulating = false;

    NavigationBar() {

        this.setOpaque(false);
        this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));

        String buttonStyle = "segmentedCapsule";

        String shortcutMod = MiscUtils.MAC ? "Cmd" : "Ctrl";

        add(getRigidPadding());

        JButton loadGenomeButton = (JButton) add(new JButton(""));
        loadGenomeButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.GENOME));
        loadGenomeButton.setToolTipText("Load or change genome");
        loadGenomeButton.setFocusable(false);
        loadGenomeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Savant.getInstance().showOpenGenomeDialog();
            }
        });
        loadGenomeButton.putClientProperty("JButton.buttonType", buttonStyle);
        loadGenomeButton.putClientProperty("JButton.segmentPosition", "first");
        loadGenomeButton.setPreferredSize(ICON_SIZE);
        loadGenomeButton.setMinimumSize(ICON_SIZE);
        loadGenomeButton.setMaximumSize(ICON_SIZE);

        JButton loadTrackButton = (JButton) add(new JButton(""));
        loadTrackButton.setFocusable(false);
        loadTrackButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK));
        loadTrackButton.setToolTipText("Load a track");
        loadTrackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Savant.getInstance().openTrack();
            }
        });
        loadTrackButton.putClientProperty("JButton.buttonType", buttonStyle);
        loadTrackButton.putClientProperty("JButton.segmentPosition", "last");
        loadTrackButton.setPreferredSize(ICON_SIZE);
        loadTrackButton.setMinimumSize(ICON_SIZE);
        loadTrackButton.setMaximumSize(ICON_SIZE);

        if (!Savant.getInstance().isStandalone()) {
            add(loadGenomeButton);
            add(loadTrackButton);
            add(getRigidPadding());
            add(getRigidPadding());
        } else {
            loadGenomeButton.setVisible(false);
            loadTrackButton.setVisible(false);
        }

        JLabel rangeText = new JLabel("Location ");
        add(rangeText);

        String[] a = {" ", " ", " ", " ", " ", " ", " ", " ", " ", " "};
        locationField = new JComboBox(a);
        locationField.setEditable(true);
        locationField.setRenderer(new ReferenceListRenderer());


        // When the item is chosen from the menu, navigate to the given feature/reference.
        locationField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (!currentlyPopulating) {
                    if (ae.getActionCommand().equals("comboBoxChanged")) {
                        // Assumes that combo-box items created by populateCombo() are of the form "GENE (chrX:1-1000)".
                        String itemText = locationField.getSelectedItem().toString();
                        int lastBracketPos = itemText.lastIndexOf('(');
                        if (lastBracketPos > 0) {
                            itemText = itemText.substring(lastBracketPos + 1, itemText.length() - 1);
                        }
                        setRangeFromText(itemText);

                    }
                }
            }
        });

        // When the combo-box is popped open, we may want to repopulate the menu.
        locationField.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {
                String text = (String) locationField.getEditor().getItem();
                if (!text.equals(lastPoppedUp)) {
                    try {
                        // Building the menu could take a while.
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        populateCombo();
                    } finally {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent pme) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent pme) {
            }
        });

        // Add our special keystroke-handling to the JComboBox' text-field.
        // We have to turn off default tab-handling so that tab can pop up our list.
        Component textField = locationField.getEditor().getEditorComponent();
        textField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_TAB) {
                    locationField.showPopup();
                } else if (evt.getModifiers() == KeyEvent.SHIFT_MASK) {
                    switch (evt.getKeyCode()) {
                        case KeyEvent.VK_LEFT:
                            locationController.shiftRangeLeft();
                            evt.consume();
                            break;
                        case KeyEvent.VK_RIGHT:
                            locationController.shiftRangeRight();
                            evt.consume();
                            break;
                        case KeyEvent.VK_UP:
                            locationController.zoomIn();
                            evt.consume();
                            break;
                        case KeyEvent.VK_DOWN:
                            locationController.zoomOut();
                            evt.consume();
                            break;
                        case KeyEvent.VK_HOME:
                            locationController.shiftRangeFarLeft();
                            evt.consume();
                            break;
                        case KeyEvent.VK_END:
                            locationController.shiftRangeFarRight();
                            evt.consume();
                            break;
                    }
                }
            }
        });
        add(locationField);
        locationField.setToolTipText("Current display range");
        locationField.setPreferredSize(LOCATION_SIZE);
        locationField.setMaximumSize(LOCATION_SIZE);
        locationField.setMinimumSize(LOCATION_SIZE);

        add(getRigidPadding());

        JButton goButton = (JButton) add(new JButton("  Go  "));
        goButton.putClientProperty("JButton.buttonType", buttonStyle);
        goButton.putClientProperty("JButton.segmentPosition", "only");
        goButton.setToolTipText("Go to specified range (Enter)");
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRangeFromText(locationField.getEditor().getItem().toString());
            }
        });

        add(getRigidPadding());

        JLabel l = new JLabel("Length: ");
        add(l);

        lengthLabel = (JLabel) add(new JLabel());
        lengthLabel.setToolTipText("Length of the current range");
        lengthLabel.setPreferredSize(LENGTH_SIZE);
        lengthLabel.setMaximumSize(LENGTH_SIZE);
        lengthLabel.setMinimumSize(LENGTH_SIZE);

        add(Box.createGlue());

        double screenwidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();

        JButton afterGo = null;
        //if (screenwidth > 800) {
        final JButton undoButton = (JButton) add(new JButton(""));
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
        undoButton.setPreferredSize(ICON_SIZE);
        undoButton.setMinimumSize(ICON_SIZE);
        undoButton.setMaximumSize(ICON_SIZE);

        final JButton redo = (JButton) add(new JButton(""));
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
        redo.setPreferredSize(ICON_SIZE);
        redo.setMinimumSize(ICON_SIZE);
        redo.setMaximumSize(ICON_SIZE);
        //}

        add(getRigidPadding());
        add(getRigidPadding());

        final JButton zoomInButton = (JButton) add(new JButton());
        if (afterGo == null) {
            afterGo = zoomInButton;
        }
        zoomInButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.ZOOMIN));
        zoomInButton.putClientProperty("JButton.buttonType", buttonStyle);
        zoomInButton.putClientProperty("JButton.segmentPosition", "first");
        zoomInButton.setPreferredSize(ICON_SIZE);
        zoomInButton.setMinimumSize(ICON_SIZE);
        zoomInButton.setMaximumSize(ICON_SIZE);
        zoomInButton.setToolTipText("Zoom in (Shift+Up)");
        zoomInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                locationController.zoomIn();
                AnalyticsAgent.log(
                        new NameValuePair[]{
                            new NameValuePair("navigation-event", "zoomed"),
                            new NameValuePair("navigation-direction", "in"),
                            new NameValuePair("navigation-modality", "navbar")
                        });
            }
        });

        final JButton zoomOut = (JButton) add(new JButton(""));
        zoomOut.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.ZOOMOUT));
        zoomOut.setToolTipText("Zoom out (Shift+Down)");
        zoomOut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                locationController.zoomOut();
                AnalyticsAgent.log(
                        new NameValuePair[]{
                            new NameValuePair("navigation-event", "zoomed"),
                            new NameValuePair("navigation-direction", "out"),
                            new NameValuePair("navigation-modality", "navbar")
                        });
            }
        });
        zoomOut.putClientProperty("JButton.buttonType", buttonStyle);
        zoomOut.putClientProperty("JButton.segmentPosition", "last");
        zoomOut.setPreferredSize(ICON_SIZE);
        zoomOut.setMinimumSize(ICON_SIZE);
        zoomOut.setMaximumSize(ICON_SIZE);

        add(getRigidPadding());
        add(getRigidPadding());

        final JButton shiftFarLeft = (JButton) add(new JButton());
        shiftFarLeft.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SHIFT_FARLEFT));
        shiftFarLeft.putClientProperty("JButton.buttonType", buttonStyle);
        shiftFarLeft.putClientProperty("JButton.segmentPosition", "first");
        shiftFarLeft.setToolTipText("Move to the beginning of the genome (Shift+Home)");
        shiftFarLeft.setPreferredSize(ICON_SIZE);
        shiftFarLeft.setMinimumSize(ICON_SIZE);
        shiftFarLeft.setMaximumSize(ICON_SIZE);
        shiftFarLeft.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                locationController.shiftRangeFarLeft();
                AnalyticsAgent.log(
                        new NameValuePair[]{
                            new NameValuePair("navigation-event", "panned"),
                            new NameValuePair("navigation-direction", "left"),
                            new NameValuePair("navigation-modality", "navbar")
                        });
            }
        });

        final JButton shiftLeft = (JButton) add(new JButton());
        shiftLeft.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SHIFT_LEFT));
        shiftLeft.putClientProperty("JButton.buttonType", buttonStyle);
        shiftLeft.putClientProperty("JButton.segmentPosition", "middle");
        shiftLeft.setToolTipText("Move left (Shift+Left)");
        shiftLeft.setPreferredSize(ICON_SIZE);
        shiftLeft.setMinimumSize(ICON_SIZE);
        shiftLeft.setMaximumSize(ICON_SIZE);
        shiftLeft.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                locationController.shiftRangeLeft();
                AnalyticsAgent.log(
                        new NameValuePair[]{
                            new NameValuePair("navigation-event", "panned"),
                            new NameValuePair("navigation-direction", "left"),
                            new NameValuePair("navigation-modality", "navbar")
                        });
            }
        });

        final JButton shiftRight = (JButton) add(new JButton());
        shiftRight.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SHIFT_RIGHT));
        shiftRight.putClientProperty("JButton.buttonType", buttonStyle);
        shiftRight.putClientProperty("JButton.segmentPosition", "middle");
        shiftRight.setToolTipText("Move right (Shift+Right)");
        shiftRight.setPreferredSize(ICON_SIZE);
        shiftRight.setMinimumSize(ICON_SIZE);
        shiftRight.setMaximumSize(ICON_SIZE);
        shiftRight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                locationController.shiftRangeRight();
                AnalyticsAgent.log(
                        new NameValuePair[]{
                            new NameValuePair("navigation-event", "panned"),
                            new NameValuePair("navigation-direction", "right"),
                            new NameValuePair("navigation-modality", "navbar")
                        });
            }
        });

        final JButton shiftFarRight = (JButton) add(new JButton());
        shiftFarRight.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SHIFT_FARRIGHT));
        shiftFarRight.putClientProperty("JButton.buttonType", buttonStyle);
        shiftFarRight.putClientProperty("JButton.segmentPosition", "last");
        shiftFarRight.setToolTipText("Move to the end of the genome (Shift+End)");
        shiftFarRight.setPreferredSize(ICON_SIZE);
        shiftFarRight.setMinimumSize(ICON_SIZE);
        shiftFarRight.setMaximumSize(ICON_SIZE);
        shiftFarRight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                locationController.shiftRangeFarRight();
                AnalyticsAgent.log(
                        new NameValuePair[]{
                            new NameValuePair("navigation-event", "panned"),
                            new NameValuePair("navigation-direction", "right"),
                            new NameValuePair("navigation-modality", "navbar")
                        });
            }
        });

        add(getRigidPadding());

        locationController.addListener(new Listener<LocationChangedEvent>() {
            @Override
            public void handleEvent(LocationChangedEvent event) {
                updateLocation(event.getReference(), (Range) event.getRange());
            }
        });

        // When the genome changes, we may need to invalidate our menu.
        GenomeController.getInstance().addListener(new Listener<GenomeChangedEvent>() {
            @Override
            public void handleEvent(GenomeChangedEvent event) {
                lastPoppedUp = "INVALID";
            }
        });

        this.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent ce) {
                int width = ce.getComponent().getWidth();

                undoButton.setVisible(true);
                redo.setVisible(true);
                zoomInButton.setVisible(true);
                zoomOut.setVisible(true);
                shiftFarLeft.setVisible(true);
                shiftLeft.setVisible(true);
                shiftRight.setVisible(true);
                shiftFarRight.setVisible(true);

                // hide some components if the window isn't wide enough
                if (width < 1200) {
                    undoButton.setVisible(false);
                    redo.setVisible(false);
                }
                if (width < 1000) {
                    shiftFarLeft.setVisible(false);
                    shiftFarRight.setVisible(false);

                    shiftRight.putClientProperty("JButton.segmentPosition", "last");
                    shiftLeft.putClientProperty("JButton.segmentPosition", "first");
                } else {
                    shiftRight.putClientProperty("JButton.segmentPosition", "middle");
                    shiftLeft.putClientProperty("JButton.segmentPosition", "middle");
                }
            }

            public void componentMoved(ComponentEvent ce) {
            }

            @Override
            public void componentShown(ComponentEvent ce) {
            }

            @Override
            public void componentHidden(ComponentEvent ce) {
            }
        });
    }

    private static Component getRigidPadding() {
        return Box.createRigidArea(new Dimension(7, 7));
    }

    /**
     * Set the current range from a string which has been entered or selected.
     */
    private void setRangeFromText(String text) {
        try {
            for (Track t : TrackController.getInstance().getTracks()) {
                List<BookmarkAdapter> marks = t.getDataSource().lookup(text.toLowerCase());
                if (marks != null && marks.size() > 0) {
                    // Note that if there is more than one matching bookmark, this will select the first one.
                    // This allows a knowledgeable user to go directly to the desired gene without having to pop up the combo.
                    locationController.setLocation(marks.get(0).getReference(), (Range) marks.get(0).getRange());
                    return;
                }
            }
            // No lookup found, so try to parse it as a range string.
            Bookmark mark = new Bookmark(text);
            locationController.setLocation(mark.getReference(), (Range) mark.getRange());
        } catch (Exception x) {
            DialogUtils.displayMessage(String.format("Unable to parse \"%s\" as a location.", text));
        }
    }

    private void updateLocation(String ref, Range r) {
        String s = String.format("%s: %,d - %,d", ref, r.getFrom(), r.getTo());
        AnalyticsAgent.log(
                new NameValuePair[]{
                    new NameValuePair("navigation-event", "moved"),
                    new NameValuePair("navigation-range", s)
                });
        locationField.setSelectedItem(s);
        lengthLabel.setText(String.format("%,d", r.getLength()));
        locationField.requestFocusInWindow();
        locationField.getEditor().selectAll();
    }

    /**
     * The menu needs to be popped up. We may need to repopulate it.
     */
    private void populateCombo() {
        String text = (String) locationField.getEditor().getItem();
        if (!text.equals(lastPoppedUp)) {
            Collection<String> newItems = new ArrayList<String>();
            if (text.length() > 0) {
                for (Track t : TrackController.getInstance().getTracks()) {
                    List<BookmarkAdapter> marks = t.getDataSource().lookup(text.toLowerCase() + "*");
                    if (marks != null && marks.size() > 0) {
                        for (BookmarkAdapter bm : marks) {
                            newItems.add(String.format("%s (%s)", bm.getAnnotation(), ((Bookmark) bm).getLocationText()));
                        }
                    }
                }
            }
            if (newItems.size() > 0 || lastPoppedUp != null) {
                if (newItems.size() > 0) {
                    lastPoppedUp = text;
                } else {
                    lastPoppedUp = null;
                    newItems = GenomeController.getInstance().getGenome().getReferenceNames();
                }
                try {
                    currentlyPopulating = true;
                    locationField.removeAllItems();
                    for (String s : newItems) {
                        locationField.addItem(s);
                    }
                } finally {
                    currentlyPopulating = false;
                }
            }
        }
    }

    private class ReferenceListRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String s = (String) value;
            int bracketPos = s.lastIndexOf('(');
            if (bracketPos > 0) {
                c.setText(String.format("<html>%s <small>%s</small></html>", s.substring(0, bracketPos - 1), s.substring(bracketPos)));
            }
            return c;
        }
    }
}
