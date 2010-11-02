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

package savant.format;

import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;
import org.apache.commons.logging.LogFactory;
import java.io.*;
import java.util.*;
import savant.file.FileType;
import savant.file.FieldType;
import savant.util.MiscUtils;

public class BAMToCoverage extends SavantFileFormatter {

    private static String fileSeparator = System.getProperty("file.separator");

    public static final int RECORDS_PER_INTERRUPT_CHECK = 5000;

    private String indexFile;   // xx.bai file
    private String outDir;      // xx_cov directory

    private DataOutputStream outfile;

    private SAMFileReader samFileReader;

    // map of reference sequence names and lengths
    private Map<String, Integer>sequenceDictionary = new HashMap<String, Integer>();

    // keep track of the sequence we're processing and the sequence we've just read
    private String processingSequenceName;
    private int processingSequenceLength;
    private String referenceName;
    private int referenceSequenceLength;

    private long totalLength=0;
    private long previousSequencesCumulativeLength=0;

    public BAMToCoverage(String inFile) throws IOException, Exception {
        this(inFile, inFile + ".cov.savant");
    }

    public BAMToCoverage(String inFile, String outFile) throws IOException, Exception {
        super(inFile,outFile,FileType.CONTINUOUS_GENERIC);

        log = LogFactory.getLog(BAMToCoverage.class);

        //this.inFilePath = inFile;

        // determine index and output file names
        prepareFiles(inFile);

        this.samFileReader = new SAMFileReader(new File(inFile), new File(indexFile));
        samFileReader.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);

