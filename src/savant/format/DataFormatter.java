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

import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.FieldType;
import savant.format.util.data.interval.IntervalSearchTree;
import savant.format.util.data.interval.IntervalTreeNode;
import savant.util.*;
import java.io.*;
import java.text.ParseException;
import java.util.*;

// TODO: make this a DataFormatterFactory, make things like WIGToContinuous inherit from an abstract class, implement all formats as classes.
/**
 * Class to perform formatting of biological data files (FASTA, BED, etc.) into Savant's binary formats.
 * Sometimes a separate index file is created. Occasionally, auxiliary files are created, such as
 * coverage files for BAM maps.
 *
 * @author mfiume
 */
public class DataFormatter implements FormatProgressListener {

    private static boolean intersects(Range r1, Range r2) {
        if( r1.getFrom() >= r2.getTo() || r2.getFrom() >= r1.getTo()) {
            return false;
        } else {
            return true;
        }
    }

    // variables to keep track of progress processing the input file(s)
    private int progress; // 0 to 100%
    // property change support to make progress changes visible to UI
    // FIXME: figure out why PropertyChangeSupport does not work. Then get rid of FormatProgressListener and related stuff.
//    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private List<FormatProgressListener> listeners = new ArrayList<FormatProgressListener>();

    private String inPath;
    private String outPath;
    private String sortPath;
    private DataOutputStream outFile;
    private FileType fileType;
    private String tmpOutPath = "tmp";
    public static final String indexExtension = ".index";

    // TODO: this should be in a property file
    private static final int currentVersion = 1;

    int baseOffset = 0; // 0 if 1-based; 1 if 0-based

    public DataFormatter(String inPath, String outPath, FileType fileType, boolean isInputOneBased) {
        this.inPath = inPath;
        this.outPath = outPath;
        this.fileType = fileType;

        // progress indication for UI
        this.progress = 0;

        setInputOneBased(isInputOneBased);
    }

    public DataFormatter(String inPath, String outPath, FileType fileType) {
        this(inPath, outPath,fileType,true);
    }

