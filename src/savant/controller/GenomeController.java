/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.controller;

import savant.controller.event.GenomeChangedEvent;
import savant.data.types.Genome;
import savant.view.swing.sequence.SequenceTrack;

/**
 *
 * @author tarkvara
 */
public class GenomeController extends Controller<GenomeChangedEvent> {
    private static GenomeController instance;

    /** Current genome. */
    private Genome loadedGenome;

    public static GenomeController getInstance() {
        if (instance == null) {
            instance = new GenomeController();
        }
        return instance;
    }

    private GenomeController() {
    }

    /**
     * Get the loaded genome.
     * @return The loaded genome
     */
    public Genome getGenome() {
        return loadedGenome;
    }

    /**
     * Get whether or not a genome has been loaded.
     * @return True iff a genome has been loaded
     */
    public boolean isGenomeLoaded() {
        return loadedGenome != null;
    }

    public synchronized void setGenome(Genome genome) {
        if (genome == null) {
            // Sometimes we need to clear out the current genome in preparation for loading a new one.
            loadedGenome = null;
        } else {
            Genome oldGenome = loadedGenome;
            if (!genome.equals(oldGenome)) {
                loadedGenome = genome;
                fireEvent(new GenomeChangedEvent(oldGenome, loadedGenome));
            }
        }
    }

    public synchronized void setSequence(SequenceTrack t) {
        if (loadedGenome == null) {
            setGenome(Genome.createFromTrack(t));
        } else if (!loadedGenome.isSequenceSet()) {
            // We have a loaded genome, but no sequence yet.  Plug it in.  Listeners can recognise this
            // event because the oldGenome and the newGenome on the GenomeChangedEvent will be the same.
            loadedGenome.setSequenceTrack(t);
            fireEvent(new GenomeChangedEvent(loadedGenome, loadedGenome));
        }
    }
}
