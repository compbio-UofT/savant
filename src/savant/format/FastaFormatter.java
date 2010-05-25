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
import java.util.*;
import savant.format.util.data.FieldType;
import savant.util.DataFormatUtils;

public class FastaFormatter extends GenericFormatter{

    public static final int RECORDS_PER_INTERRUPT_CHECK = 100;

    public FastaFormatter(String inFile, DataOutputStream outFile) {
        this.inFile = inFile;
        this.out = outFile;
    }

    public void format() throws IOException, InterruptedException{

        // Initialize the total size of the input file, for purposes of tracking progress
        this.totalBytes = new File(inFile).length();

        inputFile = this.openInputFile();

        fields = new ArrayList<FieldType>();
        DataFormatUtils.writeFieldsHeader(out, fields);

        //Read File Line By Line
        try {
            String strLine;
            boolean done = false;
            while (!done) {
                for (int i=0; i<RECORDS_PER_INTERRUPT_CHECK; i++) {
                    if ((strLine = inputFile.readLine()) == null) {
                        done = true;
                        break;
                    }
                    // update bytes read from input
                    this.byteCount += strLine.getBytes().length;
                    // generate output
                    if (strLine.charAt(0) != '>') {
                        out.writeBytes(strLine);
                    }
                }
                // check to see if format has been cancelled
                if (Thread.interrupted()) throw new InterruptedException();
                // update progress property for UI
                updateProgress();
            }
        }
        finally {
            inputFile.close();
            out.close();

        }

    }


}
