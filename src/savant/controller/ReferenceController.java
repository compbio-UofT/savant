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

package savant.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import savant.api.util.DialogUtils;
import savant.controller.event.GenomeChangedEvent;
import savant.controller.event.ReferenceChangedEvent;
import savant.controller.event.ReferenceChangedListener;
import savant.data.types.Genome;
import savant.util.MiscUtils;
import savant.view.swing.sequence.SequenceTrack;


/**
 * Controller object to manage changes to current chromosome.
 *
 * @author vwilliams
 */
public class ReferenceController {

    private static ReferenceController instance;

    private Genome loadedGenome;

    private List<ReferenceChangedListener> referenceChangedListeners;

    private String currentReference;

    public static synchronized ReferenceController getInstance() {
        if (instance == null) {
            instance = new ReferenceController();
        }
        return instance;
    }

    private ReferenceController() {
        referenceChangedListeners = new ArrayList<ReferenceChangedListener>();
    }

    /**
     * Set the new reference, firing a ReferenceChangedEvent if appropriate.
     *
     * @param ref           the new reference
     * @param forceEvent    if true, always fire a ReferenceChangedEvent #303
     */
    public void setReference(String ref, boolean forceEvent) {
        if (getAllReferenceNames().contains(ref)) {
            if (forceEvent || !ref.equals(currentReference)) {
                currentReference = ref;
                fireReferenceChangedEvent();
            }
        } else {
            if (DataSourceController.getInstance().getDataSources().size() > 0) {
                DialogUtils.displayMessage("Reference " + ref + " not found in loaded tracks.");
            }
        }
    }

    /**
     * Set the new reference, firing a ReferenceChangedEvent only if the reference
     * has actually changed.
     *
     * @param ref   the new reference
     */
    public void setReference(String ref) {
        setReference(ref, false);
    }

    public String getReferenceName() {
        return this.currentReference;
    }

    public Set<String> getAllReferenceNames() {
        Set<String> all = new HashSet<String>();
        all.addAll(loadedGenome.getReferenceNames());
        all.addAll(getNonGenomicReferenceNames());
        return all;
    }

    public Set<String> getReferenceNames() {
        return loadedGenome.getReferenceNames();
    }

    public int getReferenceLength(String refname) {
        return loadedGenome.getLength(refname);
    }

    public Set<String> getNonGenomicReferenceNames() {
        return new HashSet<String>();
    }


    /**
     * Fire the ReferenceChangedEvent
     */
    private synchronized void fireReferenceChangedEvent() {
        ReferenceChangedEvent evt = new ReferenceChangedEvent(currentReference);
        for (ReferenceChangedListener l: referenceChangedListeners) {
            l.referenceChanged(evt);
        }
    }

    public synchronized void addReferenceChangedListener(ReferenceChangedListener l) {
        referenceChangedListeners.add(l);
    }

    public synchronized void removeReferenceChangedListener(ReferenceChangedListener l) {
        referenceChangedListeners.remove(l);
    }

    /**
     * Get whether or not a genome has been loaded.
     * @return True iff a genome has been loaded
     */
    public boolean isGenomeLoaded() {
        return loadedGenome != null;
    }

    /**
     * Get the loaded genome.
     * @return The loaded genome
     */
    public Genome getGenome() {
        return loadedGenome;
    }

    public synchronized void setGenome(Genome genome) {
        if (genome == null) {
            // Sometimes we need to clear out the current genome in preparation for loading a new one.
            loadedGenome = null;
        } else {
            Genome oldGenome = loadedGenome;
            if (!genome.equals(oldGenome)) {
                loadedGenome = genome;
                fireGenomeChangedEvent(oldGenome);

                // Auto-select the first reference on the new genome.
                String ref = MiscUtils.set2List(loadedGenome.getReferenceNames()).get(0);
                setReference(ref, true);
            }
        }
    }

    private void fireGenomeChangedEvent(Genome oldGenome) {
        GenomeChangedEvent evt = new GenomeChangedEvent(oldGenome, loadedGenome);
        for (ReferenceChangedListener l: referenceChangedListeners) {
            l.genomeChanged(evt);
        }
    }


    public synchronized void setSequence(SequenceTrack t) {
        if (loadedGenome == null) {
            setGenome(Genome.createFromTrack(t));
        } else if (!loadedGenome.isSequenceSet()) {
            // We have a loaded genome, but no sequence yet.  Plug it in.  Listeners can recognise this
            // event because the oldGenome and the newGenome on the GenomeChangedEvent will be the same.
            loadedGenome.setSequenceTrack(t);
            fireGenomeChangedEvent(loadedGenome);
        }
    }
}
