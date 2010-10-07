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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.file.*;
import savant.util.*;
import java.io.*;
import java.text.ParseException;
import java.util.*;


/**
 * Class to perform formatting of biological data files (FASTA, BED, etc.) into Savant's binary formats.
 * Sometimes a separate index file is created. Occasionally, auxiliary files are created, such as
 * coverage files for BAM maps.
 *
 * @author mfiume
 */
public class DataFormatter /* implements FormatProgressListener */ {

    private static Log log = LogFactory.getLog(DataFormatter.class);

    /**
     * VARIABLES
     */

    // input path
    private String inPath;

    // output path
    private String outPath;

    public String getInputFilePath() { return this.inPath; }
    public String getOutputFilePath() {
        if (this.getInputFileType() == FileType.INTERVAL_BAM) { return this.getInputFilePath() + ".cov.savant"; }
        else { return this.outPath; }
    }
    public FileType getInputFileType() { return this.inputFileType; }

    // sorted file path (temporary)
    //private String sortPath;

    // output file
    //private DataOutputStream outFile;

    // file type
    private FileType inputFileType;

    // path to tmp file
    //private String tmpOutPath = "tmp";

    // index extension
    //public static final String indexExtension = ".index";

    //private List<FormatStatusUpdateListener> statusListeners = new ArrayList<FormatStatusUpdateListener>();

    // variables to keep track of progress processing the input file(s)
    // property change support to make progress changes visible to UI
    // FIXME: figure out why PropertyChangeSupport does not work. Then get rid of FormatProgressListener and related stuff.
    // private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private List<FormatProgressListener> progressListeners = new ArrayList<FormatProgressListener>();


    // curent version of the formatter
    // TODO: this should be in a property file
    //private static final int currentVersion = 2;

    // used to generalize 0 vs 1-based input files
    boolean baseOffset = true; // 0 if 1-based; 1 if 0-based

    /**
     * CONSTRUCTORS
     */

    /**
     * Establishes a data formatter which can be run by
     * calling the format() function.
     * @param inPath input file path
     * @param outPath output file path (should not already exist)
     * @param fileType type of the input file (e.g. interval, point, etc)
     * @param isInputOneBased whether or not the file is one-based (i.e.
     * annotation of 10 refers to the 10th position in the genome, not the 9th
     * as in zero-based scheme)
     */
    public DataFormatter(String inPath, String outPath, FileType fileType, boolean isInputOneBased) {

        this.inPath = inPath;   // set the desired input file path
        this.outPath = outPath; // set the desired output file path
        this.inputFileType = fileType;  // set the input file type (e.g. interval, point, etc)

        setInputOneBased(isInputOneBased); // set the base offset
    }

    /**
     * Establishes a data formatter which can be run by
     * calling the format() function.
     *  - sets one-based to true
     * @param inPath input file path
     * @param outPath output file path (should not already exist)
     * @param fileType type of the input file (e.g. interval, point, etc)
     */
    public DataFormatter(String inPath, String outPath, FileType fileType) {
        this(inPath, outPath,fileType,true);
    }

    /**
     * FUNCTIONS
     */

    /**
     * Format the input file path, storing the result in the output file path
     * @return whether or not the format was successful
     * @throws InterruptedException
     * @throws IOException
     * @throws ParseException
     */
    public boolean format() throws InterruptedException, IOException, ParseException, SavantFileFormattingException, Exception {

        // start a timer
        long start = System.currentTimeMillis();

        try{

            // format a BAM file
            // (different than others because it has no Savant header)
            if (this.inputFileType == FileType.INTERVAL_BAM) {
                formatAsBAM();

            // format files with Savant header
            } else {

                // check that it really is a text file
                if (!verifyTextFile(inPath)) {
                    throw new IOException("Input file is not a text file");
                }

                // format the input file in the appropriate way
                switch (inputFileType) {
                    case POINT_GENERIC:
                        formatAsPointGeneric();
                        break;
                    case INTERVAL_GENERIC:
                        formatAsInterval("GEN");
                        break;
                    case INTERVAL_BED:
                        formatAsInterval("BED");
                        break;
                    case INTERVAL_GFF:
                        formatAsInterval("GFF");
                        break;
                    case CONTINUOUS_GENERIC:
                        formatAsContinuousGeneric();
                        break;
                    case CONTINUOUS_WIG:
                        formatAsContinuousWIG();
                        break;
                    case SEQUENCE_FASTA:
                        formatAsSequenceFasta();
                        break;
                    default:
                        return false;
                }

                // create output file and write header
                //outFile = this.openNewOutputFile();
                //DataFormatUtils.writeFileTypeHeader(outFile, new FileTypeHeader(this.inputFileType,this.currentVersion));
            }
        }
        finally {
            //deleteTmpOutputFile();
            //this.setProgress(false, 100);
        }

        // Get elapsed time in milliseconds
        long elapsedTimeMillis = System.currentTimeMillis()-start;

        // Get elapsed time in seconds
        float elapsedTimeSec = elapsedTimeMillis/1000F;

        return true;
    }

