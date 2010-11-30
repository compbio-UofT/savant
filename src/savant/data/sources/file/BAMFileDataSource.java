/*
 * BAMDataSource.java
 * Created on Jan 28, 2010
 *
 *
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

package savant.data.sources.file;

import java.io.File;
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
import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.data.sources.BAMDataSource;
import savant.data.sources.DataSource;
import savant.data.types.BAMIntervalRecord;
import savant.file.DataFormat;
import savant.util.MiscUtils;
import savant.util.Resolution;
import savant.util.NetworkUtils;

/**
 * Class to represent a track of BAM intervals. Uses SAMTools to read data within a range.
 * 
 * @author vwilliams
 */
public class BAMFileDataSource extends BAMDataSource implements FileDataSource {

    private static Log log = LogFactory.getLog(BAMFileDataSource.class);
    
    private SAMFileReader samFileReader;
    private SAMFileHeader samFileHeader;
    private URI uri;
    //private String sequenceName;

//    private String fileNameOrURL;

    public static BAMFileDataSource fromURI(URI uri) throws IOException {

        if (uri == null) throw new IllegalArgumentException("Invalid argument: URI must be non-null");

        File indexFile = null;
        // if no exception is thrown, this is an absolute URL
        String scheme = uri.getScheme();
        if ("http".equals(scheme) || "ftp".equals(scheme)) {
            indexFile = getIndexFileCached(uri);
            if (indexFile != null) {
                return new BAMFileDataSource(uri, indexFile);
            }
        }

        // infer index file name from track filename
        File bamFile = new File(uri);
        indexFile = getIndexFileLocal(bamFile);
        if (indexFile != null) {
            return new BAMFileDataSource(bamFile, indexFile);
        }

        // no success
        return null;
    }

    public BAMFileDataSource(File file) {
        this(file, getIndexFileLocal(file));

    }

    public BAMFileDataSource(File file, File index) {

        if (file == null) throw new IllegalArgumentException("File must not be null.");
        if (index == null) throw new IllegalArgumentException("Index file must not be null");

        this.uri = file.toURI().normalize();

//        this.fileNameOrURL = path.getAbsolutePath();
        
        //this.sequenceName = guessSequence(path, index);
        samFileReader = new SAMFileReader(file, index);
        samFileReader.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
        samFileHeader = samFileReader.getFileHeader();
    }

    public BAMFileDataSource(URI uri) throws IOException {
        this(uri, getIndexFileCached(uri));
    }
    
    public BAMFileDataSource(URI uri, File index) throws IOException {

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
    public List<BAMIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws OutOfMemoryError {


        //CloseableIterator<SAMRecord> recordIterator=null;
        SAMRecordIterator recordIterator=null;
        List<BAMIntervalRecord> result = new ArrayList<BAMIntervalRecord>();
        try {
            // todo: actually use the given reference

            String ref;
            if(this.getReferenceNames().contains(reference)){
                ref = reference;
            } else if(this.getReferenceNames().contains(MiscUtils.homogenizeSequence(reference))){
                ref = MiscUtils.homogenizeSequence(reference);
            } else {
                ref = guessSequence();
            }

            recordIterator = samFileReader.query(ref, range.getFromAsInt(), range.getToAsInt(), false);

            SAMRecord samRecord;
            BAMIntervalRecord bamRecord;
            while (recordIterator.hasNext()) {
                samRecord = recordIterator.next();
                // don't keep unmapped reads
                if (samRecord.getReadUnmappedFlag()) continue;

                // find out the type of the pair
                BAMIntervalRecord.PairType type = null;
                if (samRecord.getReadPairedFlag() && !samRecord.getMateUnmappedFlag()) {
                    type = getPairType(samRecord.getAlignmentStart(), samRecord.getMateAlignmentStart(), samRecord.getReadNegativeStrandFlag(), samRecord.getMateNegativeStrandFlag());
                }
                bamRecord = BAMIntervalRecord.valueOf(samRecord, type);

                result.add(bamRecord);
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
        RangeController rangeController = RangeController.getInstance();
        long referenceSequenceLength = rangeController.getMaxRangeEnd() - rangeController.getMaxRangeStart();
        assert Math.abs(referenceSequenceLength) < Integer.MAX_VALUE;

        String sequenceName = null;
        SAMSequenceDictionary sequenceDictionary = samFileHeader.getSequenceDictionary();
        // find the first sequence with the smallest difference in length from our reference sequence
        long leastDifferenceInSequenceLength = Long.MAX_VALUE;
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

    /*
     * Determine the pair type: NORMAL, INVERTED_READ, INVERTED_MATE, EVERTED pair.
     */
    private BAMIntervalRecord.PairType getPairType(int readStart, int mateStart, boolean readNegative, boolean mateNegative) {

        BAMIntervalRecord.PairType type=null;
        boolean readNegativeStrand, mateNegativeStrand;

        // by switching the negative strand flags based on which read comes first, we can reduce 8 cases to 4
        if (readStart < mateStart) {
            readNegativeStrand = readNegative;
            mateNegativeStrand = mateNegative;
        }
        else {
            readNegativeStrand = mateNegative;
            mateNegativeStrand = readNegative;
        }

        // now is the first read pointing forward & mate pointing backward?
        if (!readNegativeStrand && mateNegativeStrand) {
            // congratulations, it's a normal pair!
            type = BAMIntervalRecord.PairType.NORMAL;
        }
        // or are both reversed?
        else if (readNegativeStrand && mateNegativeStrand) {
            // this is a case of the read being inverted
            type = BAMIntervalRecord.PairType.INVERTED_READ;
        }
        // or are both forward?
        else if (!readNegativeStrand && !mateNegativeStrand) {
            // this is a case of the mate being inverted
            type = BAMIntervalRecord.PairType.INVERTED_MATE;
        }
        // are the strands pointing away from each other?
        else if (readNegativeStrand && !mateNegativeStrand) {
            // the pair is everted
            type = BAMIntervalRecord.PairType.EVERTED;
        }
        return type;
    }

    /**
     * Get the name of the sequence we are querying.
     *
     * @return sequence name
     *
    public String getSequenceName() {
        return sequenceName;
    }
     */

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

        if (this.referenceNames == null) {
            String prefix = MiscUtils.removeNumbersFromString(ReferenceController.getInstance().getReferenceName());

            SAMSequenceDictionary ssd = samFileHeader.getSequenceDictionary();

            List<SAMSequenceRecord> seqs = ssd.getSequences();

            referenceNames = new HashSet<String>();
            //System.out.println("Reading BAM sequence list");
            for (SAMSequenceRecord ssr : seqs) {
                //System.out.println(ssr.getSequenceName());
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
        return BAMIndexCache.getInstance().getBAMIndex(bamURI);
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public String getName() {
        return MiscUtils.getNeatPathFromURI(getURI());
    }
}
