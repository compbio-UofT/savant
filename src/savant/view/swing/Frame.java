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
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.jidesoft.docking.DockableFrame;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.util.DialogUtils;
import savant.controller.FrameController;
import savant.controller.GenomeController;
import savant.controller.LocationController;
import savant.controller.TrackController;
import savant.controller.event.GenomeChangedEvent;
import savant.data.event.DataRetrievalEvent;
import savant.data.event.DataRetrievalListener;
import savant.data.event.TrackCreationEvent;
import savant.data.event.TrackCreationListener;
import savant.data.sources.DataSource;
import savant.data.types.Genome;
import savant.data.types.SequenceRecord;
import savant.file.DataFormat;
import savant.plugin.SavantPanelPlugin;
import savant.settings.InterfaceSettings;
import savant.settings.SettingsDialog;
import savant.util.DrawingMode;
import savant.util.Listener;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.util.Resolution;
import savant.util.swing.ProgressPanel;
import savant.view.dialog.BAMParametersDialog;
import savant.view.icon.SavantIconFactory;
import savant.view.swing.interval.BAMTrack;
import savant.view.swing.interval.RichIntervalTrack;
import savant.view.swing.sequence.SequenceTrack;


/**
 *
 * @author mfiume, AndrewBrook
 */
public class Frame extends DockableFrame implements DataRetrievalListener, TrackCreationListener {
    private static final Log LOG = LogFactory.getLog(Frame.class);

    /** Possible interval heights available to interval renderers. */
    private static final int[] AVAILABLE_INTERVAL_HEIGHTS = new int[] { 1, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40};

    /** If true, the frame's construction was halted by an error or by the user cancelling. */
    private boolean aborted;

    /** If true, the frame will be holding a sequence track, so it can be shorter than usual. */
    private boolean sequence;

    private GraphPane graphPane;
    private JLayeredPane frameLandscape;
    private Track[] tracks = new Track[0];

    private JLayeredPane jlp;

    private JPanel arcLegend;
    private JCheckBoxMenuItem[] modeItems;
    private JMenuItem scaleToFitItem;
    private JMenu arcButton;
    private FrameSidePanel sidePanel;
    private int drawModePosition = 0;
    private JMenu intervalMenu;
    private JSlider intervalSlider;
    private JCheckBoxMenuItem setAsGenomeButton;
    private JLabel yMaxPanel;
    private Map<SavantPanelPlugin, JPanel> pluginLayers = new HashMap<SavantPanelPlugin, JPanel>();

    public JScrollPane scrollPane;

    /**
     * Construct a new Frame for holding a track.
     *
     * @param seq if true, the Frame will be holding a sequence track
     */
    public Frame(boolean seq) {
        super(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK));
        sequence = seq;

        // Panel which holds the legend component (when present).
        arcLegend = new JPanel();
        arcLegend.setLayout(new BorderLayout());
        arcLegend.setVisible(false);

        frameLandscape = new JLayeredPane();
        graphPane = new GraphPane(this);

