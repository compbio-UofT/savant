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

import java.io.*;
import savant.format.header.FileType;
import savant.util.MiscUtils;

public class FastaFormatter extends SavantFileFormatter{

    public static final int RECORDS_PER_INTERRUPT_CHECK = 1000;

    public FastaFormatter(String inFile, String outFile) {
        super(inFile, outFile, FileType.SEQUENCE_FASTA);
    }

    public void format() throws IOException, InterruptedException, SavantFileFormattingException{

        // set the input file size (for tracking progress)
        this.totalBytes = new File(inFilePath).length();

        // open the input file
        inFileReader = this.openInputFile();

        DataOutputStream outfile = null;

        //Read File Line By Line
        try {
            String strLine;
            boolean done = false;

            while (!done) {
                for (int i=0; i<RECORDS_PER_INTERRUPT_CHECK; i++) {
                    if ((strLine = inFileReader.readLine()) == null) {
                        done = true;
                        break;
                    }
                    // update bytes read from input
                    this.byteCount += strLine.getBytes().length;

                    // set the correct output stream
                    if (strLine.length() > 0 && strLine.charAt(0) == '>') {
                        String refname = MiscUtils.removeChar(strLine, '>');
                        refname = refname.replace(' ', '_');
                        outfile = this.getFileForReference(refname);
                    } else {

                        if (outfile == null) {
                            throw new SavantFileFormattingException("no FASTA line found.");
                        }

                        // generate output
                        //outfile.writeBytes(strLine);
                        if (strLine.length() > 0 && !strLine.matches("\\s*") && strLine.charAt(0) != '>') {
                            outfile.writeBytes(strLine);
                        }
                    }
                }
                // check to see if format has been cancelled
                if (Thread.interrupted()) throw new InterruptedException();
                // update progress property for UI
                updateProgress();
            }

            // close output streams; 
            // VERY IMPORTANT THAT THIS HAPPENS BEFORE COPY!
            closeOutputStreams();

            // write the output file
            this.writeOutputFile();
        }
        finally {
            inFileReader.close();

            //closeOutputStreams();
            //this.closeOutputFiles();
        }
    }


}
