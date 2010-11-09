/*
 * SeekableFTPStream.java
 * Created on Aug 23, 2010
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

package savant.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import net.sf.samtools.util.SeekableStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;


/**
 * Random access stream for FTP access to BAM files through Picard
 *
 * @author vwilliams
 */
public class SeekableFTPStream extends SeekableStream {

    private static Log LOG = LogFactory.getLog(SeekableFTPStream.class);

    private final String source;
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String fileName;

    private long length;
    private FTPClient ftpClient = null;
    private long position = 0;

    public SeekableFTPStream(URL url) {
        this(url, "anonymous", "");
    }

    public SeekableFTPStream(URL url, String user, String pwd) {
        
        if (url == null) {
            throw new IllegalArgumentException("URL may not be null");
        }
        if (!url.getProtocol().toLowerCase().equals("ftp")) {
            throw new IllegalArgumentException("Only ftp:// protocol URLs are valid.");
        }

        source = url.toString();
        username = user;
        password = pwd;
        host = url.getHost();
        int p = url.getPort();
        port = p != -1 ? p : url.getDefaultPort();
        fileName = url.getFile();
        length = 0;
    }

    @Override
    public long length() {
        if (length == 0) {
            FTPFile[] files;
            try{
                files = listFiles(fileName);
            }
            catch (IOException e) {
                try {
                    disconnect();
                    files = listFiles(fileName);
                } catch (IOException e1) {
                    LOG.warn("Unable to reconnect getting length");
                    return 0;
                }

            }
            for (int i=0; i<files.length; i++) {
                FTPFile file = files[i];
                if (file != null) {
                    if (file.getName().equals(fileName)) {
                        length = file.getSize();
                        break;
                   }
                }
            }
        }
        return length;
        
    }

    @Override
    public void seek(long pos) throws IOException {
        position = pos;
        LOG.info("FTP: seek to " + pos);
    }

    @Override
    public int read(byte[] bytes, int offset, int len) throws IOException {
        try {
            return readFromStream(bytes, offset, len);
        } catch (FTPConnectionClosedException e) {
            LOG.info("Connection closed during read.  Disconnecting and trying again at " + position);
            disconnect();
            return readFromStream(bytes, offset, len);
        }
    }

    private int readFromStream(byte[] bytes, int offset, int len) throws IOException {

        FTPClient client = getFTPClient();
        if (position != 0) {
            client.setRestartOffset(position);
        }
        InputStream is = client.retrieveFileStream(fileName);
        long oldPos = position;
        if (is != null) {
            int n = 0;
            while (n < len) {
               int bytesRead = is.read(bytes, offset+n, len-n);
               if (bytesRead < 0) {
                   if (n == 0) return -1;
                   else break;
               }
               n += bytesRead;
            }
            position += n;
            is.close();
            LOG.info(String.format("FTP reading %d bytes at %d: %02x %02x %02x %02x %02x %02x %02x %02x...", len, oldPos, bytes[offset], bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4], bytes[offset + 5], bytes[offset + 6], bytes[offset + 7]));
            try {
                client.completePendingCommand();
            } catch (FTPConnectionClosedException suppressed) {
            }
            return n;
        } else {
            String msg = String.format("Unable to retrieve input stream for file (reply code %1).", client.getReplyCode());
            LOG.error(msg);
            throw new IOException(msg);
        }
    }

    @Override
    public void close() throws IOException {
        if (ftpClient != null) {
            try {
                ftpClient.completePendingCommand();
                ftpClient.logout();
            } catch (FTPConnectionClosedException e) {
                LOG.info("Suppressing FTPConnectionClosedException exception from logout().");
            }
            disconnect();
        }
    }

    @Override
    public boolean eof() throws IOException {
        return position >= length();
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException("read() not supported for SeekableFTPStreams");
    }

    @Override
    public String getSource() {
        return source;
    }

    public FTPFile[] listFiles(String relPath) throws IOException {
        try {
            return getFTPClient().listFiles(relPath);
        } catch (FTPConnectionClosedException e) {
            disconnect();
            return getFTPClient().listFiles(relPath);
        }
    }

    public void disconnect() throws IOException {
        if (ftpClient != null) {
            try {
                ftpClient.disconnect();
            } finally {
                ftpClient = null;
            }
        }
    }

    private FTPClient getFTPClient() throws IOException {
        if (ftpClient == null) {
            ftpClient = createClient();
        }

        return ftpClient;
    }

    private FTPClient createClient() throws IOException {
        FTPClient client = new FTPClient();
        client.connect(host, port);
        client.login(username, password);
        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.enterLocalPassiveMode();
        int reply = client.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            client.disconnect();
            LOG.error("FTP server refused connection.");
            return null;
        }
        return client;
    }
}
