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
package savant.api.util;

import java.io.File;

import savant.api.adapter.GenomeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.event.GenomeChangedEvent;
import savant.controller.GenomeController;
import savant.data.types.Genome;
import savant.view.tracks.Track;


/**
 * Utility methods for dealing with Savant genomes.
 * @author tarkvara
 */
public class GenomeUtils {

    private static GenomeController genomeController = GenomeController.getInstance();

    /**
     * Tell whether Savant has loaded a genome yet.
     *
     * @return true if a genome is loaded
     */
    public static boolean isGenomeLoaded() {
        return genomeController.isGenomeLoaded();
    }

    /**
     * Get the loaded genome.
     *
     * @return the loaded genome
     */
    public static GenomeAdapter getGenome() {
        return genomeController.getGenome();
    }

    /**
     * Set the current genome.
     *
     * @param genome the genome to set
     */
    public static void setGenome(GenomeAdapter genome) {
        genomeController.setGenome((Genome) genome);
    }


    /**
     * Create a placeholder genome with the given name and length.
     */
    public static GenomeAdapter createGenome(String name, int length) {
        return new Genome(name, length);
    }

    /**
     * Create a new genome from the given track full of sequence data.
     * 
     * @param seqTrack a track containing sequence information
     * @return a genome object for the given sequence
     */
    public static GenomeAdapter createGenome(TrackAdapter seqTrack) {
        return Genome.createFromTrack((Track)seqTrack);
    }


    /**
     * Create a new genome from the given file full of sequence data.
     *
     * @param f a file containing sequence information
     * @return a genome object for the given sequence
     */
    public static GenomeAdapter createGenome(File f) throws Throwable {
        return createGenome(TrackUtils.createTrack(f)[0]);
    }
    
    /**
     * Add a listener to monitor changes in the reference genome.
     *
     * @param l the listener to be added
     */
    public static void addGenomeChangedListener(Listener<GenomeChangedEvent> l) {
        genomeController.addListener(l);
    }

    /**
     * Remove a genome change listener.
     *
     * @param l the listener to be removed
     */
    public static void removeGenomeChangedListener(Listener<GenomeChangedEvent> l) {
        genomeController.removeListener(l);
    }
}
