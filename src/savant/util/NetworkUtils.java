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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

import savant.exception.UnknownSchemeException;
import savant.settings.BrowserSettings;


/**
 * Some useful methods for performing network-related functions.
 *
 * @author vwilliams, tarkvara
 */
public class NetworkUtils {
    private static final Log LOG = LogFactory.getLog(NetworkUtils.class);

    private static final int CONNECT_TIMEOUT = 30000; // 30s timeout for making connection
    private static final int READ_TIMEOUT = 30000;    // 30s timeout for reading data

    /**
     * Given a URI, determine whether it exists or not.
     */
    public static boolean exists(URI uri) {
        try {
            if (uri.getScheme().equals("file")) {
                return new File(uri).exists();
            }
            return getHash(uri.toURL()) != null;
        } catch (Exception x) {
            return false;
        }
    }

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
        if (proto.equals("http") || proto.equals("https")) {
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

            try {
                // List the files.  We should only get one match.
                FTPFile[] files = ftp.listFiles(url.getFile());
                if (files.length > 0) {
                    return String.format("%016x-%016x", files[0].getTimestamp().getTimeInMillis(), files[0].getSize());
                } else {
                    throw new IOException("URL not found: " + url);
                }
            } finally {
                ftp.close();
            }
        } else {
            throw new IllegalArgumentException("Invalid argument; cannot get hash for " + proto + " URLs");
        }
    }

    /**
     * Given a URI, return a SeekableStream of the appropriate type.
     *
     * @param uri an ftp:, http:, or file: URI
     * @return a SeekableStream which can be passed to SavantROFile or BAMDataSource
     */
    public static SeekableStream getSeekableStreamForURI(URI uri) throws IOException {
        String proto = uri.getScheme().toLowerCase();
        SeekableStream result;
        if (proto.equals("file")) {
            result = new SeekableFileStream(new File(uri));
        } else {
            if (proto.equals("http") || proto.equals("https")) {
                result = new SeekableHTTPStream(uri.toURL());
            } else if (proto.equals("ftp")) {
                result = new SeekableFTPStream(uri.toURL());
            } else {
                throw new UnknownSchemeException(uri);
            }
            if (BrowserSettings.getCachingEnabled()) {
                //result = new CacheableSABS(result, CacheableSABS.DEFAULT_BLOCK_SIZE, uri);
                result = new CacheableSABS(result, BrowserSettings.getRemoteBufferSize(), uri);
            }
        }
        return result;
    }

    /**
     * Open a stream for the given URL with the CONNECT_TIMEOUT and READ_TIMEOUT.
     * @throws IOException
     */
    public static InputStream openStream(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        return conn.getInputStream();
    }

    /**
     * Create a URL object from a string which we know to be a valid URL.  Avoids having
     * to catch a MalformedURLException which we know will never be thrown.  Intended
     * as the URL equivalent to <code>URL.create()</code>.
     */
    public static URL getKnownGoodURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException ignored) {
            throw new IllegalArgumentException();
        }
    }


    /**
     * Create a URL object from an existing URL and a string which we know to be a valid path.  Avoids having
     * to catch a MalformedURLException which we know will never be thrown.  Intended
     * as the URL equivalent to <code>URL.create()</code>.
     */
    public static URL getKnownGoodURL(URL base, String spec) {
        try {
            return new URL(base, spec);
        } catch (MalformedURLException ignored) {
            throw new IllegalArgumentException();
        }
    }
}
