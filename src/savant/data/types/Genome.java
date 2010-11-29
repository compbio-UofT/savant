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
import savant.data.sources.FASTAFileDataSource;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantUnsupportedVersionException;
import savant.util.Resolution;
import savant.view.dialog.GenomeLengthForm.BuildInfo;
import savant.view.dialog.GenomeLengthForm.ReferenceInfo;
import savant.view.swing.ViewTrack;
import savant.view.swing.sequence.SequenceViewTrack;

/**
 *
 * @author mfiume, vwilliams
 */
public class Genome implements Serializable, GenomeAdapter {
    private String name;
    
    private boolean isAssociatedWithTrack;

    // if associated with track
    private SequenceViewTrack viewTrack;
    private FASTAFileDataSource dataSource;

    // if not associated with track
    private Map<String,Long> referenceMap;

    public Genome(String name, SequenceViewTrack t) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        isAssociatedWithTrack = true;
        setName(name);
        viewTrack = t;
        dataSource = (FASTAFileDataSource) t.getDataSource();//new FASTAFileDataSource(filename);
        setSequenceTrack(dataSource);
    }

    public Genome(BuildInfo bi) throws IOException {
        isAssociatedWithTrack = false;
        setName(bi.name);
        referenceMap = new HashMap<String,Long>();

        for (ReferenceInfo c : bi.chromosomes) {
            referenceMap.put(c.name, c.length);
        }
    }

    public Genome(String name, long length) throws IOException {
        isAssociatedWithTrack = false;
        setName("user defined");
        referenceMap = new HashMap<String,Long>();
        referenceMap.put(name, length);
    }

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

    public String getName() { return this.name; }

    public String getSequence(String reference, RangeAdapter range) throws IOException
    {
        if (!isSequenceSet()) { return null; }
        else { return dataSource.getRecords(reference, range, Resolution.VERY_HIGH).get(0).getSequence(); }
    }

    public long getLength()
    {
        return getLength(ReferenceController.getInstance().getReferenceName());
    }

    public long getLength(String reference)
    {
        if (this.isAssociatedWithTrack) {
            return this.dataSource.getLength(reference);
        } else {
            return this.referenceMap.get(reference).longValue();
        }
    }

    @Override
    public FASTAFileDataSource getTrack() { return this.dataSource; }

    public boolean isSequenceSet()
    {
        return (dataSource != null);
    }

    @Override
    public ViewTrack getViewTrack() {
        return this.viewTrack;
    }

    @Override
    public String toString()
    {
        return getName();
    }

}
