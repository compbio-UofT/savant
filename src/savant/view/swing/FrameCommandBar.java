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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.util.DialogUtils;
import savant.controller.GenomeController;
import savant.controller.LocationController;
import savant.controller.event.GenomeChangedEvent;
import savant.data.sources.DataSource;
import savant.data.types.Genome;
import savant.data.types.SequenceRecord;
import savant.file.DataFormat;
import savant.settings.InterfaceSettings;
import savant.settings.SettingsDialog;
import savant.util.DrawingMode;
import savant.util.Listener;
import savant.util.MiscUtils;
import savant.util.Resolution;
import savant.view.dialog.BAMParametersDialog;
import savant.view.swing.interval.BAMTrack;
import savant.view.swing.interval.RichIntervalTrack;

/**
 * Menu-bar which appears in the top-right of each Frame, adjusted depending on the track type.
 *
 * @author tarkvara
 */
public final class FrameCommandBar extends JMenuBar {
    private static final Log LOG = LogFactory.getLog(FrameCommandBar.class);

    /** Possible interval heights available to interval renderers. */
    private static final int[] AVAILABLE_INTERVAL_HEIGHTS = new int[] { 1, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40};

    /** The Frame on which this menu-bar appears. */
    private final Frame frame;

    /** GraphPane associated with our Frame. */
    private final GraphPane graphPane;

    /**
     * The track associated with our frame.  For simplicity's sake, we only care about the first track (e.g.
     * we consider only the BAM track and ignore the coverage track).
     */
    private final Track mainTrack;

    private JMenuItem scaleToFitItem;

    private JCheckBoxMenuItem[] modeItems;
    private int drawModePosition = 0;

    private JMenu intervalMenu;
    private JSlider intervalSlider;
    private JMenu arcButton;
    private JCheckBoxMenuItem setAsGenomeButton;

