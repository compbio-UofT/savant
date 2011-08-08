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
package savant.api.adapter;

import java.io.IOException;
import java.util.Set;


/**
 * Public interface for Savant genome class.
 *
 * @author mfiume
 */
public interface GenomeAdapter {

    /**
     * Get a set of all references associated with this genome.  The set will be appropriately ordered.
     * @return all the references associated with this genome
     */
    public Set<String> getReferenceNames();

    /**
     * Get the name of the genome (e.g.&nbsp;"hg19").
     *
     * @return the name of the genome
     */
    public String getName();

    /**
     * Get the sequence of bases for the given reference and range.  Bases will be stored
     * as the characters 'C', 'T', 'A', or 'G' cast to a byte.
     *
     * @param ref the reference being retrieved
     * @param range the range being retrieved
     * @return the sequence of bases for the given reference and range
     */
    public byte[] getSequence(String ref, RangeAdapter range) throws IOException;

    /**
     * Get the number of bases in the currently-active reference.
     *
     * @return the number of bases in the currently-active reference
     */
    public int getLength();

    /**
     * Get the number of bases in the specified reference.
     *
     * @return the number of bases in the given reference
     */
    public int getLength(String ref);

    /**
     * Get the <code>DataSource</code> associated with this genome's sequence track (if any).
     *
     * @return this genome's <code>DataSource</code> (may be null)
     */
    public DataSourceAdapter getDataSource();

    /**
     * Determine whether this genome has sequence data.  Equivalent to <code>getSequenceTrack() != null</code>.
     * @return true if this genome has sequence data
     */
    public boolean isSequenceSet();

    /**
     * Get the sequence track associated with this genome.
     *
     * @return the <code>SequenceTrack</code> associated with this genome (may be null)
     */
    public TrackAdapter getSequenceTrack();
}