    /**
     * {@inheritDoc}
     *
    public void progressUpdate(boolean isSubtask, int progress, String status) {
        setProgress(isSubtask, progress);
    }
     */

    /*
     * SEQUENCE : FASTA
     * @return
     */
    private void formatAsSequenceFasta() throws IOException, InterruptedException, SavantFileFormattingException {
        FastaFormatter ff = new FastaFormatter(this.inPath, this.outPath);
        subscribeProgressListeners(ff, this.progressListeners);
        ff.format();
        unsubscribeProgressListeners(ff, this.progressListeners);
    }

    /*
     * CONTINUOUS : GENERIC
     * @return
     */
    private void formatAsContinuousGeneric() throws IOException, InterruptedException {
        ContinuousGenericFormatter cgf = new ContinuousGenericFormatter(this.inPath, this.outPath);
        subscribeProgressListeners(cgf, this.progressListeners);
        cgf.format();
        unsubscribeProgressListeners(cgf, this.progressListeners);
    }

    /*
     * CONTINUOUS : WIG
     * @return
     */
    private void formatAsContinuousWIG() throws IOException, InterruptedException, ParseException {
        WIGFormatter wtc = new WIGFormatter(this.inPath, this.outPath);
        subscribeProgressListeners(wtc, this.progressListeners);
        wtc.format();
        unsubscribeProgressListeners(wtc, this.progressListeners);
    }

    /*
     * CONTINUOUS : BAM
     * @return
     */
    private void formatAsBAM() throws IOException, InterruptedException, Exception {
        BAMToCoverage btc = new BAMToCoverage(this.inPath);
        subscribeProgressListeners(btc, this.progressListeners);
        btc.format();
        unsubscribeProgressListeners(btc, this.progressListeners);
    }

    /*
     * INTERVAL
     */
    private void formatAsInterval(String type) throws IOException, InterruptedException, SavantUnsupportedVersionException {
        IntervalFormatter inf;

        if(type.equals("GEN")){
            inf = new IntervalFormatter(inPath, outPath,baseOffset, FileType.INTERVAL_GENERIC, 0, 1, 2, "#");
            inf.formatAsIntervalGeneric();
        } else if(type.equals("GFF")){
            inf = new IntervalFormatter(inPath, outPath,baseOffset, FileType.INTERVAL_GFF, 0, 3, 4, "#");
            inf.formatAsIntervalGFF();
        } else if(type.equals("BED")){
            inf = new IntervalFormatter(inPath, outPath,baseOffset, FileType.INTERVAL_BED, 0, 1, 2, "#");
            inf.formatAsIntervalBED();
        } else {
            return;
        }

        subscribeProgressListeners(inf, this.progressListeners);

        log.info("Beginning formatting");

        inf.format();

        log.info("Formatting complete");

        unsubscribeProgressListeners(inf, this.progressListeners);
    }

    /**
     * POINT : GENERIC
     * @return
     * @throws IOException
     */
    private void formatAsPointGeneric() throws IOException, InterruptedException {
        PointGenericFormatter pgf = new PointGenericFormatter(this.inPath, this.outPath, this.baseOffset);
        subscribeProgressListeners(pgf, this.progressListeners);
        pgf.format();
        unsubscribeProgressListeners(pgf, this.progressListeners);
    }

