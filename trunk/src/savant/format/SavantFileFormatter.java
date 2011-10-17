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

package savant.format;

import java.io.*;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.file.FileType;
import savant.file.FileTypeHeader;
import savant.file.FieldType;
import savant.file.SavantROFile;
import savant.settings.DirectorySettings;


public abstract class SavantFileFormatter {
    protected static final Log LOG = LogFactory.getLog(SavantFileFormatter.class);

    /**
     * Input file.  For now, this is usually a file URI.
     */
    protected File inFile;

    /**
     * Output file.
     */
    protected File outFile;

    protected FileType fileType;

    // size of the output buffer
    protected static final int OUTPUT_BUFFER_SIZE = 1024 * 128; // 128K

    //protected DataOutputStream out;

    protected BufferedReader inFileReader;  // input file reader
    protected DataOutputStream outFileStream;         // output file stream

    private List<String> referenceNames = new ArrayList<String>();
    private List<File> files = new ArrayList<File>();

    /* PROGRESS */

    // UI ...
    protected List<FormatProgressListener> listeners = new ArrayList<FormatProgressListener>();

    // non-UI ...
    // variables to keep track of progress processing the input file(s)
    protected long totalBytes;
    protected long byteCount;

    // TODO: remove, or make cleaner
    // stuff needed by IO; mandated by SavantFileFormatterUtils which we're depending on
    protected List<FieldType> fields;
    protected List<Object> modifiers;

    private Integer makePercentage(Integer progress) {
        if (progress == null || progress < 0 || progress > 100) { return null; }
        int p = Math.max(0, progress);
        p = Math.min(progress, 100);
        return p;
    }

    public void setSubtaskProgress(int progress) {
        this.setSubtaskProgressAndStatus(progress,null);
    }

    public void setSubtaskStatus(String status) {
        this.setSubtaskProgressAndStatus(null,status);
    }
    
    public void incrementOverallProgress() {
        fireIncrementOverallProgress();
    }

    public abstract void format() throws InterruptedException, IOException, SavantFileFormattingException;

    public void setSubtaskProgressAndStatus(Integer progress, String status) {
        fireStatusProgressUpdate(makePercentage(progress),status);
    }

    public void addProgressListener(FormatProgressListener listener) {
        listeners.add(listener);
    }

    public void removeProgressListener(FormatProgressListener listener) {
        listeners.remove(listener);
    }

    protected void fireStatusProgressUpdate(Integer progress, String status) {
        for (FormatProgressListener listener : listeners) {
            listener.taskProgressUpdate(progress,status);
        }
    }

    protected void fireIncrementOverallProgress() {
        for (FormatProgressListener listener : listeners) {
            listener.incrementOverallProgress();
        }
    }

