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
 * BAMToTuples.java
 * Created on Mar 1, 2010
 */

package savant.tools;

import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;

import java.io.*;

public class BAMToTuples {

    private SAMFileReader samFileReader;
    private int sequenceLength;

    private String sequence;

    private String outFile;
    private Writer writer;

    public BAMToTuples(String inFile, String indexFile, String outFile, int sequenceLength) {

        this.samFileReader = new SAMFileReader(new File(inFile), new File(indexFile));
        this.sequenceLength = sequenceLength;
        this.outFile = outFile;

        initOutput();

    }

    public void format() {

        // figure out what sequence we're talking about
        sequence = guessSequence(sequenceLength);

        CloseableIterator<SAMRecord> recordIterator = null;
        try {
            // get record iterator
            recordIterator=samFileReader.iterator();

            boolean finished = false;
            SAMRecord samRecord = null;

            /*
            // DEBUG:
            int reads = 0;
            // END DEBUG
            */

            while (!finished) {

                samRecord = readNextRecordInSequence(recordIterator);

                /*
                // DEBUG
                reads++;
                System.out.println("READS=" + reads);


                if (reads == 1000) {
                    break;
                }
                // END DEBUG
                */

                if (samRecord == null) {
                    // we've reached the end of the file
                    finished = true;
                }

                if (!finished) {
                    outputTuples(samRecord);
                }
            }

            closeOutput();
        }
        finally {

            // close closeable iterator -- must do
            if (recordIterator != null) recordIterator.close();

        }

    }
    
    private SAMRecord readNextRecordInSequence(CloseableIterator<SAMRecord> recordIterator) {
        SAMRecord sam = null;
        while (recordIterator.hasNext()) {
            sam = recordIterator.next();
            if (sam.getReferenceName().equals(sequence)) {
                if (!sam.getReadUnmappedFlag()) return sam;
            }
        }
        return null;
    }

    private void initOutput() {
        try {
            writer = new BufferedWriter(new FileWriter(new File(outFile)));
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void outputTuples(SAMRecord samRecord) {

        try {
            /*System.out.println("Start: " + samRecord.getAlignmentStart() + " End: " + samRecord.getAlignmentEnd()); */

            writer.write(Integer.toString(samRecord.getAlignmentStart()) + " " + "1\n");
            writer.write(Integer.toString(samRecord.getAlignmentEnd()) + " " + "0\n");
            
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void closeOutput() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
        if (args.length != 4) {
            System.out.println("Missing argument: BAM, index file, output file, and reference sequence length required");
        }
        BAMToTuples instance = new BAMToTuples(args[0], args[1], args[2], Integer.parseInt(args[3]));
        instance.format();
    }

}
