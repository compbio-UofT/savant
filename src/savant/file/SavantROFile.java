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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.sf.samtools.util.SeekableFileStream;
import net.sf.samtools.util.SeekableStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.util.MiscUtils;
import savant.util.NetworkUtils;
import savant.util.SavantFileUtils;

public class SavantROFile implements ROFile {

    private static final Log LOG = LogFactory.getLog(SavantROFile.class);

    private final SeekableStream seekStream;

    private FileTypeHeader fileTypeHeader;
    private Map<String, long[]> referenceMap;
    private List<FieldType> fields;
    private long headerOffset;
    private URI uri;

    private long filePointer = 0;

    public static final int CURRENT_FILE_VERSION = 2;
    public static final List<Integer> SUPPORTED_FILE_VERSIONS = Arrays.asList(2);

    /**
     * Construct a Savant file from a local file.
     *
     * @param file a local file
     * @throws IOException
     * @throws SavantFileNotFormattedException if file is not formatted in Savant format (no magic number)
     * @throws SavantUnsupportedVersionException if file is formatted in a currently unsupported version
     */
    public SavantROFile(File file) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {

        this.uri = file.toURI();
        LOG.debug("Adding RO File: " + file);
        LOG.debug("URI is: " + this.uri);
        this.seekStream = new SeekableFileStream(file);
        init();
    }