        // get name and length of all reference sequences in the file
        readSequenceDictionary();
        // calculate the total length of all sequences
        calculateTotalLength();
    }

    //FIXME: this code is very messy. It doesnt handle reference name = *, which occurs in many files!
    public void format() throws InterruptedException, IOException, Exception {

        //DataOutputStream outfile = null;

//        initOutput();
        //Date startTime = new Date();
        //System.out.println("Started Processing at: " + startTime);

        fields = new ArrayList<FieldType>();
        fields.add(FieldType.FLOAT);

        modifiers = new ArrayList<Object>();
        modifiers.add(null);

        CloseableIterator<SAMRecord> recordIterator = null;
        try {

            // get a record iterator
            recordIterator = samFileReader.iterator(); 

            // a sam record
            SAMRecord samRecord = null;

            // TODO: what is m?
            HashMap<Integer,Float> m = new HashMap<Integer,Float>();
            
            // TODO: what is this?
            int toWrite = 1;

            processingSequenceName = null;
            processingSequenceLength = 0;

            this.setSubtaskStatus("Processing input file ...");
            this.incrementOverallProgress();

            // keep going until EOF
            boolean done = false;
            while (!done) {

                // stop now and again to update progress
                for (int j=0; j<RECORDS_PER_INTERRUPT_CHECK; j++) {

                    samRecord = readNextRecord(recordIterator); // iterate to next record

                    if (referenceName.equals("*") && samRecord != null) {
                        continue;
                    } else if (samRecord == null || !referenceName.equals(processingSequenceName)) {

                        // we've finished reading the last sequence
                        // OR we've finished reading one sequence and have started reading the next
                        advanceToNextSequence(toWrite, m);

                        if (samRecord == null) {
                            System.out.println("Finished");
                            // we're finished
                            done = true;
                            break;
                        }

                        // reset for next sequence
                        toWrite = 1;
                        m.clear();
                    }

                    // alignment start and end
                    int alignmentStart = samRecord.getAlignmentStart();
                    int alignmentEnd = samRecord.getAlignmentEnd();

                    // write to appropriate file
                    for(int i = toWrite; i < alignmentStart; i++){
                        Float x = m.remove(i);
                        if (x == null){
                            x = 0.0f;
                        }
                        try {

                            // write to the appropriate outputfile
                            //outfile = this.getFileForReference(processingSequenceName);
                            outfile.writeFloat(x);
                            //System.out.println("Writing " + x);
                            
                            //out.writeFloat(x);
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
                this.setSubtaskProgress(this.getProgressAsInteger(positionCount, totalLength));
            }

        }
        finally {

            // close closeable iterator -- must do
            if (recordIterator != null) recordIterator.close();

            this.closeOutputStreams();
            //this.writeOutputFile();

            // map of reference name -> multiresolution *index* filename
            Map<String,String> refnameToIndexFileNameMap = ContinuousFormatterHelper.makeMultiResolutionContinuousFiles(referenceName2FilenameMap);

            List<String> refnames = MiscUtils.set2List(this.referenceName2FilenameMap.keySet());
            this.writeContinuousOutputFile(refnames, refnameToIndexFileNameMap, this.referenceName2FilenameMap);

            // cleanup output
            //closeOutput();
            //Date stopTime = new Date();
            //System.out.println("Finished processing at: " + stopTime);
        }


    }

    private SAMRecord readNextRecord(CloseableIterator<SAMRecord> recordIterator) {
        SAMRecord sam = null;
        if (recordIterator.hasNext()) {
            sam = recordIterator.next();

            System.out.println(sam + " " + sam.getReferenceName());
            // check if sequence has changed
            String refName = sam.getReferenceName();
            if (!refName.equals(referenceName)) {
                referenceName = refName;
                if (refName.equals("*")) {
                    System.out.println(sam);
                    referenceSequenceLength = 0;
                }
                else { referenceSequenceLength = sequenceDictionary.get(referenceName); }
            }
        }
        return sam;

    }

    private void advanceToNextSequence(int toWrite, HashMap<Integer,Float> m) throws FileNotFoundException {

        // write zeros to the end of the current sequence length
        for (int i = toWrite; i <= processingSequenceLength; i++){
            Float x = m.remove(i);
            if (x == null){
                x = 0.0f;
            }
            try {
                outfile.writeFloat(x);
            } catch (IOException e) {
                log.error("Problem writing data", e);
            }
        }

        // update how much of the genome length we've already processed, for calculating progress
        previousSequencesCumulativeLength +=  processingSequenceLength;

        // switch sequence and output file, as long as we're not at the last one
        if (!referenceName.equals(processingSequenceName) && !referenceName.equals("*")) {

            processingSequenceName = referenceName;
            processingSequenceLength = referenceSequenceLength;

            prepareOutputFile(processingSequenceName);
        }
    }

    private void prepareFiles(String inFile) throws IOException {

        // determine default track name from filename
        //int lastSlashIndex = inFile.lastIndexOf(System.getProperty("file.separator"));
        //String name = inFile.substring(lastSlashIndex+1, inFile.length());

        // infer index file name from track filename
        String pathWithoutExtension;
        int lastIndex = inFile.lastIndexOf(".bam");
        if(lastIndex == -1){
            log.error("BAM files should end with the \".bam\" file extension.");
            throw new IOException("BAM files should end with the \".bam\" file extension.");
        } else {
            pathWithoutExtension = inFile.substring(0, lastIndex);
        }
        //String pathWithoutExtension = inFile.substring(0, inFile.lastIndexOf(".bam"));
        if (new File(inFile + ".bai").exists()) {
            indexFile = inFile + ".bai";
        }
        else {
            if (new File(pathWithoutExtension + ".bai").exists()) {
                indexFile = pathWithoutExtension + ".bai";
            }
        }

        // infer name and make output directory
        //outDir = pathWithoutExtension + "_cov";
        //File outputDirectory = new File(outDir);
        //if (outputDirectory.exists()) outputDirectory.delete();
        //boolean created = new File(outDir).mkdir();
        //if (!created) {
        //    log.error("Output directory could not be created");
        //    throw new IOException("Could not create output directory");
        //}
    }

    private void prepareOutputFile(String sequenceName) throws FileNotFoundException {
        System.out.println("Preparing output file for : " + sequenceName);
        outfile = this.getFileForReference(sequenceName);
        //this.outFilePath = outDir + fileSeparator + sequenceName + ".cov.savant";
        //initOutput();
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
}
