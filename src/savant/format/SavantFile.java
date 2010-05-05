/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
