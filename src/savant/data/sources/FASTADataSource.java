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
package savant.data.sources;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.data.types.SequenceRecord;
import savant.file.DataFormat;
import savant.file.FileType;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantROFile;
import savant.file.SavantUnsupportedVersionException;
import savant.format.SavantFileFormatterUtils;
import savant.util.MiscUtils;
import savant.util.Resolution;

/**
 *
 * @author mfiume
 */
public class FASTADataSource implements DataSource<SequenceRecord> {

    private static Log LOG = LogFactory.getLog(FASTADataSource.class);

    private int length = -1;
    private SavantROFile dFile;

    public FASTADataSource(URI uri) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this.dFile = new SavantROFile(uri, FileType.SEQUENCE_FASTA);
    }

    public int getLength(String refname) {
        if (dFile == null) {
            this.length = -1;
        }
        long[] vals = this.getReferenceMap().get(refname);
        this.length = (int) (vals[1] / SavantFileFormatterUtils.BYTE_FIELD_SIZE);

        return this.length;
    }

    @Override
    public List<SequenceRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {

        int rangeLength = range.getLength();
        byte[] sequence = new byte[rangeLength];
        if (this.getReferenceMap().containsKey(reference)) {
            // -1 because the file is 0 based
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

    private Map<String, long[]> getReferenceMap() {
        return dFile.getReferenceMap();
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

    @Override
    public final DataFormat getDataFormat() {
        return DataFormat.SEQUENCE_FASTA;
    }
}
