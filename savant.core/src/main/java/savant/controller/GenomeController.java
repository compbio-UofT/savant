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
package savant.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.Listener;
import savant.api.event.GenomeChangedEvent;
import savant.api.event.TrackEvent;
import savant.data.types.Genome;
import savant.util.Controller;
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
