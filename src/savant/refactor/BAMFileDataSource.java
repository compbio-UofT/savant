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
 * BAMFileDataSource.java
 * Created on Jun 15, 2010
 */

package savant.refactor;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.util.CloseableIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.model.BAMIntervalRecord;
import savant.model.Resolution;
import savant.util.Range;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to represent a file of BAM intervals. Uses SAMTools to read data within a range.
 *
 * @author vwilliams
 */

public class BAMFileDataSource extends FileDataSource<BAMIntervalRecord> {

    private static Log log = LogFactory.getLog(BAMFileDataSource.class);

    private File path;
    private File index;
    private SAMFileReader samFileReader;

    public BAMFileDataSource(File path, File index) {

        if (path == null) throw new IllegalArgumentException("File must not be null.");
        if (index == null) throw new IllegalArgumentException("Index file must not be null");

        this.path = path;
        this.index = index;

        samFileReader = new SAMFileReader(path, index);
        samFileReader.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    }

    /**
     * {@inheritDoc}
     */
    public List<BAMIntervalRecord> getRange(RefSeq ref, Range range, Resolution resolution) throws OutOfMemoryError {

        CloseableIterator<SAMRecord> recordIterator=null;
        List<BAMIntervalRecord> result = new ArrayList<BAMIntervalRecord>();
        try {
            recordIterator = samFileReader.query(ref.getName(), range.getFrom(), range.getTo(), false);
            SAMRecord samRecord;
            BAMIntervalRecord bamRecord;
            while (recordIterator.hasNext()) {
                samRecord = recordIterator.next();
                // don't keep unmapped reads
                if (samRecord.getReadUnmappedFlag()) continue;
                bamRecord = new BAMIntervalRecord(samRecord);

                // find out the type of the pair
                if (samRecord.getReadPairedFlag() && !samRecord.getMateUnmappedFlag()) {
                    BAMIntervalRecord.PairType type = getPairType(samRecord.getAlignmentStart(), samRecord.getMateAlignmentStart(), samRecord.getReadNegativeStrandFlag(), samRecord.getMateNegativeStrandFlag());
                    bamRecord.setType(type);
                }

                result.add(bamRecord);
            }

        } finally {
            if (recordIterator != null) recordIterator.close();
        }

        return result;
    }

    public void close() {
        if (samFileReader!=null) samFileReader.close();
    }

    public File getPath() {
        return path;
    }

    public File getIndex() {
        return index;
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

}
