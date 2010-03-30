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

import java.io.File;
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

    private float mean;
    private float stdDeviation;

    public BAMIntervalTrack(File path, File index) {
        setPath(path, index);
        guessSequence();
        calculateStatistics();
    }

    /**
     * {@inheritDoc}
     */
    public List<BAMIntervalRecord> getRecords(Range range, Resolution resolution) {

        CloseableIterator<SAMRecord> recordIterator=null;
        List<BAMIntervalRecord> result = new ArrayList<BAMIntervalRecord>();
        List<BAMIntervalRecord> invertedMateOrEverted = new ArrayList<BAMIntervalRecord>();
        try {
            recordIterator = samFileReader.query(sequenceName, range.getFrom(), range.getTo(), false);
            SAMRecord samRecord;
            BAMIntervalRecord bamRecord;
            while (recordIterator.hasNext()) {
                samRecord = recordIterator.next();
                // don't keep unmapped reads or their mates
                if (samRecord.getReadUnmappedFlag() || !samRecord.getReadPairedFlag() || samRecord.getMateUnmappedFlag()) continue;
                bamRecord = new BAMIntervalRecord(samRecord);

                // find out the type of the pair
                BAMIntervalRecord.PairType type = getPairType(samRecord.getAlignmentStart(), samRecord.getMateAlignmentStart(), samRecord.getReadNegativeStrandFlag(), samRecord.getMateNegativeStrandFlag());
                bamRecord.setType(type);

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

    /**
     * Get mean of estimated insert sizes.
     *
     * @return mean of insert sizes
     */
    public float getMean() {
        return mean;
    }

    /**
     * Get standard deviation of estimated insert sizes.
     *
     * @return std dev of insert sizes
     */
    public float getStdDeviation() {
        return stdDeviation;
    }

    private void setPath(File path, File index) {

        if (path == null) throw new IllegalArgumentException("File must not be null.");
        if (index == null) throw new IllegalArgumentException("Index file must not be null");
        this.path = path;
        this.index = index;
        samFileReader = new SAMFileReader(path, index);

    }

    /*
     * Use the length of the reference genome to guess which sequence from the dictionary
     * we should search for reads.
     */
    private void guessSequence() {

        // Find out what sequence we're using, by reading the header for sequences and lengths
        RangeController rangeController = RangeController.getInstance();
        int referenceSequenceLength = rangeController.getMaxRangeEnd() - rangeController.getMaxRangeStart();

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
            this.sequenceName = sequenceDictionary.getSequence(closestSequenceIndex).getSequenceName();
        }

    }

    /*
     * Calculate the mean and standard deviation of normal reads.
     */
    private void calculateStatistics() {
        CloseableIterator<SAMRecord> recordIterator=null;
        try {
            recordIterator = samFileReader.query(sequenceName, 2000, 4000, false);
            SAMRecord samRecord;

            int n = 0;
            float mean = 0.0f;
            float m2 = 0.0f;
            
            while (recordIterator.hasNext()) {

                // get next record
                samRecord = recordIterator.next();

                // throw out unmapped and unpaired reads
                if (samRecord.getReadUnmappedFlag() || !samRecord.getReadPairedFlag() || samRecord.getMateUnmappedFlag()) continue;

                // only use the left-most of the pair
                if (samRecord.getAlignmentStart() > samRecord.getMateAlignmentStart() ) continue;

                // only use NORMAL pair types
                BAMIntervalRecord.PairType pairType = getPairType(samRecord.getAlignmentStart(), samRecord.getMateAlignmentStart(),
                        samRecord.getReadNegativeStrandFlag(), samRecord.getMateNegativeStrandFlag());
                if (pairType != BAMIntervalRecord.PairType.NORMAL) continue; // don't want crazy numbers blowing our statistics

                n++;
                int x = inferInsertSize(samRecord, pairType);
                if (x > 2000) Savant.log("insert size > 2000 in statistics calculation");
                float delta = x - mean;
                mean = mean + delta/n;
                m2 = m2 + delta*(x - mean);
            }
            float variance = m2/(n-1);
            this.mean = mean;
            this.stdDeviation = (float)Math.sqrt((double) variance);
//            Savant.log("Mean and Standard Deviation are: " + this.mean + " , " + this.stdDeviation);
        } finally {
            if (recordIterator != null) recordIterator.close();
        }
    }

    /**
     * Calculate approximate insert size for a given mated pair of reads. Some assumptions are
     * made about the equivalence of read and mate lengths, since there is no other reasonable
     * heuristic that works across sequencing technologies.
     *
     * @param samRecord a SAMRecord returned by SAMTools
     * @param pairType the pair type: NORMAL, INVERTED_READ, INVERTED_MATE, EVERTED
     * @return an approximate insert size
     * @throws IllegalStateException if the read is unmapped, unpaired, or its mate is unmapped.
     */
    public static int inferInsertSize(SAMRecord samRecord, BAMIntervalRecord.PairType pairType) throws IllegalStateException {

        if (samRecord.getReadUnmappedFlag() || !samRecord.getReadPairedFlag() || samRecord.getMateUnmappedFlag())
            throw new IllegalStateException("Read is either not mapped, not paired, or its mate is unmapped; cannot calculate insert size.");

        int startPos = 0;
        int endPos = 0;

        int alignmentStart = samRecord.getAlignmentStart();
        int alignmentEnd = samRecord.getAlignmentEnd();
        int mateAlignmentStart = samRecord.getMateAlignmentStart();
        // this is an approximation
        int mateAlignmentEnd = samRecord.getMateAlignmentStart() + samRecord.getReadLength();



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
