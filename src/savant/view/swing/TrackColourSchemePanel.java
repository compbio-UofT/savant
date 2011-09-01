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

package savant.view.swing;

import java.awt.Color;

import savant.settings.ColourSchemeSettingsSection;
import savant.util.ColourKey;
import savant.util.ColourScheme;

/**
 * Modifies the <code>ColourSchemeSettingsSection</code> class to make a panel suitable
 * for modifying the colour scheme of a single track.
 *
 * @author tarkvara
 */
public class TrackColourSchemePanel extends ColourSchemeSettingsSection {
    private final Track track;

    public TrackColourSchemePanel(Track t) {
        track = t;
    }

    @Override
    public void applyChanges() {
        // Only save anything if this panel has gone through lazy initialization.
        if (pane != null) {
            ColourScheme cs = track.getColourScheme();
            for (ColourKey k: map.keySet()) {
                Color c = map.get(k);
                if (c != null) {
                    cs.setColor(k, c);
                }
            }
            track.getFrame().forceRedraw();
        }
    }

    @Override
    public void populate() {
        ColourScheme cs = track.getColourScheme();
        for (ColourKey k: cs.getMap().keySet()) {
            addProperty(k, track.getName());
        }
        model.expandAll();
    }
}
