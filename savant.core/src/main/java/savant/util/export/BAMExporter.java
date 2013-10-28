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
package savant.util.export;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;

import savant.api.adapter.BAMDataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.data.Record;
import savant.api.util.Resolution;
import savant.data.types.BAMIntervalRecord;
import savant.util.DownloadEvent;

/**
 * Export a Savant alignment track (or portion thereof) to a local bam file.  This is necessary
 * because GATK only deals with local files, and not with streams.
 *
 * @author tarkvara
 */
public class BAMExporter extends TrackExporter {

    /** For writing to destination SAM/BAM file. */
    SAMFileWriter samWriter;

    /**
     * Should be instantiated from TrackExporter.getExporter();
     */
    BAMExporter(TrackAdapter t, File f) {
        super(t, f);

        // If the destFile extension is .sam, it will create a SAM text file.
        // If it's .bam, it will create a BAM file and corresponding index.
        samWriter = new SAMFileWriterFactory().setCreateIndex(true).makeSAMOrBAMWriter(((BAMDataSourceAdapter)track.getDataSource()).getHeader(), true, destFile);
    }

    /**
     * Clean up resources when the export is finished.
     */
    @Override
    void close() {
        if (samWriter != null) {
            samWriter.close();
        }
    }

    /**
     * Export the specified reference (or subrange thereof) to the given output stream.
     * This may be invoked as part of a large export (i.e. whole genome).
     * 
     * @param ref the reference containing the range be exported
     * @param r the range to be exported (must be non-null)
     * @param output destination for fasta data
     * @throws IOException 
     */
    @Override
    void exportRange(String ref, RangeAdapter r) throws IOException, InterruptedException {
        fireEvent(new DownloadEvent(-1.0));
        List<Record> recs = track.getDataSource().getRecords(ref, r, Resolution.HIGH, null);
        fireEvent(new DownloadEvent(0.5));
        for (Record rec: recs) {
            samWriter.addAlignment(((BAMIntervalRecord)rec).getSAMRecord());
        }
        fireEvent(new DownloadEvent(1.0));
    }
}
