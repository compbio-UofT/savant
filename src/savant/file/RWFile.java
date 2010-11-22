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
 * RWFile.java
 * Created on Aug 25, 2010
 */

package savant.file;

import java.io.IOException;

/**
 * @deprecated
 */
public interface RWFile extends ROFile {

    public void write(byte[] b) throws IOException;

    public void write(byte[] b, int off, int len) throws IOException;

    public void write(int b) throws IOException;

    public void writeByte(int v) throws IOException;

    public void writeBytes(String s) throws IOException;

    public void writeDouble(double v) throws IOException;

    public void writeFloat(float v) throws IOException;

    public void writeInt(int v) throws IOException;

    public void writeLong(long v) throws IOException;

}
