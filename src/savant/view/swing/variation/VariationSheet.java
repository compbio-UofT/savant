/*
 *    Copyright 2012 University of Toronto
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
package savant.view.swing.variation;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.api.data.DataFormat;
import savant.api.data.VariantRecord;
import savant.api.event.DataRetrievalEvent;
import savant.api.event.LocationChangedEvent;
import savant.api.event.TrackEvent;
import savant.api.util.Listener;
import savant.api.util.RangeUtils;
import savant.controller.GraphPaneController;
import savant.controller.LocationController;
import savant.controller.TrackController;
import savant.data.sources.TabixDataSource;
import savant.settings.TrackResolutionSettings;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.util.swing.ProgressPanel;
import savant.view.icon.SavantIconFactory;
import savant.view.tracks.VariantTrack;


/**
 * Panel which displays aggregated variation information in a variety of different ways.
 *
 * @author tarkvara
 */
public class VariationSheet extends JPanel implements Listener<DataRetrievalEvent> {
    static final Log LOG = LogFactory.getLog(VariationSheet.class);
    private static final String ZOOM_MESSAGE = MiscUtils.MAC ? "<html><center>Zoom in to see data<br><small>To view data at this range, change Preferences > Track Resolutions</small></center></html>" : "<html><center>Zoom in to see data<br><small>To view data at this range, change Edit > Preferences > Track Resolutions</small></center></html>";

    private static final Comparator<VariantRecord> VARIANT_COMPARATOR = new Comparator<VariantRecord>() {
        @Override
        public int compare(VariantRecord t, VariantRecord t1) {
            return t.compareTo(t1);
        }
    };

    List<VariantTrack> tracks = new ArrayList<VariantTrack>();
    List<List<VariantRecord>> rawData = new ArrayList<List<VariantRecord>>();
    List<VariantRecord> aggregateData = null;
    List<String> participantNames = new ArrayList<String>();
    Set<String> controls = new HashSet<String>();

    private int participantCount;
    private String visibleRef;
    private Range visibleRange;
    private boolean adjustingRange = false;
    
    // The cards we can show.
    private JTabbedPane tabs;
    private JLabel messageLabel;
    private ProgressPanel progressPanel;
    private JComponent currentCard;

    private JTable table;
    private VariantMap map;
    private LDPlot ldPlot;
    private AlleleFrequencyPlot frequencyPlot;
    
    private JTextField rangeField;
    private JScrollBar mapScroller;
    private ButtonGroup methodGroup;

