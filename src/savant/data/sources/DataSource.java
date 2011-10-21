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

package savant.data.sources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.samtools.util.BlockCompressedInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.BookmarkAdapter;
import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.util.RangeUtils;
import savant.controller.BookmarkController;
import savant.api.data.Record;
import savant.util.Bookmark;
import savant.util.IOUtils;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;


/**
 * Interface for a data source which contains records associated with a reference sequence.
 *
 * @param <E> record type
 */
public abstract class DataSource<E extends Record> implements DataSourceAdapter {
    private static final Log LOG = LogFactory.getLog(DataSource.class);

    /**
     * Dictionary which keeps track of gene names and other searchable items for this track.
     * Note that regardless of their original case, all keys are stored as lower-case.
     */
    private TreeMap<String, List<BookmarkAdapter>> dictionary;

    /**
     * So that we know how many entries this dictionary has.
     */
    private int dictionaryCount;

    @Override
    public String getName() {
        return MiscUtils.getNeatPathFromURI(getURI());
    }
    
    /**
     * Get the dictionary for performing lookups on the associated track.  Default
     * behaviour is to load it from a .dict file in the same location as the main URI.
     *
     * @return a dictionary containing the bookmarks (may be empty, but never null)
     */
    @Override
    public void loadDictionary() throws IOException {
        TreeMap<String, List<BookmarkAdapter>> newDict = new TreeMap<String, List<BookmarkAdapter>>();
        dictionaryCount = 0;
        URI dictionaryURI = URI.create(getURI().toString() + ".dict");
        if (NetworkUtils.exists(dictionaryURI)) {
            LOG.info("Starting to load dictionary from " + dictionaryURI);
            int lineNum = 0;
            try {
                String line = null;
                InputStream input = new BlockCompressedInputStream(NetworkUtils.getSeekableStreamForURI(dictionaryURI));
                while ((line = IOUtils.readLine(input)) != null) {
                    String[] words = line.split("\\t");
                    String key = words[0].toLowerCase();
                    List<BookmarkAdapter> marks = newDict.get(key);
                    if (marks == null) {
                        marks = new ArrayList<BookmarkAdapter>();
                        newDict.put(key, marks);
                    }
                    Bookmark newMark = new Bookmark(words[1], words[0]);
                    for (BookmarkAdapter m: marks) {
                        if (m.getReference().equals(newMark.getReference()) && RangeUtils.intersects(m.getRange(), newMark.getRange())) {
                            RangeAdapter newRange = RangeUtils.union(m.getRange(), newMark.getRange());
                            m.setRange(newRange);
                            newMark = null;
                            break;
                        }
                    }
                    if (newMark != null) {
                        marks.add(newMark);
                        dictionaryCount++;
                    }
                    lineNum++;
                }
                dictionary = newDict;
            } catch (ParseException x) {
                throw new IOException("Parse error in dictionary at line " + lineNum, x);
            } catch (NumberFormatException x) {
                throw new IOException("Parse error in dictionary at line " + lineNum, x);
            }
            LOG.info("Finished loading dictionary from " + dictionaryURI);
        }
    }

    @Override
    public List<BookmarkAdapter> lookup(String key) {
        if (dictionary != null) {
            if (key.endsWith("*")) {
                // Looking for a prefix-match.  Get the submap from k0 (inclusive) to k1 (exclusive);
                String k0 = key.substring(0, key.length() - 1);
                String k1 = k0 + Character.MAX_VALUE;
                Map<String, List<BookmarkAdapter>> subDict = dictionary.subMap(k0, k1);
                List<BookmarkAdapter> result = new ArrayList<BookmarkAdapter>();
                for (List<BookmarkAdapter> bms: subDict.values()) {
                    result.addAll(bms);
                }
                return result;
            } else {
                // Looking for an exact match.
                return dictionary.get(key);
            }
        }
        return null;
    }
    
    public int getDictionaryCount() {
        return dictionaryCount;
    }

    public void addDictionaryToBookmarks() {
        if (dictionary != null) {
            for (String k: dictionary.keySet()) {
                for (BookmarkAdapter b: dictionary.get(k)) {
                    BookmarkController.getInstance().addBookmark((Bookmark)b);
                }
            }
        }
    }
}
