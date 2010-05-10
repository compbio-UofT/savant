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

package savant.format;

import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.FieldType;
import savant.util.RAFUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class SavantFile extends RandomAccessFile {

    FileTypeHeader fileTypeHeader;
    List<FieldType> fields;
    long headerOffset;

    public SavantFile(String filename) throws IOException {
        super(filename,"rw");
        this.fileTypeHeader = RAFUtils.readFileTypeHeader(this);
        this.fields = RAFUtils.readFieldsHeader(this);
        this.headerOffset = super.getFilePointer();
    }

    public SavantFile(String filename, FileType ft) throws IOException {
        this(filename);
        if (!fileTypeHeader.fileType.equals(ft)) {
            throw new IOException("Wrong file type");
        }        
    }

    public List<FieldType> getFields() {
        return this.fields;
    }

    @Override
    public void seek(long pos) throws IOException {
        super.seek(pos + this.getHeaderOffset());
    }

    public void seekSuper(long pos) throws IOException {
        super.seek(pos);
    }

    @Override
    public long length() throws IOException {
        return super.length() - this.getHeaderOffset();
    }

    public long lengthSuper() throws IOException {
        return super.length();
    }

    @Override
    public long getFilePointer() throws IOException {
        return super.getFilePointer() - this.getHeaderOffset();
    }

    public long getFilePointerSuper() throws IOException {
        return super.getFilePointer();
    }

    public long getHeaderOffset() {
        return this.headerOffset;
    }

    public FileType getFileType() {
        return this.fileTypeHeader.fileType;
    }

    public int getFileTypeVersion() {
        return this.fileTypeHeader.version;
    }

}
