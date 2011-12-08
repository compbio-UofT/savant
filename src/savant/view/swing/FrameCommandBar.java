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

import savant.view.tracks.Track;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.TrackAdapter;
import savant.api.data.DataFormat;
import savant.api.data.SequenceRecord;
import savant.api.util.DialogUtils;
import savant.api.util.Resolution;
import savant.controller.GenomeController;
import savant.controller.LocationController;
import savant.data.sources.DataSource;
import savant.data.types.Genome;
import savant.settings.InterfaceSettings;
import savant.settings.SettingsDialog;
import savant.util.DrawingMode;
import savant.util.MiscUtils;
import savant.view.dialog.BAMFilterDialog;
import savant.view.tracks.BAMTrack;
import savant.view.tracks.RichIntervalTrack;

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
    private JCheckBoxMenuItem setAsGenomeButton;

    private JCheckBoxMenuItem baseQualityItem, mappingQualityItem;

    /**
     * Create command bar
     */
    FrameCommandBar(Frame f) {
        frame = f;
        graphPane = (GraphPane)f.getGraphPane();
        mainTrack = (Track)f.getTracks()[0];
        setMinimumSize(new Dimension(50,22));
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        // TODO: Should we really be doing BAM-specific stuff in this class?
        JMenu toolsMenu = createToolsMenu();
        add(toolsMenu);

        if (mainTrack.getValidDrawingModes().length > 0){
            JMenu modeMenu = createDisplayModeMenu();
            add(modeMenu);
        }

        JMenu appearanceMenu = createAppearanceMenu();
        add(appearanceMenu);

        DataFormat df = mainTrack.getDataFormat();
        if (df == DataFormat.ALIGNMENT || df == DataFormat.GENERIC_INTERVAL || df == DataFormat.RICH_INTERVAL) {
            intervalMenu = createIntervalMenu();
            add(intervalMenu);

            drawModeChanged(mainTrack.getDrawingMode());
            int h = getIntervalHeight();
            graphPane.setUnitHeight(h);
            graphPane.setScaledToFit(false);
        }
    }

    /**
     * Create Tools menu for commandBar.  This is common to all track types.
     */
    private JMenu createToolsMenu() {
        JMenu menu = new JMenu("Tools");
        JMenuItem item = new JCheckBoxMenuItem("Lock Track");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                graphPane.setLocked(!graphPane.isLocked());
            }
        });
        menu.add(item);

        item = new JMenuItem("Copy URL to Clipboard");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(mainTrack.getDataSource().getURI().toString()), null);
            }
        });
        menu.add(item);

        DataFormat df = mainTrack.getDataFormat();
        if (df == DataFormat.SEQUENCE) {
            menu.add(new JSeparator());
            JMenuItem copyItem = new JMenuItem("Copy Sequence to Clipboard");
            copyItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        LocationController lc = LocationController.getInstance();
                        byte[] seq = ((SequenceRecord)mainTrack.getDataSource().getRecords(lc.getReferenceName(), lc.getRange(), Resolution.HIGH, null).get(0)).getSequence();
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(new String(seq)), null);
                    } catch (IOException x) {
                        LOG.error(x);
                        DialogUtils.displayError("Unable to copy sequence to clipboard.");
                    }
                }
            });
            menu.add(copyItem);

            setAsGenomeButton = new JCheckBoxMenuItem("Set as Genome");
            setAsGenomeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Genome newGenome = Genome.createFromTrack(mainTrack);
                    GenomeController.getInstance().setGenome(newGenome);
                }
            });
            menu.add(setAsGenomeButton);
            menu.addMenuListener(new MenuAdapter() {
                @Override
                public void menuSelected(MenuEvent me) {
                    Track seqTrack = (Track)GenomeController.getInstance().getGenome().getSequenceTrack();
                    if (seqTrack == mainTrack) {
                        setAsGenomeButton.setSelected(true);
                        setAsGenomeButton.setEnabled(false);
                        setAsGenomeButton.setToolTipText("This track is already the reference sequence");
                    } else {
                        setAsGenomeButton.setSelected(false);
                        setAsGenomeButton.setEnabled(true);
                        setAsGenomeButton.setToolTipText("Use this track as the reference sequence");
                    }
                }
            });
        } else if (df == DataFormat.ALIGNMENT) {
            menu.add(new JSeparator());

            item = new JMenuItem("Filter...");
            item.setToolTipText("Control how records are filtered");

            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new BAMFilterDialog(DialogUtils.getMainWindow(), (BAMTrack)mainTrack).setVisible(true);
                }
            });
            menu.add(item);
        } else if (df == DataFormat.RICH_INTERVAL) {
            menu.add(new JSeparator());

            final JMenuItem bookmarkAll = new JMenuItem("Bookmark All Features");
            bookmarkAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    DataSource ds = (DataSource)mainTrack.getDataSource();
                    if (DialogUtils.askYesNo("Bookmark All Features ", String.format("This will create %d bookmarks.  Are you sure you want to do this?", ds.getDictionaryCount())) == DialogUtils.YES) {
                        ds.addDictionaryToBookmarks();
                        Savant.getInstance().displayAuxPanels();
                    }
                }
            });
            menu.add(bookmarkAll);

            menu.addMenuListener(new MenuAdapter() {
                @Override
                public void menuSelected(MenuEvent me) {
                    bookmarkAll.setEnabled(mainTrack.getDataSource() instanceof DataSource && ((DataSource)mainTrack.getDataSource()).getDictionaryCount() > 0);
                }
            });
        }
        return menu;
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
                                for (TrackAdapter t: frame.getTracks()) {
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

        // Determine position of current draw mode.
        DrawingMode currentMode = mainTrack.getDrawingMode();
        for(int i = 0; i < validModes.length; i++){
            if (validModes[i].equals(currentMode)){
                drawModePosition = i;
                break;
            }
        }

        // Allow cycling through display modes.
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

    private JMenu createAppearanceMenu() {
        JMenu menu = new JMenu("Appearance");
        JMenuItem item = new JMenuItem("Colour Settings...");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SettingsDialog dlg = new SettingsDialog(DialogUtils.getMainWindow(), "Colour Settings", new TrackColourSchemePanel(mainTrack));
                dlg.setVisible(true);
            }
        });
        menu.add(item);

        DataFormat df = mainTrack.getDataFormat();

        if (df != DataFormat.SEQUENCE) {
            scaleToFitItem = new JCheckBoxMenuItem("Scale to Fit");
            scaleToFitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (scaleToFitItem.isSelected()) {
                        graphPane.setScaledToFit(true);
                    } else {
                        // This check is kinda ugly, but we only want to set the interval height from the slider
                        // if we're showing intervals (i.e. not arc mode and not coverage).
                        if (intervalSlider != null && mainTrack.getDrawingMode() != DrawingMode.ARC && mainTrack.getDrawingMode() != DrawingMode.ARC_PAIRED && mainTrack.getResolution(LocationController.getInstance().getRange()) == Resolution.HIGH) {
                            int h = getIntervalHeight();
                            graphPane.setUnitHeight(h);
                        }
                        graphPane.setScaledToFit(false);
                    }
                }
            });
            scaleToFitItem.setToolTipText("If selected, the track's display will be scaled to fit the available height.");
            menu.addMenuListener(new MenuAdapter() {
                @Override
                public void menuSelected(MenuEvent me) {
                    scaleToFitItem.setSelected(graphPane.isScaledToFit());
                }
            });
            menu.add(scaleToFitItem);
        }

        if (df == DataFormat.RICH_INTERVAL) {
            menu.add(new JSeparator());
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
            menu.add(itemRGB);

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
            menu.add(score);

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
            menu.add(alternate);
        } else if (df == DataFormat.ALIGNMENT) {
            menu.add(new JSeparator());
            baseQualityItem = new JCheckBoxMenuItem("Enable Base Quality");
            baseQualityItem.setState(false);
            baseQualityItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (((BAMTrack)mainTrack).toggleBaseQualityEnabled()) {
                        mappingQualityItem.setState(false);
                    }
                    graphPane.setRenderForced();
                    graphPane.repaint();
                }
            });
            menu.add(baseQualityItem);

            mappingQualityItem = new JCheckBoxMenuItem("Enable Mapping Quality");
            mappingQualityItem.setState(false);
            mappingQualityItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (((BAMTrack)mainTrack).toggleMappingQualityEnabled()) {
                        baseQualityItem.setState(false);
                    }
                    graphPane.setRenderForced();
                    graphPane.repaint();
                }
            });
            menu.add(mappingQualityItem);
        }
        return menu;
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
                int h = getIntervalHeight();
                graphPane.setUnitHeight(h);
                if (graphPane.isScaledToFit()) {
                    graphPane.setScaledToFit(false);    // Forces rerender and repaint internally.
                } else {
                    graphPane.setRenderForced();
                    graphPane.repaint();
                }
            }
        });
        menu.add(intervalSlider);

        return menu;
    }

    /**
     * We don't want our menu-bar to get squozen, so make its minimum size the preferred size.
     */
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /**
     * Get the unit-height which corresponds to the current position of the interval slider.
     */
    int getIntervalHeight() {
        int slider = intervalSlider.getValue() - 1; //starts at 1
        if (slider < 0) {
            return AVAILABLE_INTERVAL_HEIGHTS[0];
        } else if (slider >= AVAILABLE_INTERVAL_HEIGHTS.length) {
            return AVAILABLE_INTERVAL_HEIGHTS[AVAILABLE_INTERVAL_HEIGHTS.length - 1];
        } else {
            return AVAILABLE_INTERVAL_HEIGHTS[slider];
        }
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

    public void drawModeChanged(DrawingMode mode) {
        if (intervalMenu != null) {
            intervalMenu.setVisible(mode != DrawingMode.ARC && mode != DrawingMode.ARC_PAIRED && mode != DrawingMode.SNP && mode != DrawingMode.STRAND_SNP);
        }
    }

    /**
     * We're generally only interested in menuSelected.
     */
    private abstract class MenuAdapter implements MenuListener {
        @Override
        public void menuDeselected(MenuEvent me) {}

        @Override
        public void menuCanceled(MenuEvent me) {}
    }
}
