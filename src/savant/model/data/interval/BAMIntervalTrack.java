/*
 *    Copyright 2010 University of Toronto
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

/*
 * BAMIntervalTrack.java
 * Created on Jan 28, 2010
 */

package savant.model.data.interval;

import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.RangeController;
import savant.model.BAMIntervalRecord;
import savant.model.Resolution;
import savant.model.data.RecordTrack;
import savant.util.Range;
import savant.view.swing.Savant;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to represent a track of BAM intervals. Uses SAMTools to read data within a range.
 * 
 * @author vwilliams
 */
public class BAMIntervalTrack implements RecordTrack<BAMIntervalRecord> {

    private static Log log = LogFactory.getLog(BAMIntervalTrack.class);
    
    private File path;
    private File index;
    private SAMFileReader samFileReader;
    private String sequenceName;


    public BAMIntervalTrack(File path, File index) {
        setPath(path, index);
        this.sequenceName = guessSequence(path, index);
        samFileReader = new SAMFileReader(path, index);
        samFileReader.setValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
    }

    /**
     * {@inheritDoc}
     */
    public List<BAMIntervalRecord> getRecords(Range range, Resolution resolution) throws OutOfMemoryError {

        CloseableIterator<SAMRecord> recordIterator=null;
        List<BAMIntervalRecord> result = new ArrayList<BAMIntervalRecord>();
        try {
            recordIterator = samFileReader.query(sequenceName, range.getFrom(), range.getTo(), false);
            SAMRecord samRecord;
            BAMIntervalRecord bamRecord;
            while (recordIterator.hasNext()) {
                samRecord = recordIterator.next();
                // don't keep unmapped reads or their mates
//                if (samRecord.getReadUnmappedFlag() || !samRecord.getReadPairedFlag() || samRecord.getMateUnmappedFlag()) continue;
                if (samRecord.getReadUnmappedFlag()) continue;
                bamRecord = new BAMIntervalRecord(samRecord);

                // find out the type of the pair
                if (samRecord.getReadPairedFlag() && !samRecord.getMateUnmappedFlag()) {
                    BAMIntervalRecord.PairType type = getPairType(samRecord.getAlignmentStart(), samRecord.getMateAlignmentStart(), samRecord.getReadNegativeStrandFlag(), samRecord.getMateNegativeStrandFlag());
                    bamRecord.setType(type);
                    // pre-calculate the insert size in case it's needed
                    bamRecord.setInsertSize(inferInsertSize(samRecord, type));
                }

                result.add(bamRecord);
            }

        } finally {
            if (recordIterator != null) recordIterator.close();
        }

        return result;
    }

    /**
     * Get the path file of this data track
     *
     * @return path
     */
    public File getPath() {
        return path;
    }

    private void setPath(File path, File index) {

        if (path == null) throw new IllegalArgumentException("File must not be null.");
        if (index == null) throw new IllegalArgumentException("Index file must not be null");
        this.path = path;
        this.index = index;

    }

    /*
     * Use the length of the reference genome to guess which sequence from the dictionary
     * we should search for reads.
     */
    public static String guessSequence(File path, File index) {

        // Find out what sequence we're using, by reading the header for sequences and lengths
        RangeController rangeController = RangeController.getInstance();
        int referenceSequenceLength = rangeController.getMaxRangeEnd() - rangeController.getMaxRangeStart();

        String sequenceName = null;
        SAMFileReader samFileReader = new SAMFileReader(path, index);
        SAMFileHeader fileHeader = samFileReader.getFileHeader();
        SAMSequenceDictionary sequenceDictionary = fileHeader.getSequenceDictionary();
        // find the first sequence with the smallest difference in length from our reference sequence
        int leastDifferenceInSequenceLength = Integer.MAX_VALUE;
        int closestSequenceIndex = Integer.MAX_VALUE;
        int i = 0;
        for (SAMSequenceRecord sequenceRecord : sequenceDictionary.getSequences()) {
            int lengthDelta = Math.abs(sequenceRecord.getSequenceLength() - referenceSequenceLength);
            if (lengthDelta < leastDifferenceInSequenceLength) {
                leastDifferenceInSequenceLength = lengthDelta;
                closestSequenceIndex = i;
            }
            i++;
        }
        if (closestSequenceIndex != Integer.MAX_VALUE) {
            sequenceName = sequenceDictionary.getSequence(closestSequenceIndex).getSequenceName();
        }
        samFileReader.close();
        return sequenceName;
    }