    /**
     * Construct a Savant file from a local file, with a given file type
     *
     * @param file file path
     * @param ft file type
     * @throws IOException if existing file type does not match that given
     * @throws SavantFileNotFormattedException if file is not formatted in Savant format (no magic number)
     * @throws SavantUnsupportedVersionException if file is formatted in a currently unsupported version
     */
    public SavantROFile(File file, FileType ft) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this(file);
        if (!fileTypeHeader.fileType.equals(ft)) {
            if (fileTypeHeader.fileType == FileType.INTERVAL_GFF && ft == FileType.INTERVAL_GENERIC) {}
            else { throw new IOException("Wrong file type"); }
        }
    }

    /**
     * Construct a Savant file from a URI
     *
     * @param uri http, ftp (or file) URI
     * @throws IOException
     * @throws SavantFileNotFormattedException if file is not formatted in Savant format (no magic number)
     * @throws SavantUnsupportedVersionException if file is formatted in a currently unsupported version
     */
    public SavantROFile(URI uri) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        this.uri = uri.normalize();
        seekStream = NetworkUtils.getSeekableStreamForURI(uri);
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

        LOG.debug("Reading file type");
        this.fileTypeHeader = SavantFileUtils.readFileTypeHeader(this);

        if (fileTypeHeader.fileType == null) throw new SavantFileNotFormattedException("This file does not appear to be formatted. Format now?");

        if (!isSupportedVersion(fileTypeHeader.version)) { throw new SavantUnsupportedVersionException(fileTypeHeader.version, getSupportedVersions()); }

        if (LOG.isDebugEnabled()) {
            LOG.debug("File type: " + this.fileTypeHeader.fileType);
            LOG.debug("Done... at " + getFilePointer() + " bytes");
            LOG.debug("Reading fields");
        }

        this.fields = SavantFileUtils.readFieldsHeader(this);
        if (LOG.isDebugEnabled()) {
            LOG.debug(fields);
            LOG.debug("Number of fields: " + fields.size());
            LOG.debug("Reading reference<-> data map");
        }

        referenceMap = SavantFileUtils.readReferenceMap(this);
        if (LOG.isDebugEnabled()) {
            for (String refname : referenceMap.keySet()) {
                long[] vals = referenceMap.get(refname);
                LOG.debug("Reference " + refname + " at " + vals[0] + " of length " + vals[1]);
            }
        }

        LOG.debug("Making note of offset: " + getFilePointer());

        headerOffset = getFilePointer();
    }

    @Override
    public synchronized long seek(String reference, long pos) throws IOException {

        //FIXME!!!

        if (!this.containsDataForReference(reference) &&
                !this.containsDataForReference(MiscUtils.homogenizeSequence(reference))) { //FIXME: temporary fix for chrx != x issue
            LOG.debug("No data for reference: " + reference);
            return -1;
        } else if (pos >= this.getReferenceLength(reference) &&
                pos >= this.getReferenceLength(reference.substring(reference.length()-1))) { //ditto ^^
            LOG.debug("End of data for reference: " + reference);
            return -1;
        }
        else {

            long refoffset = getReferenceOffset(reference);

            //FIXME: temporary fix for chrx != x issue
            if(refoffset == -1){
                refoffset = getReferenceOffset(MiscUtils.homogenizeSequence(reference));
            }


            if (LOG.isDebugEnabled()) LOG.debug("Seeking to " + (pos + refoffset+headerOffset)
                    + " pos=" + pos
                    + " ref=" + reference
                    + " refoffset=" + refoffset
                    + " headeroffset=" + headerOffset
                    + " file="+this.uri.toString());


            seek(pos+refoffset+headerOffset);
            return pos+refoffset+headerOffset;
        }
    }

    @Override
    public synchronized void seek(long pos) throws IOException {
        if (filePointer != pos) {
            seekStream.seek(pos);
            filePointer = pos;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Seeking to " + pos);
                LOG.debug("warning: consider calling seek (string reference, long pos) instead");
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        seekStream.close();
    }

    @Override
    public synchronized long getFilePointer() throws IOException {
        return filePointer;
    }

    @Override
    public synchronized long length() throws IOException {
        return seekStream.length();
    }

    @Override
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

    @Override
    public synchronized int read(byte[] b) throws IOException {
        int result = seekStream.read(b, 0, b.length);
        if (result != -1) filePointer += result;
        return result;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        int result = seekStream.read(b, off, len);
        if (result != -1) filePointer += result;
        return result;
    }

    @Override
    public synchronized byte readByte() throws IOException {
        byte result = (byte)(read()&0xFF);
        return result;
    }

    @Override
    public synchronized double readDouble() throws IOException {
        byte[] bytes = new byte[8];
        int result = read(bytes);
        if (result != 8) {
            LOG.warn("Could not read 8 bytes for a double");
            throw new IOException("At EOF");
        }
        long longBits = ((long)bytes[0]&0xFF)<<56 | ((long)bytes[1]&0xFF)<<48 | ((long)bytes[2]&0xFF)<<40 | ((long)bytes[3]&0xFF)<<32 |
                ((long)bytes[4]&0xFF)<<24 | ((long)bytes[5]&0xFF)<<16 | ((long)bytes[6]&0xFF)<<8 | ((long)bytes[7]&0xFF);
        return Double.longBitsToDouble(longBits);
    }

    @Override
    public synchronized float readFloat() throws IOException {
        byte[] bytes = new byte[4];
        int result = read(bytes);
        if (result != 4) {
            LOG.warn("Could not read 4 bytes for float");
            throw new IOException("At EOF");
        }
        int intBits = (bytes[0]&0xFF)<<24 | (bytes[1]&0xFF)<<16 | (bytes[2]&0xFF)<<8 | bytes[3]&0xFF;
        return Float.intBitsToFloat(intBits);
    }

    @Override
    public synchronized int readInt() throws IOException {
        byte[] bytes = new byte[4];
        int result = read(bytes);
        if (result != 4) {
            LOG.warn("Could not read 4 bytes for int");
            throw new IOException("At EOF");
        }
        int intBits = (bytes[0]&0xFF)<<24 | (bytes[1]&0xFF)<<16 | (bytes[2]&0xFF)<<8 | bytes[3]&0xFF;
        return intBits;
    }

    @Override
    public synchronized String readLine() throws IOException {

        byte[] readBytes = new byte[2];
        StringBuilder sb = new StringBuilder();
        while (true) {
            int read = read(readBytes);
            if (read != 2) {
                LOG.warn("Unable to read 2 bytes");
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

    @Override
    public synchronized long readLong() throws IOException {
        byte[] bytes = new byte[8];
        int result = read(bytes);
        if (result != 8) {
            LOG.warn("Could not read 8 bytes for a long");
            throw new IOException("At EOF");
        }
        long longBits = ((long)bytes[0]&0xFF)<<56 | ((long)bytes[1]&0xFF)<<48 | ((long)bytes[2]&0xFF)<<40 | ((long)bytes[3]&0xFF)<<32 |
                ((long)bytes[4]&0xFF)<<24 | ((long)bytes[5]&0xFF)<<16 | ((long)bytes[6]&0xFF)<<8 | ((long)bytes[7]&0xFF);
        return longBits;
    }

    @Override
    public List<FieldType> getFields() {
        return this.fields;
    }

    @Override
    public Map<String, long[]> getReferenceMap() {
        return referenceMap;
    }

    @Override
    public long getHeaderOffset() {
        return headerOffset;
    }

    @Override
    public void setHeaderOffset(long offset) {
        this.headerOffset = offset;
    }
    
    public boolean isSupportedVersion(int version) {
        return SUPPORTED_FILE_VERSIONS.contains(version);
    }

    public String getSupportedVersions() {
        StringBuilder sb = new StringBuilder();
        for (Integer version: SUPPORTED_FILE_VERSIONS) {
            sb.append(version).append(" ");
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
        LOG.debug("Getting URI: " + this.uri);
        return this.uri;
    }
}
