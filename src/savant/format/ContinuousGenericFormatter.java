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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import savant.format.header.FileType;
import savant.format.util.data.FieldType;
import savant.util.MiscUtils;
import savant.util.SavantFileFormatterUtils;

public class ContinuousGenericFormatter extends SavantFileFormatter {

    public static final int RECORDS_PER_INTERRUPT_CHECK = 100;

    public ContinuousGenericFormatter(String inFile, String outFile) {
        super(inFile, outFile, FileType.CONTINUOUS_GENERIC);
       // this.inFilePath = inFile;
        //this.out = outFile;
    }

    public void format() throws IOException, InterruptedException{

        // Initialize the total size of the input file, for purposes of tracking progress
        this.totalBytes = new File(inFilePath).length();

        inFileReader = this.openInputFile();

        DataOutputStream outfile = null;

        List<FieldType> readfields = new ArrayList<FieldType>();
        readfields.add(FieldType.STRING);
        readfields.add(FieldType.FLOAT);

        fields = new ArrayList<FieldType>();
        fields.add(FieldType.FLOAT);

        modifiers = new ArrayList<Object>();
        modifiers.add(null);

        try {
            String strLine;
            List<Object> line;
            List<Object> entry = new ArrayList<Object>();

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
                    if ((line = SavantFileFormatterUtils.parseTxtLine(strLine, readfields)) != null && line.size() > 0) {
                        outfile = this.getFileForReference((String) line.get(0));
                        entry.add(line.get(1));
                        SavantFileFormatterUtils.writeBinaryRecord(outfile, entry, fields, modifiers);
                        entry.remove(0);
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

            // map of reference name -> multiresolution *index* filename
            Map<String,String> refnameToIndexFileNameMap = ContinuousFormatterHelper.makeMultiResolutionContinuousFiles(referenceName2FilenameMap);

            List<String> refnames = MiscUtils.set2List(this.referenceName2FilenameMap.keySet());
            this.writeContinuousOutputFile(refnames, refnameToIndexFileNameMap, this.referenceName2FilenameMap);

        } finally {
            inFileReader.close();
        }
    }

}