    public static Map<String,IntervalSearchTree> readIntervalBSTs(SavantROFile dFile) throws IOException {

        // read the refname -> index position map
        Map<String,Long[]> refMap = SavantFileUtils.readReferenceMap(dFile);

        if (log.isDebugEnabled()) log.debug("\n=== DONE PARSING REF<->DATA MAP ===\n\n");

        // change the offset
        dFile.setHeaderOffset(dFile.getFilePointer());

        /*
        for (String s : refMap.keySet()) {
            Long[] vals = refMap.get(s);
            //System.out.println("Reference " + s + " at " + vals[0] + " of length " + vals[1]);
        }
         */

        Map<String,IntervalSearchTree> trees = new HashMap<String,IntervalSearchTree>();

        int treenum = 0;

        if (log.isDebugEnabled()) log.debug("Number of trees to get: " + refMap.keySet().size());

        // keep track of the maximum end of tree position
        // (IMPORTANT NOTE: order of elements returned by keySet() is not gauranteed!!!)
        long maxend = Long.MIN_VALUE;
        for (String refname : refMap.keySet()) {
            Long[] v = refMap.get(refname);
            if (log.isDebugEnabled()) log.debug("========== Reading tree for reference " + refname + " ==========");
            dFile.seek(v[0] + dFile.getHeaderOffset());

            if (log.isDebugEnabled()) log.debug("Starting tree at: " + dFile.getFilePointer());

            IntervalSearchTree t = readIntervalBST(dFile);

            if (log.isDebugEnabled()) log.debug("Finished tree at: " + dFile.getFilePointer());

            maxend = Math.max(maxend,dFile.getFilePointer());

            trees.put(refname, t);
            treenum++;
        }

        if (log.isDebugEnabled()) {
            log.debug("Read " + treenum + " trees (i.e. indicies)");
            log.debug("\n=== DONE PARSING REF<->INDEX MAP ===");
            log.debug("Changing offset from " + dFile.getHeaderOffset() + " to " + (dFile.getFilePointer()+dFile.getHeaderOffset()) + "\n");
        }

        // set the header offset appropriately
        dFile.setHeaderOffset(maxend);

        return trees;
    }

    /**
     * Reads an IntervalSearchTree from file
     * @param file The file containing the IntervalSearchTree. The IntervalSearchTree
     * must start at the current position in the file.
     * @return An IntervalSearchTree which was represented in the file
     * @throws IOException
     */
    private static IntervalSearchTree readIntervalBST(SavantROFile file) throws IOException {

        //RandomAccessFile file = SavantFileUtils.openFile(indexFileName, false);

        // the node list
        List<IntervalTreeNode> nodes = new ArrayList<IntervalTreeNode>();

        // fields for a node
        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);  // node ID
        fields.add(FieldType.RANGE);    // range
        fields.add(FieldType.LONG);     // start position in file
        fields.add(FieldType.INTEGER);  // size
        fields.add(FieldType.INTEGER);  // subtree size
        fields.add(FieldType.INTEGER);  // parent index

        // map from node index to parent index
        HashMap<Integer,Integer> nodeIndex2ParentIndices = new HashMap<Integer,Integer>();

        int i = 0;

        // keep reading nodes until done
        while(true) {

            if (log.isDebugEnabled()) log.debug("Reading node at byte position: " + file.getFilePointer());

            // read in the node fields
            List<Object> r1;
            try { r1 = SavantFileUtils.readBinaryRecord(file, fields); }
            catch (EOFException e) {
                System.err.println("error: hit EOF while trying to parse IntervalSearchTree from file");
                break;
            }

            // create an IntervalTreeNode
            IntervalTreeNode n = new IntervalTreeNode((Range) r1.get(1), (Integer) r1.get(0));
            if (n.index == -1) {
                if (log.isDebugEnabled()) log.debug("Tree contains " + i + " nodes");
                break;
            }   // the "null" terminator node has -1 as its index

            if (log.isDebugEnabled()) {
                log.debug("Node params read: ");
                for (int j = 0; j < 6; j++) {
                    log.debug(j + ". " + r1.get(j));
                }
            }

            n.startByte = (Long) r1.get(2);
            n.size = (Integer) r1.get(3);
            n.subtreeSize = (Integer) r1.get(4);
            nodeIndex2ParentIndices.put(n.index, (Integer) r1.get(5));

            if (log.isDebugEnabled()) log.debug("Node:\tindex: " + n.index + "\trange: " + n.range + "\tsize: " + n.size + "\tsubsize: " + n.subtreeSize + "\tbyte: " + n.startByte);


            // add this node to the list
            nodes.add(n);

            i++;
            if (log.isDebugEnabled()) log.debug((i) + ". Read node with range " + n.range + " and index " + n.index);
        }

