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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import com.jidesoft.docking.DockableFrame;
import java.awt.event.MouseListener;
import javax.swing.event.MouseInputListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.api.util.DialogUtils;
import savant.controller.DockableFrameController;

import savant.controller.ReferenceController;
import savant.controller.DrawModeController;
import savant.controller.FrameController;
import savant.controller.RangeController;
import savant.controller.event.DrawModeChangedEvent;
import savant.data.event.DataRetrievalEvent;
import savant.data.event.DataRetrievalListener;
import savant.data.event.TrackCreationEvent;
import savant.data.event.TrackCreationListener;
import savant.file.DataFormat;
import savant.settings.ColourSettings;
import savant.swing.component.ProgressPanel;
import savant.util.Range;
import savant.view.icon.SavantIconFactory;
import savant.view.swing.interval.BAMCoverageTrack;
import savant.view.swing.interval.BAMTrack;
import savant.view.swing.interval.BAMTrackRenderer;
import savant.view.swing.interval.BEDTrack;

/**
 *
 * @author mfiume
 */
public class Frame extends DockableFrame implements DataRetrievalListener, TrackCreationListener {

    private static final Log LOG = LogFactory.getLog(Frame.class);
    private GraphPane graphPane;
    private JLayeredPane frameLayeredPane;
    private Track[] tracks = new Track[0];
    private boolean isLocked;
    private Range currentRange;
    private JLayeredPane trackLayeredPane;
    private JMenuBar commandBar;
    private JPanel arcLegend;
    private List<JCheckBoxMenuItem> visItems;
    
    private FrameControlPanel controlPanel;
    private FrameSidePanel sidePanel;
    public JScrollPane scrollPane;
    private FrameControl trackMoveControl;
    private FrameControl settingsControl;
    private JPopupMenu settingsPopup;
    private List<JMenu> additionalSettingsMenu = new ArrayList<JMenu>();
    private JMenu bedMenu;
    private JMenu intervalMenu;
    private JMenu arcMenu;

    /*
    private JMenu arcButton;
    private JMenu intervalButton;
    private JMenu bedButton;
     * 
     */

    @Override
    public boolean isDraggingTarget(MouseEvent e) {
        return super.isDraggingTarget(e) || e.getSource() == trackMoveControl;
    }

    public Frame() {
        super(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK));

        // Panel which holds the legend component (when present).
        arcLegend = new JPanel();
        arcLegend.setLayout(new BorderLayout());
        arcLegend.setVisible(false);

        isLocked = false;

        frameLayeredPane = new JLayeredPane();
        graphPane = new GraphPane(this);
        graphPane.setBackground(ColourSettings.getFrameBackground());

