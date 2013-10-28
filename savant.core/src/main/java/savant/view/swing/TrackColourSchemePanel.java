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

import savant.view.tracks.Track;
import java.awt.Color;

import savant.settings.ColourSettingsSection;
import savant.util.ColourKey;
import savant.util.ColourScheme;

/**
 * Modifies the <code>ColourSchemeSettingsSection</code> class to make a panel suitable
 * for modifying the colour scheme of a single track.
 *
 * @author tarkvara
 */
public class TrackColourSchemePanel extends ColourSettingsSection {
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
