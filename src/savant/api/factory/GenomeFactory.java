/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.api.factory;

import java.io.IOException;
import savant.api.adapter.GenomeAdapter;
import savant.data.types.Genome;

/**
 *
 * @author mfiume
 */
public class GenomeFactory {

    public GenomeAdapter createGenome(String name, long length) throws IOException {
        return new Genome(name, length);
    }

}