    /**
     * Create command bar
     */
    FrameCommandBar(Frame f) {
        frame = f;
        graphPane = f.getGraphPane();
        mainTrack = f.getTracks()[0];
        setName("commandBar"); //used by sidePanel
        setMinimumSize(new Dimension(50,22));
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        DataFormat df = mainTrack.getDataFormat();

        // TODO: Should we really be doing BAM-specific stuff in this class?
        JMenu settingsMenu = createSettingsMenu(df != DataFormat.SEQUENCE_FASTA);
        add(settingsMenu);

        if (mainTrack.getValidDrawingModes().length > 1){
            JMenu displayMenu = createDisplayModeMenu();
            add(displayMenu);

            //determine position of current draw mode
            DrawingMode currentMode = mainTrack.getDrawingMode();
            DrawingMode[] validModes = mainTrack.getValidDrawingModes();
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

        if (df == DataFormat.INTERVAL_BAM) {
            arcButton = createArcButton();
            add(arcButton);
            arcButton.setVisible(false);

            //intervalButton = createIntervalButton();
            //commandBar.add(intervalButton);

            intervalMenu = createIntervalMenu();
            add(intervalMenu);


            DrawingMode mode = mainTrack.getDrawingMode();
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
                        frame.forceRedraw();
                    }
                }
            });
        } else if (df == DataFormat.INTERVAL_RICH) {
            JMenu bedMenu = createBEDButton();
            add(bedMenu);
        } else if (df == DataFormat.SEQUENCE_FASTA){
            JMenu sequenceMenu = createSequenceMenu();
            toggleSetAsGenomeButton();
            add(sequenceMenu);
        }

        if (df == DataFormat.INTERVAL_RICH || df == DataFormat.INTERVAL_GENERIC) {
            //intervalButton = createIntervalButton();
            //commandBar.add(intervalButton);
            intervalMenu = createIntervalMenu();
            add(intervalMenu);
            DrawingMode mode = mainTrack.getDrawingMode();
            if (mode == DrawingMode.SQUISH || mode == DrawingMode.ARC){
                intervalMenu.setVisible(false);
            }
        }
        if (intervalMenu != null) {
            setHeightFromSlider();
        }
    }

    /**
     * Create settings menu for commandBar.  This is common to all track types.
     */
    private JMenu createSettingsMenu(boolean allowScaleToFit) {
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
                SettingsDialog dlg = new SettingsDialog(DialogUtils.getMainWindow(), "Colour Settings", new TrackColourSchemePanel(mainTrack));
                dlg.setVisible(true);
            }
        });
        menu.add(item);

        if (allowScaleToFit) {
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
            menu.addMenuListener(new MenuListener() {
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
            menu.add(scaleToFitItem);
        }

        item = new JMenuItem("Copy URL to Clipboard");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(mainTrack.getDataSource().getURI().toString()), null);
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
                BAMParametersDialog dlg = new BAMParametersDialog(DialogUtils.getMainWindow(), (BAMTrack)mainTrack);
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
        intervalSlider.setValue(getSliderFromIntervalHeight(InterfaceSettings.getIntervalHeight(mainTrack.getDataFormat())));
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
                ((RichIntervalTrack)mainTrack).toggleItemRGBEnabled();
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
                ((RichIntervalTrack)mainTrack).toggleScoreEnabled();
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
                ((RichIntervalTrack)mainTrack).toggleAlternateName();
                graphPane.setRenderForced();
                graphPane.repaint();
            }
        });
        button.add(alternate);

        final DataSourceAdapter ds = mainTrack.getDataSource();
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
                Genome newGenome = Genome.createFromTrack(mainTrack);
                GenomeController.getInstance().setGenome(newGenome);
            }
        });
        menu.add(setAsGenomeButton);
        JMenuItem copyItem = new JMenuItem("Copy Sequence to Clipboard");
        copyItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    LocationController lc = LocationController.getInstance();
                    byte[] seq = ((SequenceRecord)mainTrack.getDataSource().getRecords(lc.getReferenceName(), lc.getRange(), Resolution.HIGH).get(0)).getSequence();
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
        Track seqTrack = (Track)GenomeController.getInstance().getGenome().getSequenceTrack();
        if (seqTrack != null && seqTrack.getName() != null && seqTrack.getName().equals(mainTrack.getName())){
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
        DrawingMode[] validModes = mainTrack.getValidDrawingModes();
        modeItems = new JCheckBoxMenuItem[validModes.length];
        for(int i = 0; i < validModes.length; i++){
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(validModes[i].toString());
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
                    if (item.getState()) {
                        DrawingMode[] validModes = mainTrack.getValidDrawingModes();
                        for (int j = 0; j < modeItems.length; j++){
                            if (item.getText().equals(validModes[j].toString())) {
                                for (Track t: frame.getTracks()) {
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
            if (validModes[i] == mainTrack.getDrawingMode()) {
                item.setState(true);
            }
            modeItems[i] = item;
            menu.add(item);
        }
        return menu;
    }

    private void cycleDisplayMode(){
        if(modeItems == null) return;
        modeItems[drawModePosition].setState(false);
        drawModePosition++;
        DrawingMode[] drawModes = mainTrack.getValidDrawingModes();
        if (drawModePosition >= drawModes.length) drawModePosition = 0;
        modeItems[drawModePosition].setState(true);

        mainTrack.setDrawingMode(drawModes[drawModePosition]);
    }

    public void drawModeChanged(DrawingMode mode) {
        if (mainTrack.getDataFormat() == DataFormat.INTERVAL_BAM) {
            if (mode == DrawingMode.ARC_PAIRED) {
                //intervalButton.setVisible(false);
                intervalMenu.setVisible(false);
                arcButton.setVisible(true);
            } else {
                // Show interval options button unless in SNP mode.
                intervalMenu.setVisible(mode != DrawingMode.SNP && mode != DrawingMode.STRAND_SNP);
                arcButton.setVisible(false);
            }
        } else if (mainTrack.getDataFormat() == DataFormat.INTERVAL_RICH || mainTrack.getDataFormat() == DataFormat.INTERVAL_GENERIC) {
            intervalMenu.setVisible(mode == DrawingMode.STANDARD || mode == DrawingMode.PACK);
        }
    }
}
