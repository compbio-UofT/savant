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

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import net.sf.samtools.util.SeekableFileStream;
import net.sf.samtools.util.SeekableHTTPStream;
import net.sf.samtools.util.SeekableStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPFile;
import savant.settings.BrowserSettings;


/**
 * Some useful methods for performing network-related functions.
 *
 * @author vwilliams, tarkvara
 */
public class NetworkUtils {

    private static final Log LOG = LogFactory.getLog(NetworkUtils.class);

    /**
     * Get a unique hash representing the contents of this file.  For an HTTP server,
     * this will be the ETag returned in the header; for FTP servers, we create a
     * hash based on the size and modification time.
     *
     * @param url   URL of the file to be hashed.
     * @return  a hash-value "unique" to this file.
     *
     * @throws IOException
     */
    public static String getHash(URL url) throws IOException {
        String proto = url.getProtocol().toLowerCase();
        if (proto.equals("http")) {
            URLConnection conn = null;
            try {
                conn = url.openConnection();
                return conn.getHeaderField("ETag");
            }
            finally {
                if ((conn != null) && (conn instanceof HttpURLConnection)) {
                    ((HttpURLConnection)conn).disconnect();
                }
            }
        } else if (proto.equals("ftp")) {
            SeekableFTPStream ftp = new SeekableFTPStream(url, "anonymous", "");

            // List the files.  We should only get one match.
            FTPFile[] files = ftp.listFiles(url.getFile());
            return String.format("%016x-%016x", files[0].getTimestamp().getTimeInMillis(), files[0].getSize());
        } else {
            throw new IllegalArgumentException("Invalid argument; cannot get hash for " + proto + " URLs");
        }
    }

    /**
     * Given a URI, return a SeekableStream of the appropriate type.
     *
     * @param url an ftp:, http:, or file: URI
     *
     * @return a SeekableStream which can be passed to SavantROFile or BAMDataSource
     */
    public static SeekableStream getSeekableStreamForURI(URI uri) throws IOException, MalformedURLException {
        String scheme = uri.getScheme().toLowerCase();
        SeekableStream result;
        if (scheme.equals("file")) {
            result = new SeekableFileStream(new File(uri));
        } else {
            if (scheme.equals("http")) {
                result = new SeekableHTTPStream(uri.toURL());
            } else if (scheme.equals("ftp")) {
                result = new SeekableFTPStream(uri.toURL());
            } else {
                throw new IllegalArgumentException("Only file:, ftp:, and http: URIs are valid.");
            }
            if (BrowserSettings.getCachingEnabled()) {
                result = new CacheableSABS(result, CacheableSABS.DEFAULT_BLOCK_SIZE, uri);
            }
        }
        return result;
    }
}
