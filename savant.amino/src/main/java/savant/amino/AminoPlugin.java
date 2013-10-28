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
package savant.amino;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import savant.plugin.SavantPanelPlugin;


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
                    TrackAdapter[] existingTracks = TrackUtils.getTracks(DataFormat.RICH_INTERVAL);
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
                    if (t.getDataFormat() == DataFormat.RICH_INTERVAL) {
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
        if (t.getDataFormat() == DataFormat.RICH_INTERVAL) {
            JPanel layerCanvas = t.getLayerCanvas(AminoPlugin.this);
            layerCanvas.setLayout(new BorderLayout());
            AminoCanvas c = new AminoCanvas(AminoPlugin.this, t);
            layerCanvas.add(c, BorderLayout.CENTER);
        }
    }
}
