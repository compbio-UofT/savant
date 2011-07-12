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

package savant.sql;

import java.io.IOException;
import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.BookmarkAdapter;
import savant.api.adapter.DataSourceAdapter;
import savant.api.util.BookmarkUtils;
import savant.api.util.RangeUtils;
import savant.data.types.Record;


/**
 * Base-class for DataSources which create Savant record objects from an SQL query.
 * The derived class is responsible for the nastiness of setting up the query and
 * any extra data-munging required.
 *
 * @author tarkvara
 */
abstract class SQLDataSource<E extends Record> implements DataSourceAdapter<E> {
    protected static final Log LOG = LogFactory.getLog(SQLDataSource.class);

    protected MappedTable table;
    protected ColumnMapping columns;
    private Set<String> references = new HashSet<String>();
    private PreparedStatement prep;
    private String lastChrom;

    protected SQLDataSource(MappedTable table, Set<String> references) {
        this.table = table;
        this.columns = table.mapping;
        this.references = references;
    }

    public ResultSet executeQuery(String chrom, long start, long end) throws SQLException {
        if (columns.chrom == null) {
            // For some UCSC tracks, the data is divided over multiple tables (e.g. chr1_rmsk, chr2_rmsk,...).
            return table.database.executeQuery("SELECT * FROM %s WHERE ((%s >= '%d' AND %s <= '%d') OR (%s >= '%d' AND %s <= '%d') OR (%s < '%d' AND %s > '%d')) ORDER BY %s", chrom + "_" + table.trackName,
                columns.start, start, columns.start, end,
                columns.end, start, columns.end, end,
                columns.start, start, columns.end, end, columns.start);
        } else {
            return table.database.executeQuery("SELECT * FROM %s WHERE %s = '%s' AND ((%s >= '%d' AND %s <= '%d') OR (%s >= '%d' AND %s <= '%d') OR (%s < '%d' AND %s > '%d')) ORDER BY %s", table.name, columns.chrom, chrom,
                columns.start, start, columns.start, end,
                columns.end, start, columns.end, end,
                columns.start, start, columns.end, end, columns.start);
        }
    }

    @Override
    public synchronized Set<String> getReferenceNames() {
        return references;
    }

    @Override
    public String getName() {
        return getURI().toString();
    }

    @Override
    public void close() {
        table.closeConnection();
    }

    @Override
    public URI getURI() {
        return table.getURI();
    }

    /**
     * Any table which has a name column can potentially be used as the source for a dictionary.
     *
     * @return a dictionary of name (and name2) bookmarks; may be empty but never null
     */
    @Override
    public Map<String, List<BookmarkAdapter>> loadDictionary() throws IOException {
        try {
            final Map<String, List<BookmarkAdapter>> result = new HashMap<String, List<BookmarkAdapter>>();

            if (columns.name != null) {
                if (columns.chrom == null) {
                    int lastTotal = 0;
                    long t0 = System.currentTimeMillis();
                    // For some UCSC tracks, the data is divided over multiple tables (e.g. chr1_rmsk, chr2_rmsk,...).
                    for (String ref: getReferenceNames()) {
                        // Skip bogus references like chr1random_rmsk
                        if (!ref.contains("random")) {
                            ResultSet rs;
                            if (columns.name2 != null) {
                                rs = table.database.executeQuery("SELECT '%s',%s,%s,%s,%s FROM %s_%s", ref, columns.start, columns.end, columns.name, columns.name2, ref, table.trackName);
                            } else {
                                rs = table.database.executeQuery("SELECT '%s',%s,%s,%s FROM %s_%s", ref, columns.start, columns.end, columns.name, ref, table.trackName);
                            }
                            loadDictionaryEntries(rs, result);
                            rs.close();
                            LOG.info("Loaded " + (result.size() - lastTotal) + " bookmarks for " + ref + "_" + table.trackName);
                            lastTotal = result.size();
                        }
                    }
                    LOG.info("Loaded total of " + result.size() + " bookmarks for " + table.trackName + " in " + (System.currentTimeMillis() - t0) + "ms");
                } else {
                    ResultSet rs;
                    if (columns.name2 != null) {
                        rs = table.database.executeQuery("SELECT %s,%s,%s,%s,%s FROM %s", columns.chrom, columns.start, columns.end, columns.name, columns.name2, table.name);
                    } else {
                        rs = table.database.executeQuery("SELECT %s,%s,%s,%s FROM %s", columns.chrom, columns.start, columns.end, columns.name, table.name);
                    }
                    loadDictionaryEntries(rs, result);
                    rs.close();
                    LOG.info("Loaded " + result.size() + " bookmarks for " + table.trackName);
                }
            }
            return result;
        } catch (SQLException x) {
            throw new IOException(x);
        }
    }

    private void loadDictionaryEntries(ResultSet rs, Map<String, List<BookmarkAdapter>> result) throws SQLException {
        while (rs.next()) {
            String chrom = rs.getString(1).intern();    // There will be zillions of copies of the chromosome name strings, so intern them.
            int start = rs.getInt(2);
            int end = rs.getInt(3);
            String name = rs.getString(4);
            addDictionaryEntry(name, chrom, start, end, result);

            if (columns.name2 != null) {
                String name2 = rs.getString(5);
                addDictionaryEntry(name2, chrom, start, end, result);
            }
        }
    }

    private void addDictionaryEntry(String name, String chrom, int start, int end, Map<String, List<BookmarkAdapter>> result) {
        String key = name.toLowerCase();
        List<BookmarkAdapter> entry = result.get(key);
        BookmarkAdapter mark = BookmarkUtils.createBookmark(chrom, RangeUtils.createRange(start, end));
        if (entry == null) {
            entry = new ArrayList<BookmarkAdapter>();
            result.put(key, entry);
        }
        entry.add(mark);
    }
}
