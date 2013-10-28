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
import savant.api.data.Record;


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
    private List<String> references;
    private PreparedStatement prep;

    protected SQLDataSource(MappedTable table, List<String> references) {
        this.table = table;
        this.columns = table.mapping;
        this.references = references;
    }

    public ResultSet executeQuery(String chrom, int start, int end) throws SQLException {
        if (columns.chrom != null) {
            // Normal track, where all the data is in one table.
            if (prep == null) {
                prep = table.database.prepareStatement("SELECT * FROM %s WHERE %s = ? AND ((%s >= ? AND %s <= ?) OR (%s >= ? AND %s <= ?) OR (%s < ? AND %s > ?)) ORDER BY %s", table.name, columns.chrom,
                columns.start, columns.start, columns.end, columns.end, columns.start, columns.end, columns.start);
            }
            prep.setString(1, chrom);
            prep.setInt(2, start);
            prep.setInt(3, end);
            prep.setInt(4, start);
            prep.setInt(5, end);
            prep.setInt(6, start);
            prep.setInt(7, end);
        } else {
            // For some UCSC tracks, the data for each chromosome is in a separate table (e.g. chr1_rmsk, chr2_rmsk,...).
            if (prep == null) {
                prep = table.database.prepareStatement("SELECT * FROM %s WHERE ((%s >= ? AND %s <= ?) OR (%s >= ? AND %s <= ?) OR (%s < ? AND %s > ?)) ORDER BY %s",
                       chrom + "_" + table.trackName, columns.start, columns.start, columns.end, columns.end, columns.start, columns.end, columns.start);
            }
            prep.setInt(1, start);
            prep.setInt(2, end);
            prep.setInt(3, start);
            prep.setInt(4, end);
            prep.setInt(5, start);
            prep.setInt(6, end);
        }
        return prep.executeQuery();
    }

    @Override
    public Set<String> getReferenceNames() {
        Set<String> result = new LinkedHashSet<String>();
        result.addAll(references);
        return result;
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
     * For SQL tracks, our dictionary is the database, so this method has nothing to do.
     */
    @Override
    public void loadDictionary() {
    }

    /**
     * Look up the given key in the database.  The key passed by Savant will be lower
     * case and will use '*' for wild-cards.  The following implementation depends on
     * the fact that the MySQL <code>LIKE</code> is case-insensitive.
     * @param key a lookup string typed into the Savant navigation box
     * @return list of bookmarks which can be used to populate the combo-box
     */
    @Override
    public List<BookmarkAdapter> lookup(String key) {
        List<BookmarkAdapter> result = new ArrayList<BookmarkAdapter>();
        try {
            if (columns.name != null) {
                long t0 = System.currentTimeMillis();
                String where;
                if (key.indexOf('*') >= 0) {
                    key = key.replace('*', '%');
                    where = String.format("%s LIKE '%s'", columns.name, key);
                    if (columns.name2 != null) {
                        where += String.format(" OR %s LIKE '%s'", columns.name2, key);
                    }
                } else {
                    // Exact match.  Doesn't actually happen with our current Savant UI.
                    where = String.format("%s='%s'", columns.name, key);
                    if (columns.name2 != null) {
                        where += String.format(" OR %s='%s'", columns.name2, key);
                    }
                }
                ResultSet rs;
                if (columns.chrom == null) {
                    // For some UCSC tracks, the data is divided over multiple tables (e.g. chr1_rmsk, chr2_rmsk,...).
                    StringBuilder query  = new StringBuilder();
                    for (String ref: getReferenceNames()) {
                        if (!ref.contains("random")) {
                            if (query.length() > 0) {
                                query.append(" UNION ");
                            }
                            query.append(String.format("SELECT '%s',%s,%s,%s FROM %s_%s WHERE %s", ref, columns.start, columns.end, columns.name, ref, table.trackName, where));
                        }
                    }
                    rs = table.database.executeQuery(query.toString());
                } else {
                    rs = table.database.executeQuery("SELECT %s,%s,%s,%s FROM %s WHERE %s", columns.chrom, columns.start, columns.end, columns.name, table.name, where);
                }
                while (rs.next()) {
                    String chrom = rs.getString(1).intern();
                    int start = rs.getInt(2);
                    int end = rs.getInt(3);
                    String name = rs.getString(4);
                    BookmarkAdapter mark = BookmarkUtils.createBookmark(chrom, RangeUtils.createRange(start, end), name);

                    result.add(mark);
                }
                rs.close();
                LOG.info("Found " + result.size() + " bookmarks for " + table.trackName + " in " + (System.currentTimeMillis() - t0) + " ms");
            }
        } catch (SQLException x) {
            LOG.error("Lookup for \"" + key + "\" failed.", x);
        }
        return result;
    }
}
