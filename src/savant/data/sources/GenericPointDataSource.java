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
 * GenericPointDataSource.java
 * Created on Jan 7, 2010
 */

package savant.data.sources;

import savant.data.types.GenericPointRecord;
import savant.data.types.Point;
import savant.file.FileType;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantROFile;
import savant.file.SavantUnsupportedVersionException;
import savant.util.Range;
import savant.util.Resolution;
import savant.util.SavantFileUtils;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data access object for accessing generic point files.
 * 
 * @author mfiume, vwilliams
 */
public class GenericPointDataSource implements DataSource<GenericPointRecord> {

    private SavantROFile savantFile;

    private int recordSize;

    public GenericPointDataSource(String filename) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this.savantFile = new SavantROFile(filename, FileType.POINT_GENERIC);
        this.recordSize = SavantFileUtils.getRecordSize(savantFile);
    }

    private GenericPointRecord convertRecordToGenericPointRecord(List<Object> record) {
        return GenericPointRecord.valueOf((String) record.get(0), Point.valueOf((Integer) record.get(1)), (String) record.get(2));
    }

    public List<GenericPointRecord> getRecords(String reference, Range range, Resolution resolution) throws IOException {
        
        List<GenericPointRecord> data = new ArrayList<GenericPointRecord>();

        if (!this.savantFile.containsDataForReference(reference)) { return data; }

        try {
            long indexOfStart = seekToStart(reference, range.getFrom(), 0, getNumRecords(reference), savantFile);

            while (true) {
                savantFile.seek(reference, (indexOfStart++) * getRecordSize());
                List<Object> record = SavantFileUtils.readBinaryRecord(savantFile, savantFile.getFields());
                GenericPointRecord p = convertRecordToGenericPointRecord(record);
                Point pnt = p.getPoint();

                // TODO: remove the necessity to trim ... this is a problem with the delimiter in formatting
                if (pnt.getPosition() > range.getTo() || !p.getReference().trim().equals(reference)) {
                    break;
                }
                
                data.add(p);
            }

        }
        catch (EOFException ignore) {}

        // return result
        return data;
    }

    public void close() {
        try {
            if (savantFile != null) savantFile.close();
        } catch (IOException ignore) { }
    }

    private int getRecordSize() { return this.recordSize; }


    private int getNumRecords(String reference) {
        if (!this.savantFile.containsDataForReference(reference)) { return -1; }
        return (int) (this.savantFile.getReferenceLength(reference) / getRecordSize());
    }

    private long seekToStart(String reference, long pos, long low, long high, SavantROFile rof) throws IOException {

        long mid = low + ((high - low) / 2);

        if (high < low) {
            return low;
        }

        int posAtMid = getStartPosOfRecord(reference, mid, rof);
        
        if (posAtMid > pos) {
            return seekToStart(reference, pos, low, mid - 1, rof);
        } else if (posAtMid < pos) {
            return seekToStart(reference, pos, mid + 1, high, rof);
        } else {
            return low;
        }

    }

    private int getStartPosOfRecord(String reference, long record_num, SavantROFile br) throws IOException {
        br.seek(reference, record_num * getRecordSize());
        List<Object> line = SavantFileUtils.readBinaryRecord(savantFile, savantFile.getFields());
        br.seek(reference, record_num * getRecordSize());
        return (Integer) line.get(1);
    }

    public Set<String> getReferenceNames() {
        Map<String, Long[]> refMap = savantFile.getReferenceMap();
        return refMap.keySet();

    }
    
    public String getPath() {
        return savantFile.getPath();
    }
}
