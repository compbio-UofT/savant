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
package savant.sql;

import java.io.IOException;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.util.SeekableStream;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.data.DataFormat;
import savant.api.util.Resolution;
import savant.data.types.GenericContinuousRecord;
import savant.util.NetworkUtils;


/**
 * DataSource class which retrieves continuous data from an SQL database.  The actual
 * data values are stored in an external Wib (binned Wig) file.  This is very idiosyncratic
 * to UCSC, so the class might more properly belong in the savant.ucsc plugin.
 *
 * @author tarkvara
 */
public class WigSQLDataSource extends SQLDataSource<GenericContinuousRecord> {
    private final String ucscDownloadURL;

    WigSQLDataSource(MappedTable table, List<String> references, String ucsc) throws SQLException {
        super(table, references);
        ucscDownloadURL = ucsc;
    }

    @Override
    public List<GenericContinuousRecord> getRecords(String reference, RangeAdapter range, Resolution resolution, RecordFilterAdapter filt) throws IOException {
        if (resolution != Resolution.HIGH) {
            throw new IOException("Zoom in to see data");
        }
        // TODO: This is wrong.  We should be stuffing in NaNs, not zeroes.
        List<GenericContinuousRecord> result = new ArrayList<GenericContinuousRecord>();
        try {
            int nextPos = range.getFrom();
            ResultSet rs = executeQuery(reference, range.getFrom(), range.getTo());
            URI wibURI = null;
            SeekableStream wibStream = null;
            while (rs.next()) {
                int start = rs.getInt(columns.start) + 1;
                if (nextPos < start) {
                    result.add(GenericContinuousRecord.valueOf(reference, nextPos, Float.NaN));
                    nextPos = start;
                }

                int span = columns.span != null ? rs.getInt(columns.span) : 1;
                int count = rs.getInt(columns.count);
                int offset = rs.getInt(columns.offset);
                String file = rs.getString(columns.file);
                float lowerLimit = rs.getFloat(columns.lowerLimit);
                float dataRange = rs.getFloat(columns.dataRange);

                // If the URI has changed, open a new stream.
                URI newWibURI = URI.create(ucscDownloadURL + file);
                if (!newWibURI.equals(wibURI)) {
                    if (wibStream != null) {
                        wibStream.close();
                    }
                    wibURI = newWibURI;
                    wibStream = NetworkUtils.getSeekableStreamForURI(wibURI);
                }

                wibStream.seek(offset);
                byte[] buf = new byte[count];
                wibStream.read(buf);

                int p = start;
                for (int i = 0; i < count && nextPos < range.getTo(); i++) {
                    for (int j = 0; j < span; j++) {
                        if (p >= nextPos) {
                            float value = Float.NaN;
                            if (buf[i] >= 0) {
                                value = lowerLimit + dataRange * buf[i] / 127.0F;
                            }
                            GenericContinuousRecord rec = GenericContinuousRecord.valueOf(reference, nextPos++, value);
                            if (filt == null || filt.accept(rec)) {
                                result.add(rec);
                            }
                        }
                        p++;
                    }
                }
            }
            rs.close();
        } catch (SQLException sqlx) {
            LOG.error(sqlx);
            throw new IOException(sqlx);
        }
        return result;
    }


    @Override
    public DataFormat getDataFormat() {
        return DataFormat.CONTINUOUS;
    }

    @Override
    public String[] getColumnNames() {
        return GenericContinuousRecord.COLUMN_NAMES;
    }
}
