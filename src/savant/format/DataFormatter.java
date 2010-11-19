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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.file.*;
import savant.util.*;
import java.io.*;
import java.net.URI;
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

    private static Log LOG = LogFactory.getLog(DataFormatter.class);

    private SavantFileFormatter sff;

    /**
     * Input path
     */
    private File inFile;

    /**
     * output path
     */
    private File outFile;

    public final File getInputFile() { return inFile; }

    public File getOutputFile() {
        if (getInputFileType() == FileType.INTERVAL_BAM) {
            return new File(inFile + ".cov.savant").getAbsoluteFile();
        } else {
            return outFile;
        }
    }

    public FileType getInputFileType() { return inputFileType; }

    /**
     * File type
     */
    private FileType inputFileType;

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
    public DataFormatter(File inFile, File outFile, FileType fileType, boolean isInputOneBased) {

        this.inFile = inFile;   // set the desired input file path
        this.outFile = outFile; // set the desired output file path
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
    public DataFormatter(File inFile, File outFile, FileType fileType) {
        this(inFile, outFile, fileType, true);
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
    public boolean format() throws InterruptedException, IOException, ParseException, SavantFileFormattingException {

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
                if (!verifyTextFile(inFile)) {
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
            //deleteOutputFiles();
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
        FastaFormatter ff = new FastaFormatter(inFile, outFile);
        subscribeProgressListeners(ff, this.progressListeners);
        runFormatter(ff);
        unsubscribeProgressListeners(ff, this.progressListeners);
    }

    /*
     * CONTINUOUS : GENERIC
     * @return
     */
    private void formatAsContinuousGeneric() throws IOException, InterruptedException, SavantFileFormattingException {
        ContinuousGenericFormatter cgf = new ContinuousGenericFormatter(inFile, outFile);
        subscribeProgressListeners(cgf, this.progressListeners);
        runFormatter(cgf);
        unsubscribeProgressListeners(cgf, this.progressListeners);
    }

    /*
     * CONTINUOUS : WIG
     * @return
     */
    private void formatAsContinuousWIG() throws IOException, InterruptedException, ParseException, SavantFileFormattingException {
        WIGFormatter wtc = new WIGFormatter(this.inFile, this.outFile);
        subscribeProgressListeners(wtc, this.progressListeners);
        runFormatter(wtc);
        unsubscribeProgressListeners(wtc, this.progressListeners);
    }

    /*
     * CONTINUOUS : BAM
     * @return
     */
    private void formatAsBAM() throws IOException, InterruptedException, SavantFileFormattingException {
            BAMToCoverage btc = new BAMToCoverage(inFile);
            subscribeProgressListeners(btc, this.progressListeners);
            runFormatter(btc);
            unsubscribeProgressListeners(btc, this.progressListeners);
    }

    /*
     * INTERVAL
     */
    private void formatAsInterval(String type) throws IOException, InterruptedException, SavantFileFormattingException {
        IntervalFormatter inf;

        if(type.equals("GEN")){
            inf = new IntervalFormatter(inFile, outFile,baseOffset, FileType.INTERVAL_GENERIC, 0, 1, 2, "#");
            inf.formatAsIntervalGeneric();
        } else if(type.equals("GFF")){
            inf = new IntervalFormatter(inFile, outFile,baseOffset, FileType.INTERVAL_GFF, 0, 3, 4, "#");
            inf.formatAsIntervalGFF();
        } else if(type.equals("BED")){
            inf = new IntervalFormatter(inFile, outFile,baseOffset, FileType.INTERVAL_BED, 0, 1, 2, "#");
            inf.formatAsIntervalBED();
        } else {
            return;
        }

        subscribeProgressListeners(inf, this.progressListeners);

        LOG.info("Beginning formatting");

        runFormatter(inf);

        LOG.info("Formatting complete");

        unsubscribeProgressListeners(inf, this.progressListeners);
    }

    /**
     * POINT : GENERIC
     * @return
     * @throws IOException
     */
    private void formatAsPointGeneric() throws IOException, InterruptedException, SavantFileFormattingException {
        PointGenericFormatter pgf = new PointGenericFormatter(this.inFile, this.outFile, this.baseOffset);
        subscribeProgressListeners(pgf, this.progressListeners);
        runFormatter(pgf);
        unsubscribeProgressListeners(pgf, this.progressListeners);
    }

    public static Map<String,IntervalSearchTree> readIntervalBSTs(SavantROFile dFile) throws IOException {

        // read the refname -> index position map
        Map<String,Long[]> refMap = SavantFileUtils.readReferenceMap(dFile);

        if (LOG.isDebugEnabled()) LOG.debug("\n=== DONE PARSING REF<->DATA MAP ===\n\n");

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

        if (LOG.isDebugEnabled()) LOG.debug("Number of trees to get: " + refMap.keySet().size());

        // keep track of the maximum end of tree position
        // (IMPORTANT NOTE: order of elements returned by keySet() is not gauranteed!!!)
        long maxend = Long.MIN_VALUE;
        for (String refname : refMap.keySet()) {
            Long[] v = refMap.get(refname);
            if (LOG.isDebugEnabled()) LOG.debug("========== Reading tree for reference " + refname + " ==========");
            dFile.seek(v[0] + dFile.getHeaderOffset());

            if (LOG.isDebugEnabled()) LOG.debug("Starting tree at: " + dFile.getFilePointer());

            IntervalSearchTree t = readIntervalBST(dFile);

            if (LOG.isDebugEnabled()) LOG.debug("Finished tree at: " + dFile.getFilePointer());

            maxend = Math.max(maxend,dFile.getFilePointer());

            trees.put(refname, t);
            treenum++;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Read " + treenum + " trees (i.e. indicies)");
            LOG.debug("\n=== DONE PARSING REF<->INDEX MAP ===");
            LOG.debug("Changing offset from " + dFile.getHeaderOffset() + " to " + (dFile.getFilePointer()+dFile.getHeaderOffset()) + "\n");
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

            LOG.debug("Reading node at byte position: " + file.getFilePointer());

            // read in the node fields
            List<Object> r1;
            try {
                r1 = SavantFileUtils.readBinaryRecord(file, fields);
            } catch (EOFException e) {
                LOG.error("Hit EOF while trying to parse IntervalSearchTree from file");
                break;
            }

            // create an IntervalTreeNode
            IntervalTreeNode n = new IntervalTreeNode((Range) r1.get(1), (Integer) r1.get(0));
            if (n.index == -1) {
                LOG.debug("Tree contains " + i + " nodes");
                break;
            }   // the "null" terminator node has -1 as its index

            if (LOG.isDebugEnabled()) {
                LOG.debug("Node params read: ");
                for (int j = 0; j < 6; j++) {
                    LOG.debug(j + ". " + r1.get(j));
                }
            }

            n.startByte = (Long) r1.get(2);
            n.size = (Integer) r1.get(3);
            n.subtreeSize = (Integer) r1.get(4);
            nodeIndex2ParentIndices.put(n.index, (Integer) r1.get(5));

            if (LOG.isDebugEnabled()) LOG.debug("Node:\tindex: " + n.index + "\trange: " + n.range + "\tsize: " + n.size + "\tsubsize: " + n.subtreeSize + "\tbyte: " + n.startByte);


            // add this node to the list
            nodes.add(n);

            i++;
            LOG.debug((i) + ". Read node with range " + n.range + " and index " + n.index);
        }

        // sort node list by index
        Collections.sort(nodes);

        LOG.debug("Finished parsing IBST");

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

           if (LOG.isDebugEnabled()) LOG.debug("Node " + n.index + " [ ");

           for (Integer childIndex : cis) {
               if (LOG.isDebugEnabled()) LOG.debug(childIndex + " ");
                n.children.add(nodes.get(childIndex));
           }

           if (LOG.isDebugEnabled()) LOG.debug("]");
       }

       return new IntervalSearchTree(nodes);
    }


    /** FILE OPENING **/

    private DataOutputStream openNewOutputFile() throws IOException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
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

    private boolean verifyTextFile(File fileName) {
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

    private void runFormatter(SavantFileFormatter ff) throws IOException, InterruptedException, SavantFileFormattingException {
        sff = ff;
        ff.format();
    }
}
