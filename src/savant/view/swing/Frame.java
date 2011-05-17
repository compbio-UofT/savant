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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import com.jidesoft.docking.DockableFrame;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.controller.DockableFrameController;
import savant.controller.ReferenceController;
import savant.controller.DrawModeController;
import savant.controller.FrameController;
import savant.controller.RangeController;
import savant.controller.TrackController;
import savant.controller.event.DrawModeChangedEvent;
import savant.data.event.DataRetrievalEvent;
import savant.data.event.DataRetrievalListener;
import savant.data.event.TrackCreationEvent;
import savant.data.event.TrackCreationListener;
import savant.file.DataFormat;
import savant.settings.ColourSettings;
import savant.swing.component.ProgressPanel;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.icon.SavantIconFactory;
import savant.view.swing.interval.BAMCoverageTrack;
import savant.view.swing.interval.BAMTrack;
import savant.view.swing.interval.BAMTrackRenderer;
import savant.view.swing.interval.IntervalTrackRenderer;
import savant.view.swing.interval.RichIntervalTrack;
import savant.view.swing.interval.RichIntervalTrackRenderer;

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

    private JMenuBar commandBar;
    private JPanel arcLegend;
    private List<JCheckBoxMenuItem> visItems;
    private JMenu arcButton;
    private JMenu intervalButton;
    private JMenu bedButton;
    private FrameSidePanel sidePanel;
    private int drawModePosition = 0;

    public JScrollPane scrollPane;

    public Frame() {
        super(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK));

        // Panel which holds the legend component (when present).
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
        jlp = new JLayeredPane();
        jlp.setLayout(new GridBagLayout());
        GridBagConstraints gbc= new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        jlp.add(graphPane, gbc, 0);

        scrollPane.getViewport().add(jlp);

        //GRID FRAMEWORK AND COMPONENT ADDING...
        frameLandscape.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        //add sidepanel
        sidePanel = new FrameSidePanel();
        sidePanel.setVisible(false);
        c.weightx = 1.0;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = 0;
        frameLandscape.add(sidePanel, c, 5);

        
        initCommandBar();
        sidePanel.addPanel(commandBar);
        //sidePanel.addPanel(new TrackSettingsMenu());
        sidePanel.addPanel(arcLegend);

        //add filler to left
        JLabel l = new JLabel();
        l.setOpaque(false);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        frameLandscape.add(l, c);

        //add graphPane to all cells
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        frameLandscape.add(scrollPane, c, 0);

        frameLandscape.setLayer(sidePanel, JLayeredPane.PALETTE_LAYER);
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

    public JScrollBar getVerticalScrollBar() {
        return scrollPane.getVerticalScrollBar();
    }

    public final Track[] getTracks() {
        return tracks;
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
                ReferenceController.getInstance().setGenome(Track.createGenome(newTracks[0]));
            } else {
                trackCreationFailed(null);
                for(Track track : newTracks) TrackController.getInstance().removeTrack(track);
                DialogUtils.displayError("Sorry", "This does not appear to be a genome track. Please load a genome first.");
                return;
            }
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

            //determine position of current draw mode
            String currentDrawMode = t0.getDrawMode();
            List<String> drawModes = t0.getDrawModes();
            for(int i = 0; i < drawModes.size(); i++){
                if(drawModes.get(i).equals(currentDrawMode)){
                    drawModePosition = i;
                }
            }

            //allow cycling through display modes
            graphPane.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {

                    //check for: Mac + Command + 'm' OR !Mac + Ctrl + 'm'
                    if((MiscUtils.MAC && e.getModifiersEx() == 256 && e.getKeyChar() == 'm') ||
                            (!MiscUtils.MAC && e.getKeyChar() == '\n' && e.isControlDown())){
                        cycleDisplayMode();
                    }
                }
                @Override
                public void keyPressed(KeyEvent e) {}
                @Override
                public void keyReleased(KeyEvent e) {}
            });

        }

        // TODO: Should we really be doing BAM-specific stuff in this class?
        if (t0.getDataSource().getDataFormat() == DataFormat.INTERVAL_BAM) {
            arcButton = createArcButton();
            commandBar.add(arcButton);
            arcButton.setVisible(false);

            intervalButton = createIntervalButton();
            commandBar.add(intervalButton);
            String drawMode = t0.getDrawMode();
            if(drawMode.equals(BAMTrackRenderer.ARC_PAIRED_MODE) || drawMode.equals(BAMTrackRenderer.SNP_MODE)){
                intervalButton.setVisible(false);
            }
            if (drawMode.equals("STANDARD") || drawMode.equals("VARIANTS")){
                intervalButton.setVisible(true);
            }
        } else if (t0.getDataSource().getDataFormat() == DataFormat.INTERVAL_BED) {
            bedButton = createBEDButton();
            commandBar.add(bedButton);
        }

        if(t0.getDataSource().getDataFormat() == DataFormat.INTERVAL_BED ||
                t0.getDataSource().getDataFormat() == DataFormat.INTERVAL_GENERIC){
            intervalButton = createIntervalButton();
            commandBar.add(intervalButton);
            String drawMode = t0.getDrawMode();
            if(drawMode.equals(RichIntervalTrackRenderer.SQUISH_MODE) ||
                    drawMode.equals(IntervalTrackRenderer.ARC_MODE) ||
                    drawMode.equals(IntervalTrackRenderer.SQUISH_MODE)){
                intervalButton.setVisible(false);
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
        sidePanel.setVisible(true);
    }

    public void setInactiveFrame(){
        sidePanel.setVisible(false);
    }

    public void resetLayers(){
        frameLandscape.moveToBack(graphPane);
    }

    /**
     * Create command bar
     */
    private void initCommandBar() {
        commandBar = new JMenuBar();
        JMenu optionsMenu = createOptionsMenu();
        commandBar.add(optionsMenu);
        commandBar.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
    }

    public void redrawSidePanel(){
        this.sidePanel.repaint();
    }

    /**
     * Add a component to the set of panels on side
     */
    public void addToSidePanel(JComponent comp){
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
    private JMenu createOptionsMenu() {
        JCheckBoxMenuItem item;
        //JMenu menu = new JideMenu("Settings");
        JMenu menu = new JMenu("Settings");
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
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                graphPane.getBAMParams((BAMTrack)tracks[0]);
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
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tracks[0].captureIntervalParameters();
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
        final JCheckBoxMenuItem score = new JCheckBoxMenuItem("Enable Score"){};
        
        itemRGB.setState(false);
        score.setState(false);

        itemRGB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((RichIntervalTrack)tracks[0]).toggleItemRGBEnabled();
                graphPane.setRenderForced();
                graphPane.repaint();
            }
        });
        score.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((RichIntervalTrack)tracks[0]).toggleScoreEnabled();
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
                                drawModePosition = j;
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
            t.getRenderer().clearInstructions();
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

        boolean reRender = true;
        if (track.getDataSource().getDataFormat() == DataFormat.INTERVAL_BAM) {
            if (evt.getMode().equals(BAMTrackRenderer.ARC_PAIRED_MODE)) {
                setCoverageEnabled(false);
                intervalButton.setVisible(false);
                arcButton.setVisible(true);
                arcLegend.setVisible(true);
            } else {
                setCoverageEnabled(true);
                //show interval options button unless in snp or arc modes
                intervalButton.setVisible(!evt.getMode().equals(BAMTrackRenderer.SNP_MODE));
                arcButton.setVisible(false);
                arcLegend.setVisible(false);
            }
        } else if (track.getDataSource().getDataFormat() == DataFormat.INTERVAL_BED || track.getDataSource().getDataFormat() == DataFormat.INTERVAL_GENERIC) {
            intervalButton.setVisible(evt.getMode().equals(RichIntervalTrackRenderer.STANDARD_MODE) || evt.getMode().equals(IntervalTrackRenderer.PACK_MODE));
        }
        if (reRender) {
            validate();
            drawTracksInRange(ReferenceController.getInstance().getReferenceName(), RangeController.getInstance().getRange());
        }
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
        LOG.trace("Frame received dataRetrievalCompleted.  Forcing full render.");
        graphPane.setRenderForced();
        graphPane.repaint();
    }

    @Override
    public void dataRetrievalFailed(DataRetrievalEvent evt) {
        LOG.trace("Frame received dataRetrievalFailed.  Forcing full render.");
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

    public void cycleDisplayMode(){
        if(visItems == null) return;
        visItems.get(drawModePosition).setState(false);
        drawModePosition++;
        List<String> drawModes = tracks[0].getDrawModes();
        if(drawModePosition >= drawModes.size()) drawModePosition = 0;
        visItems.get(drawModePosition).setState(true);
        DrawModeController.getInstance().switchMode(tracks[0], drawModes.get(drawModePosition));
    }
}
