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

import java.awt.Component;
import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;
import org.apache.commons.logging.LogFactory;
import java.io.*;
import java.util.*;
import savant.format.header.FileType;
import javax.swing.JOptionPane;
import savant.format.util.data.FieldType;
import savant.view.swing.Savant;

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
    private String readSequenceName;
    private int readSequenceLength;

    private long totalLength=0;
    private long previousSequencesCumulativeLength=0;

    public BAMToCoverage(String inFile) throws IOException {
        this(inFile, inFile + ".cov.savant");
    }

    public BAMToCoverage(String inFile, String outFile) throws IOException {
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

    public void format() throws InterruptedException, IOException {

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

            // keep going until EOF
            boolean done = false;
            while (!done) {

                // stop now and again to update progress
                for (int j=0; j<RECORDS_PER_INTERRUPT_CHECK; j++) {

    //                samRecord = readNextRecordInSequence(recordIterator);
                    
                    samRecord = readNextRecord(recordIterator); // iterate to next record

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
                updateProgress();
            }

        }
        finally {

            // close closeable iterator -- must do
            if (recordIterator != null) recordIterator.close();

            this.closeOutputStreams();
            this.writeOutputFile();

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
            // check if sequence has changed
            String sequenceName = sam.getReferenceName();
            if (!sequenceName.equals(readSequenceName)) {
                readSequenceName = sequenceName;
                readSequenceLength = sequenceDictionary.get(readSequenceName);
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
        if (!readSequenceName.equals(processingSequenceName)) {
            //closeOutput();

            processingSequenceName = readSequenceName;
            processingSequenceLength = readSequenceLength;

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

    @Override
    protected void updateProgress() {
        float proportionDone = (float)this.positionCount/(float)this.totalLength;
        int percentDone = (int)Math.round(proportionDone * 100.0);
        setProgress(percentDone);
    }

}
