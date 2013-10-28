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
package savant.api.adapter;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

import savant.api.data.Record;
import savant.api.data.DataFormat;
import savant.api.util.Resolution;


/**
 * Interface which plugins should use when accessing (or implementing) Savant data sources.
 *
 * @author tarkvara
 */
public interface DataSourceAdapter<E extends Record> {

    /**
     * Get a set of all references associated with this data source.  The set will be appropriately ordered.
     * @return all the references associated with this data source
     */
    public Set<String> getReferenceNames();

    /**
     * Get all records in the given range at the given resolution.
     *
     * @param ref the reference sequence name for which to fetch records
     * @param range the range for which to fetch records
     * @param resolution the resolution currently being viewed (allows <code>DataSource</code>s to substitute a low-resolution rendition if appropriate.
     * @param filter class which allows results to be filtered out (<code>null</code> if no filtering to be done)
     * @return an ordered list of records
     * 
     * @since 2.0.0
     */
    public List<E> getRecords(String ref, RangeAdapter range, Resolution resolution, RecordFilterAdapter<E> filter) throws IOException, InterruptedException;

    public URI getURI();

    public String getName();

    /**
     * Close the source
     */
    public void close();

    public DataFormat getDataFormat();

    /**
     * Get the column names associated with this <code>DataSource</code>.  For most formats,
     * the list of columns is fixed, but for <code>INTERVAL_TABIX</code> it will depend on the columns
     * found in the data file.
     */
    public String[] getColumnNames();

    /**
     * Get the dictionary for performing lookups on the associated track.  Default
     * behaviour is to load it from a .dict file in the same location as the main URI.
     */
    public void loadDictionary() throws IOException;
    
    /**
     * Look up a feature by name.  Implementors should recognise "*" as a wild-card.
     * @param key e.g. a gene name
     * @return a list of features which match <code>key</code>
     */
    public List<BookmarkAdapter> lookup(String key);
}