    public SavantFileFormatter(File inFile, File outFile, FileType fileType) {
        
        this.inFile = inFile;
        this.outFile = outFile;
        this.fileType = fileType;
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Savant Formatter Created");
            LOG.debug("input URI: " + inFile);
            LOG.debug("output file: " + outFile);
        }
    }


    private static int partsMade = 0;

    protected DataOutputStream getOutputForReference(String referenceName) throws FileNotFoundException {
        partsMade++;
        File f = new File(DirectorySettings.getTmpDirectory(), inFile.getName() + ".part_" + partsMade);

        DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f), OUTPUT_BUFFER_SIZE));
        referenceNames.add(referenceName);
        files.add(f);
        return output;
    }

    /**
     * Open the input file.
     *
     * @throws FileNotFoundException
     */
    protected BufferedReader openInputFile() throws FileNotFoundException {
        return new BufferedReader(new FileReader(inFile));
    }

    /**
     * Open the output file.

     * @throws FileNotFoundException
     */
    protected DataOutputStream openOutputFile() throws FileNotFoundException {
        outFileStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile), OUTPUT_BUFFER_SIZE));
        return outFileStream;
    }

    protected void deleteOutputFile() {
        if (outFile.exists()) {
            if (!outFile.delete()) {
                outFile.deleteOnExit();
            }
        }
    }

    protected void writeSavantHeader() throws FileNotFoundException, IOException {
        // open the output file
        outFileStream = openOutputFile();

        // ALL SAVANT FILES HAVE 1-3
        // ONLY INTERVAL AND CONTINUOUS FILES CURRENTLY HAVE 4

        // 1. WRITE FILE TYPE HEADER (MAGIC NUMBER AND VERSION)
        LOG.debug("Writing file type header");
        FileTypeHeader fileTypeHeader = new FileTypeHeader(this.fileType, SavantROFile.CURRENT_FILE_VERSION);
        SavantFileFormatterUtils.writeFileTypeHeader(outFileStream,fileTypeHeader);
        outFileStream.flush();

        // 2. WRITE FIELD HEADER
        LOG.debug("Writing fields header");
        SavantFileFormatterUtils.writeFieldsHeader(outFileStream, fields);
        outFileStream.flush();

        // 3. WRITE REFERENCE MAP
        LOG.debug("Writing reference<->data map");
        writeReferenceMap();
        outFileStream.flush();
    }

    protected void writeAdditionalIndices(List<String> refnames, Map<String,String> refToIndexFileNameMap) throws FileNotFoundException, IOException {
        // 4. WRITE INDEX
        if (refToIndexFileNameMap != null) {
            LOG.debug("Writing reference<->index map");
            writeReferenceMap();
            concatenateFiles();
            deleteFiles();
            outFileStream.flush();
        }
    }

    protected void writeData() throws IOException {
        concatenateFiles();
        deleteFiles();
        outFileStream.flush();

        // close the output file
        outFileStream.close();
    }

    protected void writeOutputFile() throws FileNotFoundException, IOException {
        this.setSubtaskStatus("Writing output file ...");
        this.incrementOverallProgress();
        writeSavantHeader();
        writeData();
    }

    protected void writeReferenceMap() throws IOException {

        long refOffset = 0;

        outFileStream.writeInt(referenceNames.size());

        // write a record for each reference sequence
        for (int i = 0; i < referenceNames.size(); i++) {

            String ref = referenceNames.get(i);
            File f = files.get(i);

            // write the name of the reference
            SavantFileFormatterUtils.writeString(outFileStream, ref);

            // write the byte offset to the data
            outFileStream.writeLong(refOffset);

            // write the length of the data
            outFileStream.writeLong(f.length());

            LOG.debug("Ref: " + ref + " Fn: " + f + " Offset: " + refOffset + " Length: " + f.length());

            // increment the offset for next iteration
            refOffset += f.length();
        }
    }


    private void concatenateFiles() throws FileNotFoundException, IOException {
        LOG.debug("Concatenating files...");

        // 10MB buffer
        byte[] buffer = new byte[1024*10000];
        int bytesRead = 0;
        int currreference = 0;
        int numreferences = files.size();

        int b = outFileStream.size();

        for (File f : files) {

            currreference++;

            setSubtaskStatus("Writing output file (part " + currreference + " of " + numreferences + ") ...");

            if (LOG.isDebugEnabled()) {
                LOG.debug("COPY: [ FILE:\t" + f + "] (" + currreference + " of " + numreferences + ")\t");
                LOG.debug("[ bytes/copied: " + f.length() + " / ");
                LOG.debug("Copying " + f);
            }


            long totalBytesToRead = f.length();

            long br = 0;

            BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));

            while((bytesRead = is.read(buffer)) > 0) {
                br += bytesRead;
                LOG.debug("Read " + bytesRead + " bytes");
                outFileStream.write(buffer, 0, bytesRead);
                this.setSubtaskProgress((int)((br*100)/totalBytesToRead));
            }

            LOG.debug(br + " ]");

            is.close();
        }

        LOG.debug(b + " bytes in file after concatenation");

        outFileStream.flush();
    }

    private void deleteFiles() {
        for (File f : files) {
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }
    }

    public int getProgressAsInteger(float current, float total) {
        return (int) Math.round((current*100/total));
    }

}
