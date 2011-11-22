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

package savant.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.settings.DirectorySettings;


/**
 * Index shared by CacheableSABS and files exported by Tool plugins.
 *
 * @author tarkvara
 */
public class CacheIndex {
    private static final Log LOG = LogFactory.getLog(CacheIndex.class);

    /** So multiple threads don't try updating the cache index at the same time. */
    private static final Object indexLock = new Object();

    /**
     * Get the cache file corresponding to the given path.
     * @param url URL whose hash we use to determine changes
     * @param source path to our resource (often the same as url)
     * @param bufferSize block size of buffer (0 if not using block-based cacheing)
     * @param length file length (written to index but not actually used)
     * @return
     * @throws IOException 
     */
    public static File getCacheFile(URL url, String source, int bufferSize, long length) throws IOException {
        synchronized (indexLock) {
            //create index
            File cacheDir = DirectorySettings.getCacheDirectory();
            File index = new File(cacheDir, "cacheIndex");
            index.createNewFile();

            // Check for entry
            String newETag = NetworkUtils.getHash(url);
            boolean entryFound = false;
            boolean entryInvalid = false;
            BufferedReader bufferedReader = new BufferedReader(new FileReader(index));
            String line = null;
            List<String> allLines = new ArrayList<String>();
            File cacheFile = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (!entryFound) {
                    String[] lineArray = line.split(",");

                    if (source.equals(lineArray[0])) {
                        entryFound = true;

                        // Equivalent entry found
                        cacheFile = new File(lineArray[4]);

                        // Compare ETags and buffer sizes.  Could also check file lengths (lineArray[2]), but
                        // we currently don't do that, since the ETag should reflect such a change.
                        if (!lineArray[1].equals(newETag) || bufferSize != Integer.parseInt(lineArray[3])) {
                            // ETag changed or new buffer size.  Cache file is invalid.
                            LOG.info("Removed out-of-date cache file " + cacheFile + " for " + url);
                            entryInvalid = true;
                            cacheFile.delete();
                            continue;
                        }
                    }
                }
                allLines.add(line);
            }


            if (entryInvalid) {
                // We've invalidated a cache entry, so rewrite the index file without the entry.
                BufferedWriter out = new BufferedWriter(new FileWriter(index, false));
                for (String l: allLines) {
                    out.write(l);
                    out.newLine();
                }
                out.close();
            }

            // Add entry
            if (entryInvalid || !entryFound) {

                // Special case.  If it's a non-blocked file, write -1 for bufferSize so that we know the file has not
                // yet been completely written.
                if (bufferSize == 0) {
                    bufferSize = -1;
                }
                BufferedWriter out = new BufferedWriter(new FileWriter(index, true));
                cacheFile = new File(cacheDir, source.replaceAll("[\\:/]", "+"));
                out.write(source + "," +
                        newETag + "," +
                        length + "," +
                        bufferSize + "," +
                        cacheFile);               // replace all instances of \/:*?"<>|
                out.newLine();
                out.close();
            }
            
            return cacheFile;
        }
    }

    /**
     * For un-blocked files we use the bufferSize to indicate whether the file has been
     * completely downloaded.  It is set to -1 when the download starts and to 0 when it completes.
     *
     * @param path the path of the cache file
     * @throws IOException 
     */
    public static void updateCacheEntry(String path) throws IOException {
        synchronized (indexLock) {
            //create index
            File cacheDir = DirectorySettings.getCacheDirectory();
            File index = new File(cacheDir, "cacheIndex");
            index.createNewFile();

            // Check for entry
            BufferedReader bufferedReader = new BufferedReader(new FileReader(index));
            String line = null;
            List<String> allLines = new ArrayList<String>();
            String[] updatedLine = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (updatedLine == null) {
                    String[] lineArray = line.split(",");

                    if (path.equals(lineArray[4])) {
                        updatedLine = lineArray;
                        continue;
                    }
                }
                allLines.add(line);
            }

            // We're updating a cache entry, so rewrite the index file without the entry.
            BufferedWriter out = new BufferedWriter(new FileWriter(index, false));
            for (String l: allLines) {
                out.write(l);
                out.newLine();
            }
            if (updatedLine != null) {
                out.write(updatedLine[0] + "," + updatedLine[1] + "," + updatedLine[2] + ",0," + updatedLine[4]);
                out.newLine();
            }
            out.close();
        }
    }
    
    /**
     * Determine whether there is a valid entry for the given source URL.
     * @param source URL of the source
     * @return true if we successfully found the source in our index
     */
    public static boolean findCacheEntry(String source) {
        BufferedReader reader = null;
        try {
            File cacheDir = DirectorySettings.getCacheDirectory();
            File index = new File(cacheDir, "cacheIndex");
            index.createNewFile();

            // Check for entry
            reader = new BufferedReader(new FileReader(index));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] lineArray = line.split(",");

                if (source.equals(lineArray[0]) && !lineArray[3].equals("-1")) {
                    return true;
                }
            }
        } catch (IOException x) {
            LOG.error("Error reading cache.", x);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return false;
    }
}
