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

package savant.data.types;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import savant.api.adapter.GenomeAdapter;
import savant.api.adapter.RangeAdapter;
import savant.controller.ReferenceController;
import savant.data.sources.file.FASTAFileDataSource;
import savant.util.Resolution;
import savant.view.dialog.LoadGenomeDialog.BuildInfo;
import savant.view.dialog.LoadGenomeDialog.ReferenceInfo;
import savant.view.swing.Track;
import savant.view.swing.sequence.SequenceTrack;

/**
 *
 * @author mfiume, vwilliams
 */
public class Genome implements Serializable, GenomeAdapter {
    private String name;
    
    private boolean isAssociatedWithTrack;

    // if associated with track
    private SequenceTrack track;
    private FASTAFileDataSource dataSource;

    // if not associated with track
    private Map<String,Long> referenceMap;

    public Genome(String name, SequenceTrack t) {
        isAssociatedWithTrack = true;
        setName(name);
        track = t;
        dataSource = (FASTAFileDataSource) t.getDataSource();//new FASTAFileDataSource(filename);
        setSequenceTrack(dataSource);

        referenceMap = new HashMap<String,Long>();

        for (String refname : this.dataSource.getReferenceNames()) {
            referenceMap.put(refname, (long) this.dataSource.getLength(refname));
        }
    }

    public Genome(BuildInfo bi) throws IOException {
        isAssociatedWithTrack = false;
        setName(bi.name);
        referenceMap = new HashMap<String,Long>();

        for (ReferenceInfo c : bi.chromosomes) {
            referenceMap.put(c.name, c.length);
        }
    }

    public Genome(String name, long length) {
        isAssociatedWithTrack = false;
        setName("user defined");
        referenceMap = new HashMap<String,Long>();
        referenceMap.put(name, length);
    }

    @Override
    public Set<String> getReferenceNames() {
        if (this.isAssociatedWithTrack) {
            return this.dataSource.getReferenceNames();
        } else {
            return this.referenceMap.keySet();
        }
    }

    private void setSequenceTrack(FASTAFileDataSource sequenceTrack) {
        this.dataSource = sequenceTrack;
    }

    private void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() { return this.name; }

    @Override
    public byte[] getSequence(String reference, RangeAdapter range) throws IOException {
        return isSequenceSet() ? dataSource.getRecords(reference, range, Resolution.VERY_HIGH).get(0).getSequence() : null;
    }

    @Override
    public long getLength() {
        return getLength(ReferenceController.getInstance().getReferenceName());
    }

    @Override
    public long getLength(String reference) {
        if (isAssociatedWithTrack) {
            return dataSource.getLength(reference);
        } else {
            return referenceMap.get(reference).longValue();
        }
    }

    @Override
    public FASTAFileDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isSequenceSet() {
        return dataSource != null;
    }

    @Override
    public Track getTrack() {
        return track;
    }

    @Override
    public String toString() {
        return getName();
    }

    public Long getReferenceLength(String refname) {

        System.out.println("requesting length of reference: " + refname);

        if (this.referenceMap.containsKey(refname)) {
            return this.referenceMap.get(refname);
        } else {
            return (long) -1;
        }
    }

}
