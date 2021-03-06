/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.data.sources;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.data.DataFormat;
import savant.api.util.Resolution;
import savant.data.types.GenericPointRecord;
import savant.file.*;
import savant.util.SavantFileUtils;

/**
 * Data access object for accessing generic point files.
 *
 * @author mfiume, vwilliams
 */
public class GenericPointDataSource extends DataSource<GenericPointRecord> {

    private SavantROFile savantFile;

    private int recordSize;

    public GenericPointDataSource(URI uri) throws IOException, SavantFileNotFormattedException {
        this.savantFile = new SavantROFile(uri, FileType.POINT_GENERIC);
        this.recordSize = SavantFileUtils.getRecordSize(savantFile);
    }

    private GenericPointRecord convertRecordToGenericPointRecord(List<Object> record) {
        return GenericPointRecord.valueOf((String) record.get(0), (Integer)record.get(1), (String)record.get(2));
    }

    @Override
    public List<GenericPointRecord> getRecords(String reference, RangeAdapter range, Resolution resolution, RecordFilterAdapter filt) throws IOException {

        List<GenericPointRecord> data = new ArrayList<GenericPointRecord>();

        if (!this.savantFile.containsDataForReference(reference)) { return data; }

        try {
            long indexOfStart = seekToStart(reference, range.getFrom(), 0, getNumRecords(reference), savantFile);

            while (true) {
                savantFile.seek(reference, (indexOfStart++) * getRecordSize());
                List<Object> record = SavantFileUtils.readBinaryRecord(savantFile, savantFile.getFields());
                GenericPointRecord p = convertRecordToGenericPointRecord(record);
                int pnt = p.getPosition();

                // TODO: remove the necessity to trim ... this is a problem with the delimiter in formatting
                if (pnt > range.getTo() || !p.getReference().trim().equals(reference)) {
                    break;
                }

                data.add(p);
            }

        }
        catch (EOFException ignore) {}

        // return result
        return data;
    }

    @Override
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

    @Override
    public Set<String> getReferenceNames() {
        return savantFile.getReferenceMap().keySet();

    }

    @Override
    public URI getURI() {
        return savantFile.getURI();
    }

    @Override
    public final DataFormat getDataFormat() {
        return DataFormat.POINT;
    }

    @Override
    public final String[] getColumnNames() {
        return new String[] { "Reference", "Position", "Description" };
    }
}
