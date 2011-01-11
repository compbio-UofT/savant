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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import com.jidesoft.action.CommandBar;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideMenu;
import java.awt.event.MouseListener;
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

/**
 *
 * @author mfiume
 */
public class Frame extends DockableFrame implements DataRetrievalListener, TrackCreationListener {
    private static final Log LOG = LogFactory.getLog(Frame.class);

    private GraphPane graphPane;
    private JLayeredPane frameLandscape;
    private Track[] tracks = new Track[0];
    private boolean isLocked;
    private Range currentRange;

    private JLayeredPane jlp;

    JMenuBar commandBar;
    CommandBar commandBarHidden;
    JPanel arcLegend;
    private List<JCheckBoxMenuItem> visItems;
    private JMenu arcButton;
    private JMenu intervalButton;

    private boolean commandBarActive = true;

    public JScrollPane scrollPane;

    public Frame() {
        super(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK));

        //INIT LEGEND PANEL
        arcLegend = new JPanel();
        arcLegend.setLayout(new BorderLayout());
        arcLegend.setVisible(false);

        isLocked = false;

        frameLandscape = new JLayeredPane();
        graphPane = new GraphPane(this);
        graphPane.setBackground(ColourSettings.getFrameBackground());

                //scrollpane
        scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setBorder(null);

        //hide commandBar while scrolling
        MouseAdapter ml = new MouseAdapter() {
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
        }


        //add graphPane -> jlp -> scrollPane
        jlp = new JLayeredPane();
        jlp.setLayout(new GridBagLayout());
        GridBagConstraints gbc= new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        jlp.add(graphPane, gbc, 0);


        //scrollPane.getViewport().add(this.graphPane);
        scrollPane.getViewport().add(jlp);

        //frameLandscape.setLayout(new BorderLayout());
        //frameLandscape.add(getGraphPane());

        //COMMAND BAR
        commandBar = new JMenuBar();

        //THE FOLLOWING BLOCK ONLY APPLIED TO JIDE COMMANDBAR
        //commandBar = new CommandBar();
        //commandBar.setStretch(false);
        //commandBar.setPaintBackground(false);
        //commandBar.setChevronAlwaysVisible(false);

        commandBar.setOpaque(true);

        JMenu optionsMenu = createOptionsMenu();
        //JMenu infoMenu = createInfoMenu();
        //JideButton lockButton = createLockButton();
        JideButton hideButton = createHideButton();
        //JideButton colorButton = createColorButton();
        //commandBar.add(infoMenu);
        //commandBar.add(lockButton);
        //commandBar.add(colorButton);
        commandBar.add(optionsMenu);
        commandBar.add(new JSeparator(SwingConstants.VERTICAL));
        commandBar.add(hideButton);
        commandBar.setVisible(false);

        //COMMAND BAR HIDDEN
        //commandBarHidden.setVisible(false);
        commandBarHidden = new CommandBar();
        //commandBarHidden.setVisible(false);
        commandBarHidden.setOpaque(true);
        commandBarHidden.setChevronAlwaysVisible(false);
        JideButton showButton = createShowButton();
        commandBarHidden.add(showButton);
        commandBarHidden.setVisible(false);

        //GRID FRAMEWORK AND COMPONENT ADDING...
        frameLandscape.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        //force commandBars to be full size
        JPanel contain1 = new JPanel();
        contain1.setOpaque(false);
        contain1.setLayout(new BorderLayout());
        contain1.setCursor(new Cursor(Cursor.HAND_CURSOR));
        contain1.add(commandBar, BorderLayout.WEST);
        JPanel contain2 = new JPanel();
        contain2.setOpaque(false);
        contain2.setLayout(new BorderLayout());
        contain2.setCursor(new Cursor(Cursor.HAND_CURSOR));
        contain2.add(commandBarHidden, BorderLayout.WEST);

        //add commandBar/hidden to top left
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        frameLandscape.add(contain1, c, 5);
        frameLandscape.add(contain2, c, 6);
        //commandBarHidden.setVisible(true);

        //filler to extend commandBars
        JPanel a = new JPanel();
        a.setCursor(new Cursor(Cursor.HAND_CURSOR));
        a.setMinimumSize(new Dimension(400,30));
        a.setOpaque(false);
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        frameLandscape.add(a, c, 5);

        //add filler to top middle
        JLabel l = new JLabel();
        //l.setOpaque(false);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 1;
        c.gridy = 0;
        frameLandscape.add(l, c);

        //add arcLegend to bottom right
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 2;
        c.gridy = 1;
        c.anchor = GridBagConstraints.NORTHEAST;
        frameLandscape.add(arcLegend, c, 6);

