/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import savant.api.adapter.RecordFilterAdapter;
import savant.api.data.DataFormat;
import savant.api.data.IntervalRecord;
import savant.api.util.Resolution;
import savant.data.types.BEDIntervalRecord;
import savant.file.*;
import savant.format.IntervalRecordGetter;
import savant.format.IntervalSearchTree;
import savant.format.SavantFileFormatter;
import savant.util.Range;


/**
 * Class to represent an track of generic intervals. Responsible for reading records within a given range.
 *
 * @author vwilliams
 */
public class BEDDataSource extends DataSource<BEDIntervalRecord> {
    private static final Log LOG = LogFactory.getLog(BEDDataSource.class);

    private SavantROFile dFile;

    private Map<String, IntervalSearchTree> refnameToIntervalBSTIndex;

    public BEDDataSource(URI uri) throws IOException, SavantFileNotFormattedException {
        dFile = new SavantROFile(uri, FileType.INTERVAL_BED);
        refnameToIntervalBSTIndex = SavantFileFormatter.readIntervalBSTs(dFile);
    }

    public IntervalSearchTree getIntervalSearchTreeForReference(String refname){
        return refnameToIntervalBSTIndex.get(refname);
     }

    @Override
    public List<BEDIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution, RecordFilterAdapter filt) throws IOException {

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
        return DataFormat.RICH_INTERVAL;
    }

    @Override
    public final String[] getColumnNames() {
        return new String[] {"Reference", "Start", "End", "Name", "Block Count"};
    }
}
