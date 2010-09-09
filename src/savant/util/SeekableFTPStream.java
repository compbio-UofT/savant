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
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Random access stream for FTP access to BAM files through Picard
 *
 * @author vwilliams
 */
public class SeekableFTPStream /*extends SeekableStream*/ {

    private static Log log = LogFactory.getLog(SeekableFTPStream.class);

    private final String host;
    private final int port;
    private final String file;
    private final String dir;
    private final String fileName;

    private long length;

    public SeekableFTPStream(String path) throws MalformedURLException {
        
        URL url= null;
        try {
            url = new URL(path);
        }
        finally {
            host = url != null ? url.getHost() : null;
            file = url != null ? url.getFile() : null;
            dir = url != null ? url.getPath() : null;
            port = url != null ? url.getPort() : -1;
            fileName = url != null ? url.getFile() : null;
            length = 0;
        }

    }

    //@Override
    //public long length() {
        //if (length == 0) {
            //FTPClient ftpClient = new FTPClient();
            //connectAndChangeWD(ftpClient);
            //FTPFile[] files = ftpClient.listFiles(fileName);
            //for (int i=0; i<files.length; i++) {
            //    FTPFile file = files[i];
            //    if (file != null) {
            //        if (file.getName().equals(fileName)) {
            //            length = file.getSize();
            //            break;
            //       }
            //    }
            //}
        //}
        //return length;
    //}

    //@Override
    public void seek(long l) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    //@Override
    public int read(byte[] bytes, int i, int i1) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //@Override
    public void close() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    //@Override
    public byte[] readBytes(long l, int i) throws IOException {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    //@Override
    public boolean eof() throws IOException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //@Override
    public int read() throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
