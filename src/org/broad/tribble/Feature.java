package org.broad.tribble;


/**
 * Represents a locus on a reference sequence.   The coordinate conventions for start and end are implementation
 * dependent, no specific contact is specified here.
 */
public interface Feature {

    /**
     * Return the features reference sequence name, e.g chromosome or contig
     */
    public String getChr();

    /**
     * Return the start position
     */
    public int getStart();

    /**
     * Return the end position
     */
    public int getEnd();
}
