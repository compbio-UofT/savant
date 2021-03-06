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
package savant.settings;

import java.awt.Color;
import java.util.EnumMap;

import savant.util.ColourKey;

/**
 * Class which keeps track of global colour-scheme settings.
 *
 * @author AndrewBrook, tarkvara
 */
public class ColourSettings {
    private static final EnumMap<ColourKey, Color> DEFAULT_SETTINGS;

    static {
        DEFAULT_SETTINGS = new EnumMap<ColourKey, Color>(ColourKey.class);
        DEFAULT_SETTINGS.put(ColourKey.A, new Color(27, 97, 97));
        DEFAULT_SETTINGS.put(ColourKey.C, new Color(162, 45, 45));
        DEFAULT_SETTINGS.put(ColourKey.G, new Color(36, 130, 36));
        DEFAULT_SETTINGS.put(ColourKey.T, new Color(162, 98, 45));
        DEFAULT_SETTINGS.put(ColourKey.N, new Color(100,100,100));
        DEFAULT_SETTINGS.put(ColourKey.DELETED_BASE, Color.BLACK);
        DEFAULT_SETTINGS.put(ColourKey.INSERTED_BASE, new Color(204, 102, 255));
        DEFAULT_SETTINGS.put(ColourKey.SKIPPED, Color.GRAY);

        DEFAULT_SETTINGS.put(ColourKey.FORWARD_STRAND, new Color(0, 131, 192));
        DEFAULT_SETTINGS.put(ColourKey.REVERSE_STRAND, new Color(0, 174, 255));

        DEFAULT_SETTINGS.put(ColourKey.CONCORDANT_LENGTH, new Color(0, 174, 255));
        DEFAULT_SETTINGS.put(ColourKey.DISCORDANT_LENGTH, Color.RED);
        DEFAULT_SETTINGS.put(ColourKey.ONE_READ_INVERTED, Color.BLUE);
        DEFAULT_SETTINGS.put(ColourKey.EVERTED_PAIR, Color.YELLOW);
        DEFAULT_SETTINGS.put(ColourKey.UNMAPPED_MATE, Color.GRAY);

        /*
         *  DEFAULT_SETTINGS.put(ColourKey.CONCORDANT_LENGTH, new Color(128, 177, 211));//new Color(0, 174, 255));
        DEFAULT_SETTINGS.put(ColourKey.DISCORDANT_LENGTH, new Color(190, 186, 218));//Color.BLUE);
        DEFAULT_SETTINGS.put(ColourKey.ONE_READ_INVERTED, new Color(251, 128, 114));//Color.MAGENTA);
        DEFAULT_SETTINGS.put(ColourKey.EVERTED_PAIR, new Color(255, 255, 179));//Color.GRAY);
        DEFAULT_SETTINGS.put(ColourKey.UNMAPPED_MATE, Color.BLACK);
         */

        DEFAULT_SETTINGS.put(ColourKey.CONTINUOUS_FILL, new Color(0, 174, 255, 200));
        DEFAULT_SETTINGS.put(ColourKey.CONTINUOUS_LINE, new Color(0, 50, 50, 50));

        DEFAULT_SETTINGS.put(ColourKey.POINT_FILL, new Color(0, 174, 255, 150));
        DEFAULT_SETTINGS.put(ColourKey.POINT_LINE, Color.BLACK);

        DEFAULT_SETTINGS.put(ColourKey.INTERVAL_LINE, Color.GRAY);
        DEFAULT_SETTINGS.put(ColourKey.INTERVAL_TEXT, Color.DARK_GRAY);
        DEFAULT_SETTINGS.put(ColourKey.OPAQUE_GRAPH, new Color(0, 174, 255));
        DEFAULT_SETTINGS.put(ColourKey.TRANSLUCENT_GRAPH, new Color(0, 174, 255, 100));

        DEFAULT_SETTINGS.put(ColourKey.SPLITTER, new Color(210, 210, 210));

        DEFAULT_SETTINGS.put(ColourKey.GRAPH_PANE_MESSAGE, Color.DARK_GRAY);
        DEFAULT_SETTINGS.put(ColourKey.GRAPH_PANE_BACKGROUND_TOP, Color.WHITE);
        DEFAULT_SETTINGS.put(ColourKey.GRAPH_PANE_BACKGROUND_BOTTOM, new Color(210, 210, 210));
        DEFAULT_SETTINGS.put(ColourKey.GRAPH_PANE_ZOOM_FILL, new Color(0, 0, 255, 100));
        DEFAULT_SETTINGS.put(ColourKey.GRAPH_PANE_SELECTION_FILL, new Color(120, 70, 10, 100));
        DEFAULT_SETTINGS.put(ColourKey.AXIS_GRID, Color.LIGHT_GRAY);

        DEFAULT_SETTINGS.put(ColourKey.HEATMAP_LOW, new Color(0, 0, 255));
        DEFAULT_SETTINGS.put(ColourKey.HEATMAP_MEDIUM, new Color(192, 128, 192));
        DEFAULT_SETTINGS.put(ColourKey.HEATMAP_HIGH, new Color(255, 0, 0));
    }

    private static PersistentSettings settings = PersistentSettings.getInstance();

    public static Color getColor(ColourKey key) {
        return settings.getColor(key, DEFAULT_SETTINGS.get(key));
    }

    public static void setColor(ColourKey key, Color value) {
        settings.setColor(key, value);
    }
}
