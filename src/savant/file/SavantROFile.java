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

import net.sf.samtools.util.SeekableBufferedStream;
import net.sf.samtools.util.SeekableFileStream;
import net.sf.samtools.util.SeekableHTTPStream;
import net.sf.samtools.util.SeekableStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.util.MiscUtils;
import savant.util.SavantFileUtils;
import savant.util.SeekableAdjustableBufferedStream;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
    private URI uri;

    private long filePointer = 0;

    public static final int CURRENT_FILE_VERSION = 2;
    public static final List<Integer> SUPPORTED_FILE_VERSIONS = Arrays.asList(2);

    public static SavantROFile fromStringAndType(String fileOrURI, FileType ft) throws URISyntaxException, IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        String lowerCaseName = fileOrURI.toLowerCase();
        if (lowerCaseName.startsWith("http") || lowerCaseName.startsWith("ftp")) {
            return new SavantROFile(new URI(fileOrURI), ft);
        }
        else {
            return new SavantROFile(fileOrURI, ft);
        }
    }

    public static SavantROFile fromString(String fileOrURI) throws URISyntaxException, IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        String lowerCaseName = fileOrURI.toLowerCase();
        if (lowerCaseName.startsWith("http") || lowerCaseName.startsWith("ftp")) {
            return new SavantROFile(new URI(fileOrURI));
        }
        else {
            return new SavantROFile(fileOrURI);
        }
    }

    /**
     * Construct a Savant file from a local file name.
     *
     * @param filename name of file
     * @throws IOException
     * @throws SavantFileNotFormattedException if file is not formatted in Savant format (no magic number)
     * @throws SavantUnsupportedVersionException if file is formatted in a currently unsupported version
     */
    public SavantROFile(String filename) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {

        File inFile = new File(filename);
        this.uri = inFile.toURI();
        if (log.isDebugEnabled()) log.debug("Adding RO File: " + filename);
        if (log.isDebugEnabled()) log.debug("URI is: " + this.uri);
        this.seekStream = new SeekableFileStream(inFile);
        init();
    }

    /**
     * Construct a Savant file from a local file name, with a given file type
     *
     * @param filename filename
     * @param ft file type
     * @throws IOException if existing file type does not match that given
     * @throws SavantFileNotFormattedException if file is not formatted in Savant format (no magic number)
     * @throws SavantUnsupportedVersionException if file is formatted in a currently unsupported version
     */
    public SavantROFile(String filename, FileType ft) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this(filename);
        if (!fileTypeHeader.fileType.equals(ft)) {
            if (fileTypeHeader.fileType == FileType.INTERVAL_GFF && ft == FileType.INTERVAL_GENERIC) {}
            else { throw new IOException("Wrong file type"); }
        }
    }

    /**
     * Construct a Savant file from a URI
     *
     * @param uri HTTP URI
     * @throws IOException
     * @throws SavantFileNotFormattedException if file is not formatted in Savant format (no magic number)
     * @throws SavantUnsupportedVersionException if file is formatted in a currently unsupported version
     */
    public SavantROFile(URI uri) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this.uri = uri.normalize();
        this.seekStream = new SeekableAdjustableBufferedStream(new SeekableHTTPStream(uri.toURL()), 4096);
        init();
    }

    /**
     * Construct a Savant file from a URI, with a given file type
     *
     * @param uri HTTP URI
     * @param ft file type
     * @throws IOException if existing file type does not match that given
     * @throws SavantFileNotFormattedException if file is not formatted in Savant format (no magic number)
     * @throws SavantUnsupportedVersionException if file is formatted in a currently unsupported version
     */
    public SavantROFile(URI uri, FileType ft) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this(uri);
        if (!fileTypeHeader.fileType.equals(ft)) {
            if (fileTypeHeader.fileType == FileType.INTERVAL_GFF && ft == FileType.INTERVAL_GENERIC) { return; }
            throw new IOException("Wrong file type");
        }
    }
    private void init() throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {

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

    public synchronized long seek(String reference, long pos) throws IOException {

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
                    + " file="+this.uri.toString());


            seek(pos+refoffset+headerOffset);
            return pos+refoffset+headerOffset;
        }
    }

    public synchronized void seek(long pos) throws IOException {
        seekStream.seek(pos);
        filePointer = pos;
        if (log.isDebugEnabled()) {
            log.debug("Seeking to " + pos);
            log.debug("warning: consider calling seek (string reference, long pos) instead");
        }
    }

    public synchronized void close() throws IOException {
        seekStream.close();
    }

    public synchronized long getFilePointer() throws IOException {
        return filePointer;
    }

    public synchronized long length() throws IOException {
        return seekStream.length();
    }

    public synchronized int read() throws IOException {
        byte[] buf = new byte[1];
        int bytesRead = seekStream.read(buf,0,1);
        if (bytesRead  != -1) {
            int result = buf[0];
            filePointer++;
            return result;
        }
        else
            return -1;
    }

    public synchronized int read(byte[] b) throws IOException {
        int result = seekStream.read(b, 0, b.length);
        if (result != -1) filePointer += result;
        return result;
    }

    public synchronized int read(byte[] b, int off, int len) throws IOException {
        int result = seekStream.read(b, off, len);
        if (result != -1) filePointer += result;
        return result;
    }

    public synchronized byte readByte() throws IOException {
        byte result = (byte)(read()&0xFF);
        return result;
    }

    public synchronized double readDouble() throws IOException {
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

    public synchronized float readFloat() throws IOException {
        byte[] bytes = new byte[4];
        int result = read(bytes);
        if (result != 4) {
            log.error("Could not read 4 bytes for float");
            return 0;
        }
        int intBits = (bytes[0]&0xFF)<<24 | (bytes[1]&0xFF)<<16 | (bytes[2]&0xFF)<<8 | bytes[3]&0xFF;
        return Float.intBitsToFloat(intBits);
    }

    public synchronized int readInt() throws IOException {
        byte[] bytes = new byte[4];
        int result = read(bytes);
        if (result != 4) {
            log.error("Could not read 4 bytes for int");
            return 0;
        }
        int intBits = (bytes[0]&0xFF)<<24 | (bytes[1]&0xFF)<<16 | (bytes[2]&0xFF)<<8 | bytes[3]&0xFF;
        return intBits;
    }

    public synchronized String readLine() throws IOException {

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

    public synchronized long readLong() throws IOException {
        byte[] bytes = new byte[8];
        int result = read(bytes);
        if (result != 8) {
            log.error("Could not read 8 bytes for a long");
            return 0;
        }
        long longBits = ((long)bytes[0]&0xFF)<<56 | ((long)bytes[1]&0xFF)<<48 | ((long)bytes[2]&0xFF)<<40 | ((long)bytes[3]&0xFF)<<32 |
                ((long)bytes[4]&0xFF)<<24 | ((long)bytes[5]&0xFF)<<16 | ((long)bytes[6]&0xFF)<<8 | ((long)bytes[7]&0xFF);
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

    public URI getURI() {
        if (log.isDebugEnabled()) log.debug("Getting URI: " + this.uri);
        return this.uri;
    }
}