    public VariationSheet() {
        super(new GridBagLayout());

        // Toolbar shared by all panels.
        JToolBar tools = new JToolBar();
        tools.setFloatable(false);
        rangeField = new JTextField();
        tools.add(rangeField);
        tools.addSeparator();
        
        JButton zoomInButton = new JButton();
        zoomInButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.ZOOMIN));
        zoomInButton.setBorderPainted(false);
        zoomInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomIn();
            }
        });
        tools.add(zoomInButton);

        JButton zoomOutButton = new JButton();
        zoomOutButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.ZOOMOUT));
        zoomOutButton.setBorderPainted(false);
        zoomOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomOut();
            }
        });
        tools.add(zoomOutButton);

        tools.addSeparator();
        JButton controlsButton = new JButton("Controls");
        controlsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tracks.size() > 0) {
                    new CaseControlDialog(VariationSheet.this).setVisible(true);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        tools.add(controlsButton);
        
        tabs = new JTabbedPane();
        table = new JTable(new VariantTableModel(null));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    VariantRecord varRec = getData().get(table.getSelectedRow());
                    navigateToRecord(varRec);
                }
            }
        });
        tabs.addTab("Table", new JScrollPane(table));

        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());
        
        mapScroller = new JScrollBar();
        mapScroller.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent ae) {
                if (!adjustingRange) {
                    int start = ae.getValue();
                    if (start != visibleRange.getFrom()) {
                        setVisibleRange(new Range(start, Math.min(start + visibleRange.getLength(), LocationController.getInstance().getMaxRangeEnd())));
                    }
                }
            }
        });

        map = new VariantMap(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mapPanel.add(map, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        mapPanel.add(mapScroller, gbc);
        tabs.addTab("Map", mapPanel);

        frequencyPlot = new AlleleFrequencyPlot(this);
        tabs.addTab("Allele Frequency", frequencyPlot);
        
        JPanel ldPanel = new JPanel();
        ldPanel.setLayout(new GridBagLayout());

        ActionListener redrawForcer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                ldPlot.recalculate();
                ldPlot.repaint();
            }
        };
        JRadioButton dPrimeButton = new JRadioButton("D'", true);
        dPrimeButton.setActionCommand("true");
        dPrimeButton.addActionListener(redrawForcer);
        JRadioButton rSquaredButton = new JRadioButton("r²", false);
        rSquaredButton.setActionCommand("false");
        rSquaredButton.addActionListener(redrawForcer);
        
        methodGroup = new ButtonGroup();
        JPanel methodPanel = new JPanel();
        methodPanel.setBorder(BorderFactory.createTitledBorder("Calculation Method"));
        methodPanel.add(dPrimeButton);
        methodGroup.add(dPrimeButton);
        methodPanel.add(rSquaredButton);
        methodGroup.add(rSquaredButton);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        ldPanel.add(methodPanel, gbc);

        ldPlot = new LDPlot(this);
        gbc.weighty = 1.0;
        ldPanel.add(ldPlot, gbc);

        tabs.addTab("LD Plot", ldPanel);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        add(tools, gbc);

        // Create the informative cards, but don't use them.
        messageLabel = new JLabel();
        messageLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 24));
        messageLabel.setAlignmentX(0.5f);
        progressPanel = new ProgressPanel(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                LOG.info("Cancelling data requests for " + tracks.size() + " variant tracks.");
                for (VariantTrack t: tracks) {
                    t.cancelDataRequest();
                }
            }
        });
        
        showCard(tabs, null);

        LocationController.getInstance().addListener(new Listener<LocationChangedEvent>() {
            @Override
            public void handleEvent(LocationChangedEvent event) {
                // Only change the variation range if the track range falls outside.
                String ref = event.getReference();
                RangeAdapter r = event.getRange();
                if (!ref.equals(visibleRef) || !RangeUtils.contains(visibleRange, r)) {
                    setLocation(ref, (Range)r);
                }
            }
        });

        // Attach listeners for track change events.
        TrackController.getInstance().addListener(new Listener<TrackEvent>() {

            @Override
            public void handleEvent(TrackEvent event) {
                if (event.getTrack().getDataFormat() == DataFormat.VARIANT) {
                    VariantTrack t = (VariantTrack)event.getTrack();
                    switch (event.getType()) {
                        case ADDED:
                            tracks.add(t);
                            rawData.add(null);
                            participantCount += t.getParticipantCount();
                            t.addListener(VariationSheet.this);
                            break;
                        case REMOVED:
                            participantCount -= t.getParticipantCount();
                            int index = tracks.indexOf(t);
                            tracks.remove(index);
                            rawData.remove(index);
                            t.removeListener(VariationSheet.this);
                            recalculate();
                            break;
                    }
                }
            }
        });
    }

    /**
     * Sets the data to null so we know that there's nothing to render.
     *
     * @param evt describes the data being received
     */
    @Override
    public void handleEvent(DataRetrievalEvent evt) {
        if (evt.getRange().equals(visibleRange)) {
            int index = tracks.indexOf(evt.getTrack());
            if (index >= 0) {
                switch (evt.getType()) {
                    case STARTED:
                        rawData.set(index, null);
                        showCard(progressPanel, "Retrieving variant data…");
                        break;
                    case COMPLETED:
                        if (evt.getData() != null) {
                            LOG.trace("Received " + evt.getData().size() + " records for " + evt.getTrack() + "; recalculating.");
                            rawData.set(index, (List)evt.getData());
                            recalculate();
                        }
                        break;
                    case FAILED:
                        LOG.info("Received " + evt.getError() + " error for " + evt.getTrack());
                        showCard(messageLabel, MiscUtils.getMessage(evt.getError()));
                        break;
                }
            }
        }
    }

    synchronized List<VariantRecord> getData() {
        if (aggregateData == null) {
            int n = 0;
            int i = 0;
            participantNames.clear();
            for (VariantTrack t: tracks) {
                List<VariantRecord> trackData = rawData.get(i++);
                if (trackData != null) {
                    if (aggregateData == null) {
                        aggregateData = new ArrayList<VariantRecord>(trackData.size());
                        for (VariantRecord rec: trackData) {
                            aggregateData.add(new PaddedVariantRecord(rec, 0));
                        }
                    } else {
                        // Slower process.  Traverse the list inserting data as we go.
                        // It would might be more efficient to insert everything and sort,
                        // but we have to allow for the possibility of having to munge together
                        // two VariantRecords.
                        for (VariantRecord rec: trackData) {
                            int index = Collections.binarySearch(aggregateData, rec, VARIANT_COMPARATOR);
                            if (index < 0) {
                                // Not found in list.  Insert it at the given location.
                                int insertionPos = -index - 1;
                                if (LOG.isDebugEnabled()) {
                                    String before = insertionPos > 0 ? aggregateData.get(insertionPos - 1).toString() : "START";
                                    String after = insertionPos < aggregateData.size() ? aggregateData.get(insertionPos).toString() : "END";

                                    LOG.debug("Inserting " + rec + " after " + before + " and before " + after);
                                }
                                aggregateData.add(insertionPos, new PaddedVariantRecord(rec, n));
                            } else {
                                VariantRecord oldRec = aggregateData.get(index);
                                LOG.debug("Merging " + rec + " into " + oldRec + " padding " + (n - oldRec.getParticipantCount()));
                                aggregateData.set(index, new MergedVariantRecord(oldRec, rec, n - oldRec.getParticipantCount()));
                            }
                        }
                    }
                    participantNames.addAll(Arrays.asList(((TabixDataSource)t.getDataSource()).getExtraColumns()));
                    n += t.getParticipantCount();
                }
            }
            participantCount = n;
        }
        return aggregateData;
    }

    int getParticipantCount() {
        return participantCount;
    }

    private void setLocation(String ref, Range r) {
        visibleRef = ref;
        setVisibleRange(r);
    }

    Range getVisibleRange() {
        return visibleRange;
    }

    void setVisibleRange(Range r) {
        if (!r.equals(visibleRange)) {
            adjustingRange = true;
            visibleRange = r;
            if (r.getLength() > TrackResolutionSettings.getVariantLowToHighThreshold()) {
                showCard(messageLabel, ZOOM_MESSAGE);
            } else {
                for (VariantTrack t: tracks) {
                    t.requestData(visibleRef, visibleRange);
                }
                mapScroller.setMaximum(LocationController.getInstance().getMaxRangeEnd());
                mapScroller.setValue(visibleRange.getFrom());
                mapScroller.setVisibleAmount(visibleRange.getLength());
                mapScroller.setBlockIncrement(visibleRange.getLength());
                mapScroller.repaint();
            }
            adjustingRange = false;
        }
        rangeField.setText(String.format("%s:%d-%d", visibleRef, r.getFrom(), r.getTo()));
    }

    /**
     * Zoom out by a factor of two.
     */
    void zoomOut() {
        zoomToLength(visibleRange.getLength() * 2);
    }

    /**
     * Zoom in by a factor of two.
     */
    void zoomIn() {
        zoomToLength(visibleRange.getLength() / 2);
    }

    private void zoomToLength(int length) {
        int maxLen = LocationController.getInstance().getMaxRangeEnd();
        if (length > maxLen) {
            length = maxLen;
        } else if (length < 1) {
            length = 1;
        }
        LOG.trace("Zooming to length " + length);
        int from = (visibleRange.getFrom() + 1 + visibleRange.getTo() - length) / 2;
        int to = from + length - 1;

        if (from < 1) {
            to += 1 - from;
            from = 1;
        }

        if (to > maxLen) {
            from -= to - maxLen;
            to = maxLen;

            if (from < 1) {
                from = 1;
            }
        }

        setVisibleRange(new Range(from, to));
    }

    /**
     * One of our panels has clicked or double-clicked.  Navigate to the record in the main window.
     */
    void navigateToRecord(VariantRecord rec) {
        LocationController.getInstance().setLocation((Range)RangeUtils.addMargin(new Range(rec.getPosition(), rec.getPosition())));
    }
    
    /**
     * A mouse-move on one of our sub-panels.  Update the status bar in the main window.
     */
    void updateStatusBar(VariantRecord rec) {
        if (rec != null) {
            GraphPaneController.getInstance().setMouseXPosition(rec.getPosition());
        } else {
            GraphPaneController.getInstance().setMouseXPosition(-1);
        }
    }
    
    void recalculate() {
        for (List<VariantRecord> trackData: rawData) {
            if (trackData == null) {
                // One of the tracks hasn't reported in yet.
                return;
            }
        }
        showCard(progressPanel, "Aggregating variant data…");
        aggregateData = null;
        table.setModel(new VariantTableModel(getData()));
        if (aggregateData.size() > 0) {
            showCard(tabs, null);
            map.repaint();
            ldPlot.recalculate();
            ldPlot.repaint();
        } else {
            showCard(messageLabel, "No data in range");
        }
    }

    boolean isDPrimeSelected() {
        return Boolean.parseBoolean(methodGroup.getSelection().getActionCommand());
    }

    final void showCard(JComponent card, String message) {
        if (currentCard != card) {
            if (currentCard != null) {
                remove(currentCard);
            }
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            
            if (card == tabs) {
                gbc.fill = GridBagConstraints.BOTH;
            } else {
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(20, 20, 20, 20);
                gbc.anchor = GridBagConstraints.NORTH;
                if (message != null) {
                    if (card == messageLabel) {
                        messageLabel.setText(message);
                    } else {
                        progressPanel.setMessage(message);
                    }
                }
            }
            add(card, gbc);
            currentCard = card;
            repaint();
            validate();
        }
    }
}
