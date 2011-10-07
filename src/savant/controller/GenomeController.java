/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.controller;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.event.GenomeChangedEvent;
import savant.controller.event.TrackEvent;
import savant.data.types.Genome;
import savant.util.Controller;
import savant.util.Listener;
import savant.view.swing.Frame;
import savant.view.tracks.SequenceTrack;

/**
 *
 * @author tarkvara
 */
public class GenomeController extends Controller<GenomeChangedEvent> {
    private static final Log LOG = LogFactory.getLog(GenomeController.class);
    private static GenomeController instance;

    /** Current genome. */
    private Genome loadedGenome;

    public static GenomeController getInstance() {
        if (instance == null) {
            instance = new GenomeController();
            TrackController.getInstance().addListener(new Listener<TrackEvent>() {
                @Override
                public void handleEvent(TrackEvent event) {
                    if (event.getType() == TrackEvent.Type.REMOVED && instance.loadedGenome != null && event.getTrack() == instance.loadedGenome.getSequenceTrack()) {
                        instance.setSequence(null);
                    }
                }
            });
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
        //} else if (loadedGenome.getSequenceTrack() != t) {
        } else if (!loadedGenome.isSequenceSet()) {
            loadedGenome.setSequenceTrack(t);
            LOG.info("Firing sequence set/unset event for " + loadedGenome);
            fireEvent(new GenomeChangedEvent(loadedGenome, loadedGenome));
        }
    }
}
