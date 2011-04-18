/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.api.adapter;

import java.io.IOException;
import java.util.Set;
import savant.data.sources.DataSource;

/**
 *
 * @author mfiume
 */
public interface GenomeAdapter {

    public Set<String> getReferenceNames();
    public String getName();
    public byte[] getSequence(String reference, RangeAdapter range) throws IOException;
    public long getLength();
    public long getLength(String reference);
    public DataSource getDataSource();
    public boolean isSequenceSet();
    public TrackAdapter getTrack();

}
