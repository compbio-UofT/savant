/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.settings;

/**
 *
 * @author mfiume
 */
public class TrackResolutionSettings {

    // Sequence Resolution
    private static int SEQUENCE_LOW_TO_HIGH = 500000;
    private static int INTERVAL_LOW_TO_HIGH = 1000000;
    private static int BAM_DEFAULT_LOW_TO_HIGH = 20000;
    private static int BAM_ARC_LOW_TO_HIGH = 100000;

    // Interval Resolution


    // BAM Rresolution

    // Continuous resolution
    // NONE: automatic

    public static int getSequenceLowToHighThresh() { return SEQUENCE_LOW_TO_HIGH; }

    public static int getIntervalLowToHighThresh() { return INTERVAL_LOW_TO_HIGH; }

    public static int getBAMDefaultModeLowToHighThresh() { return BAM_DEFAULT_LOW_TO_HIGH; }

    public static int getBAMArcModeLowToHighThresh() { return BAM_ARC_LOW_TO_HIGH; }


    public static void setBAMArcModeLowToHighThresh(int BAM_ARC_LOW_TO_HIGH) {
        TrackResolutionSettings.BAM_ARC_LOW_TO_HIGH = BAM_ARC_LOW_TO_HIGH;
    }

    public static void setBAMDefaultModeLowToHighThresh(int BAM_DEFAULT_LOW_TO_HIGH) {
        TrackResolutionSettings.BAM_DEFAULT_LOW_TO_HIGH = BAM_DEFAULT_LOW_TO_HIGH;
    }

    public static void setIntervalLowToHighThresh(int INTERVAL_LOW_TO_HIGH) {
        TrackResolutionSettings.INTERVAL_LOW_TO_HIGH = INTERVAL_LOW_TO_HIGH;
    }

    public static void setSequenceLowToHighThresh(int SEQUENCE_LOW_TO_HIGH) {
        TrackResolutionSettings.SEQUENCE_LOW_TO_HIGH = SEQUENCE_LOW_TO_HIGH;
    }


}
