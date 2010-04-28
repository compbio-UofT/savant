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
 * BAMToCoverage.java
 * Created on Feb 28, 2010
 */

//package savant.tools;
package savant.format;

import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.FieldType;

import java.io.*;
import java.util.*;

/**
 * Tool to create a coverage file from a BAM file.
 *
 * Usage:
 * - first argument: input BAM file
 * - second argument: input BAM index file
 * - third argument: output coverage file
 * - fourth argument: sequence length
 */
public class BAMToCoverage {

    private static Log log = LogFactory.getLog(BAMToCoverage.class);

    private static String fileSeparator = System.getProperty("file.separator");

    public static final int RECORDS_PER_INTERRUPT_CHECK = 5000;

    private String inFile;      // xx.bam file
    private String indexFile;   // xx.bai file
    private String outDir;      // xx_cov directory
    private String outFile;
    private DataOutputStream out;

    private SAMFileReader samFileReader;

    // map of reference sequence names and lengths
    private Map<String, Integer>sequenceDictionary = new HashMap<String, Integer>();

    // keep track of the sequence we're processing and the sequence we've just read
    private String processingSequenceName;
    private int processingSequenceLength;
    private String readSequenceName;
    private int readSequenceLength;

    // stuff needed by IO; mandated by DataFormatUtils which we're depending on
    private List<FieldType> fields;
    private List<Object> modifiers;

    // variables to keep track of progress processing the input file(s)
    private long totalLength=0;
    private long previousSequencesCumulativeLength=0;
    private long positionCount=0;
    private int progress=0; // 0 to 100%
    private List<FormatProgressListener> listeners = new ArrayList<FormatProgressListener>();


//    private int sequenceLength;
//    String sequence;
//    boolean sequenceFound = false;

    public BAMToCoverage(String inFile) throws IOException {

        this.inFile = inFile;
        // determine index and output file names
        prepareFiles(inFile);
        this.samFileReader = new SAMFileReader(new File(inFile), new File(indexFile));
        // get name and length of all reference sequences in the file
        readSequenceDictionary();
        // calculate the total length of all sequences
        calculateTotalLength();
    }

//    public BAMToCoverage(String inFile, String indexFile, String outFile, String sequenceName, int sequenceLength) {
//
//        this.samFileReader = new SAMFileReader(new File(inFile), new File(indexFile));
//        this.sequenceLength = sequenceLength;
//        this.outFile = outFile;
//
//        if (sequenceName != null && ! sequenceName.equals("null")) {
//            this.sequence = sequenceName;
//        }
//        else {
//            this.sequence = guessSequence(sequenceLength);
//        }
//
//
//    }