        final MouseInputListener listener = this.createDockableFrameMouseInputListener();
        trackMoveControl = new FrameControl(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK_MOVE).getImage());
        trackMoveControl.addMouseListener(listener);
        trackMoveControl.addMouseMotionListener(listener);

        //scrollpane
        scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setBorder(null);

        //hide commandBar while scrolling
        /*MouseAdapter ml = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
        tempHideCommands();
        }
        @Override
        public void mouseReleased(MouseEvent e) {
        tempShowCommands();
        }
        };
        scrollPane.getVerticalScrollBar().addMouseListener(ml);
        JScrollBar vsb = scrollPane.getVerticalScrollBar();
        for(int i = 0; i < vsb.getComponentCount(); i++){
        vsb.getComponent(i).addMouseListener(ml);
        }*/

        //add graphPane -> jlp -> scrollPane
        trackLayeredPane = new JLayeredPane();
        trackLayeredPane.setLayout(new GridBagLayout());
        GridBagConstraints fillConstraints = new GridBagConstraints();
        fillConstraints.fill = GridBagConstraints.BOTH;
        fillConstraints.weightx = 1.0;
        fillConstraints.weighty = 1.0;
        fillConstraints.gridwidth = 2;
        fillConstraints.gridx = 0;
        fillConstraints.gridy = 0;

        GridBagConstraints centerConstraints = new GridBagConstraints();
        centerConstraints.weightx = 1.0;
        centerConstraints.weighty = 0;
        centerConstraints.fill = GridBagConstraints.HORIZONTAL;
        centerConstraints.anchor = GridBagConstraints.NORTH;
        centerConstraints.gridwidth = 2;
        centerConstraints.gridx = 0;
        centerConstraints.gridy = 0;

        GridBagConstraints leftSideConstraints = new GridBagConstraints();
        leftSideConstraints.weightx = 1.0;
        leftSideConstraints.weighty = 0;
        leftSideConstraints.fill = GridBagConstraints.HORIZONTAL;
        leftSideConstraints.anchor = GridBagConstraints.NORTHWEST;
        leftSideConstraints.gridx = 0;
        leftSideConstraints.gridy = 0;

        trackLayeredPane.add(graphPane, fillConstraints, 0);

        scrollPane.getViewport().add(trackLayeredPane);

        frameLayeredPane.setLayout(new GridBagLayout());

        this.controlPanel = new FrameControlPanel(this);

        //add sidepanel
        sidePanel = new FrameSidePanel();
        sidePanel.setVisible(false);

        //initCommandBar();
        //sidePanel.addPanel(commandBar);
        sidePanel.addPanel(arcLegend);

        frameLayeredPane.add(scrollPane, fillConstraints);
        frameLayeredPane.add(sidePanel, leftSideConstraints);
        frameLayeredPane.add(controlPanel, centerConstraints);

        frameLayeredPane.setLayer(scrollPane, 0, -1);
        frameLayeredPane.setLayer(sidePanel, 0, 1);
        frameLayeredPane.setLayer(controlPanel, 0, 0);

        // Add our progress-panel.  If setTracks is called promptly, it will be cleared
        // away before it ever has a chance to draw.
        getContentPane().add(new ProgressPanel());
    }

    public JLayeredPane getFrameLandscape() {
        return frameLayeredPane;
    }

    public GraphPane getGraphPane() {
        return graphPane;
    }

    public JScrollBar getVerticalScrollBar() {
        return scrollPane.getVerticalScrollBar();
    }

    public final Track[] getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> newTracks) {
        setTracks(newTracks.toArray(tracks));
    }

    private void showSettingsMenu() {

        if (settingsPopup == null) {
            settingsPopup = createOptionsMenu();
            for (JMenu m : additionalSettingsMenu) {
                settingsPopup.add(m);
            }
        }
        
        settingsPopup.show(settingsControl, 0, 15);


    }

    /**
     * Set the tracks associated with this frame.  Normally, this should only be done
     * once, since the Frame also uses this opportunity to set up some GUI elements
     * which depend on the presence of loaded tracks.
     *
     * @param newTracks the tracks to be displayed in this frame
     */
    public void setTracks(Track[] newTracks) {

        if (!ReferenceController.getInstance().isGenomeLoaded()) {
            if (newTracks[0].getDataFormat() == DataFormat.SEQUENCE_FASTA) {
                Savant.getInstance().setGenomeFromTrack(newTracks[0], this);
            } else {
                trackCreationFailed(null);
                DialogUtils.displayError("Sorry", "This does not appear to be a genome track. Please load a genome first.");
            }
            return;
        }

        LOG.trace("Frame being set up with " + newTracks.length + " tracks.");
        tracks = newTracks;
        graphPane.setTracks(tracks);

        for (Track t : tracks) {
            t.setFrame(this);

            //CREATE LEGEND PANEL
            if (t.getDataSource().getDataFormat() == DataFormat.INTERVAL_BAM) {
                arcLegend.add(t.getRenderer().arcLegendPaint());
            }
        }

        // We get the name and other properties from the zero'th track.
        Track t0 = tracks[0];
        setName(t0.getName());
        setKey(t0.getName());

        /** MENU **/

        // name of track
        controlPanel.addFrameControl(new FrameControl(t0.getName()));

        //.add(Box.createHorizontalGlue());

        // adjust settings
        settingsControl = new FrameControl(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SETTINGS).getImage());
        settingsControl.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                showSettingsMenu();
                
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        settingsControl.setToolTipText("Track settings");
        controlPanel.addFrameControl(settingsControl);

        if (t0.getDrawModes().size() > 0) {
            // display mode
            final FrameControl displayModeControl = new FrameControl(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK_DISPLAYMODE).getImage());
            displayModeControl.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    JPopupMenu popup = createVisualizationModeMenu();
                    popup.show(displayModeControl, 0, 15);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }

            });
            controlPanel.addFrameControl(displayModeControl);
            displayModeControl.setToolTipText("Visualization mode");
        }

        // move track
        controlPanel.addFrameControl(trackMoveControl);
        trackMoveControl.setToolTipText("Click and drag to reorder track");

        // close the track
        final Frame instance = this;

        final FrameControl maximizeControl = new FrameControl(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.PLUS).getImage());
        final FrameControl minimizeControl = new FrameControl(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.MINUS).getImage());
        controlPanel.addFrameControl(maximizeControl);
        controlPanel.addFrameControl(minimizeControl);
        minimizeControl.setVisible(false);
        controlPanel.remove(controlPanel.getComponentCount()-2);
        maximizeControl.setToolTipText("Maximize track");
        minimizeControl.setToolTipText("Restore track");
        maximizeControl.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                    //instance.setMaximized(true);
                    instance.getDockingManager().maximizeFrame(instance.getKey());
                    maximizeControl.setVisible(false);
                    minimizeControl.setVisible(true);
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        minimizeControl.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                    //instance.setMaximized(false);
                    instance.getDockingManager().restoreFrame();//instance.getKey());
                    maximizeControl.setVisible(true);
                    minimizeControl.setVisible(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });


        FrameControl closeFrameControl = new FrameControl(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK_CLOSE).getImage());
        controlPanel.addFrameControl(closeFrameControl);
        closeFrameControl.setToolTipText("Close track");
        closeFrameControl.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                int result = DialogUtils.askYesNo("Close Track", "Are you sure you want to close this track?");
                if (result == DialogUtils.YES) {
                    FrameController.getInstance().closeFrameAndTrack(instance);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        /*
        if (t0.getDrawModes().size() > 0) {
            JMenu displayMenu = createDisplayMenu();
            commandBar.add(displayMenu);
        }
         * 
         */

        // TODO: Should we really be doing BAM-specific stuff in this class?
        if (t0.getDataSource().getDataFormat() == DataFormat.INTERVAL_BAM) {
            arcMenu = createArcButton();
            this.addToSettingsMenu(arcMenu);
            //commandBar.add(arcButton);
            arcMenu.setVisible(false);

            intervalMenu = createIntervalButton();
            this.addToSettingsMenu(intervalMenu);
            //commandBar.add(intervalButton);
            String drawMode = t0.getDrawMode();
            if (drawMode.equals(BAMTrackRenderer.ARC_PAIRED_MODE) || drawMode.equals(BAMTrackRenderer.SNP_MODE)) {
                intervalMenu.setVisible(false);
            }
            if (drawMode.equals("STANDARD") || drawMode.equals("VARIANTS")) {
                intervalMenu.setVisible(true);
            }
        } else if (t0.getDataSource().getDataFormat() == DataFormat.INTERVAL_BED) {
            bedMenu = createBEDButton();
            addToSettingsMenu(bedMenu);
            //commandBar.add(bedButton);
        }

        JPanel contentPane = (JPanel) getContentPane();

        // This calls drawFrames() which sets up the initial render.
        FrameController.getInstance().addFrame(this, contentPane);

        contentPane.setLayout(new BorderLayout());
        contentPane.add(frameLayeredPane);
    }

    /**
     * Convenience method.  More often than not, we just want the frame to host a single track.
     *
     * @param newTrack the track to be used for this frame.
     */
    public void setTrack(Track newTrack) {
        setTracks(new Track[]{newTrack});
    }

    public boolean isOpen() {
        return getGraphPane() != null;
    }

    public void setActiveFrame() {
        sidePanel.setVisible(true);
    }

    public void setInactiveFrame() {
        sidePanel.setVisible(false);
    }

    public void resetLayers() {
        frameLayeredPane.moveToBack(graphPane);
    }

    /**
     * Create command bar
     *
    private void initCommandBar() {
        commandBar = new JMenuBar();
        JMenu optionsMenu = createOptionsMenu();
        commandBar.add(optionsMenu);
        commandBar.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
    }
     */

    public void redrawSidePanel() {
        this.sidePanel.repaint();
    }

    public void redrawBanner() {
        this.controlPanel.repaint();
    }

    /**
     * Add a component to the set of panels on side
     */
    public void addToSidePanel(JComponent comp) {
        this.sidePanel.addPanel(comp);
    }

    /**
     * Create the button to hide the commandBar
     */
    /*private JideButton createHideButton() {
    JideButton button = new JideButton();
    button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/arrow_left.png")));
    button.setToolTipText("Hide this toolbar");
    button.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
    commandBar.setVisible(false);
    commandBarHidden.setVisible(true);
    commandBarActive = false;
    ((JideButton)commandBarHidden.getComponent(commandBarHidden.getComponentCount()-1)).setFocusPainted(false);
    }
    });
    button.setFocusPainted(false);
    return button;
    }*/
    /**
     * Create the button to show the commandBar
     */
    /*private JideButton createShowButton() {
    JideButton button = new JideButton();
    button.setLayout(new BorderLayout());
    JLabel l1 = new JLabel("Settings");
    l1.setOpaque(false);
    button.add(l1, BorderLayout.WEST);
    JLabel l2 = new JLabel(new javax.swing.ImageIcon(getClass().getResource("/savant/images/arrow_right.png")));
    button.add(l2, BorderLayout.EAST);
    button.setToolTipText("Show the toolbar");
    button.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
    commandBar.setVisible(true);
    commandBarHidden.setVisible(false);
    commandBarActive = true;
    }
    });
    button.setFocusPainted(false);
    return button;
    }*/
    /**
     * Create options menu for commandBar
     */
    private JPopupMenu createOptionsMenu() {
        JCheckBoxMenuItem item;
        //JMenu menu = new JideMenu("Settings");
        JPopupMenu menu = new JPopupMenu();
        item = new JCheckBoxMenuItem("Lock Track");
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                graphPane.setLocked(!graphPane.isLocked());
            }
        });
        menu.add(item);

        JMenuItem item1;
        item1 = new JMenuItem("Colour Settings...");
        item1.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tracks[0].captureColorParameters();
            }
        });
        menu.add(item1);
        return menu;
    }

    /**
     * Create the button to show the arc params dialog
     */
    private JMenu createArcButton() {
        JMenu button = new JMenu("Read Pair Settings");
        button.setToolTipText("Change mate pair parameters");
        button.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                graphPane.getBAMParams((BAMTrack) tracks[0]);
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });
        button.setFocusPainted(false);
        return button;
    }

    /**
     * Create interval button for commandBar
     */
    private JMenu createIntervalButton() {
        JMenu button = new JMenu("Interval Options");
        button.setToolTipText("Change interval display parameters");
        button.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                tracks[0].captureIntervalParameters();
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });
        button.setFocusPainted(false);
        return button;
    }

    /**
     * Create interval button for commandBar
     */
    private JMenu createBEDButton() {
        JMenu button = new JMenu("BED Options");
        button.setToolTipText("Change BED display parameters");

        final JCheckBoxMenuItem itemRGB = new JCheckBoxMenuItem("Enable ItemRGB");
        final JCheckBoxMenuItem score = new JCheckBoxMenuItem("Enable Score") {
        };

        itemRGB.setState(false);
        score.setState(false);

        itemRGB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ((BEDTrack) tracks[0]).toggleItemRGBEnabled();
                graphPane.setRenderForced();
                graphPane.repaint();
            }
        });
        score.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ((BEDTrack) tracks[0]).toggleScoreEnabled();
                graphPane.setRenderForced();
                graphPane.repaint();
            }
        });

        button.add(itemRGB);
        button.add(score);

        button.setFocusPainted(false);
        return button;
    }

    /**
     * Create display menu for commandBar
     */
    private JPopupMenu createVisualizationModeMenu() {
        JPopupMenu menu = new JPopupMenu();

        //display modes
        List<String> drawModes = this.tracks[0].getDrawModes();
        visItems = new ArrayList<JCheckBoxMenuItem>();
        for (int i = 0; i < drawModes.size(); i++) {
            final JCheckBoxMenuItem item = new JCheckBoxMenuItem(drawModes.get(i));
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (item.getState()) {
                        for (int j = 0; j < visItems.size(); j++) {
                            visItems.get(j).setState(false);
                            if (item.getText().equals(tracks[0].getDrawModes().get(j))) {
                                DrawModeController.getInstance().switchMode(tracks[0], tracks[0].getDrawModes().get(j));
                            }
                        }
                    }
                    item.setState(true);
                }
            });
            if (drawModes.get(i).equals(tracks[0].getDrawMode())) {
                item.setState(true);
            }
            visItems.add(item);
            menu.add(item);
        }
        return menu;
    }

    public void redrawTracksInRange() {
        drawTracksInRange(ReferenceController.getInstance().getReferenceName(), currentRange);
    }

    /**
     * Prepare the data for the tracks in range and fire off a repaint.
     *
     * @param range
     */
    public void drawTracksInRange(String reference, Range range) {
        if (!isLocked()) {
            currentRange = range;
        }
        if (graphPane.isLocked()) {
            return;
        }

        graphPane.setXRange(currentRange);

        for (Track t : tracks) {
            t.getRenderer().clearInstructions();
            t.prepareForRendering(reference, range);
        }
        resetLayers();
    }

    // TODO: what is locking for?
    public void lockRange(Range r) {
        setLocked(true, r);
    }

    public void unlockRange() {
        setLocked(false, null);
    }

    public void setLocked(boolean b, Range r) {
        this.isLocked = b;
        this.currentRange = r;
    }

    public boolean isLocked() {
        return this.isLocked;
    }

    // FIXME: this is a horrible kludge
    public void drawModeChanged(DrawModeChangedEvent evt) {

        Track track = evt.getTrack();

        boolean reRender = true;
        if (track.getDataSource().getDataFormat() == DataFormat.INTERVAL_BAM) {
            if (evt.getMode().equals(BAMTrackRenderer.ARC_PAIRED_MODE)) {
                setCoverageEnabled(false);
                intervalMenu.setVisible(false);
                arcMenu.setVisible(true);
                arcLegend.setVisible(true);
            } else {
                setCoverageEnabled(true);
                //show interval options button unless in snp or arc modes
                intervalMenu.setVisible(!evt.getMode().equals(BAMTrackRenderer.SNP_MODE));
                arcMenu.setVisible(false);
                arcLegend.setVisible(false);
            }
        }
        if (reRender) {
            validate();
            drawTracksInRange(ReferenceController.getInstance().getReferenceName(), RangeController.getInstance().getRange());
        }
    }

    private void setCoverageEnabled(boolean enabled) {

        for (Track track : getTracks()) {
            if (track instanceof BAMCoverageTrack) {
                ((BAMCoverageTrack) track).setEnabled(enabled);
            }
        }
    }

    /**
     * Create a new panel to draw on.
     */
    public JPanel getLayerCanvas() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 2;
        trackLayeredPane.add(p, c, 2);
        trackLayeredPane.setLayer(p, 50);
        return p;
    }

    /**
     * Export this frame as an image.
     */
    public BufferedImage frameToImage() {
        BufferedImage bufferedImage = new BufferedImage(graphPane.getWidth(), graphPane.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bufferedImage.createGraphics();
        graphPane.setRenderForced();
        graphPane.forceFullHeight();
        graphPane.render(g);
        graphPane.unforceFullHeight();
        g.setColor(Color.black);
        g.setFont(new Font(null, Font.BOLD, 13));
        g.drawString(tracks[0].getName(), 2, 15);
        return bufferedImage;
    }

    @Override
    public void dataRetrievalStarted(DataRetrievalEvent evt) {
    }

    @Override
    public void dataRetrievalCompleted(DataRetrievalEvent evt) {
        LOG.trace("Frame received dataRetrievalCompleted. Forcing full render.");
        graphPane.setRenderForced();
        graphPane.repaint();
    }

    @Override
    public void dataRetrievalFailed(DataRetrievalEvent evt) {
        LOG.trace("Frame received dataRetrievalFailed. Forcing full render.");
        graphPane.setRenderForced();
        graphPane.repaint();
    }

    @Override
    public void trackCreationStarted(TrackCreationEvent evt) {
    }

    @Override
    public void trackCreationCompleted(TrackCreationEvent evt) {
        setTracks(evt.getTracks());
    }

    @Override
    public void trackCreationFailed(TrackCreationEvent evt) {
        DockableFrameController.getInstance().closeDockableFrame(this, false);
    }

    private void addToSettingsMenu(JMenu m) {
        this.additionalSettingsMenu.add(m);
    }
}
