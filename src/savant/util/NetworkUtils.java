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
import java.net.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.sf.samtools.util.SeekableFileStream;
import net.sf.samtools.util.SeekableHTTPStream;
import net.sf.samtools.util.SeekableStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPFile;

import savant.api.util.Listener;
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
    private static final int BUF_SIZE = 8192;         // 8kB buffer

    static {
        // Create a trust manager that does not validate certificate chains.
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                
                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            LOG.info("Setting default SSL socket factory...");
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception x) {
            LOG.error("Unable to set socket factory.", x);
        }
    }

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
        } else if (proto.equals("file")) {
            // Cheesy fake hash-code based on the modification time and size.
            try {
                File f = new File(url.toURI());
                return String.format("%016x-%016x", f.lastModified(), f.length());
            } catch (URISyntaxException x) {
                throw new IllegalArgumentException("Invalid argument; cannot parse " + url + " as a file.");
            }
        } else {
            throw new IllegalArgumentException("Invalid argument; cannot get hash for " + proto + " URLs.");
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
     * Extract the file extension from the given URL.
     *
     * @param url The URL from which to extract the extension
     * @return The extension of the URL
     */
    public static String getExtension(URL url) {
        return MiscUtils.getExtension(url.toString());
    }

    /**
     * Extract the file-name portion of a URI.
     * 
     * @param uri the URI to be processed
     * @return the file-name portion of the URI
     */
    public static String getFileName(URI uri) {
        String path = uri.toString();
        int lastSlashIndex = path.lastIndexOf("/");
        return path.substring(lastSlashIndex + 1, path.length());
    }

    /**
     * If u is a file:// URI, return the absolute path.  If it's a network URI, leave
     * it unchanged.
     *
     * @param u the URI to be neatened
     * @return a canonical string representing the URI.
     */
    public static String getNeatPathFromURI(URI u) {
        if(u == null) { return ""; }
        if ("file".equals(u.getScheme())) {
            return (new File(u)).getAbsolutePath();
        }
        return u.toString();
     }

    /**
     * Get a URI from a string.  In most cases, this is just the plain URI, but in the
     * case of file-paths, this may include escaping special characters.  Effectively
     * the inverse of getNeatPathFromURI().
     *
     * @param fileOrURI a string containing either a URI or a file-system path.
     * @return a properly-formed URI
     */
    public static URI getURIFromPath(String fileOrURI) {
        URI uri = null;
        try {
            uri = new URI(fileOrURI);
            if (uri.getScheme() == null) {
                uri = new File(fileOrURI).toURI();
            }
        } catch (URISyntaxException usx) {
            // This can happen if we're passed a file-name containing spaces.
            uri = new File(fileOrURI).toURI();
        }
        return uri;
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
            String baseStr = base.toString();
            if (!baseStr.endsWith("/")) {
                baseStr += "/";
            }
            return new URL(baseStr + spec);
        } catch (MalformedURLException ignored) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Synchronously download the given URL to the given destination directory.
     *
     * @param u the URL to be downloaded
     * @param destDir the destination directory
     * @param fileName the destination file within <code>destDir</code>; use <code>null</code> to infer the name from the URL
     * @return the downloaded file
     */
    public static File downloadFile(URL u, File destDir, String fileName) throws IOException {
        File f = new File(destDir, fileName != null ? fileName : MiscUtils.getFilenameFromPath(u.getPath()));

        InputStream in = NetworkUtils.openStream(u);
        OutputStream out = new FileOutputStream(f);
        byte[] buf = new byte[BUF_SIZE];
        int bytesRead;
        while ((bytesRead = in.read(buf)) != -1) {
            out.write(buf, 0, bytesRead);
        }

        return f;
    }

    /**
     * Synchronously download a (small) file and read its contents to a String.
     *
     * @param u the URL to be downloaded
     * @return a string containing the contents of the URL
     */
    public static String downloadFile(URL u) throws IOException {

        StringBuilder result = new StringBuilder();

        InputStream in = NetworkUtils.openStream(u);
        byte[] buf = new byte[BUF_SIZE];
        int bytesRead;
        while ((bytesRead = in.read(buf)) != -1) {
            char [] r = (new String(buf)).toCharArray();
            result.append(r, 0, bytesRead);
        }

        return result.toString();
    }

    /**
     * Download a file in the background.  Notification events will be sent to the
     * supplied listener.
     *
     * @param u the HTTP URL to be downloaded
     * @param destDir destination directory for the file
     * @param fileName the destination file within <code>destDir</code>; use <code>null</code> to infer the name from the URL
     * @param monitor will receive DownloadEvents
     */
    public static void downloadFile(final URL u, final File destDir, final String fileName, final DownloadMonitor monitor) {
        new Thread("NetworkUtils.downloadFile") {
            double totalBytes;

            @Override
            public void run() {
                try {
                    HttpURLConnection httpConn = (HttpURLConnection) u.openConnection();
                    totalBytes = httpConn.getContentLength();

                    File destFile = new File(destDir, fileName != null ? fileName : MiscUtils.getFilenameFromPath(u.getPath()));
                    OutputStream out = new FileOutputStream(destFile);
                    InputStream in = NetworkUtils.openStream(u);
                    fireDownloadEvent(monitor, new DownloadEvent(DownloadEvent.Type.STARTED));
                    byte[] buf = new byte[BUF_SIZE];
                    long bytesSoFar = 0;
                    int bytesRead;
                    while ((bytesRead = in.read(buf)) != -1 && !monitor.isCancelled()) {
                        out.write(buf, 0, bytesRead);
                        if (totalBytes > 0.0) {
                            bytesSoFar += bytesRead;
                            monitor.handleEvent(new DownloadEvent(bytesSoFar / totalBytes));
                        }
                    }
                    fireDownloadEvent(monitor, new DownloadEvent(destFile));
                } catch (IOException x) {
                    fireDownloadEvent(monitor, new DownloadEvent(x));
                }
            }
        }.start();
    }

    private static void fireDownloadEvent(final Listener<DownloadEvent> listener, final DownloadEvent e) {
        MiscUtils.invokeLaterIfNecessary(new Runnable() {
            @Override
            public void run() {
                listener.handleEvent(e);
            }
        });
    }
}