    public void format() throws InterruptedException {

//        initOutput();
        Date startTime = new Date();
        System.out.println("Started Processing at: " + startTime);
        
        CloseableIterator<SAMRecord> recordIterator = null;
        try {

            recordIterator = samFileReader.iterator();
            SAMRecord samRecord = null;

            HashMap<Integer,Float> m = new HashMap<Integer,Float>();
            int toWrite = 1;

            processingSequenceName = null;
            processingSequenceLength = 0;
            boolean done = false;
            while (!done) {

                for (int j=0; j<RECORDS_PER_INTERRUPT_CHECK; j++) {

    //                samRecord = readNextRecordInSequence(recordIterator);
                    samRecord = readNextRecord(recordIterator);
                    if (samRecord == null || !readSequenceName.equals(processingSequenceName)) {

                        // we've finished reading the last sequence
                        // OR we've finished reading one sequence and have started reading the next
                        advanceToNextSequence(toWrite, m);

                        if (samRecord == null) {
                            // we're finished
                            done = true;
                            break;
                        }

                        // reset for next sequence
                        toWrite = 1;
                        m.clear();
                    }

                    int alignmentStart = samRecord.getAlignmentStart();
                    int alignmentEnd = samRecord.getAlignmentEnd();

                    for(int i = toWrite; i < alignmentStart; i++){
                        Float x = m.remove(i);
                        if (x == null){
                            x = 0.0f;
                        }
                        try {
                            out.writeFloat(x);
                        } catch (IOException e) {
                            log.error("Problem writing data", e);
                        }
                    }
                    toWrite = alignmentStart;
                    // update how much we've processed, for the sake of calculating progress
                    positionCount = previousSequencesCumulativeLength + toWrite;

                    for(int i = alignmentStart; i <= alignmentEnd; i++){
                        Float count = m.get(i);
                        if(count == null){
                            m.put(i,1.0f);
                        } else {
                            m.put(i,count + 1);
                        }
                    }

                }

                // check to see if format has been cancelled
                if (Thread.interrupted()) throw new InterruptedException();
                updateProgress();
            }

        }
        finally {

            // close closeable iterator -- must do
            if (recordIterator != null) recordIterator.close();

            // cleanup output
            closeOutput();
            Date stopTime = new Date();
            System.out.println("Finished processing at: " + stopTime);
        }


    }

//    private SAMRecord readNextRecordInSequence(CloseableIterator<SAMRecord> recordIterator) {
//        SAMRecord sam = null;
//        if (recordIterator.hasNext()) {
//            sam = recordIterator.next();
//            // check for the right sequence
//            if (sam.getReferenceName().equals(sequence)) {
//                if (!sequenceFound) sequenceFound = true;
//                return sam;
//            }
//            else {
//                // we can bail now since we've gone past all entries in our sequence
//                if (sequenceFound) return null;
//            }
//        }
//        return null;
//    }
    
    private SAMRecord readNextRecord(CloseableIterator<SAMRecord> recordIterator) {
        SAMRecord sam = null;
        if (recordIterator.hasNext()) {
            sam = recordIterator.next();
            // check if sequence has changed
            String sequenceName = sam.getReferenceName();
            if (!sequenceName.equals(readSequenceName)) {
                readSequenceName = sequenceName;
                readSequenceLength = sequenceDictionary.get(readSequenceName);
            }
        }
        return sam;

    }

    private void advanceToNextSequence(int toWrite, HashMap<Integer,Float> m) {
        // write zeros to the end of the current sequence length
        for (int i = toWrite; i <= processingSequenceLength; i++){
            Float x = m.remove(i);
            if (x == null){
                x = 0.0f;
            }
            try {
                out.writeFloat(x);
            } catch (IOException e) {
                log.error("Problem writing data", e);
            }
        }

        // update how much of the genome length we've already processed, for calculating progress
        previousSequencesCumulativeLength +=  processingSequenceLength;

        // switch sequence and output file
        processingSequenceName = readSequenceName;
        processingSequenceLength = readSequenceLength;
        closeOutput();
        prepareOutputFile(processingSequenceName);

    }

    private void initOutput() {

        try {
            // open output stream
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));

            // write file type header
            FileTypeHeader fileTypeHeader = new FileTypeHeader(FileType.CONTINUOUS_GENERIC, 1);
            out.writeInt(fileTypeHeader.fileType.getMagicNumber());
            out.writeInt(fileTypeHeader.version);

            // prepare and write fields header
            fields = new ArrayList<FieldType>();
            fields.add(FieldType.FLOAT);
            modifiers = new ArrayList<Object>();
            modifiers.add(null);
            out.writeInt(fields.size());
            for (FieldType ft : fields) {
                out.writeInt(ft.ordinal());
            }


        } catch (IOException e) {
            log.error("Error initializing output file " + outFile, e);
        }
    }

    private void closeOutput() {
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            log.warn("Error closing output file " + outFile, e);
        }
    }
    
    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        fireProgressUpdate(progress);

    }

    public void addProgressListener(FormatProgressListener listener) {
        listeners.add(listener);
    }

    public void removeProgressListener(FormatProgressListener listener) {
        listeners.remove(listener);
    }

    private void fireProgressUpdate(int value) {
        for (FormatProgressListener listener : listeners) {
            listener.progressUpdate(value);
        }
    }

    private void updateProgress() {
        float proportionDone = (float)this.positionCount/(float)this.totalLength;
        int percentDone = (int)Math.round(proportionDone * 100.0);
        setProgress(percentDone);
    }

