/*
 *    Copyright 2010-2011 University of Toronto
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

package savant.data.sources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.samtools.*;
import net.sf.samtools.util.SeekableStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.controller.LocationController;
import savant.util.IndexCache;
import savant.data.types.BAMIntervalRecord;
import savant.file.DataFormat;
import savant.util.AxisRange;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;
import savant.util.Resolution;

/**
 * Class to represent a track of BAM intervals. Uses SAMTools to read data within a range.
 *
 * @author vwilliams
 */
public class BAMDataSource extends DataSource<BAMIntervalRecord> {

    private static final Log LOG = LogFactory.getLog(BAMDataSource.class);

    private SAMFileReader samFileReader;
    private SAMFileHeader samFileHeader;
    private URI uri;

    public static BAMDataSource fromURI(URI uri) throws IOException {

        if (uri == null) throw new IllegalArgumentException("Invalid argument: URI must be non-null");

        File indexFile = null;
        // if no exception is thrown, this is an absolute URL
        String proto = uri.getScheme();
        if ("http".equals(proto) || "https".equals(proto) || "ftp".equals(proto)) {
            indexFile = getIndexFileCached(uri);
            if (indexFile != null) {
                return new BAMDataSource(uri, indexFile);
            }
        }

        // infer index file name from track filename
        File bamFile = new File(uri);
        indexFile = getIndexFileLocal(bamFile);
        if (indexFile != null) {
            return new BAMDataSource(bamFile.toURI(), indexFile);
        }

        // Unable to find index file anywhere.
        throw new FileNotFoundException(bamFile.getAbsolutePath() + ".bai");
    }

    private BAMDataSource(URI uri, File index) throws IOException {

        if (uri == null) throw new IllegalArgumentException("URI must not be null");
        if (index == null) throw new IllegalArgumentException("Index file must not be null");

        this.uri = uri.normalize();

        SeekableStream stream = NetworkUtils.getSeekableStreamForURI(uri);
        samFileReader = new SAMFileReader(stream, index, false);
        samFileReader.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
        samFileHeader = samFileReader.getFileHeader();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<BAMIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) {
        return getRecords(reference, range, resolution, Double.MAX_VALUE, null, false);
    }

    public List<BAMIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution, double threshold, AxisRange axisRange, boolean arcMode) {

        //CloseableIterator<SAMRecord> recordIterator=null;
        SAMRecordIterator recordIterator=null;
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
                
                // don't keep unmapped reads
                if (samRecord.getReadUnmappedFlag()) continue;
                
                //discard reads that
                if(arcMode){
                    int arcLength = Math.abs(samRecord.getInferredInsertSize());
                    // skip reads with a zero insert length--probably mapping errors
                    if (arcLength == 0) continue;               

                    if ((axisRange != null && threshold != 0.0d && threshold < 1.0d && arcLength < axisRange.getXRange().getLength()*threshold)
                            || (threshold > 1.0d && arcLength < threshold)) {
                        continue;
                    }
                }

                record = BAMIntervalRecord.valueOf(samRecord);

                result.add(record);
            }

        } finally {
            if (recordIterator != null) recordIterator.close();
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
            int lengthDelta = Math.abs(sequenceRecord.getSequenceLength() - (int)referenceSequenceLength);
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

    private static File getIndexFileLocal(File bamFile) {
        String bamPath = bamFile.getAbsolutePath();
        File indexFile = new File(bamPath + ".bai");
        if (indexFile.exists()) {
            return indexFile;
        }
        else {
            // try alternate index file name
            indexFile = new File(bamPath.replace(".bam", ".bai"));
            if (indexFile.exists()) {
                return indexFile;
            }
        }
        return null;
    }

    private static File getIndexFileCached(URI bamURI) throws IOException {
        return IndexCache.getInstance().getIndex(bamURI,"bai","bam");
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public final DataFormat getDataFormat() {
        return DataFormat.INTERVAL_BAM;
    }

    @Override
    public final String[] getColumnNames() {
        return new String[] { "Read Name", "Sequence", "Length", "First of Pair", "Position", "Strand +", "Mapping Quality", "Base Qualities", "CIGAR", "Mate Position", "Strand +", "Inferred Insert Size" };
    }

    /**
     * For use by the Data Table plugin, which needs to know the header for export purposes.
     */
    public SAMFileHeader getHeader() {
        return samFileHeader;
    }
}
