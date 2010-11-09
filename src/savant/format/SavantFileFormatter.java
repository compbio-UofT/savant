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

public class SavantFileFormatter {

    /* LOG */

    // a log
    protected Log log;

    /* FILES */
    protected String inFilePath;        // input file path
    protected String outFilePath;       // output file path
    //protected String sortPath;        // sort path

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

    public SavantFileFormatter(String inFilePath, String outFilePath, FileType fileType) {
        
        this.inFilePath = inFilePath;
        this.outFilePath = outFilePath;
        this.fileType = fileType;
        
        log = LogFactory.getLog(SavantFileFormatter.class);
        referenceName2FileMap = new HashMap<String,DataOutputStream>();
        referenceName2FilenameMap = new HashMap<String,String>();

        if (log.isDebugEnabled()) {
            log.debug("Savant Formatter Created");
            log.debug("input file: " + inFilePath);
            log.debug("output file: " + outFilePath);
        }
    }


    /**
     * FILE MANAGEMENT
     */

    protected DataOutputStream addReferenceFile(String referenceName) throws FileNotFoundException {

        String fn = DirectorySettings.getTmpDirectory() + System.getProperty("file.separator") + (new File(inFilePath)).getName() + ".part_" + referenceName;
        //String fn = inFilePath + ".part_" + referenceName;

        DataOutputStream f = new DataOutputStream(
                new BufferedOutputStream(
                new FileOutputStream(fn), OUTPUT_BUFFER_SIZE));
        referenceName2FileMap.put(referenceName, f);
        referenceName2FilenameMap.put(referenceName,fn);
        return f;
    }

    protected DataOutputStream getFileForReference(String referenceName) throws FileNotFoundException {
        if (this.referenceName2FileMap.containsKey(referenceName)) {
            return this.referenceName2FileMap.get(referenceName);
        } else {
            return addReferenceFile(referenceName);
        }
    }

    /*
    protected void addReferenceFile(String referenceName, DataOutputStream file) {
        referenceName2FileMap.put(referenceName, file);
    }
     * 
     */

    /*
    protected void closeOutputFiles() {
        for (String ref : this.referenceName2FileMap.keySet()) {
            DataOutputStream f = this.referenceName2FileMap.get(ref);
            try {
                f.close();
            } catch (IOException ex) {
                log.warn("could not close output file: "
                        + this.referenceName2FilenameMap.get(ref));
            }
        }
    }
     */

    protected void closeOutputStreams() throws IOException {
        for (DataOutputStream o : referenceName2FileMap.values()) {
            o.close();
        }
    }

    /*
    protected DataOutputStream initOutput() throws FileNotFoundException {

        // open output stream
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFilePath), OUTPUT_BUFFER_SIZE));

        // write file type header
        FileTypeHeader fileTypeHeader = new FileTypeHeader(FileType.CONTINUOUS_GENERIC, 1);
        out.writeInt(fileTypeHeader.fileType.getMagicNumber());
        out.writeInt(fileTypeHeader.version);

        // prepare and write fields header
        fields = new ArrayList<FieldType>();
        fields.add(FieldType.FLOAT);
        modifiers = new ArrayList<Object>();
        modifiers.add(null);
        out.writeInt(fields.size());
        for (FieldType ft : fields) {
            out.writeInt(ft.ordinal());
        }

        return out;
    }
     */

    /*
    protected void closeOutput() {
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            log.warn("Error closing output file", e);
        }
    }
     */

    /**
     * Open the input file
     * @return
     * @throws FileNotFoundException
     */
    protected BufferedReader openInputFile() throws FileNotFoundException {
        return new BufferedReader(new FileReader(inFilePath));
    }

    /**
     * Open the output file
     * @return
     * @throws FileNotFoundException
     */
    protected DataOutputStream openOutputFile() throws FileNotFoundException {
        outFileStream = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(outFilePath), OUTPUT_BUFFER_SIZE));
        return outFileStream;
    }

    /*
    protected DataOutputStream openNewTmpOutputFile() throws IOException {
        deleteTmpOutputFile();
        return openTmpOutputFile();
    }
     */
     

    /**
     * Open the output file
     * @return
     * @throws FileNotFoundException
     *
    protected DataOutputStream openTmpOutputFile() throws IOException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpOutputPath)));
    }

    protected void deleteTmpOutputFile() {
        File f = new File(tmpOutputPath);
        if (f.exists()) {
            f.delete();
        }
    }
     */

    protected void deleteOutputFile() {
        File f = new File(outFilePath);
        if (f.exists()) {
            if (!f.delete()) {
                f.deleteOnExit();
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
        if (log.isDebugEnabled()) log.debug("Writing file type header");
        FileTypeHeader fileTypeHeader = new FileTypeHeader(this.fileType, SavantROFile.CURRENT_FILE_VERSION);
        SavantFileFormatterUtils.writeFileTypeHeader(outFileStream,fileTypeHeader);
        outFileStream.flush();

        // 2. WRITE FIELD HEADER
        if (log.isDebugEnabled()) log.debug("Writing fields header");
        SavantFileFormatterUtils.writeFieldsHeader(outFileStream, fields);
        outFileStream.flush();

        // 3. WRITE REFERENCE MAP
        if (log.isDebugEnabled()) log.debug("Writing reference<->data map");
        writeReferenceMap(outFileStream,refnames,refToDataFileNameMap);
        outFileStream.flush();
    }

    protected void writeAdditionalIndices(List<String> refnames, Map<String,String> refToIndexFileNameMap) throws FileNotFoundException, IOException {
        // 4. WRITE INDEX
        if (refToIndexFileNameMap != null) {
            if (log.isDebugEnabled()) log.debug("Writing reference<->index map");
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

            if (log.isDebugEnabled()) log.debug("Ref: " + refname + " Fn: " + fn + " Offset: " + refOffset + " Length: " + reffile.length());

            // increment the offset for next iteration
            refOffset += reffile.length();
        }
    }


    private void concatenateFiles(DataOutputStream f, List<String> filenames) throws FileNotFoundException, IOException {
        if (log.isDebugEnabled()) log.debug("Concatenating files...");

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

            if (log.isDebugEnabled()) {
                log.debug("COPY: [ FILE:\t" + fn + "] (" + currreference + " of " + numreferences + ")\t");
                log.debug("[ bytes/copied: " + tmp.length() + " / ");
                log.debug("Copying " + fn);
            }


            long totalBytesToRead = tmp.length(); tmp = null;

            long br = 0;

            BufferedInputStream is = new BufferedInputStream(new FileInputStream(fn));

            while((bytesRead = is.read(buffer)) > 0) {
                br += bytesRead;
                if (log.isDebugEnabled()) log.debug("Read " + bytesRead + " bytes");
                f.write(buffer, 0, bytesRead);
                this.setSubtaskProgress((int)((br*100)/totalBytesToRead));
            }

            if (log.isDebugEnabled()) log.debug(br + " ]");

            is.close();

            //deleteFile(fn);
        }

        if (log.isDebugEnabled()) log.debug(b + " bytes in file after concatenation");

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
