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
import net.sf.samtools.SAMSequenceDictionary;

import net.sf.samtools.SAMSequenceRecord;
import savant.api.adapter.RangeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.data.Record;
import savant.api.util.Resolution;
import savant.data.sources.BAMDataSource;
import savant.data.types.BAMIntervalRecord;
import savant.settings.TrackResolutionSettings;
import savant.util.Range;

/**
 * Export a Savant alignment track (or portion thereof) to a local bam file.  This is necessary
 * because GATK only deals with local files, and not with streams.
 *
 * @author tarkvara
 */
public class BamExporter extends TrackExporter {
    /** Destination for export of BAM tracks. */
    private SAMFileWriter samWriter;

    /**
     * Should be instantiated from TrackExporter.getExporter();
     */
    BamExporter(TrackAdapter t) {
        super(t);
    }

    /**
     * Export the given range of the Fasta track.
     *
     * @param t the track to be exported
     * @param ref the reference containing the range be exported
     * @param r the range to be exported
     */
    @Override
    public void exportRange(String ref, RangeAdapter r, File destFile) throws IOException {
        try {
            // If the file extension is .sam, it will create a SAM text file.
            // If it's .bam, it will create a BAM file and corresponding index.
            samWriter = new SAMFileWriterFactory().setCreateIndex(true).makeSAMOrBAMWriter(((BAMDataSource)track.getDataSource()).getHeader(), true, destFile);

            int chunkSize = Math.max(TrackResolutionSettings.getBamDefaultModeLowToHighThresh(), TrackResolutionSettings.getBamArcModeLowToHighThresh());
            for (int i = r.getFrom(); i < r.getTo(); i += chunkSize) {
                List<Record> recs = track.getDataSource().getRecords(ref, new Range(i, i + chunkSize), Resolution.HIGH);
                for (Record rec: recs) {
                    samWriter.addAlignment(((BAMIntervalRecord)rec).getSamRecord());
                }
            }
        } finally {
            if (samWriter != null) {
                samWriter.close();
            }
        }
    }
}