    /**
     * <p>Calculate approximate insert size for a given mated pair of reads. Some assumptions are
     * made about the equivalence of read and mate lengths, since there is no other reasonable
     * heuristic that works across sequencing technologies.<p>
     * <p>Also note that here the insert size goes from the 3' end of the read to the 3' end of the mate.
     * It does not include both read lengths in the normal pair type case. Only for everted reads (both
     * pointing in the "wrong" direction, will the read lengths be included in the returned size.</p>
     *
     * @param samRecord a SAMRecord returned by SAMTools
     * @param pairType the pair type: NORMAL, INVERTED_READ, INVERTED_MATE, EVERTED
     * @return an approximate insert size
     * @throws IllegalStateException if the read is unmapped, unpaired, or its mate is unmapped.
     */
    public static int inferInsertSize(SAMRecord samRecord, BAMIntervalRecord.PairType pairType) throws IllegalStateException {

        if (samRecord.getReadUnmappedFlag() || !samRecord.getReadPairedFlag() || samRecord.getMateUnmappedFlag()) {
            throw new IllegalStateException("Read is either not mapped, not paired, or its mate is unmapped; cannot calculate insert size.");
        }

        int startPos = 0;
        int endPos = 0;

        // deal with which read comes first, the read or the mate,
        // and set alignmentStart/End to the 1st and mateAlignmentStart/End to the 2nd
        int alignmentStart;
        int alignmentEnd;
        int mateAlignmentStart;
        int mateAlignmentEnd;
        if (samRecord.getAlignmentStart() < samRecord.getMateAlignmentStart()) {
            alignmentStart = samRecord.getAlignmentStart();
            alignmentEnd = samRecord.getAlignmentEnd();
            mateAlignmentStart = samRecord.getMateAlignmentStart();
            // this is an approximation
            mateAlignmentEnd = samRecord.getMateAlignmentStart() + samRecord.getReadLength();
        }
        else {
            alignmentStart = samRecord.getMateAlignmentStart();
            // this is an approximation because mate length is not known
            alignmentEnd = samRecord.getMateAlignmentStart() + samRecord.getReadLength();
            mateAlignmentStart = samRecord.getAlignmentStart();
            mateAlignmentEnd = samRecord.getAlignmentEnd();
        }

        // now use the pair type to determine start and end position of the arc to be drawn
        // as the insert
        switch (pairType) {
            case NORMAL:
                startPos = alignmentEnd;
                endPos = mateAlignmentStart;
                break;
            case INVERTED_READ:
                startPos = alignmentStart;
                endPos = mateAlignmentStart;
                break;
            case INVERTED_MATE:
                startPos = alignmentEnd;
                endPos = mateAlignmentEnd;
                break;
            case EVERTED:
                startPos = alignmentStart;
                endPos = mateAlignmentEnd;
                break;
        }
        // return the length of the arc.
        return endPos - startPos + 1;
    }

    /*
     * Determine the pair type: NORMAL, INVERTED_READ, INVERTED_MATE, EVERTED pair.
     */
    private BAMIntervalRecord.PairType getPairType(int readStart, int mateStart, boolean readNegative, boolean mateNegative) {

        BAMIntervalRecord.PairType type=null;
        boolean readNegativeStrand, mateNegativeStrand;

        // by switching the negative strand flags based on which read comes first, we can reduce 8 cases to 4
        if (readStart < mateStart) {
            readNegativeStrand = readNegative;
            mateNegativeStrand = mateNegative;
        }
        else {
            readNegativeStrand = mateNegative;
            mateNegativeStrand = readNegative;
        }

        // now is the first read pointing forward & mate pointing backward?
        if (!readNegativeStrand && mateNegativeStrand) {
            // congratulations, it's a normal pair!
            type = BAMIntervalRecord.PairType.NORMAL;
        }
        // or are both reversed?
        else if (readNegativeStrand && mateNegativeStrand) {
            // this is a case of the read being inverted
            type = BAMIntervalRecord.PairType.INVERTED_READ;
        }
        // or are both forward?
        else if (!readNegativeStrand && !mateNegativeStrand) {
            // this is a case of the mate being inverted
            type = BAMIntervalRecord.PairType.INVERTED_MATE;
        }
        // are the strands pointing away from each other?
        else if (readNegativeStrand && !mateNegativeStrand) {
            // the pair is everted
            type = BAMIntervalRecord.PairType.EVERTED;
        }
        return type;
    }

    /**
     * Get the name of the sequence we are querying.
     *
     * @return sequence name
     */
    public String getSequenceName() {
        return sequenceName;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {

        if (samFileReader != null) {
            samFileReader.close();
        }
    }
}
