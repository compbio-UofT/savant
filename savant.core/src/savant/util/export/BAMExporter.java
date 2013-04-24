/*
 *    Copyright 2011 University of Toronto
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
