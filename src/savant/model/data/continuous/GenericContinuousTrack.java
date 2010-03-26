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

import savant.format.SavantFile;
import savant.model.Continuous;
import savant.model.ContinuousRecord;
import savant.model.Resolution;
import savant.model.data.point.PointTrack;
import savant.util.RAFUtils;
import savant.util.Range;
import savant.view.swing.Savant;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    
    private Hashtable<Resolution, int[]> resolutionToSamplingMap;

    public GenericContinuousTrack(String filename) throws FileNotFoundException, IOException {

        this.savantFile = new SavantFile(filename);

        setRecordSize();

        setNumRecords();

        setResolutionToFrequencyMap();
    }

    @Override
    public List<ContinuousRecord> getRecords(Range range, Resolution resolution) {

        List<ContinuousRecord> data = new ArrayList<ContinuousRecord>();

        try {
            int binSize = getSamplingFrequency(resolution);
            int contiguousSamples = getNumContinuousSamples(resolution);

            int index = range.getFrom();
            int lastIndex = range.getTo() + binSize;
            for  (int i = index; i <= lastIndex; i += binSize) {

                savantFile.seek((i-1)*recordSize);

                double sum = 0.0;
                int j;
                try
                {
                    for (j = 0; j < contiguousSamples; j++) {
                        sum += savantFile.readDouble();
                    }
                } catch (Exception e) { break; }
                int pos;
                if (contiguousSamples > 1) pos = i+contiguousSamples/2;
                else pos = i;
                ContinuousRecord p = new ContinuousRecord(i+contiguousSamples/2, new Continuous( sum/j));
                data.add(p);
            }

        } catch (IOException ex) {
            Savant.log("Warning: IO Exception when getting continuous data");
            Logger.getLogger(PointTrack.class.getName()).log(Level.SEVERE, null, ex);
        }

        // return result
        return data;
    }

    public void close() {
        try {
            if (savantFile != null) savantFile.close();
        } catch (IOException ignore) { }
    }

    public int getNumRecords() {
        return numRecords;
    }

    // TODO: make this a helper method, since it's used in all tracks. Pass File and recordSize. Get rid of Savant.log.
    public void setNumRecords() {
        try {
            int numbytes = (int) savantFile.length();
            this.numRecords = numbytes / getRecordSize();
        } catch (IOException ex) {
            Savant.log("Error: setting number of records in point track");
        }

    }

    public int getRecordSize() {
        return recordSize;
    }

    public void setRecordSize() throws IOException {
        this.recordSize = RAFUtils.getRecordSize(savantFile);
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

}
