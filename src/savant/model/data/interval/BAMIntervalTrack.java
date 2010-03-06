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

import savant.controller.RangeController;
import savant.model.BAMIntervalRecord;
import savant.model.Resolution;
import savant.model.data.RecordTrack;
import savant.util.Range;
import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    public BAMIntervalTrack(File path, File index) {
        setPath(path, index);
    }

    public List<BAMIntervalRecord> getRecords(Range range, Resolution resolution) {

        CloseableIterator<SAMRecord> recordIterator=null;
        List<BAMIntervalRecord> result = new ArrayList<BAMIntervalRecord>();
        try {
            recordIterator = samFileReader.query(sequenceName, range.getFrom(), range.getTo(), false);
            SAMRecord samRecord;
            BAMIntervalRecord bamRecord;
            while (recordIterator.hasNext()) {
                samRecord = recordIterator.next();
                bamRecord = new BAMIntervalRecord(samRecord);
                result.add(bamRecord);
            }
        } finally {
            if (recordIterator != null) recordIterator.close();
        }
        return result;
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path, File index) {

        if (path == null) throw new IllegalArgumentException("File must not be null.");
        if (index == null) throw new IllegalArgumentException("Index file must not be null");
        this.path = path;
        this.index = index;
        samFileReader = new SAMFileReader(path, index);

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

    public String getSequenceName() {
        return sequenceName;
    }

    public void close() {

        if (samFileReader != null) {
            samFileReader.close();
        }
    }
}
