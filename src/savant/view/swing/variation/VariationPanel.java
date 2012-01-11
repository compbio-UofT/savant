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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.data.DataFormat;
import savant.api.data.VariantRecord;
import savant.api.event.DataRetrievalEvent;
import savant.api.event.LocationChangedEvent;
import savant.api.event.TrackEvent;
import savant.api.util.Listener;
import savant.controller.LocationController;
import savant.controller.TrackController;
import savant.util.Range;
import savant.view.icon.SavantIconFactory;
import savant.view.tracks.VariantTrack;


/**
 * Panel which displays aggregated variation information in a variety of different ways.
 *
 * @author tarkvara
 */
public class VariationPanel extends JPanel implements Listener<DataRetrievalEvent> {
    private static final Log LOG = LogFactory.getLog(VariantMap.class);

    Map<VariantTrack, List<VariantRecord>> rawData = new HashMap<VariantTrack, List<VariantRecord>>();
    private int participantCount;
    private String reference;
    private Range visibleRange;
    
    private VariantMap map;
    
    private JTextField rangeField;
    private JScrollBar mapScroller;

    public VariationPanel() {
        setLayout(new GridBagLayout());
        
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
        
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Table", new VariantTable(this));
        
        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());
        
        mapScroller = new JScrollBar();
        mapScroller.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent ae) {
                int start = ae.getValue();
                if (start != visibleRange.getFrom()) {
                    setVisibleRange(new Range(start, Math.min(start + visibleRange.getLength(), LocationController.getInstance().getMaxRangeEnd())));
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

        tabs.addTab("LD Plot", new LDPlot(this));

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        add(tools, gbc);
        gbc.weighty = 1.0;
        add(tabs, gbc);

        LocationController.getInstance().addListener(new Listener<LocationChangedEvent>() {
            @Override
            public void handleEvent(LocationChangedEvent event) {
                setLocation(event.getReference(), (Range)event.getRange());
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
                            rawData.put(t, null);
                            participantCount += t.getParticipantCount();
                            t.addListener(VariationPanel.this);
                            break;
                        case REMOVED:
                            participantCount -= t.getParticipantCount();
                            rawData.remove(t);
                            t.removeListener(VariationPanel.this);
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
        switch (evt.getType()) {
            case STARTED:
                rawData.put((VariantTrack)evt.getTrack(), null);
                break;
            case COMPLETED:
                rawData.put((VariantTrack)evt.getTrack(), (List)evt.getData());
                map.repaint();
                break;
        }
    }

    // TODO: Should aggregate variants from all VCF tracks.
    List<VariantRecord> getData() {
        return rawData.entrySet().iterator().next().getValue();
    }

    int getParticipantCount() {
        return participantCount;
    }

    private void setLocation(String ref, Range r) {
        reference = ref;
        setVisibleRange(r);
    }

    void setVisibleRange(Range r) {
        if (!r.equals(visibleRange)) {
            visibleRange = r;
            for (VariantTrack t: rawData.keySet()) {
                t.requestData(reference, visibleRange);
            }
            mapScroller.setMaximum(LocationController.getInstance().getMaxRangeEnd());
            mapScroller.setValue(visibleRange.getFrom());
            mapScroller.setVisibleAmount(visibleRange.getLength());
            mapScroller.repaint();
        }
        rangeField.setText(String.format("%s:%d-%d", reference, r.getFrom(), r.getTo()));
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
        LOG.info("Zooming to length " + length);
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
}
