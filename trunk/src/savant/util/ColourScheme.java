/*
 *    Copyright 2009-2011 University of Toronto
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

package savant.util;

import java.awt.Color;
import java.util.EnumMap;

import savant.settings.ColourSettings;

/**
 * Stores the colour scheme associated with a given track.
 *
 * @author tarkvara
 */
public class ColourScheme {

    /**
     * Contains only colours which have been explicitly set.  Other colours will be
     * pulled from the global ColourSettings.
     */
    private EnumMap<ColourKey, Color> colours;

    public ColourScheme(ColourKey... keys) {
        colours = new EnumMap<ColourKey, Color>(ColourKey.class);
        for (ColourKey k: keys) {
            colours.put(k, null);
        }
    }

    public Color getColor(ColourKey key) {
        Color result = colours.get(key);
        if (result == null) {
            // If the colour doesn't already exist, retrieve the one from the global colour table.
            result = ColourSettings.getColor(key);
        }
        return result;
    }

    public void setColor(ColourKey key, Color value) {
        colours.put(key, value);
    }

    /**
     * Often we just want to get the colour of a base.
     *
     * @param baseLetter 'A', 'C', 'T', 'G', or 'N'
     * @return
     */
    public Color getBaseColor(char baseLetter) {

        switch (baseLetter) {
            case 'A':
                return getColor(ColourKey.A);
            case 'C':
                return getColor(ColourKey.C);
            case 'G':
                return getColor(ColourKey.G);
            case 'T':
                return getColor(ColourKey.T);
            case 'N':
                return getColor(ColourKey.N);
            default:
                return null;
        }
    }

    public EnumMap<ColourKey, Color> getMap() {
        return colours;
    }
}
