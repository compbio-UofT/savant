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
package savant.util;

import java.awt.Color;
import java.util.EnumMap;

import savant.api.data.VariantType;
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
     * @return the {@link Color} to be used for the given base
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

    public Color getVariantColor(VariantType var) {
        switch (var) {
            case SNP_A:
                return getBaseColor('A');
            case SNP_C:
                return getBaseColor('C');
            case SNP_G:
                return getBaseColor('G');
            case SNP_T:
                return getBaseColor('T');
            case INSERTION:
                return getColor(ColourKey.INSERTED_BASE);
            case DELETION:
                return getColor(ColourKey.DELETED_BASE);
            case OTHER:
                return getColor(ColourKey.N);
            default:
                return null;
        }
    }

    public EnumMap<ColourKey, Color> getMap() {
        return colours;
    }
}