        // sort node list by index
        Collections.sort(nodes);

        if (log.isDebugEnabled()) log.debug("Finished parsing IBST");

        // make a map of node to child indicies
       HashMap<Integer,List<Integer>> nodeIndex2ChildIndices = new HashMap<Integer,List<Integer>>();

       for (Integer key : nodeIndex2ParentIndices.keySet()) {
           int parent = nodeIndex2ParentIndices.get(key);
           if (!nodeIndex2ChildIndices.containsKey(parent)) {
               nodeIndex2ChildIndices.put(parent, new ArrayList<Integer>());
           }
           List<Integer> children = nodeIndex2ChildIndices.get(parent);
           children.add(key);
           nodeIndex2ChildIndices.put(parent, children);
       }

       for (Integer index : nodeIndex2ChildIndices.keySet()) {

           if (index == -1) { continue; }

           IntervalTreeNode n = nodes.get(index);
           List<Integer> cis = nodeIndex2ChildIndices.get(index);

           if (log.isDebugEnabled()) log.debug("Node " + n.index + " [ ");

           for (Integer childIndex : cis) {
               if (log.isDebugEnabled()) log.debug(childIndex + " ");
                n.children.add(nodes.get(childIndex));
           }

           if (log.isDebugEnabled()) log.debug("]");
       }

       return new IntervalSearchTree(nodes);
    }


    /** FILE OPENING **/

    private DataOutputStream openNewOutputFile() throws IOException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outPath)));
    }


    /**
     * Open the output file
     * @return
     * @throws FileNotFoundException
     */
    private RandomAccessFile openOutputRAFFile(String path) throws IOException {
        RandomAccessFile f = new RandomAccessFile(path, "rw");
        if (f != null) f.seek(f.length());
        return f;
    }

    /*
    private void deleteTmpOutputFile() {
        File f = new File(tmpOutPath);
        if (f.exists()) {
            f.delete();
        }
    }
     */

    private boolean verifyTextFile(String fileName) {
        boolean result = false;
        BufferedReader reader=null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            char[] readBuf = new char[1000];
            int charsRead = reader.read(readBuf);
            if (charsRead != -1) {
                String readStr = new String(readBuf);
                if (readStr.contains("\r") || readStr.contains("\n")) {
                    // newline found in first 1000 characters, probably is a text file
                    result = true;
                }
            }
        } catch (IOException e) {
            // result will be false
        } finally {
            try { if (reader != null) reader.close(); } catch (IOException ignore) {}
        }
        return result;
    }

    private void setInputOneBased(boolean inputOneBased) {
        if (inputOneBased) { this.baseOffset = true; }
        else { this.baseOffset = false; }
    }

    public void addProgressListener(FormatProgressListener listener) {
        progressListeners.add(listener);
    }

    public void removeProgressListener(FormatProgressListener listener) {
        progressListeners.remove(listener);
    }

    private void subscribeProgressListeners(SavantFileFormatter ff, List<FormatProgressListener> progressListeners) {
        for (FormatProgressListener listener : progressListeners) {
            ff.addProgressListener(listener);
        }
    }

    private void unsubscribeProgressListeners(SavantFileFormatter ff, List<FormatProgressListener> progressListeners) {
        for (FormatProgressListener listener : progressListeners) {
            ff.removeProgressListener(listener);
        }
    }
}
