/*
 *    Copyright 2011 University of Toronto
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

package savant.amino;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.SavantPanelPlugin;
import savant.api.adapter.TrackAdapter;
import savant.api.data.DataFormat;
import savant.api.event.LocationChangedEvent;
import savant.api.event.PluginEvent;
import savant.api.event.TrackEvent;
import savant.api.util.Listener;
import savant.api.util.NavigationUtils;
import savant.api.util.PluginUtils;
import savant.api.util.SettingsUtils;
import savant.api.util.TrackUtils;


/**
 * Demonstration plugin to show how to do simple drawing on top of a Savant track.
 *
 * @author tarkvara
 */
public class AminoPlugin extends SavantPanelPlugin {
    static final Log LOG = LogFactory.getLog(AminoPlugin.class);

    /**
     * Create the user-interface which appears within the panel.
     *
     * @param panel provided by Savant
     */
    @Override
    public void init(JPanel panel) {
        panel.setLayout(new GridLayout(5, 4, 2, 2));

        // Create a label for each amino-acid.  For now, these are just for reference,
        // but we might make them into buttons to change the colour scheme.
        for (AminoAcid a: AminoAcid.values()) {
            JLabel label = new JLabel(a.name, SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(a.color);
            label.setForeground(a == AminoAcid.STOP ? Color.WHITE : Color.BLACK);
            panel.add(label);
        }

        // First time through, create canvasses for any existing gene tracks.  We
        // hook this onto a listener so that we'll know that Savant has fully loaded
        // the plugin before we try to do anything.
        PluginUtils.addPluginListener(new Listener<PluginEvent>() {
            @Override
            public void handleEvent(PluginEvent event) {
                if (event.getType() == PluginEvent.Type.LOADED && event.getPlugin() instanceof AminoPlugin) {
                    PluginUtils.removePluginListener(this);
                    TrackAdapter[] existingTracks = TrackUtils.getTracks(DataFormat.INTERVAL_RICH);
                    for (TrackAdapter t: existingTracks) {
                        createCanvas(t);
                    }
                }
            }
        });

        TrackUtils.addTrackListener(new Listener<TrackEvent>() {
            @Override
            public void handleEvent(TrackEvent event) {
                if (event.getType() == TrackEvent.Type.ADDED) {
                    createCanvas(event.getTrack());
                }
            }
        });

        NavigationUtils.addLocationChangedListener(new Listener<LocationChangedEvent>() {
            @Override
            public void handleEvent(LocationChangedEvent event) {
                for (TrackAdapter t: TrackUtils.getTracks()) {
                    if (t.getDataFormat() == DataFormat.INTERVAL_RICH) {
                        t.getLayerCanvas(AminoPlugin.this).repaint();
                    }
                }
            }
        });
    }

    /**
     * Title which will appear on plugin's tab in Savant user interface.
     */
    @Override
    public String getTitle() {
        return "Amino Acid Plugin";
    }

    /**
     * Just as a simple example of the settings API, the plugin retrieves the desired transparency from the savant.settings file.
     */
    public int getAlpha() {
        return SettingsUtils.getInt(this, "ALPHA", 40);
    }

    /**
     * Creates the actual canvas which gets drawn on top of the tracks.  If the track
     * is not a gene track, does nothing.
     *
     * @param t the track to be considered
     */
    private void createCanvas(TrackAdapter t) {
        if (t.getDataFormat() == DataFormat.INTERVAL_RICH) {
            JPanel layerCanvas = t.getLayerCanvas(AminoPlugin.this);
            layerCanvas.setLayout(new BorderLayout());
            AminoCanvas c = new AminoCanvas(AminoPlugin.this, t);
            layerCanvas.add(c, BorderLayout.CENTER);
        }
    }
}