//    private String guessSequence(int sequenceLength) {
//
//        SAMFileHeader fileHeader = samFileReader.getFileHeader();
//        SAMSequenceDictionary sequenceDictionary = fileHeader.getSequenceDictionary();
//        // find the first sequence with the smallest difference in length from our reference sequence
//        int leastDifferenceInSequenceLength = Integer.MAX_VALUE;
//        int closestSequenceIndex = Integer.MAX_VALUE;
//        int i = 0;
//        for (SAMSequenceRecord sequenceRecord : sequenceDictionary.getSequences()) {
//            int lengthDelta = Math.abs(sequenceRecord.getSequenceLength() - sequenceLength);
//            if (lengthDelta < leastDifferenceInSequenceLength) {
//                leastDifferenceInSequenceLength = lengthDelta;
//                closestSequenceIndex = i;
//            }
//            i++;
//        }
//        if (closestSequenceIndex != Integer.MAX_VALUE) {
//            return sequenceDictionary.getSequence(closestSequenceIndex).getSequenceName();
//        }
//        return null;
//
//    }

    private void prepareFiles(String inFile) throws IOException {

        // determine default track name from filename
        int lastSlashIndex = inFile.lastIndexOf(System.getProperty("file.separator"));
        String name = inFile.substring(lastSlashIndex+1, inFile.length());

        // infer index file name from track filename
        String pathWithoutExtension = inFile.substring(0, inFile.lastIndexOf(".bam"));
        if (new File(inFile + ".bai").exists()) {
            indexFile = inFile + ".bai";
        }
        else {
            if (new File(pathWithoutExtension + ".bai").exists()) {
                indexFile = pathWithoutExtension + ".bai";
            }
        }

        // infer name and make output directory
        outDir = pathWithoutExtension + "_cov";
        File outputDirectory = new File(outDir);
        if (outputDirectory.exists()) outputDirectory.delete();
        boolean created = new File(outDir).mkdir();
        if (!created) {
            log.error("Output directory could not be created");
            throw new IOException("Could not create output directory");
        }
    }

    private void prepareOutputFile(String sequenceName) {
        this.outFile = outDir + fileSeparator + sequenceName + ".cov.savant";
        initOutput();
    }

    private void readSequenceDictionary() {
        SAMFileHeader fileHeader = samFileReader.getFileHeader();
        SAMSequenceDictionary dictionary = fileHeader.getSequenceDictionary();
        List<SAMSequenceRecord> sequences = dictionary.getSequences();
        for (SAMSequenceRecord sequence : sequences) {
            sequenceDictionary.put(sequence.getSequenceName(), sequence.getSequenceLength());
        }
        
    }

    private void calculateTotalLength() {
        for (String name : sequenceDictionary.keySet()) {
            totalLength += sequenceDictionary.get(name);
        }
    }

    public static void main(String args[]) {
        if (args.length != 1) {
//            System.out.println("Missing argument: BAM, index file, output file, reference sequence name (may be \"null\") and length required");
            System.out.println("Missing argument: BAM file required");
            System.exit(1);
        }
//        BAMToCoverage instance = new BAMToCoverage(args[0], args[1], args[2], args[3], Integer.parseInt(args[4]));
        BAMToCoverage instance = null;
        try {
            instance = new BAMToCoverage(args[0]);
        } catch (IOException e) {
            log.error("Error formatting file " + args[0], e);
        }
        try {
            instance.format();
        } catch (InterruptedException e) {
            log.info("Formatting interrupted");
        }
    }
}
