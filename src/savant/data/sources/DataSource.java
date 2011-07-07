/*
 * DataSource.java
 * Created on Aug 23, 2010
 *
 *
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
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.samtools.util.BlockCompressedInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.data.types.Record;
import savant.file.DataFormat;
import savant.util.Bookmark;
import savant.util.IOUtils;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;
import savant.util.Resolution;


/**
 * Interface for a data source which contains records associated with a reference sequence.
 *
 * @param <E> record type
 */
public abstract class DataSource<E extends Record> {
    private static final Log LOG = LogFactory.getLog(DataSource.class);

    /**
     * Get the list of references for which this RecordTrack contains data.
     * @return A set of reference names
     */
    public abstract Set<String> getReferenceNames();

    /**
     * Get all records in the given range at the given resolution
     *
     * @param reference the reference sequence name for which to fetch records
     * @param range
     * @param resolution
     * @return an ordered list of records
     */
    public abstract List<E> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException;

    public abstract URI getURI();

    public String getName() {
        return MiscUtils.getNeatPathFromURI(getURI());
    }
    
    /**
     * Close the source
     */
    public abstract void close();

    public abstract DataFormat getDataFormat();

    /**
     * Get the column names associated with this <code>DataSource</code>.  For most formats,
     * the list of columns is fixed, but for <code>INTERVAL_TABIX</code> it will depend on the columns
     * found in the data file.
     */
    public abstract String[] getColumnNames();

    /**
     * Get the URI for the dictionary associated with this data-source.
     */
    public URI getDictionaryURI() {
        return URI.create(getURI().toString() + ".dict");
    }

    /**
     * Get the dictionary for performing lookups on the associated track.  Default
     * behaviour is to load it from a .dict file in the same location as the main URI.
     * It is the caller's responsibility to check that the dictionary actually exists before
     * trying to load it.
     *
     * @return a dictionary containing the bookmarks (may be empty, but never null)
     */
    public synchronized Map<String, Bookmark> loadDictionary() throws IOException {
        Map<String, Bookmark> dictionary = new HashMap<String, Bookmark>();
        URI dictionaryURI = getDictionaryURI();
        LOG.info("Starting to load dictionary from " + dictionaryURI);
        int lineNum = 0;
        try {
            String line = null;
            InputStream input = new BlockCompressedInputStream(NetworkUtils.getSeekableStreamForURI(dictionaryURI));
            while ((line = IOUtils.readLine(input)) != null) {
                String[] entry = line.split("\\t");
                dictionary.put(entry[0].toLowerCase(), new Bookmark(entry[1]));
                lineNum++;
            }
        } catch (ParseException x) {
            throw new IOException("Parse error in dictionary at line " + lineNum, x);
        } catch (NumberFormatException x) {
            throw new IOException("Parse error in dictionary at line " + lineNum, x);
        }
        LOG.info("Finished loading dictionary from " + dictionaryURI);
        return dictionary;
    }
}