    /*
     * Formats the file
     * @return
     */
    public boolean format() throws InterruptedException, IOException, ParseException {

        // Get current time
        long start = System.currentTimeMillis();

        try{

            // FIXME: another hack for coverage files
            if (this.fileType != FileType.INTERVAL_BAM) {

                // check that it really is a text file
                if (!verifyTextFile(inPath)) {
                    throw new IOException("Not a text file");
                }

                // create output file and write header
                outFile = this.openNewOutputFile();
                DataFormatUtils.writeFileTypeHeader(outFile, new FileTypeHeader(this.fileType,this.currentVersion));
            }

            switch (fileType) {
                case POINT_GENERIC:
                    formatAsPointGeneric();
                    break;
                case INTERVAL_BAM:
                    formatAsBAM();
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
        }
        finally {
            deleteTmpOutputFile();
        }

        // Get elapsed time in milliseconds
        long elapsedTimeMillis = System.currentTimeMillis()-start;

        // Get elapsed time in seconds
        float elapsedTimeSec = elapsedTimeMillis/1000F;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void progressUpdate(int value) {
        setProgress(value);
    }

    /*
     * SEQUENCE : FASTA
     * @return
     */
    private void formatAsSequenceFasta() throws IOException, InterruptedException {
        FastaFormatter ff = new FastaFormatter(this.inPath, this.outFile);
        ff.addProgressListener(this);
        ff.format();
        ff.removeProgressListener(this);
    }

    /*
     * CONTINUOUS : GENERIC
     * @return
     */
    private void formatAsContinuousGeneric() throws IOException, InterruptedException {
        ContinuousGenericFormatter cgf = new ContinuousGenericFormatter(this.inPath, this.outFile);
        cgf.addProgressListener(this);
        cgf.format();
        cgf.removeProgressListener(this);
    }

    /*
     * CONTINUOUS : WIG
     * @return
     */
    private void formatAsContinuousWIG() throws IOException, InterruptedException, ParseException {
        outFile.close();
        WIGToContinuous wtc = new WIGToContinuous(this.inPath, this.outPath);
        wtc.addProgressListener(this);
        wtc.format();
        wtc.removeProgressListener(this);
    }

    /*
     * CONTINUOUS : BAM
     * @return
     */
    private void formatAsBAM() throws IOException, InterruptedException {
        //outFile.close();
        BAMToCoverage btc = new BAMToCoverage(this.inPath);
        btc.addProgressListener(this);
        btc.format();
        btc.removeProgressListener(this);
    }

    /*
     * INTERVAL
     */
    private void formatAsInterval(String type) throws IOException, InterruptedException {
        IntervalFormatter inf = new IntervalFormatter(sortPath, inPath, baseOffset, outPath, outFile);
        inf.addProgressListener(this);
        if(type=="GEN"){
            inf.formatAsIntervalGeneric();
        } else if(type=="GFF"){
            inf.formatAsIntervalGFF();
        } else if(type=="BED"){
            inf.formatAsIntervalBED();
        } else {
            return;
        }
        inf.format();
        inf.removeProgressListener(this);
    }

    /**
     * POINT : GENERIC
     * @return
     * @throws IOException
     */
    private void formatAsPointGeneric() throws IOException, InterruptedException {
        PointGenericFormatter pgf = new PointGenericFormatter(this.inPath, this.outFile);
        pgf.addProgressListener(this);
        pgf.format();
        pgf.removeProgressListener(this);
    }

    /** INTERVAL BST **/
    /**
     * Write the Interval BST Index
     * @param indexOutFile The output file to which to write
     * @param ibst The BST to write
     * @throws IOException
     *
    private void writeIntervalBSTIndex(RandomAccessFile indexOutFile, IntervalSearchTree ibst, HashMap<Integer, Long> node2startByte) throws IOException {

        indexOutFile.writeInt(ibst.getNumNodes());

        for (IntervalTreeNode n : ibst.getNodes()) {
            writeIntervalTreeNode(n, indexOutFile);
        }
    }
     */

    public static IntervalSearchTree readIntervalBST(String indexFileName) throws IOException {

        RandomAccessFile indexRaf = RAFUtils.openFile(indexFileName, false);

        List<IntervalTreeNode> nodes = new ArrayList<IntervalTreeNode>();

        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.RANGE);
        fields.add(FieldType.LONG);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);

        HashMap<Integer,Integer> nodeIndex2ParentIndices = new HashMap<Integer,Integer>();

        int i = 0;

        while(true) {
            
            List<Object> r1;
            try {
                r1 = RAFUtils.readBinaryRecord(indexRaf, fields);
            }
            catch (EOFException e) {
                break;
            }

            IntervalTreeNode n = new IntervalTreeNode((Range) r1.get(1), (Integer) r1.get(0));

            //System.out.println((++i) + ". Read node with range " + n.range + " and index " + n.index);

            n.startByte = (Long) r1.get(2);
            n.size = (Integer) r1.get(3);
            n.subtreeSize = (Integer) r1.get(4);
            nodeIndex2ParentIndices.put(n.index, (Integer) r1.get(5));

            nodes.add(n);
        }

        System.out.println("Rearranging node list");

       Collections.sort(nodes);

       System.out.println("Finished parsing IBST");

       indexRaf.close();

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

           //System.out.print("Node " + n.index + " [ ");

           for (Integer childIndex : cis) {
               //System.out.print(childIndex + " ");
                n.children.add(nodes.get(childIndex));
           }

           //System.out.println("]");
       }

       return new IntervalSearchTree(nodes);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        int oldValue = this.progress;
        this.progress = progress;
//        propertyChangeSupport.firePropertyChange("progress", oldValue, this.progress);
        fireProgressUpdate(progress);
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

    private void deleteTmpOutputFile() {
        File f = new File(tmpOutPath);
        if (f.exists()) {
            f.delete();
        }
    }

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
        if (inputOneBased) { this.baseOffset = 1; }
        else { this.baseOffset = 0; }
    }

    private void fireProgressUpdate(int value) {
        for (FormatProgressListener listener : listeners) {
            listener.progressUpdate(value);
        }
    }

    public void addProgressListener(FormatProgressListener listener) {
        listeners.add(listener);
    }

    public void removeProgressListener(FormatProgressListener listener) {
        listeners.remove(listener);
    }

}
