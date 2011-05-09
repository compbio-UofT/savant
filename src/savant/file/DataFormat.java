package savant.file;

/*
 * Note: if changes are made to this file, corresponding changes should be made
 * to MiscUtils.dataFormatToString() and MiscUtils.dataFormatFromString()
 */

public enum DataFormat {

    SEQUENCE_FASTA,
    POINT_GENERIC,
    CONTINUOUS_GENERIC,
    INTERVAL_GENERIC,
    INTERVAL_BED,
    INTERVAL_BAM,
    TABIX;

}