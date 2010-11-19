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
 * GenericIntervalDataSource.java
 * Created on Jan 12, 2010
 */

package savant.data.sources;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.data.types.GenericIntervalRecord;
import savant.data.types.IntervalRecord;
import savant.file.FileType;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantROFile;
import savant.file.SavantUnsupportedVersionException;
import savant.format.DataFormatter;
import savant.format.IntervalRecordGetter;
import savant.format.IntervalSearchTree;
import savant.util.Range;
import savant.util.Resolution;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to represent an track of generic intervals. Responsible for reading records within a given range.
 * 
 * @author vwilliams
 */
public class GenericIntervalDataSource implements DataSource<GenericIntervalRecord> {

    private static Log log = LogFactory.getLog(GenericIntervalDataSource.class);

    private SavantROFile dFile;

    private Map<String,IntervalSearchTree> refnameToIntervalBSTIndex;

    public GenericIntervalDataSource(URI uri) throws URISyntaxException, IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this.dFile = new SavantROFile(uri, FileType.INTERVAL_GENERIC);
        this.refnameToIntervalBSTIndex = DataFormatter.readIntervalBSTs(this.dFile);

        if (log.isDebugEnabled()) {
            log.debug("HEADER SIZE && : "+  dFile.getFilePointer());

            log.debug("Found indicies for:");
            for (String refname : refnameToIntervalBSTIndex.keySet()) {
                log.debug(refname);
            }
        }

    }

    public IntervalSearchTree getIntervalSearchTreeForReference(String refname) {
        return refnameToIntervalBSTIndex.get(refname);
    }

    @Override
    public List<GenericIntervalRecord> getRecords(String reference, Range range, Resolution resolution) throws IOException {
        List<IntervalRecord> data = null;

        IntervalSearchTree ist = getIntervalSearchTreeForReference(reference);

        if (ist == null) { return new ArrayList<GenericIntervalRecord>(); }
        

        data = IntervalRecordGetter.getData(this.dFile, reference, range, ist.getRoot());

        List<GenericIntervalRecord> girList = new ArrayList<GenericIntervalRecord>(data.size());
        for (int i = 0; i < data.size(); i++) {
            girList.add((GenericIntervalRecord) data.get(i));
        }

        return girList;
    }

    @Override
    public void close() {
        try {
            this.dFile.close();
        } catch (IOException ex) {
            Logger.getLogger(GenericIntervalDataSource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Set<String> getReferenceNames() {
        Map<String, Long[]> refMap = dFile.getReferenceMap();
        return refMap.keySet();
    }

    @Override
    public URI getURI() {
        return dFile.getURI();
    }
}
