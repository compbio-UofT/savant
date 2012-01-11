/*
 *    Copyright 2009-2012 University of Toronto
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
        DEFAULT_SETTINGS.put(ColourKey.INSERTED_BASE, Color.MAGENTA);
        DEFAULT_SETTINGS.put(ColourKey.SKIPPED, Color.GRAY);

        DEFAULT_SETTINGS.put(ColourKey.FORWARD_STRAND, new Color(0, 131, 192));
        DEFAULT_SETTINGS.put(ColourKey.REVERSE_STRAND, new Color(0, 174, 255));

        DEFAULT_SETTINGS.put(ColourKey.CONCORDANT_LENGTH, new Color(0, 174, 255));
        DEFAULT_SETTINGS.put(ColourKey.DISCORDANT_LENGTH, Color.BLUE);
        DEFAULT_SETTINGS.put(ColourKey.ONE_READ_INVERTED, Color.MAGENTA);
        DEFAULT_SETTINGS.put(ColourKey.EVERTED_PAIR, Color.GRAY);

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

        DEFAULT_SETTINGS.put(ColourKey.TOOLS_MARGIN_BACKGROUND, new Color(236, 236, 236));
        DEFAULT_SETTINGS.put(ColourKey.TOOLS_BACKGROUND, Color.WHITE);
    }

    private static PersistentSettings settings = PersistentSettings.getInstance();

    public static Color getColor(ColourKey key) {
        return settings.getColor(key, DEFAULT_SETTINGS.get(key));
    }

    public static void setColor(ColourKey key, Color value) {
        settings.setColor(key, value);
    }
}
