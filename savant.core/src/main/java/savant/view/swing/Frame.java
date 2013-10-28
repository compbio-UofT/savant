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

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

import com.jidesoft.docking.DockableFrame;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.FrameAdapter;
import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.DataFormat;
import savant.api.event.GenomeChangedEvent;
import savant.api.event.DataRetrievalEvent;
import savant.api.util.DialogUtils;
import savant.api.util.Listener;
import savant.controller.FrameController;
import savant.controller.GenomeController;
import savant.controller.LocationController;
import savant.controller.TrackController;
import savant.plugin.SavantPanelPlugin;
import savant.settings.InterfaceSettings;
import savant.util.DrawingMode;
import savant.util.Range;
import savant.util.swing.ProgressPanel;
import savant.view.icon.SavantIconFactory;
import savant.view.tracks.SequenceTrack;
import savant.view.tracks.Track;
import savant.view.tracks.TrackCreationEvent;
import savant.view.tracks.TrackFactory.TrackCreationListener;

/**
 *
 * @author mfiume, AndrewBrook
 */
public class Frame extends DockableFrame implements FrameAdapter, TrackCreationListener {

    private static final Log LOG = LogFactory.getLog(Frame.class);
    /**
     * If true, the frame's construction was halted by an error or by the user
     * cancelling.
     */
    private boolean aborted;
    /**
     * If true, the frame will be holding a sequence track, so it can be shorter
     * than usual.
     */
    private boolean sequence;
    private GraphPane graphPane;
    private JLayeredPane frameLandscape;
    private Track[] tracks = new Track[0];
    private DrawingMode initialDrawingMode;
    private JLayeredPane jlp;
    private FrameCommandBar commandBar;
    private JComponent legend;
    private JPanel sidePanel;
    private JLabel yMaxPanel;
    private Map<SavantPanelPlugin, JPanel> pluginLayers = new HashMap<SavantPanelPlugin, JPanel>();
    private String progressMessage = null;
    private double progressFraction = -1.0;
    private boolean closeable = true;

    /**
     * Construct a new Frame for holding a track.
     *
     * @param df the DataFormat, so the frame can do any format-specific
     * initialisation (e.g. smaller height for sequence tracks)
     */
    public Frame(DataFormat df) {
        super(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK));
        sequence = df == DataFormat.SEQUENCE;

