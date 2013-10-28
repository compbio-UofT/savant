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
    public byte[] getSequence(String ref, RangeAdapter range) throws IOException, InterruptedException;

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
