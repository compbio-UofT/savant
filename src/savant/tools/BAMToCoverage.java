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
package savant.tools;

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

    private SAMFileReader samFileReader;
    private int sequenceLength;

    private String outFile;
    private DataOutputStream out;

    // stuff needed by IO; mandated by IOUtils and DataUtils which we're depending on
    //private RandomAccessFile raf;

    private List<FieldType> fields;
    private List<Object> modifiers;


    String sequence;
    boolean sequenceFound = false;

    public BAMToCoverage(String inFile, String indexFile, String outFile, String sequenceName, int sequenceLength) {

        this.samFileReader = new SAMFileReader(new File(inFile), new File(indexFile));
        this.sequenceLength = sequenceLength;
        this.outFile = outFile;

        if (sequenceName != null && ! sequenceName.equals("null")) {
            this.sequence = sequenceName;
        }
        else {
            this.sequence = guessSequence(sequenceLength);
        }

        initOutput();

    }

    public void format() {

        CloseableIterator<SAMRecord> recordIterator = null;
        try {

            recordIterator = samFileReader.iterator();
            SAMRecord samRecord = null;

            HashMap<Integer,Double> m = new HashMap<Integer,Double>();
            int toWrite = 1;

            while (true) {
                samRecord = readNextRecordInSequence(recordIterator);
                if (samRecord == null) {
                    for (int i = toWrite; i <= this.sequenceLength; i++){
                        Double x = m.remove(i);
                        if (x == null){
                            x = 0.0;
                        }
                        try {
                            out.writeDouble(x);
                            //System.out.println(x);
                        } catch (IOException e) {
                            log.error("Problem writing data", e);
                        }
                    }
                    break;
                }

                int alignmentStart = samRecord.getAlignmentStart();
                int alignmentEnd = samRecord.getAlignmentEnd();

                for(int i = toWrite; i < alignmentStart; i++){
                	Double x = m.remove(i);
                	if (x == null){
                		x = 0.0;
                	}
                    try {
                        out.writeDouble(x);
//                        System.out.println(x);
                    } catch (IOException e) {
                        log.error("Problem writing data", e);
                    }
                }
                toWrite = alignmentStart;

                for(int i = alignmentStart; i <= alignmentEnd; i++){
                	Double count = m.get(i);
                	if(count == null){
                		m.put(i,1.0);
                	} else {
                		m.put(i,count + 1);
                	}
                }

            }

        }
        finally {

            // close closeable iterator -- must do
            if (recordIterator != null) recordIterator.close();

            // cleanup output
            closeOutput();
        }


    }

    private SAMRecord readNextRecordInSequence(CloseableIterator<SAMRecord> recordIterator) {
        SAMRecord sam = null;
        if (recordIterator.hasNext()) {
            sam = recordIterator.next();
            // check for the right sequence
            if (sam.getReferenceName().equals(sequence)) {
                if (!sequenceFound) sequenceFound = true;
                return sam;
            }
            else {
                // we can bail now since we've gone past all entries in our sequence
                if (sequenceFound) return null;
            }
        }
        return null;
    }

    private void output(double value) {
        try {
            out.writeDouble(value);
        } catch (IOException e) {
            e.printStackTrace();  // TODO: log properly
        }
    }

    private void initOutput() {

        try {
            // open output stream
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));

            // write file type header
            FileTypeHeader fileTypeHeader = new FileTypeHeader(FileType.CONTINUOUS_GENERIC, 1);
            out.writeInt(fileTypeHeader.fileType.ordinal());
            out.writeInt(fileTypeHeader.version);

            // prepare and write fields header
            fields = new ArrayList<FieldType>();
            fields.add(FieldType.DOUBLE);
            modifiers = new ArrayList<Object>();
            modifiers.add(null);
            out.writeInt(fields.size());
            for (FieldType ft : fields) {
                out.writeInt(ft.ordinal());
            }


        } catch (IOException e) {
            e.printStackTrace();  // TODO: log properly
        }
    }

    private void closeOutput() {
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();  // TODO: log properly
        }
    }
    private String guessSequence(int sequenceLength) {

        SAMFileHeader fileHeader = samFileReader.getFileHeader();
        SAMSequenceDictionary sequenceDictionary = fileHeader.getSequenceDictionary();
        // find the first sequence with the smallest difference in length from our reference sequence
        int leastDifferenceInSequenceLength = Integer.MAX_VALUE;
        int closestSequenceIndex = Integer.MAX_VALUE;
        int i = 0;
        for (SAMSequenceRecord sequenceRecord : sequenceDictionary.getSequences()) {
            int lengthDelta = Math.abs(sequenceRecord.getSequenceLength() - sequenceLength);
            if (lengthDelta < leastDifferenceInSequenceLength) {
                leastDifferenceInSequenceLength = lengthDelta;
                closestSequenceIndex = i;
            }
            i++;
        }
        if (closestSequenceIndex != Integer.MAX_VALUE) {
            return sequenceDictionary.getSequence(closestSequenceIndex).getSequenceName();
        }
        return null;

    }

    public static void main(String args[]) {
        if (args.length != 5) {
            System.out.println("Missing argument: BAM, index file, output file, reference sequence name (may be \"null\") and length required");
            System.exit(1);
        }
        BAMToCoverage instance = new BAMToCoverage(args[0], args[1], args[2], args[3], Integer.parseInt(args[4]));
        instance.format();
    }
}
