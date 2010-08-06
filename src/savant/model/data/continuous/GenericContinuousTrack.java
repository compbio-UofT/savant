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
 * GenericContinuousTrack.java
 * Created on Jan 11, 2010
 */

package savant.model.data.continuous;

import java.util.Set;
import savant.format.SavantFile;
import savant.model.Continuous;
import savant.model.ContinuousRecord;
import savant.model.Resolution;
import savant.util.RAFUtils;
import savant.util.Range;
import savant.view.swing.Savant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import savant.format.ContinuousFormatterHelper;
import savant.format.ContinuousFormatterHelper.Level;

/**
 * A data track containing ContinuousRecords. Data is sampled differently depending on
 * data resolution.
 * 
 * @author vwilliams
 */
public class GenericContinuousTrack extends ContinuousTrack {

    private SavantFile savantFile;

    private int numRecords;
    private int recordSize;

    private Map<String,List<Level>> refnameToLevelsIndex;
    
    private Hashtable<Resolution, int[]> resolutionToSamplingMap;

    public GenericContinuousTrack(String filename) throws IOException {

        this.savantFile = new SavantFile(filename);
        this.refnameToLevelsIndex = ContinuousFormatterHelper.readLevelHeadersFromBinaryFile(savantFile);

        //printLevelsMap(refnameToLevelsIndex);

        setRecordSize();

        setResolutionToFrequencyMap();
    }

    @Override
    public List<ContinuousRecord> getRecords(String reference, Range range, Resolution resolution) {

        List<ContinuousRecord> data = new ArrayList<ContinuousRecord>();

        try {
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
                ContinuousRecord p = new ContinuousRecord(reference, pos, new Continuous( sum/j));

                data.add(p);
            }

        } catch (IOException ex) {
            Savant.log("Warning: IO Exception when getting continuous data");
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
        this.recordSize = RAFUtils.getRecordSize(savantFile);
        //System.out.println("Setting record size to " + this.recordSize);
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
        return this.savantFile.getReferenceNames();
    }

    private void printLevelsMap(Map<String, List<Level>> refnameToLevelsIndex) {
        for (String refname : refnameToLevelsIndex.keySet()) {
            System.out.println("Level header for reference " + refname);
            System.out.println("Levels list " + refnameToLevelsIndex.get(refname));
            System.out.println("Number of levels " + refnameToLevelsIndex.get(refname).size());
            for (Level l : refnameToLevelsIndex.get(refname)) {
                System.out.println("Offset: " + l.offset);
                System.out.println("Size: " + l.size);
                System.out.println("Record size: " + l.recordSize);
                System.out.println("Type: " + l.mode.type);
            }
        }
    }
}