        //add graphPane to all cells
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.gridheight = 2;
        frameLandscape.add(scrollPane, c, 0);

        frameLandscape.setLayer(commandBar, JLayeredPane.PALETTE_LAYER);
        frameLandscape.setLayer(scrollPane, JLayeredPane.DEFAULT_LAYER);

        // Add our progress-panel.  If setTracks is called promptly, it will be cleared
        // away before it ever has a chance to draw.
        getContentPane().add(new ProgressPanel());
    }

    public JLayeredPane getFrameLandscape() {
        return frameLandscape;
    }

    public GraphPane getGraphPane() {
        return graphPane;
    }

    public final Track[] getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> newTracks) {
        setTracks(newTracks.toArray(tracks));
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

        for (Track t: tracks) {
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
        if (t0.getDrawModes().size() > 0){
            JMenu displayMenu = createDisplayMenu();
            commandBar.add(displayMenu);
        }

        // TODO: Should we really be doing BAM-specific stuff in this class?
        if (t0.getDataSource().getDataFormat() == DataFormat.INTERVAL_BAM) {
            arcButton = createArcButton();
            commandBar.add(arcButton);
            arcButton.setVisible(false);

            intervalButton = createIntervalButton();
            commandBar.add(intervalButton);
            intervalButton.setVisible(false);
            String drawMode = t0.getDrawMode();
            if (drawMode.equals("STANDARD") || drawMode.equals("VARIANTS")){
                intervalButton.setVisible(true);
            }
        }

        JPanel contentPane = (JPanel)getContentPane();

        // This calls drawFrames() which sets up the initial render.
        FrameController.getInstance().addFrame(this, contentPane);

        contentPane.setLayout(new BorderLayout());
        contentPane.add(frameLandscape);
    }

    /**
     * Convenience method.  More often than not, we just want the frame to host a single track.
     *
     * @param newTrack the track to be used for this frame.
     */
    public void setTrack(Track newTrack) {
        setTracks(new Track[] { newTrack });
    }


    public boolean isOpen() { return getGraphPane() != null; }


    public void setActiveFrame(){
        if (commandBarActive) {
            commandBar.setVisible(true);
        } else {
            commandBarHidden.setVisible(true);
        }
    }

    public void setInactiveFrame(){
        commandBarHidden.setVisible(false);
        commandBar.setVisible(false);
    }

    public void resetLayers(){
        if (tracks.length > 0) {
            arcLegend.setVisible(tracks[0].getDrawModes().size() > 0 && tracks[0].getDrawMode().equals(BAMTrackRenderer.MATE_PAIRS_MODE));
        }
        frameLandscape.moveToBack(graphPane);
    }

    private void tempHideCommands(){
        if (isActive()) {
            commandBar.setVisible(false);
            commandBarHidden.setVisible(false);
            arcLegend.setVisible(false);
        }
    }

    public void tempShowCommands(){
        if (isActive()) {
            commandBar.setVisible(commandBarActive);
            commandBarHidden.setVisible(!commandBarActive);
            arcLegend.setVisible(tracks[0].getDrawModes().size() > 0 && tracks[0].getDrawMode().equals(BAMTrackRenderer.MATE_PAIRS_MODE));
        }
    }

    /**
     * Create the button to hide the commandBar
     */
    private JideButton createHideButton() {
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
    }

    /**
     * Create the button to show the commandBar
     */
    private JideButton createShowButton() {
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
    }

    /**
     * Create lock button for commandBar
     */
    private JideButton createColorButton() {
        //TODO: This is temporary until there is an options menu
        JideButton button = new JideButton("Colour Settings  ");
        button.setToolTipText("Change the colour scheme for this track");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tracks[0].captureColorParameters();
            }
        });
        button.setFocusPainted(false);
        return button;
    }

    /**
     * Create options menu for commandBar
     */
    private JMenu createOptionsMenu() {
        JCheckBoxMenuItem item;
        //JMenu menu = new JideMenu("Settings");
        JMenu menu = new JMenu("Settings");
        item = new JCheckBoxMenuItem("Lock Track");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                graphPane.switchLocked();
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
     * Create lock button for commandBar
     */
    private JideButton createLockButton() {
        //TODO: This is temporary until there is an options menu
        JideButton button = new JideButton("Lock Track  ");
        button.setToolTipText("Prevent range changes on this track");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JideButton button = new JideButton();
                for(int i = 0; i < commandBar.getComponentCount(); i++){
                    if(commandBar.getComponent(i).getClass() == JideButton.class){
                        button = (JideButton)commandBar.getComponent(i);
                        if(button.getText().equals("Lock Track  ") || button.getText().equals("Unlock Track  ")) break;
                    }
                }
                if(button.getText().equals("Lock Track  ")){
                    button.setText("Unlock Track  ");
                    button.setToolTipText("Allow range changes on this track");
                } else {
                    button.setText("Lock Track  ");
                    button.setToolTipText("Prevent range changes on this track");
                }
                graphPane.switchLocked();
                resetLayers();
            }
        });
        button.setFocusPainted(false);
        return button;
    }

    /**
     * Create info menu for commandBar
     */
    private JMenu createInfoMenu() {
        JMenu menu = new JideMenu("Info");
        JMenuItem item = new JMenuItem("Track Info...");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //TODO
            }
        });
        menu.add(item);

        return menu;
    }

    /**
     * Create the button to show the arc params dialog
     */
    private JMenu createArcButton() {
        JMenu button = new JMenu("Arc Options");
        button.setToolTipText("Change mate pair parameters");
        button.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                graphPane.getBAMParams((BAMTrack)tracks[0]);
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
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
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
        button.setFocusPainted(false);
        return button;
    }

    /**
     * Create display menu for commandBar
     */
    private JMenu createDisplayMenu() {
        JMenu menu = new JMenu("Display Mode");
        
        //display modes
        List<String> drawModes = this.tracks[0].getDrawModes();
        visItems = new ArrayList<JCheckBoxMenuItem>();
        for(int i = 0; i < drawModes.size(); i++){
            final JCheckBoxMenuItem item = new JCheckBoxMenuItem(drawModes.get(i));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(item.getState()){
                        for(int j = 0; j < visItems.size(); j++){
                            visItems.get(j).setState(false);
                            if(item.getText().equals(tracks[0].getDrawModes().get(j))){
                                DrawModeController.getInstance().switchMode(tracks[0], tracks[0].getDrawModes().get(j));
                            }
                        }
                    }
                    item.setState(true);
                }
            });
            if(drawModes.get(i).equals(tracks[0].getDrawMode())) {
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
        if (!isLocked()) { currentRange = range; }
        if (graphPane.isLocked()) { return; }

        graphPane.setXRange(currentRange);

        for (Track t : tracks) {
            t.prepareForRendering(reference, range);
        }
        resetLayers();
    }

    // TODO: what is locking for?
    public void lockRange(Range r) { setLocked(true, r); }
    public void unlockRange() { setLocked(false, null); }
    public void setLocked(boolean b, Range r) {
        this.isLocked = b;
        this.currentRange = r;
    }

    public boolean isLocked() { return this.isLocked; }

    // FIXME: this is a horrible kludge
    public void drawModeChanged(DrawModeChangedEvent evt) {

        Track track = evt.getTrack();

//        if (getTracks().contains(track)) {
        boolean reRender = true;
        if (track.getDataSource().getDataFormat() == DataFormat.INTERVAL_BAM) {
            if (evt.getMode().equals(BAMTrackRenderer.MATE_PAIRS_MODE)) {
                reRender = true;
                setCoverageEnabled(false);
                this.arcButton.setVisible(true);
                this.intervalButton.setVisible(false);
            }else {
                if(evt.getMode().equals(BAMTrackRenderer.STANDARD_MODE) || evt.getMode().equals(BAMTrackRenderer.MISMATCH_MODE)){
                    this.intervalButton.setVisible(true);
                } else {
                    this.intervalButton.setVisible(false);
                }
                setCoverageEnabled(true);
                reRender = true;
                this.arcButton.setVisible(false);
            }
        }
        if (reRender) {
            drawTracksInRange(ReferenceController.getInstance().getReferenceName(), RangeController.getInstance().getRange());
        }
//        }
    }

    private void setCoverageEnabled(boolean enabled) {

        for (Track track: getTracks()) {
            if (track instanceof BAMCoverageTrack) {
                ((BAMCoverageTrack) track).setEnabled(enabled);
            }
        }
    }

    /**
     * Create a new panel to draw on.
     */
    public JPanel getLayerCanvas(){
        JPanel p = new JPanel();
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.gridheight = 2;
        jlp.add(p,c,2);
        jlp.setLayer(p, 50);
        return p;
    }

    /**
     * Export this frame as an image.
     */
    public BufferedImage frameToImage(){
        BufferedImage bufferedImage = new BufferedImage(graphPane.getWidth(), graphPane.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bufferedImage.createGraphics();
        graphPane.setRenderRequired();
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
        LOG.trace("Frame received dataRetrievalCompleted.  Forcing full render.");
        graphPane.setRenderRequired();
        graphPane.repaint();
    }

    @Override
    public void dataRetrievalFailed(DataRetrievalEvent evt) {
        LOG.trace("Frame received dataRetrievalFailed.  Forcing full render.");
        graphPane.setRenderRequired();
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
}
