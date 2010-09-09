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
 * GenericContinuousDataSource.java
 * Created on Jan 11, 2010
 */

package savant.data.sources;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.data.types.Continuous;
import savant.data.types.GenericContinuousRecord;
import savant.file.*;
import savant.format.ContinuousFormatterHelper;
import savant.format.ContinuousFormatterHelper.Level;
import savant.util.Range;
import savant.util.Resolution;
import savant.util.SavantFileUtils;
import savant.view.swing.Savant;

import java.io.IOException;
import java.util.*;

/**
 * A data track containing ContinuousRecords. Data is sampled differently depending on
 * data resolution.
 * 
 * @author vwilliams
 */
public class GenericContinuousDataSource implements DataSource<GenericContinuousRecord> {

    private static Log log = LogFactory.getLog(GenericContinuousDataSource.class);

    private SavantROFile savantFile;

    private int numRecords;
    private int recordSize;

    private Map<String,List<Level>> refnameToLevelsIndex;
    
    private Hashtable<Resolution, int[]> resolutionToSamplingMap;

    public GenericContinuousDataSource(String filename) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {

        this.savantFile = new SavantROFile(filename, FileType.CONTINUOUS_GENERIC);
        this.refnameToLevelsIndex = ContinuousFormatterHelper.readLevelHeadersFromBinaryFile(savantFile);

        printLevelsMap(refnameToLevelsIndex);

        setRecordSize();

        setResolutionToFrequencyMap();
    }

    public List<GenericContinuousRecord> getRecords(String reference, Range range, Resolution resolution) throws IOException {

        List<GenericContinuousRecord> data = new ArrayList<GenericContinuousRecord>();


//            int binSize = getSamplingFrequency(resolution);
//            int contiguousSamples = getNumContinuousSamples(resolution);
        int binSize = getSamplingFrequency(range);
        int contiguousSamples = getNumContinuousSamples(range);

        // int index = range.getFrom();
        int index = range.getFrom() - binSize; // to avoid missing a value at the start of the range, go back one bin
        int lastIndex = range.getTo() + binSize;
        for  (int i = index; i <= lastIndex; i += binSize) {

            long seekpos = (i-1)*recordSize;
            if (seekpos < 0) continue; // going back one bin may not be possible if we're near the start
            long bytepos = savantFile.seek(reference, (i-1)*recordSize);

            float sum = 0.0f;
            int j;
            try
            {
                for (j = 0; j < contiguousSamples; j++) {
                    sum += savantFile.readFloat();
                }
            } catch (Exception e) { break; }
            int pos;
            if (contiguousSamples > 1) pos = i+contiguousSamples/2;
            else pos = i;
            GenericContinuousRecord p = GenericContinuousRecord.valueOf(reference, pos, Continuous.valueOf( sum/j));

            data.add(p);
        }

        return data;
    }

    public void close() {
        try {
            if (savantFile != null) savantFile.close();
        } catch (IOException ignore) { }
    }

    private int getNumRecords(String reference) {
        if (!this.savantFile.containsDataForReference(reference)) { return -1; }
        return (int) (this.savantFile.getReferenceLength(reference) / getRecordSize());
        //return this.numRecords;
    }

    public int getRecordSize() {
        return recordSize;
    }

    public void setRecordSize() throws IOException {
        this.recordSize = SavantFileUtils.getRecordSize(savantFile);
        if (log.isDebugEnabled()) log.debug("Setting record size to " + this.recordSize);
    }

    private void setResolutionToFrequencyMap()
    {
        resolutionToSamplingMap = new Hashtable<Resolution, int[]>();
        resolutionToSamplingMap.put(Resolution.VERY_LOW, new int[] {   1000000,    1000 });
        resolutionToSamplingMap.put(Resolution.LOW, new int[] {        50000,      1000 });
        resolutionToSamplingMap.put(Resolution.MEDIUM, new int[] {     5000,       2500 });
        resolutionToSamplingMap.put(Resolution.HIGH, new int[] {       100,        100 });
        resolutionToSamplingMap.put(Resolution.VERY_HIGH, new int[] {  1,          1 });
    }

    private int getSamplingFrequency(Resolution r)
    {
        int[] ar = resolutionToSamplingMap.get(r);
        return ar[0];
    }

    private int getNumContinuousSamples(Resolution r)
    {
        int[] ar = resolutionToSamplingMap.get(r);
        return ar[1];
    }

    // FIXME: this is a nasty kludge to accommodate BAM coverage tracks
    private int getSamplingFrequency(Range r)
    {
        int length = r.getLength();
        if (length < 10000) { return 1; }
        else if (length < 50000) { return 100; }
        else if (length < 1000000) { return 5000; }
        else if (length < 10000000) { return 50000; }
        else if (length >= 10000000) { return 1000000; }
        else { return 1; }
    }

    private int getNumContinuousSamples(Range r)
    {
        int length = r.getLength();
        if (length < 10000) { return 1; }
        else if (length < 50000) { return 100; }
        else if (length < 1000000) { return 2500; }
        else if (length < 10000000) { return 1000; }
        else if (length >= 10000000) { return 1000; }
        else { return 1; }
    }

    public Set<String> getReferenceNames() {
        Map<String, Long[]> refMap = savantFile.getReferenceMap();
        return refMap.keySet();
    }

    private void printLevelsMap(Map<String, List<Level>> refnameToLevelsIndex) {
        if (log.isDebugEnabled()) {
            for (String refname : refnameToLevelsIndex.keySet()) {
                log.debug("Level header for reference " + refname);
                log.debug("Levels list " + refnameToLevelsIndex.get(refname));
                log.debug("Number of levels " + refnameToLevelsIndex.get(refname).size());
                for (Level l : refnameToLevelsIndex.get(refname)) {
                    log.debug("Offset: " + l.offset);
                    log.debug("Size: " + l.size);
                    log.debug("Record size: " + l.recordSize);
                    log.debug("Type: " + l.mode.type);
                }
            }
        }
    }

    public String getPath() {
        return savantFile.getPath();
    }
}
