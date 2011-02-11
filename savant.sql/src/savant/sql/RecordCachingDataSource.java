/*
 *    Copyright 2011 University of Toronto
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

package savant.sql;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.data.sources.DataSource;
import savant.data.types.Record;
import savant.file.DataFormat;
import savant.util.Resolution;

/**
 * Class which maintains a record-based cache of data.  This is intended for the
 * SQL DataSource Plugin, but could be repurposed for other DataSource classes.
 *
 * @author tarkvara
 */
public class RecordCachingDataSource<E extends Record> implements DataSource<E> {
    private static final Log LOG = LogFactory.getLog(RecordCachingDataSource.class);

    DataSource<E> source;
    Map<String, RecordCache<E>> wholeCache = new HashMap<String, RecordCache<E>>();

    /**
     * Construct a RecordCachingDataSource by wrapping it around an existing DataSource.
     *
     * @param ds the DataSource to be accessed whenever we get a cache miss
     */
    public RecordCachingDataSource(DataSource<E> ds) {
        source = ds;
    }

    /**
     * Get all records in the given range at the given resolution
     *
     * @param ref the reference sequence name for which to fetch records
     * @param range
     * @param res
     * @return an ordered list of records
     */
    @Override
    public List<E> getRecords(String ref, RangeAdapter range, Resolution res) throws IOException {
        List<E> result = null;
        String subID = ref + ":" + res;
        RecordCache subCache = wholeCache.get(subID);
        if (subCache == null) {
            subCache = new RecordCache<E>(source, ref, res);
            wholeCache.put(subID, subCache);
        }
        result = subCache.getRecords(range);
        return result;
    }

    @Override
    public Set<String> getReferenceNames() {
        return source.getReferenceNames();
    }

    @Override
    public URI getURI() {
        return source.getURI();
    }

    @Override
    public String getName() {
        return source.getName() + " (cached)";
    }

    @Override
    public void close() {
        source.close();
    }

    @Override
    public DataFormat getDataFormat() {
        return source.getDataFormat();
    }
}
