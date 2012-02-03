/*
 *    Copyright 2012 University of Toronto
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

package savant.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
                        faiOutput.write(String.format("%s\t%d\t%d\t%d\t%d\n", ref, numBases, refStart, lineLen, lineLen + 1).getBytes());
                        entries.add(new IndexEntry(ref, numBases, refStart, lineLen));
                    }
                    ref = s.substring(1);
                    refStart = readPos + ref.length() + 2;
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
                faiOutput.write(String.format("%s\t%d\t%d\t%d\t%d\n", ref, numBases, refStart, lineLen, lineLen + 1).getBytes());
                entries.add(new IndexEntry(ref, numBases, refStart, lineLen));
            }
            return sortEntries(entries);
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
