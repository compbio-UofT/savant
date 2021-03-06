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
package savant.sql;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.BookmarkAdapter;
import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.data.DataFormat;
import savant.api.data.Record;
import savant.api.util.Resolution;

/**
 * Class which maintains a record-based cache of data.  This is intended for the
 * SQL DataSource Plugin, but could be repurposed for other DataSource classes.
 *
 * @author tarkvara
 */
public class RecordCachingDataSource<E extends Record> implements DataSourceAdapter<E> {
    private static final Log LOG = LogFactory.getLog(RecordCachingDataSource.class);

    DataSourceAdapter<E> source;
    Map<String, RecordCache<E>> wholeCache = new HashMap<String, RecordCache<E>>();

    /**
     * Construct a RecordCachingDataSource by wrapping it around an existing DataSource.
     *
     * @param ds the DataSource to be accessed whenever we get a cache miss
     */
    public RecordCachingDataSource(DataSourceAdapter<E> ds) {
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
    public List<E> getRecords(String ref, RangeAdapter range, Resolution res, RecordFilterAdapter filt) throws IOException, InterruptedException {
        List<E> result = null;
        String subID = ref + ":" + res;
        RecordCache subCache = wholeCache.get(subID);
        if (subCache == null) {
            subCache = new RecordCache<E>(source, ref, res);
            wholeCache.put(subID, subCache);
        }
        result = subCache.getRecords(range, filt);
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

    @Override
    public String[] getColumnNames() {
        return source.getColumnNames();
    }

    @Override
    public void loadDictionary(){
    }

    @Override
    public List<BookmarkAdapter> lookup(String key) {
        return source.lookup(key);
    }
}
