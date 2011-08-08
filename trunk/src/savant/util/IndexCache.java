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

package savant.util;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.settings.DirectorySettings;


/**
 * Singleton class to manage a cache of indices for remote files.
 *
 * @author vwilliams
 */
public class IndexCache {
    private static final Log LOG = LogFactory.getLog(IndexCache.class);
    private static final int BUF_SIZE = 8 * 1024; // 8K is optimal size for HTTP transfer
    
    private static IndexCache instance;

    private File cacheDir;

    private Properties tags;
    private Properties indices;

    private File tagPropFile = new File(getCacheDir(), "etags");
    private File indexPropFile = new File(getCacheDir(), "indices");

    public static synchronized IndexCache getInstance() {
        if (instance == null) {
            instance = new IndexCache();
        }
        return instance;
    }

    private IndexCache() {}

    /**
     * Get the local index file corresponding to the given URI.
     *
     * @param uri the URI for which we're providing the index
     * @param extension index extension ("bai" or "tbi")
     * @param alternate alternate index extension ("bam" or "gz")
     */
    public File getIndex(URI uri, String extension, String alternate) throws IOException {

        String indexURLString = uri.toString() + "." + extension;
        URL indexURL = new URL(indexURLString);
        String hash = null;

        try {
            hash = NetworkUtils.getHash(indexURL);
        } catch (IOException e) {
            // file probably doesn't exist
            LOG.warn("Remote file can't be accessed: "+ indexURL, e);
        }

        if (hash == null) {
            // File doesn't exist, try alternate.  If it doesn't exist, an exception
            // will be thrown.
            indexURLString = uri.toString().replace("." + alternate, "." + extension);
            indexURL = new URL(indexURLString);
            hash = NetworkUtils.getHash(indexURL);
        }

        String indexFilename = getIndexFileName(uri.toString(), indexURLString);
        File indexFile = new File(getCacheDir(), indexFilename);
        if (indexFile.exists()) {
            String cachedTag = getETagForURL(uri.toString());
            if (cachedTag == null || hash == null || !hash.equals(cachedTag)) {
                indexFile.delete();    
            }
        }
        if (!indexFile.exists()) {
            loadRemoteIndex(indexURL, indexFile);
            setETagForURL(hash, indexURLString);
        }
        return indexFile;
    }

    public void clearCache() {
        IOUtils.deleteDirectory(getCacheDir());
    }

    private synchronized Properties getETags() {
        if (tags == null) {
            tags = loadPropertiesFile(tagPropFile);
        }
        return tags;
    }

    private String getETagForURL(String URL) {
        return getETags().getProperty(URL);
    }

    private void setETagForURL(String etag, String url) {
        if (etag == null) {
            getETags().remove(url);
        } else {
            getETags().put(url, etag);
        }
        writePropertiesToFile(getETags(), tagPropFile);
    }

    private synchronized Properties getIndices() {
        if (indices == null) {
            indices = loadPropertiesFile(indexPropFile);
        }
        return indices;
    }

    private String getIndexForURL(String URL) {
        return getIndices().getProperty(URL);
    }

    private void setIndexForURL(String index, String url) {
        getIndices().put(url, index);
        writePropertiesToFile(getIndices(), indexPropFile);
    }
    
    private File getCacheDir() {
        if (cacheDir == null) {
            cacheDir = new File(DirectorySettings.getSavantDirectory(), "index");
            if (!cacheDir.exists()) {
                cacheDir.mkdir();
            }
        }
        return cacheDir;
    }

    private synchronized String getIndexFileName(String url, String indexURL) {
        String indexFilename = getIndexForURL(url);
        if (indexFilename == null) {
            String extension = MiscUtils.getExtension(indexURL);
            indexFilename = indexURL.substring(0,indexURL.lastIndexOf("."));
            int offset = indexURL.lastIndexOf("/");
            String name = indexURL.substring(offset+1);
            indexFilename = name + System.currentTimeMillis() + "." + extension;
            setIndexForURL(indexFilename, url);
        }
        return indexFilename;
    }
    
    private Properties loadPropertiesFile(File propFile) {

        Properties result = new Properties();

        if (propFile.exists()) {
            
            InputStream is = null;
            try {
                is = new FileInputStream(propFile);
                result.load(is);
            } catch (FileNotFoundException e) {
                // should not happen
                LOG.error("Properties file not found",e);
            } catch (IOException e) {
                LOG.error("Exception reading properties file",e);
            } finally {
                if (is != null) { try { is.close(); } catch (IOException ignore) { }
                }
            }
        }
        return result;
    }

    private void writePropertiesToFile(Properties props, File propFile) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(propFile);
            props.store(os,null);
        } catch (FileNotFoundException e) {
            LOG.error("Unable to save Cache properties file",e);
        } catch (IOException e) {
            LOG.error("Unable to save Cache properties file",e);
        } finally {
            if (os != null) try { os.close(); } catch (IOException ignore) {}
        }
    }

    private void loadRemoteIndex(URL indexURL, File indexFile) throws IOException {
        
        InputStream is = null;
        OutputStream os = null;
        try {
            is = indexURL.openStream();
            os = new FileOutputStream(indexFile);
            byte[] buf = new byte[BUF_SIZE];
            int read;
            while ((read=is.read(buf)) != -1) {
                os.write(buf, 0, read);
            }
        }
        finally {
            if (is != null) try { is.close(); } catch (IOException ignore) {}
            if (os != null) try { os.close(); } catch (IOException ignore) {}
        }
    }
}