        //scrollpane
        scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setBorder(null);

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

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension dim = getSize();
                if(dim == null) return;
                sidePanel.setContainerHeight(dim.height);
            }
        });
        sidePanel.setShowPanel(false);

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
        getContentPane().add(new ProgressPanel(null));
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

    /**
     * Set the tracks associated with this frame.  Normally, this should only be done
     * once, since the Frame also uses this opportunity to set up some GUI elements
     * which depend on the presence of loaded tracks.
     *
     * @param newTracks the tracks to be displayed in this frame
     */
    public void setTracks(Track[] newTracks) {
        JMenuBar commandBar = createCommandBar();
        JMenu settingsMenu = createSettingsMenu();
        commandBar.add(settingsMenu);

        sidePanel.addPanel(commandBar);
        sidePanel.addPanel(arcLegend);

        if (!GenomeController.getInstance().isGenomeLoaded() && newTracks[0].getDataFormat() != DataFormat.SEQUENCE_FASTA) {
            trackCreationFailed(null);
            for (Track track : newTracks) {
                TrackController.getInstance().removeTrack(track);
            }
            DialogUtils.displayError("Sorry", "This does not appear to be a genome track. Please load a genome first.");
            return;
        }
        if (newTracks[0].getDataFormat() == DataFormat.SEQUENCE_FASTA) {
            GenomeController.getInstance().setSequence((SequenceTrack)newTracks[0]);
        }

        tracks = newTracks;
        graphPane.setTracks(tracks);

        for (Track t: tracks) {
            t.setFrame(this);

            //CREATE LEGEND PANEL
            if (t.getDataFormat() == DataFormat.INTERVAL_BAM) {
                arcLegend.add(t.getRenderer().arcLegendPaint());
            }

            // Adds the track to the TrackController's internal list, and fires a TrackEvent.ADDED event to all listeners.
            TrackController.getInstance().addTrack(t);
        }

        // We get the name and other properties from the zero'th track.
        Track t0 = tracks[0];
        setName(t0.getName());
        setKey(t0.getName());
        if (t0.getValidDrawingModes().length > 0){
            JMenu displayMenu = createDisplayModeMenu();
            commandBar.add(displayMenu);

            //determine position of current draw mode
            DrawingMode currentMode = t0.getDrawingMode();
            DrawingMode[] validModes = t0.getValidDrawingModes();
            for(int i = 0; i < validModes.length; i++){
                if(validModes[i].equals(currentMode)){
                    drawModePosition = i;
                }
            }

            //allow cycling through display modes
            graphPane.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    //check for: Mac + Command + 'm' OR !Mac + Ctrl + 'm'
                    if((MiscUtils.MAC && e.getModifiersEx() == 256 && e.getKeyChar() == 'm') ||
                            (!MiscUtils.MAC && e.getKeyChar() == '\n' && e.isControlDown())){
                        cycleDisplayMode();
                    }
                }
            });

        }

        DataFormat df = t0.getDataFormat();

        // TODO: Should we really be doing BAM-specific stuff in this class?
        if (df == DataFormat.INTERVAL_BAM) {
            arcButton = createArcButton();
            commandBar.add(arcButton);
            arcButton.setVisible(false);

            //intervalButton = createIntervalButton();
            //commandBar.add(intervalButton);

            intervalMenu = createIntervalMenu();
            commandBar.add(intervalMenu);


            DrawingMode mode = t0.getDrawingMode();
            if (mode == DrawingMode.ARC_PAIRED || mode == DrawingMode.SNP || mode == DrawingMode.STRAND_SNP){
                //intervalButton.setVisible(false);
                intervalMenu.setVisible(false);
            }
            if (mode == DrawingMode.STANDARD || mode == DrawingMode.MISMATCH) {
                intervalMenu.setVisible(true);
            }

            // We need to listen to genome changes so that we can redraw mismatches as appropriate.
            GenomeController.getInstance().addListener(new Listener<GenomeChangedEvent>() {
                @Override
                public void handleEvent(GenomeChangedEvent event) {
                    // In certain BAM modes, we care about whether the sequence has been set (or unset).
                    if (event.getNewGenome() == event.getOldGenome()) {
                        forceRedraw();
                    }
                }
            });
        } else if (df == DataFormat.INTERVAL_RICH) {
            JMenu bedMenu = createBEDButton();
            commandBar.add(bedMenu);
        } else if (df == DataFormat.SEQUENCE_FASTA){
            JMenu sequenceMenu = createSequenceMenu();
            toggleSetAsGenomeButton();
            commandBar.add(sequenceMenu);
        }

        if (df == DataFormat.INTERVAL_RICH || df == DataFormat.INTERVAL_GENERIC) {
            //intervalButton = createIntervalButton();
            //commandBar.add(intervalButton);
            intervalMenu = createIntervalMenu();
            commandBar.add(intervalMenu);
            DrawingMode mode = t0.getDrawingMode();
            if (mode == DrawingMode.SQUISH || mode == DrawingMode.ARC){
                intervalMenu.setVisible(false);
            }
        }
        switch (df) {
            case SEQUENCE_FASTA:
                break;
            default:
                yMaxPanel = new JLabel();
                yMaxPanel.setBorder(BorderFactory.createLineBorder(Color.darkGray));
                yMaxPanel.setBackground(new Color(240,240,240));
                yMaxPanel.setOpaque(true);
                yMaxPanel.setAlignmentX(0.5f);
                sidePanel.addPanel(yMaxPanel);

                scaleToFitItem = new JCheckBoxMenuItem("Scale to Fit");
                scaleToFitItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        graphPane.setScaledToFit(scaleToFitItem.isSelected());
                        if (intervalSlider != null && !graphPane.isScaledToFit()) {
                            setHeightFromSlider();
                        }
                    }
                });
                scaleToFitItem.setToolTipText("If selected, the track's display will be scaled to fit the available height.");
                settingsMenu.addMenuListener(new MenuListener() {
                    @Override
                    public void menuSelected(MenuEvent me) {
                        scaleToFitItem.setSelected(graphPane.isScaledToFit());
                    }

                    @Override
                    public void menuDeselected(MenuEvent me) {
                    }

                    @Override
                    public void menuCanceled(MenuEvent me) {
                    }
                });
                settingsMenu.add(scaleToFitItem);
                if (intervalMenu != null) {
                    setHeightFromSlider();
                }
                break;
        }
        drawTracksInRange(LocationController.getInstance().getReferenceName(), LocationController.getInstance().getRange());

        JPanel contentPane = (JPanel)getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(frameLandscape);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(100, sequence ? 100 : 200);
    }

    /**
     * Convenience method.  More often than not, we just want the frame to host a single track.
     *
     * @param newTrack the track to be used for this frame.
     */
    public void setTrack(Track newTrack) {
        setTracks(new Track[] { newTrack });
    }


    public void setActiveFrame(){
        sidePanel.setVisible(true);
        sidePanel.setShowPanel(true);
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
    private JMenuBar createCommandBar() {
        JMenuBar commandBar = new JMenuBar();
        commandBar.setName("commandBar"); //used by sidePanel
        commandBar.setMinimumSize(new Dimension(50,22));
        commandBar.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        return commandBar;
    }

    public void redrawSidePanel(){
        sidePanel.repaint();
    }

    /**
     * Create settings menu for commandBar.  This is common to all track types.
     */
    private JMenu createSettingsMenu() {
        JMenu menu = new JMenu("Settings");
        JMenuItem item = new JCheckBoxMenuItem("Lock Track");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                graphPane.setLocked(!graphPane.isLocked());
            }
        });
        menu.add(item);

        item = new JMenuItem("Colour Settings...");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SettingsDialog dlg = new SettingsDialog(DialogUtils.getMainWindow(), "Colour Settings", new TrackColourSchemePanel(tracks[0]));
                dlg.setVisible(true);
            }
        });
        menu.add(item);
        return menu;
    }

    /**
     * Create the button to show the arc params dialog
     */
    private JMenu createArcButton() {
        JMenu button = new JMenu("Read Pair Settings...");
        button.setToolTipText("Change mate pair parameters");

        // Because we're using a JMenu, we have to hang our action off mouseClicked rather than actionPerformed.
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BAMParametersDialog dlg = new BAMParametersDialog(DialogUtils.getMainWindow(), (BAMTrack)tracks[0]);
                dlg.setVisible(true);
            }
        });
        button.setFocusPainted(false);
        return button;
    }

    /**
     * Create interval height slider for commandBar
     */
    private JMenu createIntervalMenu() {
        JMenu menu = new JMenu("Interval Height");
        intervalSlider = new JSlider(JSlider.VERTICAL, 1, AVAILABLE_INTERVAL_HEIGHTS.length, 1);
        intervalSlider.setMinorTickSpacing(1);
        intervalSlider.setMajorTickSpacing(AVAILABLE_INTERVAL_HEIGHTS.length / 2);
        intervalSlider.setSnapToTicks(true);
        intervalSlider.setPaintTicks(true);
        intervalSlider.setValue(getSliderFromIntervalHeight(InterfaceSettings.getIntervalHeight(tracks[0].getDataFormat())));
        intervalSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setHeightFromSlider();
                graphPane.setRenderForced();
                graphPane.repaint();
            }
        });
        menu.add(intervalSlider);

        return menu;
    }

    /**
     * Set the unit-height based on the current position of the interval slider.
     */
    public void setHeightFromSlider() {
        int slider = intervalSlider.getValue() - 1; //starts at 1
        int height;
        if (slider < 0) {
            height = AVAILABLE_INTERVAL_HEIGHTS[0];
        } else if (slider >= AVAILABLE_INTERVAL_HEIGHTS.length) {
            height = AVAILABLE_INTERVAL_HEIGHTS[AVAILABLE_INTERVAL_HEIGHTS.length - 1];
        } else {
            height = AVAILABLE_INTERVAL_HEIGHTS[slider];
        }
        graphPane.setUnitHeight(height);
        graphPane.setScaledToFit(false);
    }

    /**
     * Given an interval height, determine the slider value which corresponds to it.
     * @return
     */
    private static int getSliderFromIntervalHeight(int intervalHeight){
        int newValue = 0;
        int diff = Math.abs(AVAILABLE_INTERVAL_HEIGHTS[0] - intervalHeight);
        for(int i = 1; i < AVAILABLE_INTERVAL_HEIGHTS.length; i++){
            int currVal = AVAILABLE_INTERVAL_HEIGHTS[i];
            int currDiff = Math.abs(currVal - intervalHeight);
            if(currDiff < diff){
                newValue = i;
                diff = currDiff;
            }
        }
        return newValue + 1; //can't be 0
    }

    /**
     * Create interval button for commandBar
     */
    private JMenu createBEDButton() {
        JMenu button = new JMenu("BED Options");
        button.setToolTipText("Change BED display parameters");

        JCheckBoxMenuItem itemRGB = new JCheckBoxMenuItem("Enable ItemRGB");
        itemRGB.setState(false);
        itemRGB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((RichIntervalTrack)tracks[0]).toggleItemRGBEnabled();
                graphPane.setRenderForced();
                graphPane.repaint();
            }
        });
        button.add(itemRGB);

        JCheckBoxMenuItem score = new JCheckBoxMenuItem("Enable Score");
        score.setState(false);
        score.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((RichIntervalTrack)tracks[0]).toggleScoreEnabled();
                graphPane.setRenderForced();
                graphPane.repaint();
            }
        });
        button.add(score);

        JCheckBoxMenuItem alternate = new JCheckBoxMenuItem("Display Alternate Name");
        alternate.setState(false);
        alternate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((RichIntervalTrack)tracks[0]).toggleAlternateName();
                graphPane.setRenderForced();
                graphPane.repaint();
            }
        });
        button.add(alternate);

        final DataSourceAdapter ds = tracks[0].getDataSource();
        JSeparator sep = new JSeparator();
        button.add(sep);

        final JMenuItem bookmarkAll = new JMenuItem("Bookmark All Features");
        bookmarkAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (DialogUtils.askYesNo("Bookmark All Features ", String.format("This will create %d bookmarks.  Are you sure you want to do this?", ((DataSource)ds).getDictionaryCount())) == DialogUtils.YES) {
                    ((DataSource)ds).addDictionaryToBookmarks();
                    Savant.getInstance().displayAuxPanels();
                }
            }
        });
        button.add(bookmarkAll);

        button.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent me) {
                bookmarkAll.setEnabled(ds instanceof DataSource && ((DataSource)ds).getDictionaryCount() > 0);
            }

            @Override
            public void menuDeselected(MenuEvent me) {
            }

            @Override
            public void menuCanceled(MenuEvent me) {
            }
        });
        button.setFocusPainted(false);
        return button;
    }
    
    /**
     * Create a menu for controlling sequence options.  Currently the only one is to set this as the current genome.
     */
    private JMenu createSequenceMenu() {
        JMenu menu = new JMenu("Sequence Options");
        menu.setToolTipText("Change sequence parameters");
        setAsGenomeButton = new JCheckBoxMenuItem("Set as Genome");
        setAsGenomeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Genome newGenome = Genome.createFromTrack(tracks[0]);
                GenomeController.getInstance().setGenome(newGenome);
            }
        });
        menu.add(setAsGenomeButton);
        JMenuItem copyItem = new JMenuItem("Copy to Clipboard");
        copyItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    LocationController lc = LocationController.getInstance();
                    byte[] seq = ((SequenceRecord)tracks[0].getDataSource().getRecords(lc.getReferenceName(), lc.getRange(), Resolution.HIGH).get(0)).getSequence();
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(new String(seq)), null);
                } catch (IOException x) {
                    LOG.error(x);
                    DialogUtils.displayError("Unable to copy sequence to clipboard.");
                }
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
        menu.add(copyItem);
        menu.setFocusPainted(false);
        return menu;
    }
    
    /**
     * If track is a sequence, check whether it is the current reference. 
     * If so, enable "Set Genome" button. And vice versa. 
     */
    public void toggleSetAsGenomeButton(){
        if(setAsGenomeButton == null) return; //all non-sequence tracks
        TrackAdapter ta = GenomeController.getInstance().getGenome().getSequenceTrack();    
        if(ta != null && ta.getName() != null && ta.getName().equals(tracks[0].getName())){
            setAsGenomeButton.setSelected(true);
            setAsGenomeButton.setEnabled(false);
            setAsGenomeButton.setToolTipText("This track is already the reference sequence");
        } else {
            setAsGenomeButton.setSelected(false);
            setAsGenomeButton.setEnabled(true);
            setAsGenomeButton.setToolTipText("Use this track as the reference sequence");
        }
    }

    /**
     * Create display menu for commandBar
     */
    private JMenu createDisplayModeMenu() {
        JMenu menu = new JMenu("Display Mode");

        //display modes
        DrawingMode[] validModes = tracks[0].getValidDrawingModes();
        modeItems = new JCheckBoxMenuItem[validModes.length];
        for(int i = 0; i < validModes.length; i++){
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(validModes[i].toString());
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
                    if (item.getState()) {
                        DrawingMode[] validModes = tracks[0].getValidDrawingModes();
                        for (int j = 0; j < modeItems.length; j++){
                            if (item.getText().equals(validModes[j].toString())) {
                                for (Track t: tracks) {
                                    t.setDrawingMode(validModes[j]);
                                }
                                drawModePosition = j;
                            } else {
                                modeItems[j].setState(false);
                            }
                        }
                    } else {
                        item.setState(true);
                    }
                }
            });
            if (validModes[i] == tracks[0].getDrawingMode()) {
                item.setState(true);
            }
            modeItems[i] = item;
            menu.add(item);
        }
        return menu;
    }

    public void updateYMax(int value) {
        if (yMaxPanel != null) {
            yMaxPanel.setText(String.format(" ymax=%d ", value));
        }
    }

    /**
     * When the GraphPane's y-axis type has been updated, update the visibility of the yMax value.
     */
    public void setYMaxVisible(boolean flag) {
        if (yMaxPanel != null) {
            yMaxPanel.setVisible(flag);
        }
    }

    /**
     * Force the associated track to redraw.  Used when the colour scheme has been changed by the Preferences dialog.
     */
    public void forceRedraw() {
        graphPane.setRenderForced();
        drawTracksInRange(LocationController.getInstance().getReferenceName(), graphPane.getXRange());
    }

    /**
     * Prepare the data for the tracks in range and fire off a repaint.
     *
     * @param range
     */
    public void drawTracksInRange(String reference, Range range) {
        if (graphPane.isLocked()) { return; }

        graphPane.setXRange(range);

        for (Track t : tracks) {
            t.getRenderer().clearInstructions();
            t.prepareForRendering(reference, range);
        }
        resetLayers();
    }

    public void drawModeChanged(Track track, DrawingMode mode) {

        boolean reRender = true;
        if (track.getDataFormat() == DataFormat.INTERVAL_BAM) {
            if (mode == DrawingMode.ARC_PAIRED) {
                //intervalButton.setVisible(false);
                intervalMenu.setVisible(false);
                arcButton.setVisible(true);
                arcLegend.setVisible(true);
            } else {
                // Show interval options button unless in SNP mode.
                intervalMenu.setVisible(mode != DrawingMode.SNP && mode != DrawingMode.STRAND_SNP);
                arcButton.setVisible(false);
                arcLegend.setVisible(false);
            }
        } else if (track.getDataFormat() == DataFormat.INTERVAL_RICH || track.getDataFormat() == DataFormat.INTERVAL_GENERIC) {
            intervalMenu.setVisible(mode == DrawingMode.STANDARD || mode == DrawingMode.PACK);
        }
        setYMaxVisible(true);

        if (reRender) {
            validate();
            drawTracksInRange(LocationController.getInstance().getReferenceName(), LocationController.getInstance().getRange());
        }
    }

    /**
     * Get a panel for a plugin to draw on.  If necessary, a new one will be created.
     */
    public JPanel getLayerCanvas(SavantPanelPlugin plugin, boolean mayCreate) {
        JPanel p = pluginLayers.get(plugin);
        if (p == null && mayCreate) {
            p = new JPanel();
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
            pluginLayers.put(plugin, p);
            if (plugin != null) {
                p.setVisible(plugin.isVisible());
            }
        }
        return p;
    }

    /**
     * Export this frame as an image.
     */
    public BufferedImage frameToImage(int baseSelected){
        BufferedImage bufferedImage = new BufferedImage(graphPane.getWidth(), graphPane.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        graphPane.setRenderForced();
        graphPane.forceFullHeight();
        graphPane.render(g2);
        graphPane.unforceFullHeight();
        g2.setColor(Color.black);
        if(baseSelected > 0){
            double h = (double)graphPane.getHeight();
            double spos = graphPane.transformXPos(baseSelected);
            g2.draw(new Line2D.Double(spos, 0.0, spos, h));
            double rpos = graphPane.transformXPos(baseSelected + 1);
            g2.draw(new Line2D.Double(rpos, 0.0, rpos, h));
        }
        g2.setFont(new Font(null, Font.BOLD, 13));
        g2.drawString(tracks[0].getName(), 2, 15);
        return bufferedImage;
    }

    @Override
    public void dataRetrievalStarted(DataRetrievalEvent evt) {
    }

    @Override
    public void dataRetrievalCompleted(DataRetrievalEvent evt) {
        LOG.trace("Frame received dataRetrievalCompleted.  Forcing full render.");
        setYMaxVisible(evt.getData() != null && evt.getData().size() > 0);
        graphPane.setRenderForced();
        graphPane.repaint();
    }

    @Override
    public void dataRetrievalFailed(DataRetrievalEvent evt) {
        LOG.trace("Frame received dataRetrievalFailed.  Forcing full render.");
        setYMaxVisible(false);
        graphPane.setRenderForced();
        graphPane.repaint();
    }

    @Override
    public void trackCreationStarted(TrackCreationEvent evt) {
    }

    @Override
    public void trackCreationCompleted(TrackCreationEvent evt) {
        if (!aborted) {
            setTracks(evt.getTracks());
        }
    }

    @Override
    public void trackCreationFailed(TrackCreationEvent evt) {
        aborted = true;
        FrameController.getInstance().closeFrame(this, false);
    }

    public void cycleDisplayMode(){
        if(modeItems == null) return;
        modeItems[drawModePosition].setState(false);
        drawModePosition++;
        DrawingMode[] drawModes = tracks[0].getValidDrawingModes();
        if (drawModePosition >= drawModes.length) drawModePosition = 0;
        modeItems[drawModePosition].setState(true);

        tracks[0].setDrawingMode(drawModes[drawModePosition]);
    }
}
