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

package savant.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.util.MiscUtils;
import savant.util.SavantFileUtils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author mfiume
 * @deprecated
 */
public class SavantRWFile implements RWFile {

    private static Log log = LogFactory.getLog(SavantRWFile.class);

    private RandomAccessFile raf;

    private FileTypeHeader fileTypeHeader;
    private Map<String,Long[]> referenceMap;
    private List<FieldType> fields;
    private long headerOffset;
    private String filename;
    
    public static final int CURRENT_FILE_VERSION = 2;
    public static final List<Integer> SUPPORTED_FILE_VERSIONS = Arrays.asList(2);

    public SavantRWFile(String filename) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this.raf = new RandomAccessFile(filename,"rw");

        init(filename);
    }

    public SavantRWFile(String filename, FileType ft) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this(filename);
        if (!fileTypeHeader.fileType.equals(ft)) {
            throw new IOException("Wrong file type");
        }        
    }

    private void init(String filename) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this.filename = filename;

        log.debug("Reading file type");
        this.fileTypeHeader = SavantFileUtils.readFileTypeHeader(this);

        if (fileTypeHeader.fileType == null) throw new SavantFileNotFormattedException("This is not a Savant file");

        if (!isSupportedVersion(fileTypeHeader.version)) { throw new SavantUnsupportedVersionException(fileTypeHeader.version, getSupportedVersions()); }

        if (log.isDebugEnabled()) {
            log.debug("File type: " + this.fileTypeHeader.fileType);
            log.debug("Done... at " + getFilePointer() + " bytes");
            log.debug("Reading fields");
        }

        this.fields = SavantFileUtils.readFieldsHeader(this);
        if (log.isDebugEnabled()) {
            log.debug(fields);
            log.debug("Number of fields: " + fields.size());
            log.debug("Reading reference<-> data map");
        }

        this.referenceMap = SavantFileUtils.readReferenceMap(this);
        if (log.isDebugEnabled()) {
            for (String refname : this.referenceMap.keySet()) {
                Long[] vals = this.referenceMap.get(refname);
                log.debug("Reference " + refname + " at " + vals[0] + " of length " + vals[1]);
            }
        }

        if (log.isDebugEnabled()) log.debug("Making note of offset: " + getFilePointer());

        this.headerOffset = getFilePointer();
    }
    
    public List<FieldType> getFields() {
        return this.fields;
    }

    public long seek(String reference, long pos) throws IOException {

        //FIXME!!!

        if (!this.containsDataForReference(reference) && 
                !this.containsDataForReference(MiscUtils.homogenizeSequence(reference))) { //FIXME: temporary fix for chrx != x issue
            if (log.isDebugEnabled()) log.debug("No data for reference: " + reference);
            return -1;
        } else if (pos >= this.getReferenceLength(reference) &&
                pos >= this.getReferenceLength(reference.substring(reference.length()-1))) { //ditto ^^
            if (log.isDebugEnabled()) log.debug("End of data for reference: " + reference);
            return -1;
        }
        else {

            long refoffset = getReferenceOffset(reference);

            //FIXME: temporary fix for chrx != x issue
            if(refoffset == -1){
                refoffset = getReferenceOffset(MiscUtils.homogenizeSequence(reference));
            }


            if (log.isDebugEnabled()) log.debug("Seeking to " + (pos + refoffset+headerOffset)
                    + " pos=" + pos
                    + " ref=" + reference
                    + " refoffset=" + refoffset
                    + " headeroffset=" + headerOffset
                    + " file="+this.filename);


            seek(pos+refoffset+headerOffset);
            return pos+refoffset+headerOffset;
        }
    }

    public void seek(long pos) throws IOException {
        raf.seek(pos);
        if (log.isDebugEnabled()) {
            log.debug("Seeking to " + pos);
            log.debug("warning: consider calling seek (string reference, long pos) instead");
        }
    }

    public long length() throws IOException {
        return raf.length();
    }

    public long getFilePointer() throws IOException {
        return raf.getFilePointer();
    }

    public void write(byte[] b) throws IOException {
        raf.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        raf.write(b, off, len);
    }

    public void write(int b) throws IOException {
        raf.write(b);
    }

    public void writeByte(int v) throws IOException {
        raf.writeByte(v);
    }

    public void writeBytes(String s) throws IOException {
        raf.writeBytes(s);
    }

    public void writeDouble(double v) throws IOException {
        raf.writeDouble(v);
    }

    public void writeFloat(float v) throws IOException {
        raf.writeFloat(v);
    }

    public void writeInt(int v) throws IOException {
        raf.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
        raf.writeLong(v);
    }

    public void close() throws IOException {
        raf.close();
    }

    public int read() throws IOException {
        return raf.read();
    }

    public int read(byte[] b) throws IOException {
        return raf.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return raf.read(b, off, len);
    }

    public byte readByte() throws IOException {
        return raf.readByte();
    }

    public double readDouble() throws IOException {
        return raf.readDouble();
    }

    public float readFloat() throws IOException {
        return raf.readFloat();
    }

    public int readInt() throws IOException {
        return raf.readInt();
    }

    public String readLine() throws IOException {
        return raf.readLine();
    }

    public long readLong() throws IOException {
        return raf.readLong();
    }

    public long getHeaderOffset() {
        return this.headerOffset;
    }

    public void setHeaderOffset(long headerOffset) {
        this.headerOffset = headerOffset;
    }

    public FileType getFileType() {
        return this.fileTypeHeader.fileType;
    }

    public int getFileTypeVersion() {
        return this.fileTypeHeader.version;
    }

    public Set<String> getReferenceNames() {
        return this.referenceMap.keySet();
    }

    public Map<String, Long[]> getReferenceMap() {
        return this.referenceMap;
    }

    public long getReferenceOffset(String reference) {
        if (!this.containsDataForReference(reference)) { return -1; }
        return this.referenceMap.get(reference)[0];
    }

    public long getReferenceLength(String reference) {
        if (!this.containsDataForReference(reference)) { return -1; }
        return this.referenceMap.get(reference)[1];
    }

    public boolean containsDataForReference(String reference) {
        return this.referenceMap.containsKey(reference);
    }

    public boolean isSupportedVersion(int version) {
        return SUPPORTED_FILE_VERSIONS.contains(version);
    }

    public String getSupportedVersions() {
        StringBuilder sb = new StringBuilder();
        for (Integer version: SUPPORTED_FILE_VERSIONS) {
            sb.append(version + " ");
        }
        return sb.toString().trim();
    }


}
