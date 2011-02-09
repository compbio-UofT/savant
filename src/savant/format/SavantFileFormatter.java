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

import java.io.*;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.file.FileType;
import savant.file.FileTypeHeader;
import savant.file.FieldType;
import savant.file.SavantROFile;
import savant.settings.DirectorySettings;
import savant.util.MiscUtils;

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

    //protected String delimiter = "\\s+";
    protected String delimiter = "( |\t)+";
    //protected String delimiter = " ";

   // protected int baseOffset = 0; // 0 if *input* file is 1-based; 1 if 0-based

    //protected String tmpOutputPath = "tmp";
    public static String indexExtension = ".index";
    public static String sortedExtension = ".sorted";

    // size of the output buffer
    protected static final int OUTPUT_BUFFER_SIZE = 1024 * 128; // 128K

    //protected DataOutputStream out;

    protected BufferedReader inFileReader;  // input file reader
    protected DataOutputStream outFileStream;         // output file stream

    // map from reference name to files
    protected Map<String,DataOutputStream> referenceName2FileMap;
    protected Map<String,String> referenceName2FilenameMap;

    /* PROGRESS */

    // UI ...
    // variables to keep track of progress processing the input file
    protected long positionCount=0;
    //protected int progress=0; // 0 to 100%
    protected List<FormatProgressListener> listeners = new ArrayList<FormatProgressListener>();

    // non-UI ...
    // variables to keep track of progress processing the input file(s)
    protected long totalBytes;
    protected long byteCount;

    /* MISC */

    // TODO: remove, or make cleaner
    // stuff needed by IO; mandated by SavantFileFormatterUtils which we're depending on
    protected List<FieldType> fields;
    protected List<Object> modifiers;

    /**
     * PROGRESS
     */

   // public int currentSubtaskNum;
   // public int numberOfSubtasks;

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
    
    // public void incrementOverallProgress() {
   //      setOverallProgress(++currentSubtaskNum);
   //  }

   // public void setOverallProgress(int at) {
   //     setOverallProgress(at,numberOfSubtasks);
   // }

    public void incrementOverallProgress() {
        fireIncrementOverallProgress();
    }

    /*
    public void setOverallProgress(int at, int total) {
        System.out.println("Setting overall progress to " + at + " of " + total);
        currentSubtaskNum = at;
        numberOfSubtasks = total;
        fireOverallProgressUpdate(at, total);
    }
     * 
     */

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

     /*
    protected void fireOverallProgressUpdate(int at, int total) {
        for (FormatProgressListener listener : listeners) {
            listener.overallProgressUpdate(at,total);
        }
    }
      * 
      */

    public SavantFileFormatter(File inFile, File outFile, FileType fileType) {
        
        this.inFile = inFile;
        this.outFile = outFile;
        this.fileType = fileType;
        
        referenceName2FileMap = new HashMap<String,DataOutputStream>();
        referenceName2FilenameMap = new HashMap<String,String>();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Savant Formatter Created");
            LOG.debug("input URI: " + inFile);
            LOG.debug("output file: " + outFile);
        }
    }


    /**
     * FILE MANAGEMENT
     */

    protected DataOutputStream addReferenceFile(String referenceName) throws FileNotFoundException {

        String fn = DirectorySettings.getTmpDirectory() + System.getProperty("file.separator") + inFile.getName() + ".part_" + referenceName;
        //String fn = inFilePath + ".part_" + referenceName;

        DataOutputStream f = new DataOutputStream(
                new BufferedOutputStream(
                new FileOutputStream(fn), OUTPUT_BUFFER_SIZE));
        referenceName2FileMap.put(referenceName, f);
        referenceName2FilenameMap.put(referenceName,fn);
        return f;
    }

    public void closeOutputForReference(String refname) {
        OutputStream o = referenceName2FileMap.get(refname);
        //referenceName2FilenameMap.remove(refname);
        
        //OutputStream o = referenceName2FileMap.remove(refname);
        //referenceName2FilenameMap.remove(refname);

        try {
            o.flush();
            o.close();
        } catch (IOException ex) {
        }
    }

    protected DataOutputStream getFileForReference(String referenceName) throws FileNotFoundException {
        if (this.referenceName2FileMap.containsKey(referenceName)) {
            return this.referenceName2FileMap.get(referenceName);
        } else {
            return addReferenceFile(referenceName);
        }
    }

    protected void closeOutputStreams() {
        for (DataOutputStream o : referenceName2FileMap.values()) {
            try {
                o.close();
            } catch (Exception e) {}
        }
    }

    /**
     * Open the input file
     * @return
     * @throws FileNotFoundException
     */
    protected BufferedReader openInputFile() throws FileNotFoundException {
        return new BufferedReader(new FileReader(inFile));
    }

    /**
     * Open the output file
     * @return
     * @throws FileNotFoundException
     */
    protected DataOutputStream openOutputFile() throws FileNotFoundException {
        outFileStream = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(outFile), OUTPUT_BUFFER_SIZE));
        return outFileStream;
    }

    protected void deleteOutputFile() {
        if (outFile.exists()) {
            if (!outFile.delete()) {
                outFile.deleteOnExit();
            }
        }
    }

    private void deleteFile(String fn) {
        File delme = new File(fn);
        if (!delme.delete()) {
            delme.deleteOnExit();
        }
    }

    public void deleteOutputStreams() {
        for (String refname : this.referenceName2FileMap.keySet()) {
            String fn = this.referenceName2FilenameMap.get(refname);
            deleteFile(fn);
        }
    }

    protected void writeOutputFile() throws FileNotFoundException, IOException {
        // get a list of reference names (TODO: possibly sort them in some order)
        List<String> refnames = MiscUtils.set2List(this.referenceName2FileMap.keySet());

        writeOutputFile(refnames, this.referenceName2FilenameMap);
    }

    protected void writeSavantHeader(List<String> refnames, Map<String,String> refToDataFileNameMap) throws FileNotFoundException, IOException {
        closeOutputStreams();

        // open the output file
        outFileStream = this.openOutputFile();

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
        writeReferenceMap(outFileStream,refnames,refToDataFileNameMap);
        outFileStream.flush();
    }

    protected void writeAdditionalIndices(List<String> refnames, Map<String,String> refToIndexFileNameMap) throws FileNotFoundException, IOException {
        // 4. WRITE INDEX
        if (refToIndexFileNameMap != null) {
            LOG.debug("Writing reference<->index map");
            writeReferenceMap(outFileStream,refnames,refToIndexFileNameMap);
            List<String> indexfiles = this.getMapValuesInOrder(refnames, refToIndexFileNameMap);
            concatenateFiles(outFileStream,indexfiles);
            deleteFiles(indexfiles);
            outFileStream.flush();
        }
    }

    protected void writeData(List<String> refnames, Map<String,String> refToDataFileNameMap) throws IOException {
        List<String> outfiles = this.getMapValuesInOrder(refnames, refToDataFileNameMap);
        concatenateFiles(outFileStream,outfiles);
        deleteFiles(outfiles);
        outFileStream.flush();

        // close the output file
        outFileStream.close();
    }

    protected void writeOutputFile(List<String> refnames, Map<String,String> refToDataFileNameMap) throws FileNotFoundException, IOException {
        this.setSubtaskStatus("Writing output file ...");
        this.incrementOverallProgress();
        //this.setOverallProgress(++currentSubtaskNum, numberOfSubtasks);
        writeSavantHeader(refnames,refToDataFileNameMap);
        writeData(refnames,refToDataFileNameMap);
    }

    protected void writeIntervalOutputFile(List<String> refnames, Map<String,String> refToIndexFileNameMap, Map<String,String> refToDataFileNameMap) throws FileNotFoundException, IOException {
        //this.setOverallProgress(++currentSubtaskNum, numberOfSubtasks);
        this.setSubtaskStatus("Writing output file ...");
        this.incrementOverallProgress();
        writeSavantHeader(refnames,refToDataFileNameMap);
        writeAdditionalIndices(refnames,refToIndexFileNameMap);
        writeData(refnames,refToDataFileNameMap);
    }

    protected void writeContinuousOutputFile(List<String> refnames, Map<String,String> refToIndexFileNameMap, Map<String,String> refToDataFileNameMap) throws FileNotFoundException, IOException {
        //this.setOverallProgress(++currentSubtaskNum, numberOfSubtasks);
        this.setSubtaskStatus("Writing output file ...");
        this.incrementOverallProgress();
        writeSavantHeader(refnames,refToDataFileNameMap);
        writeAdditionalIndices(refnames,refToIndexFileNameMap);
        writeData(refnames,refToDataFileNameMap);
    }

    protected void writeReferenceMap(DataOutputStream f, List<String> refnames, Map<String, String> refnameToOutputFileMap) throws IOException {

        long refOffset = 0;

        f.writeInt(refnames.size());

        // write a record for each reference sequence
        for(String refname : refnames) {

            String fn = refnameToOutputFileMap.get(refname);

            // open the file
            File reffile = new File(fn);

            // write the name of the reference
            SavantFileFormatterUtils.writeString(f, refname);

            // write the byte offset to the data
            f.writeLong(refOffset);

            // write the length of the data
            f.writeLong(reffile.length());

            LOG.debug("Ref: " + refname + " Fn: " + fn + " Offset: " + refOffset + " Length: " + reffile.length());

            // increment the offset for next iteration
            refOffset += reffile.length();
        }
    }


    private void concatenateFiles(DataOutputStream f, List<String> filenames) throws FileNotFoundException, IOException {
        LOG.debug("Concatenating files...");

        // 10MB buffer
        byte[] buffer = new byte[1024*10000];
        int bytesRead = 0;
        int currreference = 0;
        int numreferences = filenames.size();

        int b = f.size();

        for (String fn : filenames) {

            currreference++;

            this.setSubtaskStatus("Writing output file (part " + currreference + " of " + numreferences + ") ...");

            File tmp = new File(fn);

            if (LOG.isDebugEnabled()) {
                LOG.debug("COPY: [ FILE:\t" + fn + "] (" + currreference + " of " + numreferences + ")\t");
                LOG.debug("[ bytes/copied: " + tmp.length() + " / ");
                LOG.debug("Copying " + fn);
            }


            long totalBytesToRead = tmp.length(); tmp = null;

            long br = 0;

            BufferedInputStream is = new BufferedInputStream(new FileInputStream(fn));

            while((bytesRead = is.read(buffer)) > 0) {
                br += bytesRead;
                LOG.debug("Read " + bytesRead + " bytes");
                f.write(buffer, 0, bytesRead);
                this.setSubtaskProgress((int)((br*100)/totalBytesToRead));
            }

            LOG.debug(br + " ]");

            is.close();

            //deleteFile(fn);
        }

        LOG.debug(b + " bytes in file after concatenation");

        f.flush();
        //f.close();
    }

    private List<String> getMapValuesInOrder(List<String> keys, Map<String, String> map) {
        List<String> valuesInOrder = new ArrayList<String>();
        for (String key : keys) {
            valuesInOrder.add(map.get(key));
        }
        return valuesInOrder;
    }

    private void deleteFiles(Collection<String> filenames) {
        for (String fn : filenames) {
            deleteFile(fn);
        }
    }

    public int getProgressAsInteger(float current, float total) {
        return (int) Math.round((current*100/total));
    }

}
