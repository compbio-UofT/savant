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

package savant.file;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ROFile {

    public void close() throws IOException;

    public long getFilePointer() throws IOException;

    public long length() throws IOException;

    public int read() throws IOException;

    public int read(byte[] b) throws IOException;

    public int read(byte[] b, int off, int len) throws IOException;

    public byte readByte() throws IOException;

    public double readDouble() throws IOException;

    public float readFloat() throws IOException;

    public int readInt() throws IOException;

    public String readLine() throws IOException;

    public long readLong() throws IOException;

    public void seek(long pos) throws IOException;

    /**
     * Seek to a position relative to a given reference.
     * @param reference name of reference
     * @param pos position - does not need to take account of file header since pos is relative to start of reference
     * @return file pointer after seek
     * @throws IOException if there is an IO problem
     */
    public long seek(String reference, long pos) throws IOException;

    public List<FieldType> getFields();

    public Map<String, long[]> getReferenceMap();

    public long getHeaderOffset();

    public void setHeaderOffset(long offset);

}
