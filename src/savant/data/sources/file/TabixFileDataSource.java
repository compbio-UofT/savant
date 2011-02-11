/*
 * TabixFileDataSource.java
 * Created on Jan 28, 2010
 *
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

package savant.data.sources.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.data.sources.TabixDataSource;
import savant.data.types.TabixIntervalRecord;
import savant.file.TabixReader;
import savant.util.MiscUtils;
import savant.util.Resolution;

/**
 * @author mfiume
 */
public class TabixFileDataSource extends TabixDataSource {

    private static Log log = LogFactory.getLog(TabixFileDataSource.class);

    TabixReader tr;
    private URI uri;

//    private String fileNameOrURL;

    public static TabixFileDataSource fromURI(URI uri) throws IOException {

        if (uri == null) throw new IllegalArgumentException("Invalid argument: URI must be non-null");

        File indexFile = null;
        // if no exception is thrown, this is an absolute URL
        String scheme = uri.getScheme();
        if ("http".equals(scheme) || "ftp".equals(scheme)) {
            indexFile = getIndexFileCached(uri);
            if (indexFile != null) {
                return new TabixFileDataSource(uri, indexFile);
            }
        }

        // infer index file name from track filename
        File infile = new File(uri);
        indexFile = getTabixIndexFileLocal(infile);
        if (indexFile != null) {
            return new TabixFileDataSource(infile, indexFile);
        }

        // no success
        return null;
    }

    public TabixFileDataSource(File file) throws IOException {
        this(file, getTabixIndexFileLocal(file));
    }

    public TabixFileDataSource(File file, File index) throws IOException {

        if (file == null) throw new IllegalArgumentException("File must not be null.");
        if (index == null) throw new IllegalArgumentException("Index file must not be null");

        this.uri = file.toURI().normalize();

        tr = new TabixReader(file.getAbsolutePath());
    }

    public TabixFileDataSource(URI uri) throws IOException {
        this(uri, getIndexFileCached(uri));
    }
    
    public TabixFileDataSource(URI uri, File index) throws IOException {

        if (uri == null) throw new IllegalArgumentException("URI must not be null");
        if (index == null) throw new IllegalArgumentException("Index file must not be null");

        this.uri = uri.normalize();

        tr = new TabixReader(uri.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TabixIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws OutOfMemoryError {
        List<TabixIntervalRecord> result = new ArrayList<TabixIntervalRecord>();
        try {
            TabixReader.Iterator i = tr.query(reference + ":" + range.getFrom() + "-" + (range.getTo()+1));
            
            if (i != null) {
                String n = null;
                while ((n = i.next()) != null) {
                    result.add(TabixIntervalRecord.valueOf(n));
                }
            }
        } catch (IOException ex) {
            log.error(ex);
        }
        return result;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {}

    Set<String> referenceNames;
    @Override
    public Set<String> getReferenceNames() {
        // TODO: actually populate
        if (this.referenceNames == null) {
            referenceNames = new HashSet<String>();
        }
        return referenceNames;
    }

    private static File getTabixIndexFileLocal(File bamFile) {
        String bamPath = bamFile.getAbsolutePath();
        File indexFile = new File(bamPath + ".tbi");
        if (indexFile.exists()) {
            return indexFile;
        }
        else {
            // try alternate index file name
            indexFile = new File(bamPath.replace(".gz", ".tbi"));
            if (indexFile.exists()) {
                return indexFile;
            }
        }
        return null;
    }

    private static File getIndexFileCached(URI bamURI) throws IOException {
        return IndexCache.getInstance().getIndex(bamURI,"tbi","gz");
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public String getName() {
        return MiscUtils.getNeatPathFromURI(getURI());
    }
}
