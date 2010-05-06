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

import it.unipi.di.util.ExternalSort;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.format.comparator.LineRangeComparator;
import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.FieldType;
import savant.format.util.data.interval.IntervalSearchTree;
import savant.format.util.data.interval.IntervalTreeNode;
import savant.format.util.data.interval.LinePlusRange;
import savant.util.*;

import java.io.*;
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

    private static final Log log = LogFactory.getLog(DataFormatter.class);

    public static final String indexExtension = ".index";

    public static final int RECORDS_PER_INTERRUPT_CHECK = 100;

    private static final long DEFAULT_RUN_SIZE = (long)(12.5 * Math.pow(2, 20)); // 12.5MB

    private static boolean intersects(Range r1, Range r2) {
        if( r1.getFrom() >= r2.getTo() || r2.getFrom() >= r1.getTo()) {
            return false;
        } else {
            return true;
        }
    }

    // variables to keep track of progress processing the input file(s)
    private long totalBytes;
    private long byteCount;
    private int progress; // 0 to 100%
    // property change support to make progress changes visible to UI
    // FIXME: figure out why PropertyChangeSupport does not work. Then get rid of FormatProgressListener and related stuff.
//    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private List<FormatProgressListener> listeners = new ArrayList<FormatProgressListener>();


    private static final String tmpOutPath = "tmp";
    private String inPath;
    private String outPath;
    private String sortPath;
    private DataOutputStream outFile;
    private FileType fileType;

    // TODO: this should be in a property file
    private static final int currentVersion = 1;

    int baseOffset = 0; // 0 is 1-based; 1 if 0-based

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
    public boolean format() throws InterruptedException, IOException {

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
                    formatAsIntervalGeneric();
                    break;
                case INTERVAL_BED:
                    formatAsIntervalBED();
                    break;
                case INTERVAL_GFF:
                    formatAsIntervalGFF();
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

        // Initialize the total size of the input file, for purposes of tracking progress
        this.totalBytes = new File(inPath).length();

        BufferedReader inFile = this.openInputFile();

        List<FieldType> fields = new ArrayList<FieldType>();
        DataFormatUtils.writeFieldsHeader(outFile, fields);

        //Read File Line By Line
        try {
            String strLine;
            boolean done = false;
            while (!done) {
                for (int i=0; i<RECORDS_PER_INTERRUPT_CHECK; i++) {
                    if ((strLine = inFile.readLine()) == null) {
                        done = true;
                        break;
                    }
                    // update bytes read from input
                    this.byteCount += strLine.getBytes().length;
                    // generate output
                    if (strLine.charAt(0) != '>') {
                        outFile.writeBytes(strLine);
                    }
                }
                // check to see if format has been cancelled
                if (Thread.interrupted()) throw new InterruptedException();
                // update progress property for UI
                updateProgress();
            }
        }
        finally {
            inFile.close();
            outFile.close();

        }
    }

    /*
     * CONTINUOUS : GENERIC
     * @return
     */
    private void formatAsContinuousGeneric() throws IOException, InterruptedException {


        // Initialize the total size of the input file, for purposes of tracking progress
        this.totalBytes = new File(inPath).length();

        BufferedReader inFile = this.openInputFile();

        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.FLOAT);

        List<Object> modifiers = new ArrayList<Object>();
        modifiers.add(null);

        DataFormatUtils.writeFieldsHeader(outFile, fields);

        try {
            String strLine;
            List<Object> line;
            boolean done = false;
            while (!done) {
                for (int i=0; i<RECORDS_PER_INTERRUPT_CHECK; i++) {
                    if ((strLine = inFile.readLine()) == null) {
                        done = true;
                        break;
                    }
                    // update bytes read from input
                    this.byteCount += strLine.getBytes().length;
                    // parse input and write output
                    if ((line = DataFormatUtils.parseTxtLine(strLine, fields)) != null) {
                        DataFormatUtils.writeBinaryRecord(outFile, line, fields, modifiers);
                    }
                }
                // check to see if format has been cancelled
                if (Thread.interrupted()) throw new InterruptedException();
                // update progress property for UI
                updateProgress();
            }
        } finally {
            inFile.close();
            outFile.close();
        }

    }

    /*
     * CONTINUOUS : GENERIC
     * @return
     */
    private void formatAsContinuousWIG() throws IOException, InterruptedException {


        WIGToContinuous wtc = new WIGToContinuous(this.inPath, this.outPath);
        wtc.addProgressListener(this);
        wtc.format();
        wtc.removeProgressListener(this);
    }


    private void formatAsBAM() throws IOException, InterruptedException {

//        RangeController rc = RangeController.getInstance();

//        BAMToCoverage btc = new BAMToCoverage(this.inPath, this.inPath + ".bai", this.inPath + ".cov", null, rc.getMaxRange().getLength() );
        BAMToCoverage btc = new BAMToCoverage(this.inPath);
        btc.addProgressListener(this);
        btc.format();
        btc.removeProgressListener(this);

    }

    // TODO: interrupts and progress
    private void formatAsInterval(List<FieldType> fields, List<Object> modifiers) throws FileNotFoundException, IOException, InterruptedException {


        List<Range> intervalIndex2IntervalRange = new ArrayList<Range>();
        List<Long> intevalIndex2StartByte = new ArrayList<Long>();

        DataOutputStream tmpOutFile = this.openNewTmpOutputFile();

        /*
         * STEP 1:
         * Go through inFile and...
         *  - get max range
         *  - get description length
         *  - write intervals in binary to tmpfile
         *  - create a line number -> <startbyte,endbyte> map (bytes correspond to tmpfile)
         *  - create a line number -> Range map
         */
//         System.out.println("=== STEP 1 ===");

        int minRange = Integer.MAX_VALUE;
        int maxRange = Integer.MIN_VALUE;

        BufferedReader inFile = this.openInputFile();
        String strLine;
        List<Object> line;
        while((strLine = inFile.readLine()) != null) {

            if (strLine.equals("")) { continue; }

//            System.out.println(strLine);

            line = DataFormatUtils.parseTxtLine(strLine, fields);

            line.set(0, ((Integer)line.get(0)) + this.baseOffset);
            line.set(1, ((Integer)line.get(1))  + this.baseOffset);

            int startInterval = (Integer) line.get(0);
            int endInterval = (Integer) line.get(1);

            minRange = Math.min(minRange, startInterval);
            maxRange = Math.max(maxRange, endInterval);

            intervalIndex2IntervalRange.add(new Range(startInterval, endInterval));
            intevalIndex2StartByte.add((long)tmpOutFile.size());

            //System.out.println("Writing [" + startInterval + "," + endInterval + "] to tmp file");
            //System.out.println("Saving pointer to [" + tmpOutFile.size() + "]");

            DataFormatUtils.writeBinaryRecord(tmpOutFile, line, fields, modifiers);
        }

        /*
         * STEP 2:
         *  - create an interval BST
         *  - put each interval into appropriate bin
         *  - create a bin -> List<line> map
         *  - write each bin to outfile
         */
//        System.out.println("=== STEP 2 ===");

        DataFormatUtils.writeFieldsHeader(outFile, fields);
        tmpOutFile.close();

        String indexPath = outPath + indexExtension;
        File f = new File(indexPath);
        if (f.exists()) f.delete();
        RandomAccessFile indexOutFile = RAFUtils.openFile(indexPath);

        RandomAccessFile indexFile = new RandomAccessFile(tmpOutPath, "r");

        IntervalSearchTree ibst = new IntervalSearchTree(new Range(minRange, maxRange));

        HashMap<Integer,List<LinePlusRange>> nodeIndex2IntervalIndices = new HashMap<Integer,List<LinePlusRange>>();

        IntervalTreeNode currentSmallestNode = ibst.getNodeWithSmallestMax();

        //HashMap<Integer,Long> node2startByte = new HashMap<Integer,Long>();

        int lineNum = 0;
        for (Range r : intervalIndex2IntervalRange) {

            //System.out.println("Adding :" + r);

            currentSmallestNode = ibst.getNodeWithSmallestMax();
            while (r.getFrom() > currentSmallestNode.range.getTo()) {
                if (currentSmallestNode.size == 0) {
                    currentSmallestNode.startByte = (long)-1;
                    //node2startByte.put(currentSmallestNode.index, (long) -1);
                } else {
                    currentSmallestNode.startByte = (long)outFile.size();
                    //node2startByte.put(currentSmallestNode.index, (long)outFile.size());
                }
                writeIntervalTreeNode(currentSmallestNode, indexOutFile);
                writeBinToOutfile(currentSmallestNode, indexFile, outFile, nodeIndex2IntervalIndices, intevalIndex2StartByte, fields, modifiers);

                ibst.removeNode(currentSmallestNode);
                currentSmallestNode = ibst.getNodeWithSmallestMax();
            }

            IntervalTreeNode n = ibst.insert(r);

//            System.out.println("I " + n.index + "\t" + r);

            if (n.range.getTo() < currentSmallestNode.range.getTo() || (n.range.getTo() == currentSmallestNode.range.getTo() && n.range.getFrom() > currentSmallestNode.range.getFrom()) )
            {
                currentSmallestNode = n;
                //System.out.println("Smallest max:" + currentSmallestNode.range.getTo());
            }

            //System.out.println("Adding Interval: " + r + "\tNode: " + n.range + "\tIndex: " + n.index);

            List<LinePlusRange> lines;
            if (nodeIndex2IntervalIndices.containsKey(n.index)) {
                lines = nodeIndex2IntervalIndices.get(n.index);
            } else {
                lines = new ArrayList<LinePlusRange>();
            }
            lines.add(new LinePlusRange(r,lineNum));
            nodeIndex2IntervalIndices.put(n.index, lines);

            //System.out.println("Adding to node " + n.index);

            lineNum++;

            if (lineNum % 500 == 0) {
                //System.out.println("=========== " + ((lineNum *100) / intervalIndex2IntervalRange.size()) + "% done");
                setProgress( (lineNum *100) / intervalIndex2IntervalRange.size());
                if (Thread.interrupted()) throw new InterruptedException();
            }
        }

        //System.out.println("IBST created with: " + ibst.getNumNodes() + " nodes");

//        System.out.println("No intervals left to see, dumping remaining " + ibst.getNumNodes() + " nodes");

        while (ibst.getRoot() != null) {
            currentSmallestNode = ibst.getNodeWithSmallestMax();

            if (currentSmallestNode.size == 0) {
                currentSmallestNode.startByte = (long)-1;
            } else {
                currentSmallestNode.startByte = (long)outFile.size();
            }
            writeIntervalTreeNode(currentSmallestNode, indexOutFile);
            writeBinToOutfile(currentSmallestNode, indexFile, outFile, nodeIndex2IntervalIndices, intevalIndex2StartByte, fields, modifiers);

            ibst.removeNode(currentSmallestNode);
        }

        outFile.close();
        indexOutFile.close();

//        System.out.println("Done formatting");
    }


    /*
     * INTERVAL : GENERIC
     */
    private void formatAsIntervalGeneric() throws IOException, InterruptedException {

        // pre-sort by start position
        int[] columns = {0,1};
        sortInput(columns);

        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.STRING);

        List<Object> modifiers = new ArrayList<Object>();
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);

        formatAsInterval(fields,modifiers);

        // delete sorted temp file
        new File(sortPath).delete();
    }

    private void formatAsIntervalGFF() throws IOException, InterruptedException {

        // pre-sort by start position
        int[] columns = {3,4};
        sortInput(columns);

        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.IGNORE);
        fields.add(FieldType.IGNORE);
        fields.add(FieldType.IGNORE);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.STRING);
        fields.add(FieldType.STRING);
        fields.add(FieldType.STRING);

        List<Object> modifiers = new ArrayList<Object>();
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);

        formatAsInterval(fields,modifiers);

        // delete sorted temp file
        new File(sortPath).delete();

    }

    private void formatAsIntervalBED() throws IOException, InterruptedException {

        // pre-sort by start position
        int[] columns = {1,2};
        sortInput(columns);

        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.IGNORE);    // chrom
        fields.add(FieldType.INTEGER);  // start
        fields.add(FieldType.INTEGER);  // end
        fields.add(FieldType.STRING);   // name
        fields.add(FieldType.INTEGER);  // score
        fields.add(FieldType.STRING);     // strand
        fields.add(FieldType.INTEGER);  // thickstart
        fields.add(FieldType.INTEGER);  // thickend
        fields.add(FieldType.ITEMRGB);  // itemrgb
        fields.add(FieldType.BLOCKS);

        List<Object> modifiers = new ArrayList<Object>();
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);

        formatAsInterval(fields,modifiers);

        // delete sorted temp file
        new File(sortPath).delete();

    }

    private void writeBinToOutfile(IntervalTreeNode n, RandomAccessFile srcFile, DataOutputStream outFile, HashMap<Integer, List<LinePlusRange>> nodeIndex2IntervalIndices, List<Long> intevalIndex2StartByte, List<FieldType> fields, List<Object> modifiers) throws IOException {
        if (n == null) { return; }

        List<LinePlusRange> linesPlusRanges = nodeIndex2IntervalIndices.get(n.index);

        System.out.println("D " + n.index + "\t" + n.range);

        if (n.children.size() != 0) {
            System.out.println("CANT DUMP!!!! Node " + n.index + " with parent " + n.parent.index + " has " + n.children.size() + " children!!");
        }
        //System.out.println(n.index + " " + n.size + " " +  node2startByte.get(n.index));

        if (linesPlusRanges != null) {
            for (LinePlusRange lr : linesPlusRanges) {
                int intervalIndex = lr.lineNum;
                long startByte = intevalIndex2StartByte.get(intervalIndex);
                srcFile.seek(startByte);
                List<Object> rec = RAFUtils.readBinaryRecord(srcFile, fields);
                DataFormatUtils.writeBinaryRecord(outFile, rec, fields, modifiers);
            }
        }
    }

     private HashMap<Integer,Long> writeBinsToOutfile(DataOutputStream outFile, RandomAccessFile srcFile, IntervalSearchTree ibst, HashMap<Integer, List<LinePlusRange>> nodeIndex2IntervalIndices, List<Long> intevalIndex2StartByte, List<FieldType> fields, List<Object> modifiers) throws IOException {
         HashMap<Integer,Long> node2startByte = new HashMap<Integer, Long>();
         writeBinsToOutfile(node2startByte, outFile, srcFile, ibst.getRoot(), nodeIndex2IntervalIndices, intevalIndex2StartByte, fields, modifiers );
         return node2startByte;
     }

    private void writeBinsToOutfile(HashMap<Integer, Long> node2startByte, DataOutputStream outFile, RandomAccessFile srcFile, IntervalTreeNode n, HashMap<Integer, List<LinePlusRange>> nodeIndex2IntervalIndices, List<Long> intevalIndex2StartByte, List<FieldType> fields, List<Object> modifiers) throws IOException {
        if (n == null) { return; }

        List<LinePlusRange> linesPlusRanges = nodeIndex2IntervalIndices.get(n.index);

        if (n.size == 0) {
            node2startByte.put(n.index, (long) -1);
        } else {
            node2startByte.put(n.index, (long)outFile.size());
        }

        //System.out.println("Node " + " index: " + n.index + " range: " + n.range + " size: " + n.size + " startByte: " +  node2startByte.get(n.index));
        //System.out.println(n.index + " " + n.size + " " +  node2startByte.get(n.index));

        if (linesPlusRanges != null) {
            for (LinePlusRange lr : linesPlusRanges) {
                int intervalIndex = lr.lineNum;
                long startByte = intevalIndex2StartByte.get(intervalIndex);
                srcFile.seek(startByte);

                List<Object> rec = RAFUtils.readBinaryRecord(srcFile, fields);
                DataFormatUtils.writeBinaryRecord(outFile, rec, fields, modifiers);
            }
        }

        for (IntervalTreeNode child : n.children) {
            writeBinsToOutfile(node2startByte, outFile,srcFile,child,nodeIndex2IntervalIndices,intevalIndex2StartByte,fields,modifiers);
        }
    }

    /**
     * POINT : GENERIC
     * @return
     * @throws IOException
     */
    private void formatAsPointGeneric() throws IOException, InterruptedException {

        // Initialize the total size of the input file, for purposes of tracking progress
        this.totalBytes = new File(inPath).length();

        BufferedReader inFile = this.openInputFile();

        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.STRING);

        List<Object> modifiers = new ArrayList<Object>();
        modifiers.add(null);
        int descriptionLength = pointGenericGetDescriptionLength();
        modifiers.add(descriptionLength);

        DataFormatUtils.writeFieldsHeader(outFile, fields);

        try {
            String strLine;
            List<Object> line;
            boolean done = false;
            while (!done) {
                for (int i=0; i<RECORDS_PER_INTERRUPT_CHECK; i++) {
                    if ((strLine = inFile.readLine()) == null) {
                        done = true;
                        break;
                    }
                    // update bytes read from input
                    this.byteCount += strLine.getBytes().length;
                    // parse input and write output
                    if ((line = DataFormatUtils.parseTxtLine(strLine, fields)) != null) {
                        line.set(0, ((Integer) line.get(0))+ this.baseOffset);
                        DataFormatUtils.writeBinaryRecord(outFile, line, fields, modifiers);
                    }
                }
                // check to see if format has been cancelled
                if (Thread.interrupted()) throw new InterruptedException();
                // update progress property for UI
                updateProgress();
            }
        }
        finally {
            inFile.close();
            outFile.close();
        }
    }

    private int pointGenericGetDescriptionLength() throws IOException {

        BufferedReader inFile = this.openInputFile();
        String txtLine = "";
        StringTokenizer tok;
        int tokenNum;

        int maxDescriptionLength = Integer.MIN_VALUE;

        while ((txtLine = inFile.readLine()) != null) {

            tok = new StringTokenizer(txtLine);
            tokenNum = 0;
            String token = "";
            while (tok.hasMoreElements()) {
                token = tok.nextToken();
                if (tokenNum == 1) {
                    int descriptionLength = token.length();
                    maxDescriptionLength = Math.max(descriptionLength, maxDescriptionLength);
                    break;
                }
                tokenNum++;
            }
        }

        return maxDescriptionLength;
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
    private void sortInput(int[] columns) throws FileNotFoundException, IOException {

        ExternalSort externalSort = new ExternalSort();
        externalSort.setInFile(inPath);
        String sortPath = inPath + ".sort";
        externalSort.setOutFile(sortPath);
        externalSort.setColumns(columns);
        externalSort.setNumeric(true);
        externalSort.setSeparator('\t');
        externalSort.setRunSize(DEFAULT_RUN_SIZE);
        externalSort.run();
        inPath = sortPath;

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


    private DataOutputStream openNewTmpOutputFile() throws IOException {
        deleteTmpOutputFile();
        return openTmpOutputFile();
    }

    /**
     * Open the output file
     * @return
     * @throws FileNotFoundException
     */
    private DataOutputStream openTmpOutputFile() throws IOException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpOutPath)));
    }

    private void deleteTmpOutputFile() {
        File f = new File(tmpOutPath);
        if (f.exists()) {
            f.delete();
        }
    }

    private void deleteOutputFile() {
        File f = new File(outPath);
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
    
    /**
     * Open the input file
     * @return
     * @throws FileNotFoundException
     */
    private BufferedReader openInputFile() throws FileNotFoundException {
        return new BufferedReader(new FileReader(inPath));
    }

    private void setInputOneBased(boolean inputOneBased) {
        if (inputOneBased) { this.baseOffset = 1; }
        else { this.baseOffset = 0; }
    }

    private void updateProgress() {
        float proportionDone = (float)this.byteCount/(float)this.totalBytes;
        int percentDone = (int)Math.round(proportionDone * 100.0);
        setProgress(percentDone);
    }

    /*
     * Property change support methods.
     * Delegate all calls to propertyChangeSupport object.
     */

//    public void addPropertyChangeListener(PropertyChangeListener listener) {
//        this.propertyChangeSupport.addPropertyChangeListener(listener);
//    }
//
//    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
//        this.propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
//    }
//
//    public void removePropertyChangeListener(PropertyChangeListener listener) {
//        this.propertyChangeSupport.removePropertyChangeListener(listener);
//    }
//
//    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
//        this.propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
//    }

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

    private void writeIntervalTreeNode(IntervalTreeNode n, RandomAccessFile indexOutFile) throws IOException {

        List<Object> record;
        List<FieldType> fields;
        List<Object> modifiers;

        record = new ArrayList<Object>();
        fields = new ArrayList<FieldType>();
        modifiers = new ArrayList<Object>();
        //System.out.print(n.index + " " + n.range + " " + node2startByte.get(n.index) + " " + n.size + " " + n.subtreeSize + " ");
        // the index
        record.add(n.index);
        fields.add(FieldType.INTEGER);
        modifiers.add(null);
        // range
        record.add(n.range);
        fields.add(FieldType.RANGE);
        modifiers.add(null);
        // byte pointer
        record.add(n.startByte);
        fields.add(FieldType.LONG);
        modifiers.add(null);
        // size
        record.add(n.size);
        fields.add(FieldType.INTEGER);
        modifiers.add(null);
        // subtree size
        record.add(n.subtreeSize);
        fields.add(FieldType.INTEGER);
        modifiers.add(null);
        // parent index
        if (n.parent == null) { // this is root
            record.add(-1);
            fields.add(FieldType.INTEGER);
            modifiers.add(null);
        } else {
            record.add(n.parent.index);
            fields.add(FieldType.INTEGER);
            modifiers.add(null);
        }

        // now: children are implicit in order...
        /*
        // numchildren
        record.add(n.children.size());
        fields.add(FieldType.INTEGER);
        modifiers.add(null);
        // index of each child
        for (IntervalTreeNode child : n.children) {
            record.add(child.index);
            fields.add(FieldType.INTEGER);
            modifiers.add(null);
        }
         */
        RAFUtils.writeBinaryRecord(indexOutFile, record, fields, modifiers);
    }

}
