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

    public GenericPointTrack(String filename) throws FileNotFoundException, IOException {

        this.savantFile = new SavantFile(filename);
        setRecordSize();
        setNumRecords();
    }

    private GenericPointRecord convertRecordToGenericPointRecord(List<Object> record) {
        return new GenericPointRecord(new Point((Integer) record.get(0)), (String) record.get(1));
    }

    public List<GenericPointRecord> getRecords(Range range, Resolution resolution) {
        
        List<GenericPointRecord> data = new ArrayList<GenericPointRecord>();

        try {
            int indexOfStart = seekToStart(range.getFrom(), 0, getNumRecords(), savantFile);
            
            while (true) {
                savantFile.seek((indexOfStart++) * getRecordSize());
                List<Object> record = RAFUtils.readBinaryRecord(savantFile, savantFile.getFields());
                GenericPointRecord p = convertRecordToGenericPointRecord(record);

                if (((PointRecord) p).getPoint().getPosition() > range.getTo()) {
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

    private int getNumRecords() { return this.numRecords; }

    private void setNumRecords() {
        try {
            int numbytes = (int) savantFile.length();
            this.numRecords = numbytes / getRecordSize();
        } catch (IOException ex) {
            Savant.log("Error: setting number of records in point track");
        }
    }

    private int seekToStart(int pos, int low, int high, SavantFile raf) throws IOException {

        // System.out.println(pos + " " + low + " " + high + " ");

        if (high < low) {
            return low;
        }

        int mid = low + ((high - low) / 2);

        int posAtMid = getStartPosOfRecord(mid, raf);

        //Console.WriteLine("Low {0}\tMid {2} ({3})\tHigh {1}\tPos {4}", low, high, mid, posAtMid, pos);

        if (posAtMid > pos) {
            return seekToStart(pos, low, mid - 1, raf);
        } else if (posAtMid < pos) {
            return seekToStart(pos, mid + 1, high, raf);
        } else {
            return low;
        }

    }

    private int getStartPosOfRecord(int record_num, SavantFile br) throws IOException {
        br.seek(record_num * getRecordSize());
        int start = br.readInt();
        br.seek(record_num * getRecordSize());
        return start;
    }

}
