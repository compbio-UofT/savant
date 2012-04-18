/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.util;

import java.io.*;
import java.net.*;
import java.util.Map;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

/**
 * Wrapper utility class... for interacting with HttpURLConnection.
 *
 * @author Jim Robinson
 * @date 9/22/11
 */
public class HttpUtils {

    private static Logger log = Logger.getLogger(HttpUtils.class);

    public static boolean byteRangeTested = false;
    public static boolean byteRangeTestSuccess = false;
    private static HttpUtils instance;

    private final int MAX_REDIRECTS = 5;


    static {
        synchronized (HttpUtils.class) {
            instance = new HttpUtils();
            CookieHandler.setDefault(new CookieManager());
        }
    }

    /**
     * @return the single instance
     */
    public static HttpUtils getInstance() {
        return instance;
    }

    /**
     * Constructor
     */
    private HttpUtils() {
    }


    public static boolean isURL(String string) {
        String lcString = string.toLowerCase();
        return lcString.startsWith("http://") || lcString.startsWith("https://") || lcString.startsWith("ftp://")
	    || lcString.startsWith("file://");
    }

    public void shutdown() {
        // Do any cleanup required here
    }

    public InputStream openConnectionStream(URL url) throws IOException {
        if (url.getProtocol().toLowerCase().equals("ftp")) {
            return openFtpStream(url);
        } else {
            return openConnection(url, null).getInputStream();
        }
    }

    public InputStream openConnectionStream(URL url, Map<String, String> requestProperties) throws IOException {

        HttpURLConnection conn = openConnection(url, requestProperties);
        return conn.getInputStream();

    }

    private static InputStream openFtpStream(URL url) throws IOException {

        String host = url.getHost();

        FTPClient ftp = new FTPClient();
        ftp.connect(host);
        System.out.println(ftp.getReplyString());

        // After connection attempt, you should check the reply code to verify
        // success.
        int reply = ftp.getReplyCode();

        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            System.err.println("FTP server refused connection.");
            throw new RuntimeException("FTP server refused connection.");
        }

        boolean success = ftp.login("anonymous", "igv-team@broadinstitute.org");
        if (!success) {
            System.err.println("FTP login failed " + ftp.getReplyString());
            throw new RuntimeException("FTP login failed " + ftp.getReplyString());
        }

        // Use passive mode as default because most of us are
        // behind firewalls these days.
        ftp.enterLocalPassiveMode();


        String file = url.getPath();
        System.out.println("Open file: " + file);
        return ftp.retrieveFileStream(file);
    }



/*DEAD    public boolean resourceAvailable(URL url) {

        if (url.getProtocol().toLowerCase().equals("ftp")) {
            return FTPUtils.resourceAvailable(url);
        }

        try {
            HttpURLConnection conn = openConnection(url, null, "HEAD");
            int code = conn.getResponseCode();
            return code == 200;
        } catch (IOException e) {
            return false;
        }
    }*/

    public String getHeaderField(URL url, String key) throws IOException {
        HttpURLConnection conn = openConnection(url, null, "HEAD");
        int code = conn.getResponseCode();
        // TODO -- check code
        return conn.getHeaderField(key);
    }

    public long getContentLength(URL url) throws IOException {
        String contentLengthString = "";

        contentLengthString = getHeaderField(url, "Content-Length");
        if (contentLengthString == null) {
            return -1;
        } else {
            return Long.parseLong(contentLengthString);
        }
    }

    private String readContents(InputStream is) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int b;
        while ((b = bis.read()) >= 0) {
            bos.write(b);
        }
        return new String(bos.toByteArray());
    }

    private String readErrorStream(HttpURLConnection connection) throws IOException {
        InputStream inputStream = null;

        try {
            inputStream = connection.getErrorStream();
            return readContents(inputStream);
        } finally {
            if (inputStream != null) inputStream.close();
        }


    }

    private HttpURLConnection openConnection(URL url, Map<String, String> requestProperties) throws IOException {
        return openConnection(url, requestProperties, "GET");
    }

    private HttpURLConnection openConnection(URL url, Map<String, String> requestProperties, String method) throws IOException {
        return openConnection(url, requestProperties, method, 0);
    }

    /**
     * The "real" connection method
     *
     * @param url
     * @param requestProperties
     * @param method
     * @return
     * @throws java.io.IOException
     */
    private synchronized HttpURLConnection openConnection(
            URL url, Map<String, String> requestProperties, String method, int redirectCount) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(10000);
        conn.setReadTimeout(300000);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Connection", "close");

        if (requestProperties != null) {
            for (Map.Entry<String, String> prop : requestProperties.entrySet()) {
                conn.setRequestProperty(prop.getKey(), prop.getValue());
            }
        }

        if (method.equals("PUT")) {
            return conn;
        } else {
            int code = conn.getResponseCode();

            // Redirects.  These can occur even if followRedirects == true if there is a change in protocol,
            // for example http -> https.
            if (code >= 300 && code < 400) {

                if (redirectCount > MAX_REDIRECTS) {
                    throw new IOException("Too many redirects");
                }

                String newLocation = conn.getHeaderField("Location");
                log.debug("Redirecting to " + newLocation);

                return openConnection(new URL(newLocation), requestProperties, method, redirectCount++);
            }

            // TODO -- handle other response codes.
            else if (code >= 400) {

                String message;
                if (code == 404) {
                    message = "File not found: " + url.toString();
                    throw new FileNotFoundException(message);
                } else {
                    message = conn.getResponseMessage();
                }
                String details = readErrorStream(conn);

                throw new IOException("Server returned error code: " + code + " (" + message + ")");
            }
        }
        return conn;
    }
}
