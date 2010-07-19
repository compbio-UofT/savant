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
 * GenericPointTrack.java
 * Created on Jan 7, 2010
 */

package savant.model.data.point;

import java.io.Console;
import java.util.Set;
import savant.format.SavantFile;
import savant.model.GenericPointRecord;
import savant.model.Point;
import savant.model.PointRecord;
import savant.model.Resolution;
import savant.model.data.RecordTrack;
import savant.util.RAFUtils;
import savant.util.Range;
import savant.view.swing.Savant;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO:
 * 
 * @author mfiume, vwilliams
 */
// TODO: Change logging from Savant.log
    
public class GenericPointTrack implements RecordTrack<GenericPointRecord> {

    private SavantFile savantFile;

    private int numRecords;
    private int recordSize;

    /** FILE SPECIFIC VALUES */
    int descriptionLength;

    public GenericPointTrack(String filename) throws IOException {
        this.savantFile = new SavantFile(filename);
        setRecordSize();
        //setNumRecords();
    }

    private GenericPointRecord convertRecordToGenericPointRecord(List<Object> record) {
        return new GenericPointRecord(new Point((String) record.get(0), (Integer) record.get(1)), (String) record.get(2));
    }

    public List<GenericPointRecord> getRecords(String reference, Range range, Resolution resolution) {
        
        List<GenericPointRecord> data = new ArrayList<GenericPointRecord>();

        if (!this.savantFile.containsDataForReference(reference)) { return data; }

        try {
            //System.out.println("\nSeeking to start");
            int indexOfStart = seekToStart(reference, range.getFrom(), 0, getNumRecords(reference), savantFile);


            while (true) {
                savantFile.seek(reference, (indexOfStart++) * getRecordSize());
                List<Object> record = RAFUtils.readBinaryRecord(savantFile, savantFile.getFields());
                GenericPointRecord p = convertRecordToGenericPointRecord(record);
                Point pnt = ((PointRecord) p).getPoint();
                if (pnt.getPosition() > range.getTo() || !pnt.getReference().equals(reference)) {
                    break;
                }
                
                data.add(p);
            }

        }
        catch (EOFException ignore) {}
        catch (IOException ex) {
            Savant.log("Warning: IO Exception when getting point data");
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

    private int getRecordSize() { return this.recordSize; }

    private void setRecordSize() throws IOException {
        this.recordSize = RAFUtils.getRecordSize(savantFile);
    }

    private int getNumRecords(String reference) {
        if (!this.savantFile.containsDataForReference(reference)) { return -1; }
        return (int) (this.savantFile.getReferenceLength(reference) / getRecordSize());
        //return this.numRecords;
    }

    /*
    private void setNumRecords() {
        try {
            int numbytes = (int) savantFile.length();
            this.numRecords = numbytes / getRecordSize();
        } catch (IOException ex) {
            Savant.log("Error: setting number of records in point track");
        }
    }
     */

    private int seekToStart(String reference, int pos, int low, int high, SavantFile raf) throws IOException {

        int mid = low + ((high - low) / 2);

        if (high < low) {
            //System.out.println("Position: " + low);
            return low;
        }

        int posAtMid = getStartPosOfRecord(reference, mid, raf);

        //System.out.println("Low " + low + "\tMid " + mid + "(" + posAtMid + ")\tHigh " + high + "\tPos " + pos);

        
        if (posAtMid > pos) {
            return seekToStart(reference, pos, low, mid - 1, raf);
        } else if (posAtMid < pos) {
            return seekToStart(reference, pos, mid + 1, high, raf);
        } else {
            return low;
        }

    }

    private int getStartPosOfRecord(String reference, int record_num, SavantFile br) throws IOException {
        br.seek(reference, record_num * getRecordSize());
        List<Object> line = RAFUtils.readBinaryRecord(savantFile, savantFile.getFields());
        br.seek(reference, record_num * getRecordSize());
        return (Integer) line.get(1);
    }

    @Override
    public Set<String> getReferenceNames() {
        return this.savantFile.getReferenceNames();
    }

}
