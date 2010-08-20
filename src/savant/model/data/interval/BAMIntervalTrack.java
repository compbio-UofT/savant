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
 * BAMIntervalTrack.java
 * Created on Jan 28, 2010
 */

package savant.model.data.interval;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.RangeController;
import savant.data.types.BAMIntervalRecord;
import savant.model.Resolution;
import savant.model.data.RecordTrack;
import savant.util.Range;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import savant.controller.ReferenceController;
import savant.util.MiscUtils;

/**
 * Class to represent a track of BAM intervals. Uses SAMTools to read data within a range.
 * 
 * @author vwilliams
 */
public class BAMIntervalTrack implements RecordTrack<BAMIntervalRecord> {

    private static Log log = LogFactory.getLog(BAMIntervalTrack.class);
    
    private SAMFileReader samFileReader;
    private SAMFileHeader samFileHeader;
    //private String sequenceName;

    public static BAMIntervalTrack fromfileNameOrURL(String fileNameOrURL) throws IOException {

        if (fileNameOrURL == null) throw new IllegalArgumentException("Invalid argument; file name or URL must be non-null");

        URL fileURL = null;
        File indexFile = null;
        try {
            fileURL = new URI(fileNameOrURL).toURL();
            // if no exception is thrown, this is an absolute URL
            if (fileURL.getProtocol().equalsIgnoreCase("http")) {
                // for now, we can deal with http URLs only
                indexFile = getIndexFileCached(fileURL);
                if (indexFile != null) {
                    return new BAMIntervalTrack(fileURL, indexFile);
                }
            }
            
        } catch (MalformedURLException e) {
            // not a URL, try as a filename
        } catch (URISyntaxException e) {
            // not a URI, try as a filename
        } catch (IllegalArgumentException e) {
            // not an absolute URI, try a filename
        }

        if (fileURL == null) {
            
            // infer index file name from track filename
            indexFile = getIndexFileLocal(fileNameOrURL);
            if (indexFile != null) {
                return new BAMIntervalTrack(new File(fileNameOrURL), indexFile);
            }
        }

        // no success
        return null;
    }

    public BAMIntervalTrack(File path, File index) {

        if (path == null) throw new IllegalArgumentException("File must not be null.");
        if (index == null) throw new IllegalArgumentException("Index file must not be null");
        
        //this.sequenceName = guessSequence(path, index);
        samFileReader = new SAMFileReader(path, index);
        samFileReader.setValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
        samFileHeader = samFileReader.getFileHeader();
    }

    public BAMIntervalTrack(URL url, File index) {

        if (url == null) throw new IllegalArgumentException("URL must not be null");
        if (index == null) throw new IllegalArgumentException("Index file must not be null");

        samFileReader = new SAMFileReader(url, index, false);
        samFileReader.setValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
        samFileHeader = samFileReader.getFileHeader();
    }

    /**
     * {@inheritDoc}
     */
    public List<BAMIntervalRecord> getRecords(String reference, Range range, Resolution resolution) throws OutOfMemoryError {


        CloseableIterator<SAMRecord> recordIterator=null;
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

            recordIterator = samFileReader.query(ref, range.getFrom(), range.getTo(), false);

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
        int referenceSequenceLength = rangeController.getMaxRangeEnd() - rangeController.getMaxRangeStart();

        String sequenceName = null;
        SAMSequenceDictionary sequenceDictionary = samFileHeader.getSequenceDictionary();
        // find the first sequence with the smallest difference in length from our reference sequence
        int leastDifferenceInSequenceLength = Integer.MAX_VALUE;
        int closestSequenceIndex = Integer.MAX_VALUE;
        int i = 0;
        for (SAMSequenceRecord sequenceRecord : sequenceDictionary.getSequences()) {
            int lengthDelta = Math.abs(sequenceRecord.getSequenceLength() - referenceSequenceLength);
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
    public void close() {

        if (samFileReader != null) {
            samFileReader.close();
        }
    }

    Set<String> referenceNames;

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

    private static File getIndexFileLocal(String bamFileName) {
        
        File indexFile = new File(bamFileName + ".bai");
        if (indexFile.exists()) {
            return indexFile;
        }
        else {
            // try alternate index file name
            indexFile = new File(bamFileName.replace(".bam", ".bai"));
            if (indexFile.exists()) {
                return indexFile;
            }
        }
        return null;
    }

    private static File getIndexFileCached(URL bamURL) throws IOException {
        return BAMIndexCache.getInstance().getBAMIndex(bamURL);
    }
}
