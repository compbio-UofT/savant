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

import savant.api.data.DataFormat;


/**
 * Settings relevant to the Savant user interface.
 * 
 * @author AndrewBrook
 */
public class InterfaceSettings {
    private static final PersistentSettings SETTINGS = PersistentSettings.getInstance();

    // The defaults
    private static final int BAM_INTERVAL_HEIGHT = 12;
    private static final int RICH_INTERVAL_HEIGHT = 16;
    private static final int GENERIC_INTERVAL_HEIGHT = 12;

    // The property keys.
    private static final String BAM_INTERVAL_HEIGHT_KEY = "BamIntervalHeight";
    private static final String RICH_INTERVAL_HEIGHT_KEY = "RichIntervalHeight";
    private static final String GENERIC_INTERVAL_HEIGHT_KEY = "GenericIntervalHeight";
    private static final String POPUPS_DISABLED_KEY = "PopupsDisabled";
    private static final String LEGENDS_DISABLED_KEY = "LegendsDisabled";
    private static final String WHEEL_ZOOMS_KEY = "WheelZooms";

    public static int getBamIntervalHeight() {
        return SETTINGS.getInt(BAM_INTERVAL_HEIGHT_KEY, BAM_INTERVAL_HEIGHT);
    }
    
    public static int getRichIntervalHeight() {
        return SETTINGS.getInt(RICH_INTERVAL_HEIGHT_KEY, RICH_INTERVAL_HEIGHT);
    }
    
    public static int getGenericIntervalHeight() {
        return SETTINGS.getInt(GENERIC_INTERVAL_HEIGHT_KEY, GENERIC_INTERVAL_HEIGHT);
    }

    public static int getIntervalHeight(DataFormat type) {
        switch (type) {
            case ALIGNMENT:
                return getBamIntervalHeight();
            case RICH_INTERVAL:
                return getRichIntervalHeight();
            default:
                return getGenericIntervalHeight();
        }
    }

    public static void setBamIntervalHeight(int value) {
        SETTINGS.setInt(BAM_INTERVAL_HEIGHT_KEY, value);
    }

    public static void setRichIntervalHeight(int value) {
        SETTINGS.setInt(RICH_INTERVAL_HEIGHT_KEY, value);
    }

    public static void setGenericIntervalHeight(int value) {
        SETTINGS.setInt(GENERIC_INTERVAL_HEIGHT_KEY, value);
    }

    public static boolean areLegendsDisabled() {
        return SETTINGS.getBoolean(LEGENDS_DISABLED_KEY, false);
    }

    public static void setLegendsDisabled(boolean value) {
        SETTINGS.setBoolean(LEGENDS_DISABLED_KEY, value);
    }

    public static boolean arePopupsDisabled() {
        return SETTINGS.getBoolean(POPUPS_DISABLED_KEY, false);
    }

    public static void setPopupsDisabled(boolean value) {
        SETTINGS.setBoolean(POPUPS_DISABLED_KEY, value);
    }
    
    public static boolean doesWheelZoom() {
        return SETTINGS.getBoolean(WHEEL_ZOOMS_KEY, true);
    }
    
    public static void setWheelZooms(boolean value) {
        SETTINGS.setBoolean(WHEEL_ZOOMS_KEY, value);
    }
}
