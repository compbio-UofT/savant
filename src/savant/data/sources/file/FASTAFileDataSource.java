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

package savant.data.sources.file;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.data.sources.FASTADataSource;
import savant.data.types.SequenceRecord;
import savant.file.FileType;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantROFile;
import savant.file.SavantUnsupportedVersionException;
import savant.format.SavantFileFormatterUtils;
import savant.util.MiscUtils;
import savant.util.Resolution;


/**
 * Class which represents a FASTA file serving as a data source.
 *
 * @author vwilliams
 */
public class FASTAFileDataSource extends FASTADataSource {

    private static Log log = LogFactory.getLog(FASTAFileDataSource.class);

    private int length = -1;
    private SavantROFile dFile;

    public FASTAFileDataSource(URI uri) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this.dFile = new SavantROFile(uri, FileType.SEQUENCE_FASTA);
    }

    public int getLength(String refname) {
        if (dFile == null) {
            this.length = -1;
        }
        Long[] vals = this.getReferenceMap().get(refname);
        this.length = (int) (vals[1] / SavantFileFormatterUtils.BYTE_FIELD_SIZE);

        return this.length;
    }

    @Override
    public List<SequenceRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {

        int rangeLength = range.getLengthAsInt();
        byte[] sequence = new byte[rangeLength];
        if (this.getReferenceMap().containsKey(reference)) {
            dFile.seek(reference, SavantFileFormatterUtils.BYTE_FIELD_SIZE*range.getFrom()-1);

            for (int i = 0; i < rangeLength; i++) {
                try {
                    sequence[i] = (byte)Character.toUpperCase(dFile.readByte());
                } catch (IOException e) { break; }
            }
        } else {
            return null;
        }

        ArrayList<SequenceRecord> result = new ArrayList<SequenceRecord>();
        result.add(SequenceRecord.valueOf(reference, sequence));
        return result;
    }

    @Override
    public void close() {
        if (dFile != null) {
            try {
                dFile.close();
            } catch (IOException ex) {}
        }
    }

    @Override
    public Set<String> getReferenceNames() {
        return this.getReferenceMap().keySet();
    }

    private Map<String,Long[]> getReferenceMap() {
        return this.dFile.getReferenceMap();
    }

    @Override
    public URI getURI() {
        //System.out.println("Getting URI for FASTA file: " + dFile.getURI());
        return dFile.getURI();
    }

    @Override
    public String getName() {
        return MiscUtils.getNeatPathFromURI(getURI());
    }
}
