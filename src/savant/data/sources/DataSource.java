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
 * DataSource.java
 * Created on Aug 23, 2010
 */

package savant.data.sources;

import savant.data.types.Record;
import savant.util.Range;
import savant.util.Resolution;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Interface for a data source which contains records associated with a reference sequence.
 *
 * @param <E> record type
 */
public interface DataSource<E extends Record> {

    /**
     * Get the list of references for which this RecordTrack contains data
     * @return A set of reference names
     */
    public Set<String> getReferenceNames();

    /**
     * Get all records in the given range at the given resolution
     *
     * @param reference the reference sequence name for which to fetch records
     * @param range
     * @param resolution
     * @return an ordered list of records
     */
    public List<E> getRecords(String reference, Range range, Resolution resolution) throws IOException;

    public URI getURI();
    
    /**
     * Close the source
     */
    public void close();

}
