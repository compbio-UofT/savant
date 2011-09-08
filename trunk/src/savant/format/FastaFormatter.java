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

package savant.format;

import java.io.*;
import java.util.StringTokenizer;

import savant.file.FileType;


public class FastaFormatter extends SavantFileFormatter {
    public static final int RECORDS_PER_INTERRUPT_CHECK = 1000;

    public FastaFormatter(File inFile, File outFile) {
        super(inFile, outFile, FileType.SEQUENCE_FASTA);
    }

    @Override
    public void format() throws IOException, InterruptedException, SavantFileFormattingException {

        // set the input file size (for tracking progress)
        totalBytes = inFile.length();

        // open the input file
        inFileReader = openInputFile();

        DataOutputStream output = null;

        setSubtaskStatus("Processing input file ...");
        incrementOverallProgress();

        //Read File Line By Line
        try {
            String strLine;
            boolean done = false;
            String ref = null;


            while (!done) {
                for (int i=0; i<RECORDS_PER_INTERRUPT_CHECK; i++) {
                    if ((strLine = inFileReader.readLine()) == null) {
                        done = true;
                        break;
                    }
                    // update bytes read from input
                    byteCount += strLine.getBytes().length;

                    // Set the correct output stream
                    if (strLine.length() > 0 && strLine.charAt(0) == '>') {

                        if (output != null) {
                            output.close();
                        }

                        ref = strLine.substring(1).trim();
                        StringTokenizer st = new StringTokenizer(ref);
                        ref = st.nextToken();

                        output = getOutputForReference(ref);
                        LOG.debug("New reference found: " + ref);

                    } else {

                        if (output == null) {
                            LOG.error("No header line found");
                            throw new SavantFileFormattingException("No FASTA line found.");
                        }

                        // generate output
                        if (strLine.length() > 0 && !strLine.matches("\\s*") && strLine.charAt(0) != '>') {
                            output.writeBytes(strLine);
                        }
                    }
                }
                // check to see if format has been cancelled
                if (Thread.interrupted()) { throw new InterruptedException(); }
                
                // update progress property for UI
                setSubtaskProgress(getProgressAsInteger(byteCount, totalBytes));
            }

            if (output != null) {
                output.close();
            }

            // write the output file
            writeOutputFile();
        }
        finally {
            inFileReader.close();

            //closeOutputStreams();
            //this.closeOutputFiles();
        }
    }


}
