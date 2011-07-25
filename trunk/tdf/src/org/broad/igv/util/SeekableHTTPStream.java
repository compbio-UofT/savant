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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.util;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author jrobinso
 */
public class SeekableHTTPStream extends SeekableStream {

    static Log log = LogFactory.getLog(SeekableHTTPStream.class);
//    private static final String WEBSERVICE_URL = "http://www.broadinstitute.org/webservices/igv";
//    private static final String IGV_DATA_HOST = "www.broadinstitute.org";
//    private static final String DATA_PATH = "/xchip/igv/data/public";
//    private static final String DATA_HTTP_PATH = "/igvdata";


    private long position = 0;
    private long contentLength = -1;
    private URL url;

     SeekableHTTPStream(URL url) {
        this.url = url;

        // Try to get the file length
        String contentLengthString = IGVHttpUtils.getHeaderField(url, "Content-Length");
        if (contentLengthString != null) {
            try {
                contentLength = Long.parseLong(contentLengthString);
            }
            catch (NumberFormatException e) {
                log.error("Error converting content length to number: " + contentLength);
            }
        }
    }

    public void seek(long position) {
        this.position = position;
    }

    public long position() {
        return position;
    }


    @Override
    public long skip(long n) throws IOException {
        long bytesToSkip = n;
        if (contentLength > 0) {
            bytesToSkip = Math.min(n, contentLength - position);
        }
        position += bytesToSkip;
        return bytesToSkip;
    }


    @Override
    public int read(byte[] buffer, int offset, int len) throws IOException {

        if (offset < 0 || len < 0 || (offset + len) > buffer.length) {
            throw new IndexOutOfBoundsException();
        }

        if (len == 0) {
            return 0;
        }

        //HttpURLConnection connection = null;
        InputStream is = null;
        String byteRange = "";
        int n = 0;
        try {
            Map<String, String> props = new HashMap();
            long endRange = position + len - 1;
            // IF we know the total content length, limit the end range to that.
            if (contentLength > 0) {
                endRange = Math.min(endRange, contentLength);
            }
            byteRange = "bytes=" + position + "-" + endRange;
            props.put("Range", byteRange);

            is = IGVHttpUtils.openHttpStream(url, props);

            while (n < len) {
                int count = is.read(buffer, offset + n, len - n);
                if (count < 0) {
                    if (n == 0) {
                        return -1;
                    } else {
                        break;
                    }
                }
                n += count;
            }

            position += n;

            return n;

        }

        catch (IOException e) {
            // THis is a bit of a hack, but its not clear how else to handle this.  If a byte range is specified
            // that goes past the end of the file the response code will be 416.  The MAC os translates this to
            // an IOException with the 416 code in the message.  Windows translates the error to an EOFException.
            //
            //  The BAM file iterator  uses the return value to detect end of file (specifically looks for n == 0).
            if (e.getMessage().contains("416") || (e instanceof EOFException)) {
                if (n < 0) {
                    return -1;
                } else {
                    position += n;
                    // As we are at EOF, the contentLength and position are by definition =
                    contentLength = position;
                    return n;
                }
            } else {
                throw e;
            }
        }

        finally {
            if (is != null) {
                is.close();
            }
            //if (connection != null) {
            //    connection.disconnect();
            //}
        }
    }


    public void close() throws IOException {
        BufferedInputStream bis;
        // Nothing to do
    }

    public int read() throws IOException {
        throw new UnsupportedOperationException("read() is not supported on SeekableHTTPStream.  Must read in blocks.");
    }

    private void logHeaderFields(HttpURLConnection connection) {
        Map<String, List<String>> map = connection.getHeaderFields();

        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            System.out.print(entry.getKey() + ":\t");
            for (String v : entry.getValue()) {
                System.out.print(v + " ");
            }
            System.out.println();
        }
    }
}
