package savant.api.data;

public enum DataFormat {

    SEQUENCE_FASTA,
    POINT_GENERIC,
    CONTINUOUS_GENERIC,
    INTERVAL_GENERIC,
    INTERVAL_RICH,
    INTERVAL_BAM;

    @Override
    public String toString() {
        switch (this) {
            case SEQUENCE_FASTA:
                return "Fasta Sequence";
            case POINT_GENERIC:
                return "Point";
            case CONTINUOUS_GENERIC:
                return "Continuous";
            case INTERVAL_GENERIC:
                return "Generic Interval";
            case INTERVAL_RICH:
                return "Rich Interval";
            case INTERVAL_BAM:
                return "BAM Interval";
        }
        return null;
    }
}