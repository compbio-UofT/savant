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
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.controller.GenomeController;
import savant.controller.LocationController;
import savant.data.sources.DataSource;
import savant.data.types.Genome;
import savant.data.types.SequenceRecord;
import savant.file.DataFormat;
import savant.settings.InterfaceSettings;
import savant.settings.SettingsDialog;
import savant.util.DrawingMode;
import savant.util.MiscUtils;
import savant.util.Resolution;
import savant.view.dialog.BamFilterDialog;
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
    private JCheckBoxMenuItem setAsGenomeButton;

    /**
     * Create command bar
     */
    FrameCommandBar(Frame f) {
        frame = f;
        graphPane = f.getGraphPane();
        mainTrack = f.getTracks()[0];
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
        if (df == DataFormat.INTERVAL_BAM || df == DataFormat.INTERVAL_GENERIC || df == DataFormat.INTERVAL_RICH) {
            intervalMenu = createIntervalMenu();
            add(intervalMenu);

            drawModeChanged(mainTrack.getDrawingMode());
            setHeightFromSlider();
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
        if (df == DataFormat.SEQUENCE_FASTA) {
            menu.add(new JSeparator());
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
            menu.addMenuListener(new MenuListener() {
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

                @Override
                public void menuDeselected(MenuEvent me) {}

                @Override
                public void menuCanceled(MenuEvent me) {}
            });
        } else if (df == DataFormat.INTERVAL_BAM) {
            menu.add(new JSeparator());

            item = new JMenuItem("Filter...");
            item.setToolTipText("Control how records are filtered");

            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    BamFilterDialog dlg = new BamFilterDialog(DialogUtils.getMainWindow(), (BAMTrack)mainTrack);
                    dlg.setVisible(true);
                }
            });
            menu.add(item);
        } else if (df == DataFormat.INTERVAL_RICH) {
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

            menu.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent me) {
                    bookmarkAll.setEnabled(mainTrack.getDataSource() instanceof DataSource && ((DataSource)mainTrack.getDataSource()).getDictionaryCount() > 0);
                }

                @Override
                public void menuDeselected(MenuEvent me) {}

                @Override
                public void menuCanceled(MenuEvent me) {}
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

        if (df != DataFormat.SEQUENCE_FASTA) {
            scaleToFitItem = new JCheckBoxMenuItem("Scale to Fit");
            scaleToFitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (scaleToFitItem.isSelected()) {
                        graphPane.setScaledToFit(true);
                    } else {
                        if (intervalSlider != null) {
                            setHeightFromSlider();      // Calls setScaledToFit(false) internally.
                        } else {
                            graphPane.setScaledToFit(false);
                        }
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
                public void menuDeselected(MenuEvent me) {}

                @Override
                public void menuCanceled(MenuEvent me) {}
            });
            menu.add(scaleToFitItem);
        }

        if (df == DataFormat.INTERVAL_RICH) {
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
    void setHeightFromSlider() {
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

    public void drawModeChanged(DrawingMode mode) {
        if (intervalMenu != null) {
            intervalMenu.setVisible(mode != DrawingMode.ARC && mode != DrawingMode.ARC_PAIRED && mode != DrawingMode.SNP && mode != DrawingMode.STRAND_SNP);
        }
    }
}
