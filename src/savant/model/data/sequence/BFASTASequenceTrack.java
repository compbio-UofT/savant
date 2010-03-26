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
 * BFASTASequenceTrack.java
 * Created on Jan 12, 2010
 */

package savant.model.data.sequence;

import savant.format.SavantFile;
import savant.util.DataFormatUtils;
import savant.util.Range;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * TODO:
 * @author vwilliams
 */
public class BFASTASequenceTrack implements SequenceTrack {

    private int length = -1;
    SavantFile dFile;

    public BFASTASequenceTrack(String fileName) throws FileNotFoundException, IOException {
        this.dFile = new SavantFile(fileName);
    }

    public int getLength() {
        if (this.length == -1) {
            try {
                if (dFile == null) {
                    return -1;
                }
                this.length = ((int) dFile.length()) / DataFormatUtils.BYTE_FIELD_SIZE;
            } catch (IOException ex) {
                return -1;
            }
        }
        return this.length;
    }

    public String getSequence(Range range) {

        int rangeLength = range.getLength();
        byte[] sequence = new byte[rangeLength];
        try {
            dFile.seek(DataFormatUtils.BYTE_FIELD_SIZE*range.getFrom()-1);
            
            for (int i = 0; i < rangeLength; i++) {
                try {
                    sequence[i] = dFile.readByte();
                } catch (IOException e) { break; }
            }
        } catch (IOException e) {
            // TODO: log this properly
            System.err.print(e);
        }

        String s = new String(sequence);
        s = s.toUpperCase();

        return s;
    }

    public void close() {
        if (dFile != null) {
            try {
                dFile.close();
            } catch (IOException ex) {}
        }
    }
}