        // Component which displays the legend component.
        legend = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                for (Track t : tracks) {
                    Dimension d = t.getRenderer().getLegendSize(t.getDrawingMode());
                    if (d != null) {
                        return d;
                    }
                }
                return new Dimension(0, 0);
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public void paintComponent(Graphics g) {
                for (Track t : tracks) {
                    Dimension d = t.getRenderer().getLegendSize(t.getDrawingMode());
                    if (d != null) {
                        Graphics2D g2 = (Graphics2D) g;
                        GradientPaint gp = new GradientPaint(0, 0, Color.WHITE, 0, 60, new Color(230, 230, 230));
                        g2.setPaint(gp);
                        g2.fillRect(0, 0, d.width, d.height);

                        g2.setColor(Color.BLACK);
                        g2.draw(new Rectangle2D.Double(0, 0, d.width - 1, d.height - 1));
                        t.getRenderer().drawLegend(g2, t.getDrawingMode());
                        return;
                    }
                }
            }
        };
        legend.setVisible(false);

        frameLandscape = new JLayeredPane();

        //add graphPane -> jlp -> scrollPane
        jlp = new JLayeredPane();
        jlp.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        //scrollpane
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setBorder(null);

        graphPane = new GraphPane(this);
        jlp.add(graphPane, gbc, 0);

        scrollPane.getViewport().add(jlp);


        //GRID FRAMEWORK AND COMPONENT ADDING...
        frameLandscape.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        //add sidepanel
        sidePanel = new JPanel() {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, 0);
            }
        };
        sidePanel.setLayout(new GridBagLayout());
        sidePanel.setOpaque(false);
        sidePanel.setVisible(false);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 16); // Leave 16 pixels so that we don't sit on top of the scroll-bar.
        frameLandscape.setLayer(sidePanel, JLayeredPane.PALETTE_LAYER);
        frameLandscape.add(sidePanel, c);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension dim = getSize();
                if (dim != null) {
                    // TODO: The following shouldn't be necessary, but it seems to be.
                    int expectedWidth = frameLandscape.getWidth();
                    if (expectedWidth != graphPane.getWidth()) {
                        Dimension goodSize = new Dimension(expectedWidth, graphPane.getHeight());
                        graphPane.setPreferredSize(goodSize);
                        graphPane.setSize(goodSize);
                    }

                    setLegendVisible(true);
                }
            }
        });

        //add graphPane to all cells
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.insets = new Insets(0, 0, 0, 0);

        frameLandscape.setLayer(scrollPane, JLayeredPane.DEFAULT_LAYER);
        frameLandscape.add(scrollPane, c);

        // Add our progress-panel.  If setTracks is called promptly, it will be cleared
        // away before it ever has a chance to draw.
        getContentPane().add(new ProgressPanel(null), BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(100, sequence ? 100 : 200);
    }

    public JLayeredPane getFrameLandscape() {
        return frameLandscape;
    }

    @Override
    public GraphPaneAdapter getGraphPane() {
        return graphPane;
    }

    @Override
    public final Track[] getTracks() {
        return tracks;
    }

    /**
     * Set the tracks associated with this frame. Normally, this should only be
     * done once, since the Frame also uses this opportunity to set up some GUI
     * elements which depend on the presence of loaded tracks.
     *
     * @param newTracks the tracks to be displayed in this frame
     */
    public void setTracks(Track[] newTracks) {
        Track t0 = newTracks[0];
        DataFormat df = t0.getDataFormat();

        if (!GenomeController.getInstance().isGenomeLoaded() && df != DataFormat.SEQUENCE) {
            handleEvent(new TrackCreationEvent(new Exception()));
            for (Track track : newTracks) {
                TrackController.getInstance().removeTrack(track);
            }
            DialogUtils.displayError("Sorry", "This does not appear to be a genome track. Please load a genome first.");
            return;
        }
        if (df == DataFormat.SEQUENCE) {
            GenomeController.getInstance().setSequence((SequenceTrack) newTracks[0]);
        }

        tracks = newTracks;
        graphPane.setTracks(tracks);

        for (Track t : tracks) {
            t.setFrame(this, initialDrawingMode);

            // Adds the track to the TrackController's internal list, and fires a TrackEvent.ADDED event to all listeners.
            TrackController.getInstance().addTrack(t);
        }

        commandBar = new FrameCommandBar(this);

        // We get the name and other properties from the zero'th track.
        setKey(t0.getName());

        if (df != DataFormat.SEQUENCE && df != DataFormat.RICH_INTERVAL) {
            yMaxPanel = new JLabel();
            yMaxPanel.setBorder(BorderFactory.createLineBorder(Color.darkGray));
            yMaxPanel.setBackground(new Color(240, 240, 240));
            yMaxPanel.setOpaque(true);
            yMaxPanel.setAlignmentX(0.5f);

            if (df == DataFormat.ALIGNMENT) {
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
            }
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 0, 0, 0);
        sidePanel.add(commandBar, gbc);
        sidePanel.add(legend, gbc);
        if (yMaxPanel != null) {
            sidePanel.add(yMaxPanel, gbc);
        }
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        sidePanel.add(filler, gbc);

        drawTracksInRange(LocationController.getInstance().getReferenceName(), LocationController.getInstance().getRange());

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(frameLandscape);
    }

    /**
     * Convenience method. More often than not, we just want the frame to host a
     * single track.
     *
     * @param newTrack the track to be used for this frame.
     */
    public void setTrack(Track newTrack) {
        setTracks(new Track[]{newTrack});
    }

    public void setActiveFrame(boolean value) {
        sidePanel.setVisible(value);
        setLegendVisible(value);
    }

    @Override
    public void setCloseable(boolean closeable) {
        if (this.closeable != closeable) {

            this.closeable = closeable;
            if (closeable) {
                setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_MAXIMIZE | DockableFrame.BUTTON_CLOSE);
            } else {
                setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_MAXIMIZE);
            }

        }
    }

    public boolean isCloseable() {
        return closeable;
    }

    /**
     * Make the legend and ymax visible, but only if we have enough space for
     * it.
     */
    public void setLegendVisible(boolean value) {
        if (value) {
            // Need to show everything first so that sidePanel.getPreferredSize has a meaningful value.
            setYMaxVisible(true);
            legend.setVisible(!InterfaceSettings.areLegendsDisabled() && legend.getPreferredSize().height > 0);

            // We have 24 pixels of slop to allow for margins and title-bars.
            if (getHeight() < sidePanel.getPreferredSize().height + 24) {
                setYMaxVisible(false);
                legend.setVisible(false);
            }
        } else {
            setYMaxVisible(false);
            legend.setVisible(false);

        }
        legend.invalidate();
    }

    public void setInitialDrawingMode(DrawingMode dm) {
        initialDrawingMode = dm;
    }

    public void updateYMax(int value) {
        updateYMax(" ymax=%d ", value);
    }

    public void updateYMax(String format, Object... args) {
        if (yMaxPanel != null) {
            yMaxPanel.setText(String.format(format, args));
        }
    }

    /**
     * When the GraphPane's y-axis type has been updated, update the visibility
     * of the yMax value.
     */
    void setYMaxVisible(boolean flag) {
        if (yMaxPanel != null) {
            yMaxPanel.setVisible(flag);
        }
    }

    /**
     * Force the associated track to redraw. Used when the colour scheme has
     * been changed by the Preferences dialog.
     */
    @Override
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
        if (graphPane.isLocked()) {
            return;
        }

        graphPane.setXRange(range);

        for (Track t : tracks) {
            t.getRenderer().clearInstructions();
            t.prepareForRendering(reference, range);
        }
        graphPane.repaint();
    }

    /**
     * Tells the frame that the draw mode for this track has changed. Gives the
     * frame a chance to rebuild its user interface and request a repaint.
     *
     * @param t the track whose mode has changed
     */
    @Override
    public void drawModeChanged(Track t) {
        DrawingMode mode = t.getDrawingMode();
        commandBar.drawModeChanged(mode);
        setLegendVisible(true);
        validate();
        drawTracksInRange(LocationController.getInstance().getReferenceName(), LocationController.getInstance().getRange());
    }

    /**
     * Get a panel for a plugin to draw on. If necessary, a new one will be
     * created.
     */
    @Override
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
            jlp.add(p, c, 2);
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
    public BufferedImage frameToImage(int baseSelected) {
        BufferedImage bufferedImage = new BufferedImage(graphPane.getWidth(), graphPane.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        graphPane.setRenderForced();
        graphPane.forceFullHeight();
        graphPane.render(g2);
        graphPane.unforceFullHeight();
        g2.setColor(Color.black);
        if (baseSelected > 0) {
            double h = (double) graphPane.getHeight();
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
    public void handleEvent(DataRetrievalEvent evt) {
        // We're only interested in the event if it's for the range we're looking at.  This prevents
        // moving around on the VariantMap from repainting our variant tracks.
        if (LocationController.getInstance().getRange().equals(evt.getRange())) {
            switch (evt.getType()) {
                case COMPLETED:
                    LOG.trace("Frame " + getKey() + " received dataRetrievalCompleted.  Forcing full render.");
                    setYMaxVisible(evt.getData() != null && evt.getData().size() > 0);
                    graphPane.setRenderForced();
                    graphPane.repaint();
                    break;
                case FAILED:
                    LOG.trace("Frame " + getKey() + " received dataRetrievalFailed.  Forcing full render.");
                    setYMaxVisible(false);
                    graphPane.setRenderForced();
                    graphPane.repaint();
                    break;
            }
        } else {
            LOG.debug("Rejecting DataRetrievalEvent." + evt.getType() + " for " + evt.getRange() + " because real range=" + LocationController.getInstance().getRange());
        }
    }

    @Override
    public void handleEvent(TrackCreationEvent evt) {
        switch (evt.getType()) {
            case PROGRESS:
                progressMessage = evt.getProgressMessage();
                progressFraction = evt.getProgressFraction();
                updateProgress();
                break;
            case COMPLETED:
                if (!aborted) {
                    setTracks(evt.getTracks());
                }
                break;
            case FAILED:
                aborted = true;
                FrameController.getInstance().closeFrame(this, false);
                break;
        }
    }

    @Override
    public int getIntervalHeight() {
        return commandBar.getIntervalHeight();
    }

    @Override
    public void setHeightFromSlider() {
        int h = commandBar.getIntervalHeight();
        graphPane.setUnitHeight(h);
        graphPane.setScaledToFit(false);
    }

    /**
     * GraphPane wants to know what progress message to display.
     */
    public void updateProgress() {
        if (progressMessage == null) {
            graphPane.showProgress("Creating track...", -1.0);
        } else {
            graphPane.showProgress(progressMessage, progressFraction);
        }
    }
}
