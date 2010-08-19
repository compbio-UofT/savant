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
 * GenericIntervalTrack.java
 * Created on Jan 12, 2010
 */

package savant.model.data.interval;

import java.util.Set;
import savant.format.DataFormatter;
import savant.format.SavantFile;
import savant.format.SavantUnsupportedVersionException;
import savant.format.util.data.interval.IntervalRecordGetter;
import savant.format.util.data.interval.IntervalSearchTree;
import savant.model.GenericIntervalRecord;
import savant.model.IntervalRecord;
import savant.model.Resolution;
import savant.model.data.RecordTrack;
import savant.util.Range;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import savant.format.SavantFileFormatter;

/**
 * Class to represent an track of generic intervals. Responsible for reading records within a given range.
 * 
 * @author vwilliams
 */
//TODO: remove Savant.log
public class GenericIntervalTrack implements RecordTrack<GenericIntervalRecord> {

    // Track properties
    SavantFile dFile;
    //RandomAccessFile raf;

    private int numRecords;
    private int recordSize;

    private Map<String,IntervalSearchTree> refnameToIntervalBSTIndex;
    //private IntervalSearchTree intervalBSTIndex;

    public GenericIntervalTrack(String fileName) throws IOException, SavantUnsupportedVersionException {
        this.dFile = new SavantFile(fileName);
        String indexFileName = fileName + SavantFileFormatter.indexExtension;
        this.refnameToIntervalBSTIndex = DataFormatter.readIntervalBSTs(this.dFile);

        //SavantLogger.logln("HEADER SIZE && : "+  dFile.getFilePointerSuper());

        //System.out.println("Found indicies for:");
        //for (String refname : refnameToIntervalBSTIndex.keySet()) {
        //    System.out.println(refname);
        //}

        //intervalBSTIndex = DataFormatter.readIntervalBST(indexFileName);
        //this.numRecords = intervalBSTIndex.getRoot().subtreeSize;
    }

    public IntervalSearchTree getIntervalSearchTreeForReference(String refname) {
        return refnameToIntervalBSTIndex.get(refname);
    }

    public List<GenericIntervalRecord> getRecords(String reference, Range range, Resolution resolution) {
        List<IntervalRecord> data = null;

        IntervalSearchTree ist = getIntervalSearchTreeForReference(reference);

        if (ist == null) { return new ArrayList<GenericIntervalRecord>(); }
        
        try {
            data = IntervalRecordGetter.getData(this.dFile, reference, range, ist.getRoot());
        } catch (IOException ex) {
            Logger.getLogger(GenericIntervalTrack.class.getName()).log(Level.SEVERE, null, ex);
        }

        //TODO: fix me
        List<GenericIntervalRecord> girList = new ArrayList<GenericIntervalRecord>(data.size());
        for (int i = 0; i < data.size(); i++) {
            girList.add((GenericIntervalRecord) data.get(i));
        }

        return girList;
    }

    public void close() {
        try {
            this.dFile.close();
        } catch (IOException ex) {
            Logger.getLogger(GenericIntervalTrack.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Set<String> getReferenceNames() {
        return this.dFile.getReferenceNames();
    }
}
