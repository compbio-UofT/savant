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

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * @author mfiume, vwilliams
 */
public class Genome
{
    private String name;
    private BFASTASequenceTrack sequenceTrack;
    private int length;

    public Genome(String filename) throws FileNotFoundException, IOException {
        int lastSlashIndex = filename.lastIndexOf(System.getProperty("file.separator"));
        setName( filename.substring(lastSlashIndex+1, filename.length()));
        sequenceTrack = new BFASTASequenceTrack(filename);
        setSequenceTrack(sequenceTrack);
        setLength(sequenceTrack.getLength());
    }

    public Genome(String name, int length) throws IOException {
        setName(name);
        setLength(length);
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




    private void setSequenceTrack(BFASTASequenceTrack sequenceTrack) {
        this.sequenceTrack = sequenceTrack;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName() { return this.name; }

    public void setLength(int length)
    {
        this.length = length;
    }

    public String getSequence(Range range) throws IOException
    {
        if (isSequenceSet()) return sequenceTrack.getSequence(range);
        else return null;
    }

    public int getLength()
    {
        return this.length;
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

}
