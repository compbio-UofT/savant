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

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * User: jrobinso
 * Date: Apr 13, 2010
 */
public class SeekableFTPStream extends SeekableStream {

    static Log log = LogFactory.getLog(SeekableHTTPStream.class);

    private long position = 0;
    private String host;
    private String path;
    FTPClient ftp = null;

     SeekableFTPStream(URL url) throws IOException {
        this.host = url.getHost();
        this.path = url.getPath();
        ftp = openClient(host);
        ftp.setFileType(FTP.BINARY_FILE_TYPE);

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


        int n = 0;
        InputStream is = null;

        try {

            ftp.setRestartOffset(position);

            is = ftp.retrieveFileStream(path);
            System.out.println(ftp.getReplyString());

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

        catch (EOFException e) {
            if (n < 0) {
                return -1;
            } else {
                position += n;
                return n;
            }

        }

        finally {
            if (is != null) {
                is.close();
            }
            ftp.completePendingCommand();
        }
    }


    public void close() throws IOException {
        // Nothing to do
    }

    public int read() throws IOException {
        throw new UnsupportedOperationException("read() is not supported on SeekableHTTPStream.  Must read in blocks.");
    }


    private FTPClient openClient(String host) throws IOException {

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


        return ftp;
    }

}
