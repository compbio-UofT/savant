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
package savant.util;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;


/**
 * Creates a .fai index file for a .fasta file.
 * @author tarkvara
 */
public class FastaUtils {
    
    public static Map<String, IndexEntry> createIndex(URI uri, File destFile) throws IOException {
        InputStream input = null;
        OutputStream faiOutput = null;

        try {
            input = new BufferedInputStream(NetworkUtils.getSeekableStreamForURI(uri));
            // Index file is expected to be .fa.fai.
            faiOutput = new FileOutputStream(destFile.getAbsolutePath());

            long readPos = 0;
            String s;
            int lineLen = -1;
            
            String ref = null;
            long refStart = -1;
            int numBases = -1;
            List<IndexEntry> entries = new ArrayList<IndexEntry>();

            while ((s = IOUtils.readLine(input)) != null) {
                if (s.charAt(0) == '>') {
                    if (ref != null) {
                        entries.add(new IndexEntry(ref, numBases, refStart, lineLen));
                    }
                    refStart = readPos + s.length() + 1;
                    int spacePos = s.indexOf(' ');
                    if (spacePos < 0) {
                        spacePos = s.length();
                    }
                    ref = s.substring(1, spacePos);
                    numBases = 0;
                } else {
                    if (lineLen < 0) {
                        lineLen = s.length();
                    }
                    numBases += s.length();
                }
                readPos += s.length() + 1;
            }
            // Write the entry for the last chromosome.
            if (ref != null) {
                entries.add(new IndexEntry(ref, numBases, refStart, lineLen));
            }
            Map<String, IndexEntry> sortedEntryMap = sortEntries(entries);
            for(IndexEntry entry : entries){
                faiOutput.write(String.format("%s\t%d\t%d\t%d\t%d\n", entry.reference, entry.length, entry.offset, entry.lineLength, entry.lineLength + 1).getBytes());
            }
            return sortedEntryMap;
        } finally {
            if (input != null) {
                input.close();
                if (faiOutput != null) {
                    faiOutput.close();
                }
            }
        }
    }
    
    public static Map<String, IndexEntry> readIndex(File faiFile) throws IOException {
        BufferedReader faiReader = null;
        try {
            faiReader = new BufferedReader(new FileReader(faiFile));
            String line;
            List<IndexEntry> unsortedEntries = new ArrayList<IndexEntry>();
            while ((line = faiReader.readLine()) != null) {
                String[] fields = StringUtils.split(line, '\t');
                unsortedEntries.add(new IndexEntry(fields[0], Integer.valueOf(fields[1]), Long.valueOf(fields[2]), Integer.valueOf(fields[3])));
            }
            return sortEntries(unsortedEntries);
        } finally {
            if (faiReader != null) {
                faiReader.close();
            }
        }
    }
    
    private static Map<String, IndexEntry> sortEntries(List<IndexEntry> unsortedEntries) {
        // Make sure the references are sorted in a human-friendly order.
        Collections.sort(unsortedEntries, new Comparator<IndexEntry>() {
            private ReferenceComparator comparator = new ReferenceComparator();

            @Override
            public int compare(IndexEntry t1, IndexEntry t2) {
                return comparator.compare(t1.reference, t2.reference);
            }
        });

        Map<String, IndexEntry> referenceMap = new LinkedHashMap<String, IndexEntry>();
        for (IndexEntry entry: unsortedEntries) {
            referenceMap.put(entry.reference, entry);
        }
        return referenceMap;
    }

    public static class IndexEntry {
        public final String reference;
        public final int length;
        public final long offset;
        public final int lineLength;
        
        IndexEntry(String ref, int l, long off, int line) {
            reference = ref;
            length = l;
            offset = off;
            lineLength = line;
        }
    }
}
