/*
 * ReferenceController.java
 * Created on Jan 19, 2010
 *
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

package savant.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import savant.api.util.DialogUtils;
import savant.controller.event.reference.ReferenceChangedEvent;
import savant.controller.event.reference.ReferenceChangedListener;
import savant.data.types.Genome;
import savant.util.MiscUtils;

/**
 * Controller object to manage changes to viewed range.
 *
 * @author vwilliams
 */
public class ReferenceController {

    private Genome loadedGenome;

    private static ReferenceController instance;

    private List<ReferenceChangedListener> referenceChangedListeners;

    /** The maximum and current viewable range */
    private String currentReference;

    public static synchronized ReferenceController getInstance() {
        if (instance == null) {
            instance = new ReferenceController();
        }
        return instance;
    }

    private ReferenceController() {
        //genomicReferences = new HashSet<String>();
        //otherReferences = new HashSet<String>();
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
            if (TrackController.getInstance().getDataSources().size() > 0) {
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
        all.addAll(this.loadedGenome.getReferenceNames());
        all.addAll(getNonGenomicReferenceNames());
        return all;
    }

    public Set<String> getReferenceNames() {
        return this.loadedGenome.getReferenceNames();
    }

    public Set<String> getNonGenomicReferenceNames() {

        return new HashSet<String>();

        /*
        Set<String> genomicReferences = getReferenceNames();
        Set<String> nonGenomicReferences = new HashSet<String>();

        List<ViewTrack> tracks = ViewTrackController.getInstance().getTracks();

        for (ViewTrack t : tracks) {

            RecordTrack rt = t.getTrack();

            System.out.println("Track name: " + t.getName());

            //if (rt == null) { continue; }
            //System.out.println("\tGetting reference names from " + rt.toString());

            Set<String> refs = rt.getReferenceNames();
            for (String r : refs) {
                if (!genomicReferences.contains(r)) {
                    nonGenomicReferences.add(r);
                }
            }
        }

        return nonGenomicReferences;
         */
    }


    /**
     * Fire the ReferenceChangedEvent
     */
    private synchronized void fireReferenceChangedEvent() {
        ReferenceChangedEvent evt = new ReferenceChangedEvent(this, this.currentReference);
        Iterator listeners = this.referenceChangedListeners.iterator();
        while (listeners.hasNext()) {
            ((ReferenceChangedListener) listeners.next()).referenceChangeReceived(evt);
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
        return getGenome() != null;
    }

    /**
     * Get the loaded genome.
     * @return The loaded genome
     */
    public Genome getGenome() {
        return this.loadedGenome;
    }

    public void setGenome(Genome genome) {
        this.loadedGenome = genome;

        // Auto-select the first reference on the new genome.
        String ref = MiscUtils.set2List(this.loadedGenome.getReferenceNames()).get(0);
        setReference(ref, true);
    }
}
