/*
 * Copyright (c) 2007-2010 by The Broad Institute, Inc. and the Massachusetts Institute of Technology.
 * All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL), Version 2.1 which
 * is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR WARRANTIES OF
 * ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT
 * OR OTHER DEFECTS, WHETHER OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR
 * RESPECTIVE TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES OF
 * ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES, ECONOMIC
 * DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER THE BROAD OR MIT SHALL
 * BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT SHALL KNOW OF THE POSSIBILITY OF THE
 * FOREGOING.
 */

package org.broad.igv.util;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IGVHttpUtils {
    private static final Log log = LogFactory.getLog(IGVHttpUtils.class);

    /**
     * Proxy settings (can be null)
     */
    private static ProxySettings proxySettings = null;
    static boolean byteRangeTested = false;
    static boolean useByteRange = true;

    public static class ProxySettings {
        boolean auth = false;
        String user;
        String pw;
        boolean useProxy;
        String proxyHost;
        int proxyPort = -1;

        public ProxySettings(boolean useProxy, String user, String pw, boolean auth, String proxyHost, int proxyPort) {
            this.auth = auth;
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.pw = pw;
            this.useProxy = useProxy;
            this.user = user;
        }
    }


    /**
     * Wraps url.openConnection(),  and adds proxy authentication if required.
     *
     * @param url
     * @return
     * @throws java.io.IOException
     */


    public static InputStream openConnectionStream(URL url) throws IOException {
        if (url.getProtocol().toUpperCase().equals("FTP")) {
            return openFtpStream(url);
        } else {
            return openHttpStream(url, (Map<String, String>) null);
        }
    }


    public static InputStream openHttpStream(URL url, Map<String, String> requestProperties) throws IOException {

        HttpURLConnection conn = openConnectionPrivate(url, requestProperties);
        return openHttpStream(url, conn);


    }

    public static InputStream openHttpStream(URL url, HttpURLConnection conn) throws IOException {
        // IF this is a protected directory we will get a 401.  Continue requesting a user / password until
        // the user cancels or the connectino succeeds.

        while (true) {
            InputStream is = null;
            try {
                is = conn.getInputStream();
                return is;
            }
            catch (SocketTimeoutException e) {
                throw e;
            }
            catch (IOException e) {
                if (conn.getResponseCode() == 401) {
                    if (is != null) {
                        is.close();
                    }
                    conn.disconnect();

                    Map<String, String> requestProperties = new HashMap();
                    for (Map.Entry<String, java.util.List<String>> entry : conn.getRequestProperties().entrySet()) {
                        if (entry.getValue().size() > 0) {
                            requestProperties.put(entry.getKey(), entry.getValue().get(0));
                        }
                    }
                    conn.getRequestProperties();
                    conn = openConnectionPrivate(url, null);

                } else {
                    throw e;
                }
            }

        }
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


    private static HttpURLConnection openConnectionPrivate(URL url, Map<String, String> requestProperties) throws IOException {
        HttpURLConnection conn = openConnection(url);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Connection", "close");
        if (requestProperties != null) {
            for (Map.Entry<String, String> prop : requestProperties.entrySet()) {
                conn.setRequestProperty(prop.getKey(), prop.getValue());
            }
        }
        return conn;
    }


    public static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        return conn;
    }


    // TODO -- replace use of sun package

    public static String base64Encode(String str) {
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        byte[] bytes = str.getBytes();
        return encoder.encode(bytes);

    }

    // TODO -- replace use of sun package

    public static String base64Decode(String str) {
        try {
            return new String((new sun.misc.BASE64Decoder()).decodeBuffer(str));
        } catch (IOException e) {
            log.error("Error decoding string: " + str, e);
            return str;
        }
    }


    public static String getETag(URL url) {
        return getHeaderField(url, "ETag");
    }

    public static String getHeaderField(URL url, String name) {

        URLConnection conn = null;
        try {
            // Create a URLConnection object for a URL
            conn = openConnection(url);
            conn.setReadTimeout(5000);
            ((HttpURLConnection) conn).setRequestMethod("HEAD"); 
            return conn.getHeaderField(name);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (conn != null && conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).disconnect();
            }
        }
    }

    public static boolean resourceAvailable(URL url) {
        URLConnection conn = null;
        try {
            // Create a URLConnection object for a URL
            conn = openConnection(url);
            ((HttpURLConnection) conn).setRequestMethod("HEAD");
            conn.setReadTimeout(5000);
            return conn.getHeaderField("ETag") != null;
        } catch (Exception e) {
            return false;
        }
        finally {
            if (conn != null && conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).disconnect();
            }
        }
    }

}
