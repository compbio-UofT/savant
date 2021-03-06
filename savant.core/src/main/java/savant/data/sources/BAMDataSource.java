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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.samtools.*;
import net.sf.samtools.util.SeekableStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.BAMDataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.data.DataFormat;
import savant.api.util.Resolution;
import savant.controller.LocationController;
import savant.data.types.BAMIntervalRecord;
import savant.util.IndexCache;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;

/**
 * Class to represent a track of BAM intervals. Uses SAMTools to read data
 * within a range.
 *
 * @author vwilliams
 */
public class BAMDataSource extends DataSource<BAMIntervalRecord> implements BAMDataSourceAdapter {

    private static final Log LOG = LogFactory.getLog(BAMDataSource.class);
    private SAMFileReader samFileReader;
    private SAMFileHeader samFileHeader;
    private URI uri;

    public BAMDataSource(URI uri) throws IOException {
        this.uri = uri.normalize();

        File indexFile = IndexCache.getIndexFile(uri, "bai", "bam");
        SeekableStream stream = NetworkUtils.getSeekableStreamForURI(uri);
        samFileReader = new SAMFileReader(stream, indexFile, false);
        samFileReader.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
        samFileHeader = samFileReader.getFileHeader();
    }

    @Override
    public List<BAMIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution, RecordFilterAdapter filt) throws InterruptedException {

        SAMRecordIterator recordIterator = null;
        List<BAMIntervalRecord> result = new ArrayList<BAMIntervalRecord>();
        try {
            // todo: actually use the given reference

            String ref;
            if (getReferenceNames().contains(reference)) {
                ref = reference;
            } else if (getReferenceNames().contains(MiscUtils.homogenizeSequence(reference))) {
                ref = MiscUtils.homogenizeSequence(reference);
            } else {
                ref = guessSequence();
            }

            recordIterator = samFileReader.query(ref, range.getFrom(), range.getTo(), false);


            SAMRecord samRecord;
            BAMIntervalRecord record;
            while (recordIterator.hasNext()) {

                samRecord = recordIterator.next();

                // Don't keep unmapped reads
                if (samRecord.getReadUnmappedFlag()) {
                    continue;
                }

                record = BAMIntervalRecord.valueOf(samRecord);
                if (filt == null || filt.accept(record)) {
                    result.add(record);
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        } finally {
            if (recordIterator != null) {
                recordIterator.close();
            }
        }

        return result;
    }

    /*
     * Use the length of the reference genome to guess which sequence from the dictionary
     * we should search for reads.
     */
    private String guessSequence() {

        // Find out what sequence we're using, by reading the header for sequences and lengths
        LocationController locationController = LocationController.getInstance();
        int referenceSequenceLength = locationController.getMaxRangeEnd() - locationController.getMaxRangeStart();
        assert Math.abs(referenceSequenceLength) < Integer.MAX_VALUE;

        String sequenceName = null;
        SAMSequenceDictionary sequenceDictionary = samFileHeader.getSequenceDictionary();
        // find the first sequence with the smallest difference in length from our reference sequence
        int leastDifferenceInSequenceLength = Integer.MAX_VALUE;
        int closestSequenceIndex = Integer.MAX_VALUE;
        int i = 0;
        for (SAMSequenceRecord sequenceRecord : sequenceDictionary.getSequences()) {
            int lengthDelta = Math.abs(sequenceRecord.getSequenceLength() - (int) referenceSequenceLength);
            if (lengthDelta < leastDifferenceInSequenceLength) {
                leastDifferenceInSequenceLength = lengthDelta;
                closestSequenceIndex = i;
            }
            i++;
        }
        if (closestSequenceIndex != Integer.MAX_VALUE) {
            sequenceName = sequenceDictionary.getSequence(closestSequenceIndex).getSequenceName();
        }
        return sequenceName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {

        if (samFileReader != null) {
            samFileReader.close();
        }
    }
    Set<String> referenceNames;

    @Override
    public Set<String> getReferenceNames() {

        if (referenceNames == null) {
            SAMSequenceDictionary ssd = samFileHeader.getSequenceDictionary();

            List<SAMSequenceRecord> seqs = ssd.getSequences();

            referenceNames = new HashSet<String>();
            for (SAMSequenceRecord ssr : seqs) {
                referenceNames.add(ssr.getSequenceName());
            }
        }
        return referenceNames;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public final DataFormat getDataFormat() {
        return DataFormat.ALIGNMENT;
    }

    @Override
    public final String[] getColumnNames() {
        return new String[]{"Read Name", "Sequence", "Length", "First of Pair", "Position", "Strand +", "Mapping Quality", "Base Qualities", "CIGAR", "Mate Position", "Strand +", "Inferred Insert Size"};
    }

    /**
     * For use by the Data Table plugin, which needs to know the header for
     * export purposes.
     */
    @Override
    public SAMFileHeader getHeader() {
        return samFileHeader;
    }
}
