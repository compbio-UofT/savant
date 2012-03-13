/*
 *    Copyright 2011-2012 University of Toronto
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
 * Settings which control the ranges at which various tracks decide to display vs. not
 * display vs. fall back to coverage.
 *
 * @author mfiume
 */
public class ResolutionSettings {
    private static final PersistentSettings SETTINGS = PersistentSettings.getInstance();

    // The defaults
    private static final int SEQUENCE_LOW_TO_HIGH_DEFAULT = 10000;
    private static final int INTERVAL_LOW_TO_HIGH_DEFAULT = 1000000;
    private static final int BAM_LOW_TO_HIGH_DEFAULT = 20000;
    private static final int BAM_ARC_LOW_TO_HIGH_DEFAULT = 100000;
    private static final int CONTINUOUS_LOW_TO_HIGH_DEFAULT = 10000;
    private static final int VARIANT_LOW_TO_HIGH_DEFAULT = 1000000;
    private static final int LD_MAX_LOCI_DEFAULT = 500;

    // The property keys.
    private static final String SEQUENCE_LOW_TO_HIGH_KEY = "SequenceLowToHigh";
    private static final String INTERVAL_LOW_TO_HIGH_KEY = "IntervalLowToHigh";
    private static final String BAM_LOW_TO_HIGH_KEY = "BAMLowToHigh";
    private static final String BAM_ARC_LOW_TO_HIGH_KEY = "BAMArcLowToHigh";
    private static final String CONSERVATION_LOW_TO_HIGH_KEY = "ConservationLowToHigh";
    private static final String VARIANT_LOW_TO_HIGH_KEY = "VariantLowToHigh";
    private static final String LD_MAX_LOCI_KEY = "LDMaxLoci";

    public static int getBAMArcModeLowToHighThreshold() {
        return SETTINGS.getInt(BAM_ARC_LOW_TO_HIGH_KEY, BAM_ARC_LOW_TO_HIGH_DEFAULT);
    }

    public static void setBAMArcModeLowToHighThreshold(int value) {
        SETTINGS.setInt(BAM_ARC_LOW_TO_HIGH_KEY, value);
    }

    public static int getBAMLowToHighThreshold() {
        return SETTINGS.getInt(BAM_LOW_TO_HIGH_KEY, BAM_LOW_TO_HIGH_DEFAULT);
    }

    public static void setBAMLowToHighThreshold(int value) {
        SETTINGS.setInt(BAM_LOW_TO_HIGH_KEY, value);
    }

    public static int getIntervalLowToHighThreshold() {
        return SETTINGS.getInt(INTERVAL_LOW_TO_HIGH_KEY, INTERVAL_LOW_TO_HIGH_DEFAULT);
    }

    public static void setIntervalLowToHighThreshold(int value) {
        SETTINGS.setInt(INTERVAL_LOW_TO_HIGH_KEY, value);
    }

    public static int getSequenceLowToHighThreshold() {
        return SETTINGS.getInt(SEQUENCE_LOW_TO_HIGH_KEY, SEQUENCE_LOW_TO_HIGH_DEFAULT);
    }

    public static void setSequenceLowToHighThreshold(int value) {
        SETTINGS.setInt(SEQUENCE_LOW_TO_HIGH_KEY, value);
    }

    public static int getContinuousLowToHighThreshold() {
        return SETTINGS.getInt(CONSERVATION_LOW_TO_HIGH_KEY, CONTINUOUS_LOW_TO_HIGH_DEFAULT);
    }

    public static void setContinuousLowToHighThreshold(int value) {
        SETTINGS.setInt(CONSERVATION_LOW_TO_HIGH_KEY, value);
    }

    public static int getVariantLowToHighThreshold() {
        return SETTINGS.getInt(VARIANT_LOW_TO_HIGH_KEY, VARIANT_LOW_TO_HIGH_DEFAULT);
    }

    public static void setVariantLowToHighThreshold(int value) {
        SETTINGS.setInt(VARIANT_LOW_TO_HIGH_KEY, value);
    }

    public static int getLDMaxLoci() {
        return SETTINGS.getInt(LD_MAX_LOCI_KEY, LD_MAX_LOCI_DEFAULT);
    }

    public static void setLDMaxLoci(int value) {
        SETTINGS.setInt(LD_MAX_LOCI_KEY, value);
    }
}
