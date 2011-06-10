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


/**
 *
 * @author mfiume
 */
public class TrackResolutionSettings {
    private static final PersistentSettings SETTINGS = PersistentSettings.getInstance();

    // The defaults
    private static final int SEQUENCE_LOW_TO_HIGH = 10000;
    private static final int INTERVAL_LOW_TO_HIGH = 1000000;
    private static final int BAM_DEFAULT_LOW_TO_HIGH = 20000;
    private static final int BAM_ARC_LOW_TO_HIGH = 100000;

    // The property keys.
    private static final String SEQUENCE_LOW_TO_HIGH_KEY = "SequenceLowToHigh";
    private static final String INTERVAL_LOW_TO_HIGH_KEY = "IntervalLowToHigh";
    private static final String BAM_DEFAULT_LOW_TO_HIGH_KEY = "BamDefaultLowToHigh";
    private static final String BAM_ARC_LOW_TO_HIGH_KEY = "BamArcLowToHigh";

    public static int getBamArcModeLowToHighThresh() {
        return SETTINGS.getInt(BAM_ARC_LOW_TO_HIGH_KEY, BAM_ARC_LOW_TO_HIGH);
    }

    public static void setBamArcModeLowToHighThresh(int value) {
        SETTINGS.setInt(BAM_ARC_LOW_TO_HIGH_KEY, value);
    }

    public static int getBamDefaultModeLowToHighThresh() {
        return SETTINGS.getInt(BAM_DEFAULT_LOW_TO_HIGH_KEY, BAM_DEFAULT_LOW_TO_HIGH);
    }

    public static void setBamDefaultModeLowToHighThresh(int value) {
        SETTINGS.setInt(BAM_DEFAULT_LOW_TO_HIGH_KEY, value);
    }

    public static int getIntervalLowToHighThresh() {
        return SETTINGS.getInt(INTERVAL_LOW_TO_HIGH_KEY, INTERVAL_LOW_TO_HIGH);
    }

    public static void setIntervalLowToHighThresh(int value) {
        SETTINGS.setInt(INTERVAL_LOW_TO_HIGH_KEY, value);
    }

    public static int getSequenceLowToHighThresh() {
        return SETTINGS.getInt(SEQUENCE_LOW_TO_HIGH_KEY, SEQUENCE_LOW_TO_HIGH);
    }

    public static void setSequenceLowToHighThresh(int value) {
        SETTINGS.setInt(SEQUENCE_LOW_TO_HIGH_KEY, value);
    }
}
