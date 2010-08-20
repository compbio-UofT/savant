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

import java.util.List;
import savant.format.SavantFile;
import savant.format.SavantUnsupportedVersionException;
import savant.format.header.FileType;
import savant.util.SavantFileFormatterUtils;
import savant.util.Range;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import savant.format.SavantFileFormatter;

/**
 * TODO:
 * @author vwilliams
 */
public class BFASTASequenceTrack implements SequenceTrack {

    private int length = -1;
    SavantFile dFile;

    public BFASTASequenceTrack(String fileName) throws IOException, SavantUnsupportedVersionException {
        this.dFile = new SavantFile(fileName, FileType.SEQUENCE_FASTA);
    }

    public int getLength(String refname) {
        if (dFile == null) {
            this.length = -1;
        }
        Long[] vals = this.getReferenceMap().get(refname);
        this.length = (int) (vals[1] / SavantFileFormatterUtils.BYTE_FIELD_SIZE);

        return this.length;
    }

    public String getSequence(String reference, Range range) {
        
        int rangeLength = range.getLength();
        byte[] sequence = new byte[rangeLength];
        try {
            if (this.getReferenceMap().containsKey(reference)) {
                dFile.seek(reference, SavantFileFormatterUtils.BYTE_FIELD_SIZE*range.getFrom()-1);

                for (int i = 0; i < rangeLength; i++) {
                    try {
                        sequence[i] = dFile.readByte();
                    } catch (IOException e) { break; }
                }
            } else {
                return null;
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

    public Set<String> getReferenceNames() {
        return this.getReferenceMap().keySet();
    }

    private Map<String,Long[]> getReferenceMap() {
        return this.dFile.getReferenceMap();
    }

}
