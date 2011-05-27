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

package savant.settings;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author AndrewBrook
 */
public class DisplaySettings {

    // The defaults
    private static int BAM_INTERVAL_HEIGHT = 12;
    private static int RICH_INTERVAL_HEIGHT = 16;
    private static int GENERIC_INTERVAL_HEIGHT = 12;

    public static int getBamIntervalHeight() {
        return Integer.parseInt(PersistentSettings.getInstance().getProperty("BAM_INTERVAL_HEIGHT", BAM_INTERVAL_HEIGHT + ""));
    }
    
    public static int getRichIntervalHeight() {
        return Integer.parseInt(PersistentSettings.getInstance().getProperty("RICH_INTERVAL_HEIGHT", RICH_INTERVAL_HEIGHT + ""));
    }
    
    public static int getGenericIntervalHeight() {
        return Integer.parseInt(PersistentSettings.getInstance().getProperty("GENERIC_INTERVAL_HEIGHT", GENERIC_INTERVAL_HEIGHT + ""));
    }

    public static void setBamIntervalHeight(int BAM_INTERVAL_HEIGHT) {
        DisplaySettings.BAM_INTERVAL_HEIGHT = BAM_INTERVAL_HEIGHT;
        PersistentSettings.getInstance().setProperty("BAM_INTERVAL_HEIGHT", BAM_INTERVAL_HEIGHT + "");
        try {
            PersistentSettings.getInstance().store();
        } catch (IOException ex) {
            Logger.getLogger(DisplaySettings.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void setRichIntervalHeight(int RICH_INTERVAL_HEIGHT) {
        DisplaySettings.RICH_INTERVAL_HEIGHT = RICH_INTERVAL_HEIGHT;
        PersistentSettings.getInstance().setProperty("RICH_INTERVAL_HEIGHT", RICH_INTERVAL_HEIGHT + "");
        try {
            PersistentSettings.getInstance().store();
        } catch (IOException ex) {
            Logger.getLogger(DisplaySettings.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void setGenericIntervalHeight(int GENERIC_INTERVAL_HEIGHT) {
        DisplaySettings.GENERIC_INTERVAL_HEIGHT = GENERIC_INTERVAL_HEIGHT;
        PersistentSettings.getInstance().setProperty("GENERIC_INTERVAL_HEIGHT", GENERIC_INTERVAL_HEIGHT + "");
        try {
            PersistentSettings.getInstance().store();
        } catch (IOException ex) {
            Logger.getLogger(DisplaySettings.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


}
