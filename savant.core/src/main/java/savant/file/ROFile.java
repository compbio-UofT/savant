/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
