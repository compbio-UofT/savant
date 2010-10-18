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

/*
 * SeekableFTPStream.java
 * Created on Aug 23, 2010
 */

package savant.util;

import net.sf.samtools.util.SeekableStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Random access stream for FTP access to BAM files through Picard
 *
 * @author vwilliams
 */
public class SeekableFTPStream extends SeekableStream {

    private static Log log = LogFactory.getLog(SeekableFTPStream.class);

    private final URL url;
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String file;
    private final String dir;
    private final String fileName;

    private long length;
    private FTPClient ftpClient = null;
    private long restartOffset = 0;
    private long position = 0;

    public SeekableFTPStream(URL url) {
        this(url, "", "anonymous");
    }

    public SeekableFTPStream(URL url, String username, String password) {
        
        if (url == null) throw new IllegalArgumentException("URL may not be null");
        if (!url.getProtocol().toLowerCase().equals("ftp")) throw new IllegalArgumentException("Only ftp:// protocol URLs are valid");

        this.username = username;
        this.password = password;
        this.url = url;
        this.host = url.getHost();
        this.file = url.getFile();
        this.dir = url.getPath();
        this.port = url.getPort();
        this.fileName = url.getFile();
        this.length = 0;

    }

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
                    log.warn("Unable to reconnect getting length");
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

    public void seek(long l) throws IOException {
        restartOffset = l;
    }

    public int read(byte[] bytes, int offset, int len) throws IOException {
        try
         {
             return readFromStream(bytes, offset, len);
         }
         catch (FTPConnectionClosedException e)
         {
             disconnect();

             return readFromStream(bytes, offset, len);
         }

    }

    private int readFromStream(byte[] bytes, int offset, int len) throws IOException {

        FTPClient client = getFtpClient();
        if (restartOffset != 0) {
           client.setRestartOffset(restartOffset);
           position = restartOffset;
           restartOffset = 0;
        }

        InputStream is = client.retrieveFileStream(fileName);
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
            completePendingCommand();
            return n;
        }
        else {
            log.error("Unable to retrieve input stream for file");
            throw new IOException("Unable to retrieve input stream for file");
        }
    }

    public void close() throws IOException {
        if (ftpClient != null) ftpClient.logout();
        disconnect();
    }

    public boolean eof() throws IOException {
        return position >= length();
    }

    public int read() throws IOException {
        throw new UnsupportedOperationException("read() not supported for SeekableFTPStreams");
    }

     public String getSource() {
        return this.url.toString();
    }

    public boolean completePendingCommand() throws IOException
    {
        if (ftpClient != null)
        {
            return getFtpClient().completePendingCommand();
        }

        return true;
    }

    public FTPFile[] listFiles(String relPath) throws IOException
    {
        try {
            return getFtpClient().listFiles(relPath);
        }
        catch (FTPConnectionClosedException e) {
            disconnect();
            return getFtpClient().listFiles(relPath);
        }
    }

    public void disconnect() throws IOException
    {
        try
        {
            getFtpClient().disconnect();
        }
        finally
        {
            ftpClient = null;
        }
    }

    private FTPClient getFtpClient() throws IOException
    {
        if (ftpClient == null)
        {
            ftpClient = createClient();
        }

        return ftpClient;
    }

    private FTPClient createClient() throws IOException
    {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(host, port);
        ftpClient.login(username, password);
        int reply = ftpClient.getReplyCode();

        if(!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            log.error("FTP server refused connection.");
            return null;
        }
        return ftpClient;
    }
}
