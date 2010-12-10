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

package savant.api.util;

import java.io.File;
import java.io.IOException;

import savant.api.adapter.GenomeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.controller.ReferenceController;
import savant.data.types.Genome;
import savant.exception.SavantTrackCreationCancelledException;
import savant.view.swing.Track;

/**
 *
 * @author tarkvara
 */
public class GenomeUtils {

    private static ReferenceController refc = ReferenceController.getInstance();

    /**
     * Tell whether a genome has been loaded yet
     * @return Whether or not a genome has been loaded yet
     */
    public static boolean isGenomeLoaded() {
        return refc.isGenomeLoaded();
    }

    /**
     * Get the loaded genome.
     * @return The loaded genome
     */
    public static GenomeAdapter getGenome() {
        return refc.getGenome();
    }

    /**
     * Set the genome
     * @param genome The genome to set
     */
    public static void setGenome(GenomeAdapter genome) {
        refc.setGenome((Genome) genome);
    }


    /**
     * Create a placeholder genome with the given name and length.
     * @throws IOException
     */
    public static GenomeAdapter createGenome(String name, long length) {
        return new Genome(name, length);
    }

    /**
     * Create a new genome from the given track full of sequence data.
     * 
     * @param seqTrack a track containing sequence information
     * @return a genome object for the given sequence
     */
    public static GenomeAdapter createGenome(TrackAdapter seqTrack) {
        return Track.createGenome((Track)seqTrack);
    }


    /**
     * Create a new genome from the given file full of sequence data.
     *
     * @param f a file containing sequence information
     * @return a genome object for the given sequence
     */
    public static GenomeAdapter createGenome(File f) throws IOException, SavantTrackCreationCancelledException {
        return createGenome(TrackUtils.createTrack(f).get(0));
    }
}
