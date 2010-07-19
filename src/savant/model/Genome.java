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

package savant.model;

import savant.model.data.sequence.BFASTASequenceTrack;
import savant.util.Range;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import savant.controller.ReferenceController;
import savant.view.dialog.GenomeLengthForm.BuildInfo;
import savant.view.dialog.GenomeLengthForm.ReferenceInfo;

/**
 *
 * @author mfiume, vwilliams
 */
public class Genome
{
    private String name;
    
    private boolean isAssociatedWithTrack;

    // if associated with track
    private BFASTASequenceTrack sequenceTrack;

    // if not associated with track
    private Map<String,Integer> referenceMap;

    public Genome(String filename) throws IOException {
        isAssociatedWithTrack = true;
        int lastSlashIndex = filename.lastIndexOf(System.getProperty("file.separator"));
        setName( filename.substring(lastSlashIndex+1, filename.length()));
        sequenceTrack = new BFASTASequenceTrack(filename);
        setSequenceTrack(sequenceTrack);
        // get the first reference (in alphanumeric sorted order)
        //String refname = MiscUtils.set2List(sequenceTrack.getReferenceSequenceNames()).get(0);
        //setReferenceName(refname);
    }

    public Genome(BuildInfo bi) throws IOException {
        isAssociatedWithTrack = false;
        setName("user defined");
        referenceMap = new HashMap<String,Integer>();

        for (ReferenceInfo c : bi.chromosomes) {
            referenceMap.put(c.name, c.length);
        }
        //setLength(length);
    }

    public Genome(String name, int length) throws IOException {
        isAssociatedWithTrack = false;
        setName("user defined");
        referenceMap = new HashMap<String,Integer>();
        referenceMap.put(name, length);
        //setLength(length);
    }

    /*
    public Genome(String path) throws IOException {
        this(path, path, -1);
    }

    public Genome(String name, int length) throws IOException {
        this(name, null, length);
    }

    public Genome(int length) throws IOException {
        this("user", null, length);
    }

    public Genome() throws IOException {
        this(null, null, -1);
    }
    
    public Genome(String name, String path, int length) throws IOException
    {
        setName(name);
        if (path != null) {
            BFASTASequenceTrack sequenceTrack = new BFASTASequenceTrack(new File(path));
            setSequenceTrack(sequenceTrack);
            setLength(sequenceTrack.getLength());
        }
        else { setLength(length-1); }
    }
     *
     */

    public Set<String> getReferenceNames() {
        if (this.isAssociatedWithTrack) {
            return this.sequenceTrack.getReferenceSequenceNames();
        } else {
            return this.referenceMap.keySet();
        }
    }

    private void setSequenceTrack(BFASTASequenceTrack sequenceTrack) {
        this.sequenceTrack = sequenceTrack;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName() { return this.name; }

    /*
    public void setLength(int length)
    {
        this.length = length;
    }
     */

    public String getSequence(String reference, Range range) throws IOException
    {
        if (!isSequenceSet()) { return null; }
        else { return sequenceTrack.getSequence(reference,range); }
    }

    public int getLength()
    {
        return getLength(ReferenceController.getInstance().getReferenceName());
    }

    public int getLength(String reference)
    {
        if (this.isAssociatedWithTrack) {
            return this.sequenceTrack.getLength(reference);
        } else {
            return this.referenceMap.get(reference);
        }
    }

    public BFASTASequenceTrack getTrack() { return this.sequenceTrack; }

    public boolean isSequenceSet()
    {
        return (sequenceTrack != null);
    }

    @Override
    public String toString()
    {
        return getName();
    }

    /*
    public void setReferenceName(String refname) {
        this.referenceName = refname;
        setLength(sequenceTrack.getLength(refname));
    }

    public String getReference() {
        return this.referenceName;
    }
     */
}
