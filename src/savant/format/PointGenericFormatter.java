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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import savant.file.FieldType;
import savant.file.FileType;

public class PointGenericFormatter extends SavantFileFormatter {

    private static final int RECORDS_PER_INTERRUPT_CHECK = 100;

    private int baseOffset = 0;

    // todo: remove this contructor, dont make DataFormatter.java do the work
    // of making an offset, just take the boolean, as below
    public PointGenericFormatter(String inFile, String outFile, int baseOffset) {
        super(inFile, outFile, FileType.POINT_GENERIC);
        this.baseOffset = baseOffset;
    }

    public PointGenericFormatter(String inFile, String outFile, boolean isOneBased) {
        super(inFile, outFile, FileType.POINT_GENERIC);
        if (!isOneBased) {
            baseOffset = 1;
        }
    }

     public PointGenericFormatter(String inFile, String outFile) {
        this(inFile, outFile, true);
    }

    public void format() throws IOException, InterruptedException{

        // Initialize the total size of the input file, for purposes of tracking progress
        this.totalBytes = new File(inFilePath).length();

        inFileReader = this.openInputFile();

        DataOutputStream outfile = null;

        fields = new ArrayList<FieldType>();
        fields.add(FieldType.STRING);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.STRING);

        modifiers = new ArrayList<Object>();

        List<Integer> lengths = pointGenericGetStringLengths();
        int referenceLength = lengths.get(0);
        int descriptionLength = lengths.get(1);

        modifiers.add(referenceLength);
        modifiers.add(null);
        modifiers.add(descriptionLength);

        this.setSubtaskStatus("Processing input file ...");
        this.incrementOverallProgress();
        
        //SavantFileFormatterUtils.writeFieldsHeader(out, fields);

        try {
            String strLine;
            List<Object> line;
            boolean done = false;
            while (!done) {

                for (int i=0; i<RECORDS_PER_INTERRUPT_CHECK; i++) {
                    if ((strLine = inFileReader.readLine()) == null) {
                        done = true;
                        break;
                    }
                    // update bytes read from input
                    this.byteCount += strLine.getBytes().length;
                    // parse input and write output
                    if ((line = SavantFileFormatterUtils.parseTxtLine(strLine, fields)) != null) {
                        line.set(1, ((Integer) line.get(1))+ this.baseOffset);
                        outfile = this.getFileForReference((String) line.get(0));
                        
                        SavantFileFormatterUtils.writeBinaryRecord(outfile, line, fields, modifiers);
                    }
                }
                // check to see if format has been cancelled
                if (Thread.interrupted()) throw new InterruptedException();
                // update progress property for UI
                this.setSubtaskProgress(this.getProgressAsInteger(byteCount, totalBytes));
            }

            // close output streams;
            // VERY IMPORTANT THAT THIS HAPPENS BEFORE COPY!
            closeOutputStreams();

            // write the output file
            this.writeOutputFile();
        }
        finally {
            inFileReader.close();


            //this.closeOutputFiles();
        }
    }

    private List<Integer> pointGenericGetStringLengths() throws IOException {

        BufferedReader inputFileL = this.openInputFile();
        String txtLine = "";
        StringTokenizer tok;
        int tokenNum;

        int maxDescriptionLength = Integer.MIN_VALUE;
        int maxReferenceLength = Integer.MIN_VALUE;

        while ((txtLine = inputFileL.readLine()) != null) {

            tok = new StringTokenizer(txtLine,"\t");
            tokenNum = 0;
            String token = "";
            while (tok.hasMoreElements()) {
                token = tok.nextToken();
                //System.out.println("Considering token: " + token);
                if (tokenNum == 0) {
                    int referenceLength = token.length();
                    maxReferenceLength = Math.max(referenceLength, maxReferenceLength);
                }
                else if (tokenNum == 2) {
                    int descriptionLength = token.length();
                    maxDescriptionLength = Math.max(descriptionLength, maxDescriptionLength);
                    break;
                }
                tokenNum++;
            }
        }

        List<Integer> lengths = new ArrayList<Integer>();
        //System.out.println("Max ref: " + maxReferenceLength);
        //System.out.println("Max desc: " + maxDescriptionLength);
        lengths.add(maxReferenceLength);
        lengths.add(maxDescriptionLength);

        return lengths;
    }


}
