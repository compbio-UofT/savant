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
 * SavantROFile.java
 * Created on Aug 27, 2010
 */

package savant.file;

import net.sf.samtools.util.SeekableFileStream;
import net.sf.samtools.util.SeekableStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.util.MiscUtils;
import savant.util.SavantFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SavantROFile implements ROFile {

    private static Log log = LogFactory.getLog(SavantROFile.class);
    
    private final SeekableStream seekStream;

    private FileTypeHeader fileTypeHeader;
    private Map<String,Long[]> referenceMap;
    private List<FieldType> fields;
    private long headerOffset;
    private String filename;

    private long filePointer = 0;

    public static final int CURRENT_FILE_VERSION = 2;
    public static final List<Integer> SUPPORTED_FILE_VERSIONS = Arrays.asList(2);

    public SavantROFile(String filename) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this.seekStream = new SeekableFileStream(new File(filename));
        init(filename);
    }

    public SavantROFile(String filename, FileType ft) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this(filename);
        if (!fileTypeHeader.fileType.equals(ft)) {
            throw new IOException("Wrong file type");
        }

    }

    private void init(String filename) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this.filename = filename;

        log.debug("Reading file type");
        this.fileTypeHeader = SavantFileUtils.readFileTypeHeader(this);

        if (fileTypeHeader.fileType == null) throw new SavantFileNotFormattedException("This file does not appear to be formatted. Format now?");

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
        seekStream.seek(pos);
        filePointer = pos;
        if (log.isDebugEnabled()) {
            log.debug("Seeking to " + pos);
            log.debug("warning: consider calling seek (string reference, long pos) instead");
        }
    }

    public void close() throws IOException {
        seekStream.close();
    }

    public long getFilePointer() throws IOException {
        return filePointer;
    }

    public long length() throws IOException {
        return seekStream.length();
    }

    public int read() throws IOException {
        int result = seekStream.read();
        filePointer++;
        return result;
    }

    public int read(byte[] b) throws IOException {
        int result = seekStream.read(b);
        if (result != -1) filePointer += result;
        return result;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int result = seekStream.read(b, off, len);
        if (result != -1) filePointer += result;
        return result;
    }

    public byte readByte() throws IOException {
        byte result = (byte)(read()&0xFF);
        return result;
    }

    public double readDouble() throws IOException {
        byte[] bytes = new byte[8];
        int result = read(bytes);
        if (result != 8) {
            log.error("Could not read 8 bytes for a double");
            return 0;
        }
        long longBits = (bytes[0]&0xFF)<<56 | (bytes[1]&0xFF)<<48 | (bytes[2]&0xFF)<<40 | (bytes[3]&0xFF)<<32 |
                (bytes[4]&0xFF)<<24 | (bytes[5]&0xFF)<<16 | (bytes[6]&0xFF)<<8 | bytes[7]&0xFF;
        return Double.longBitsToDouble(longBits);
    }

    public float readFloat() throws IOException {
        byte[] bytes = new byte[4];
        int result = read(bytes);
        if (result != 4) {
            log.error("Could not read 4 bytes for float");
            return 0;
        }
        int intBits = (bytes[0]&0xFF)<<24 | (bytes[1]&0xFF)<<16 | (bytes[2]&0xFF)<<8 | bytes[3]&0xFF;
        return Float.intBitsToFloat(intBits);
    }

    public int readInt() throws IOException {
        byte[] bytes = new byte[4];
        int result = read(bytes);
        if (result != 4) {
            log.error("Could not read 4 bytes for int");
            return 0;
        }
        int intBits = (bytes[0]&0xFF)<<24 | (bytes[1]&0xFF)<<16 | (bytes[2]&0xFF)<<8 | bytes[3]&0xFF;
        return intBits;
    }

    public String readLine() throws IOException {

        byte[] readBytes = new byte[2];
        StringBuilder sb = new StringBuilder();
        while (true) {
            int read = read(readBytes);
            if (read != 2) {
                log.warn("Unable to read 2 bytes");
                return sb.toString();
            }
            char theChar = (char)(readBytes[0]<<8 | readBytes[1]);
            if (theChar == '\r') {
                theChar = (char)(readBytes[0]<<8 | readBytes[1]);
            }
            if (theChar == '\n') {
                return sb.toString();
            }
            sb.append(theChar);
        }
    }

    public long readLong() throws IOException {
        byte[] bytes = new byte[8];
        int result = read(bytes);
        if (result != 8) {
            log.error("Could not read 8 bytes for a double");
            return 0;
        }
        long longBits = (bytes[0]&0xFF)<<56 | (bytes[1]&0xFF)<<48 | (bytes[2]&0xFF)<<40 | (bytes[3]&0xFF)<<32 |
                (bytes[4]&0xFF)<<24 | (bytes[5]&0xFF)<<16 | (bytes[6]&0xFF)<<8 | bytes[7]&0xFF;
        return longBits;
    }

    public List<FieldType> getFields() {
        return this.fields;
    }

    public Map<String, Long[]> getReferenceMap() {
        return this.referenceMap;
    }

    public long getHeaderOffset() {
        return headerOffset;
    }

    public void setHeaderOffset(long offset) {
        this.headerOffset = offset;
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

    public FileType getFileType() {
        return fileTypeHeader.fileType;
    }

    public String getPath() { return this.filename; }
}
