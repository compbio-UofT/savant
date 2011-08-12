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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.TrackAdapter;
import savant.api.util.NavigationUtils;
import savant.api.util.SettingsUtils;
import savant.api.util.TrackUtils;
import savant.controller.event.TrackEvent;
import savant.file.DataFormat;
import savant.plugin.SavantPanelPlugin;
import savant.util.Listener;


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
     * @param canvas provided by Savant
     */
    @Override
    public void init(JPanel canvas) {
        canvas.setLayout(new GridLayout(5, 4));

        // Create a label for each amino-acid.  For now, these are just for reference,
        // but we might make them into buttons to change the colour scheme.
        for (AminoAcid a: AminoAcid.values()) {
            JLabel label = new JLabel(a.name);
            label.setOpaque(true);
            label.setBackground(a.color);
            canvas.add(label);
        }

        TrackUtils.addTrackListener(new Listener<TrackEvent>() {
            @Override
            public void handleEvent(TrackEvent event) {
                LOG.info("Received " + event.getType() + " for " + event.getTrack().getName());
                TrackAdapter t = event.getTrack();
                if (t.getDataSource().getDataFormat() == DataFormat.INTERVAL_RICH) {
                    switch (event.getType()) {
                        case OPENED:
                            // Load the initial sequence.
                            AminoCanvas c = new AminoCanvas(AminoPlugin.this, t);
                            JPanel layerCanvas = t.getLayerCanvas();
                            layerCanvas.setLayout(new BorderLayout());
                            layerCanvas.add(c, BorderLayout.CENTER);
                            NavigationUtils.addLocationChangeListener(c);
                            break;
                        case REMOVED:
                            t.getLayerCanvas().removeAll();
                            break;
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
     * Just as a simple example of the settings API, the plugin retrieves the desired text colour from the savant.settings file.
     */
    public Color getLabelColour() {
        return SettingsUtils.getColour(this, "LABEL", Color.GRAY);
    }
}
