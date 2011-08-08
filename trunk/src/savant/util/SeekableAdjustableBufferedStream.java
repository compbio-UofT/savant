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
 * SeekableAdjustableBufferedStream.java
 * Created on Sep 28, 2010
 */

package savant.util;

import net.sf.samtools.util.SeekableStream;

import java.io.BufferedInputStream;
import java.io.IOException;

public class SeekableAdjustableBufferedStream extends SeekableStream {

    protected int bufferSize = 4096; // 4K
    protected BufferedInputStream bufferedStream;
    protected SeekableStream wrappedStream;
    protected long position;

    public SeekableAdjustableBufferedStream(SeekableStream seekable) {
        this.wrappedStream = seekable;
        this.position = 0;
        bufferedStream = new BufferedInputStream(wrappedStream, this.bufferSize);
    }

    public SeekableAdjustableBufferedStream(SeekableStream seekable, int bufferSize) {
        this.wrappedStream = seekable;
        this.position = 0;
        this.bufferSize = bufferSize;
        bufferedStream = new BufferedInputStream(wrappedStream, this.bufferSize);
    }
    
    public long length() {
        return wrappedStream.length();
    }

    public void seek(long position) throws IOException {
        this.position = position;
        wrappedStream.seek(position);
        bufferedStream = new BufferedInputStream(wrappedStream, bufferSize);
    }

    public int read() throws IOException {
        int b = bufferedStream.read();
        position++;
        return b;
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        int nBytesRead = bufferedStream.read(buffer, offset, length);
        if (nBytesRead > 0) {
            position += nBytesRead;
        }
        return nBytesRead;
    }

    public void close() throws IOException {
        wrappedStream.close();
    }

    public boolean eof() throws IOException {
        return position >= wrappedStream.length();
    }

    @Override
    public String getSource() {
        return wrappedStream.getSource();
    }
}
