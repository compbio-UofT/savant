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
import savant.api.data.DataFormat;
import savant.api.data.IntervalRecord;
import savant.data.types.BEDIntervalRecord;
import savant.file.FileType;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantROFile;
import savant.file.SavantUnsupportedVersionException;
import savant.format.DataFormatter;
import savant.format.IntervalRecordGetter;
import savant.format.IntervalSearchTree;
import savant.util.Range;
import savant.api.util.Resolution;


/**
 * Class to represent an track of generic intervals. Responsible for reading records within a given range.
 *
 * @author vwilliams
 */
public class BEDDataSource extends DataSource<BEDIntervalRecord> {
    private static final Log LOG = LogFactory.getLog(BEDDataSource.class);

    private SavantROFile dFile;

    private Map<String, IntervalSearchTree> refnameToIntervalBSTIndex;

    public BEDDataSource(URI uri) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this.dFile = new SavantROFile(uri, FileType.INTERVAL_BED);
        this.refnameToIntervalBSTIndex = DataFormatter.readIntervalBSTs(this.dFile);
    }

    public IntervalSearchTree getIntervalSearchTreeForReference(String refname){
        return refnameToIntervalBSTIndex.get(refname);
     }

    @Override
    public List<BEDIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {

        IntervalSearchTree ist = getIntervalSearchTreeForReference(reference);

        if (ist == null) { return new ArrayList<BEDIntervalRecord>(); }

        List<IntervalRecord> data = IntervalRecordGetter.getData(dFile, reference, (Range)range, ist.getRoot());

        //TODO: fix me
        List<BEDIntervalRecord> girList = new ArrayList<BEDIntervalRecord>(data.size());
        for (int i = 0; i < data.size(); i++) {
            girList.add((BEDIntervalRecord) data.get(i));
        }

        return girList;
    }

    @Override
    public void close() {
        try {
            this.dFile.close();
        } catch (IOException ex) {
            LOG.error(ex);
        }
    }

    @Override
    public Set<String> getReferenceNames() {
        return dFile.getReferenceMap().keySet();
    }

    @Override
    public URI getURI() {
        return dFile.getURI();
    }

    @Override
    public final DataFormat getDataFormat() {
        return DataFormat.INTERVAL_RICH;
    }

    @Override
    public final String[] getColumnNames() {
        return new String[] {"Reference", "Start", "End", "Name", "Block Count"};
    }
}
