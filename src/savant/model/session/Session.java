/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.model.session;

import java.util.List;
import savant.data.types.Genome;
import savant.util.Range;

/**
 *
 * @author mfiume
 */
public class Session {

    public String genomeName;
    public Genome genome;
    public String reference;
    public Range range;
    public List<String> trackPaths;
    public String bookmarkPath;

    public Session(String genomeName, Genome g, List<String> tracknames, String reference, Range range, String bookmarks) {
        this.genomeName = genomeName;
        this.genome = g;
        this.trackPaths = tracknames;
        this.reference = reference;
        this.range = range;
        this.bookmarkPath = bookmarks;
    }

    
}
