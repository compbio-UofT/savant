/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.format;

import savant.format.util.data.interval.LinePlusRange;
import savant.format.util.data.FieldType;
import savant.util.DataUtils;
import savant.format.header.FileType;
import savant.format.comparator.LineRangeComparator;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.interval.IntervalTreeNode;
import savant.format.util.data.interval.IntervalSearchTree;
import savant.util.IOUtils;
import savant.util.Range;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 * @author mfiume
 */
public class DataFormatter {

    public static String defaultExtension = ".debut";
    public static String indexExtension = ".index";

    int byteCounter;
    static String tmpOutPath = "tmp";
    String inPath;
    String outPath;
    //BufferedReader inFile;
    //RandomAccessFile outFile;
    FileType fileType;
    int currentVersion = 1;

    public DataFormatter(String inPath, String outPath, FileType fileType) {
        this.inPath = inPath;
        this.outPath = outPath;
        this.fileType = fileType;
    }

    /**
     * Formats the file
     * @return
     */
    public boolean format() {

        try {
            RandomAccessFile outFile = this.openNewOutputFile();
            DataUtils.writeFileTypeHeader(outFile, new FileTypeHeader(this.fileType,this.currentVersion));
            outFile.close();

            switch (fileType) {
                case POINT_GENERIC:
                    formatAsPointGeneric();
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

        } catch (Exception e) {

            e.printStackTrace();

            //TODO: remove/delete the outFile (if created)
            deleteTmpOutputFile();
            
            return false;
        }

        deleteTmpOutputFile();
        
        return true;
    }

    /**
     * SEQUENCE : FASTA
     * @return
     */
    private void formatAsSequenceFasta() throws FileNotFoundException, IOException {

        BufferedReader inFile = this.openInputFile();
        RandomAccessFile outFile = this.openOutputFile();

        List<FieldType> fields = new ArrayList<FieldType>();
        DataUtils.writeFieldsHeader(outFile, fields);

        String strLine;
        //Read File Line By Line
        while ((strLine = inFile.readLine()) != null) {
            if (strLine.charAt(0) != '>') {
                outFile.writeBytes(strLine);
            }
        }

        inFile.close();
        outFile.close();
    }

    /**
     * CONTINUOUS : GENERIC
     * @return
     */
    private void formatAsContinuousGeneric() throws FileNotFoundException, IOException {

        BufferedReader inFile = this.openInputFile();
        RandomAccessFile outFile = this.openOutputFile();

        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.DOUBLE);

        List<Object> modifiers = new ArrayList<Object>();
        modifiers.add(null);

        DataUtils.writeFieldsHeader(outFile, fields);

        List<Object> line;
        while((line = DataUtils.parseTxtLine(inFile, fields)) != null) {
            DataUtils.writeBinaryRecord(outFile, line, fields, modifiers);
        }
        
        inFile.close();
        outFile.close();
    }

    /**
     * CONTINUOUS : GENERIC
     * @return
     */
    private void formatAsContinuousWIG() throws FileNotFoundException, IOException {

        BufferedReader inFile = this.openInputFile();
        RandomAccessFile outFile = this.openOutputFile();

        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.DOUBLE);

        List<Object> modifiers = new ArrayList<Object>();
        modifiers.add(null);
        modifiers.add(null);

        DataUtils.writeFieldsHeader(outFile, fields);

        List<Object> line;
        while((line = DataUtils.parseTxtLine(inFile, fields)) != null) {
            DataUtils.writeBinaryRecord(outFile, line, fields, modifiers);
        }

        inFile.close();
        outFile.close();
    }



    private void formatAsInterval(List<FieldType> fields, List<Object> modifiers) throws FileNotFoundException, IOException {

        List<Range> intervalIndex2IntervalRange = new ArrayList<Range>();
        List<Long> intevalIndex2StartByte = new ArrayList<Long>();

        RandomAccessFile tmpOutFile = this.openNewTmpOutputFile();

        /**
         * STEP 1:
         * Go through inFile and...
         *  - get max range
         *  - get description length
         *  - write intervals in binary to tmpfile
         *  - create a line number -> <startbyte,endbyte> map (bytes correspond to tmpfile)
         *  - create a line number -> Range map
         */
         System.out.println("=== STEP 1 ===");

        int minRange = Integer.MAX_VALUE;
        int maxRange = Integer.MIN_VALUE;

        BufferedReader inFile = this.openInputFile();
        List<Object> line;
        while((line = DataUtils.parseTxtLine(inFile, fields)) != null) {
            int startInterval = (Integer) line.get(0);
            minRange = Math.min(minRange, startInterval);
            int endInterval = (Integer) line.get(1);
            maxRange = Math.max(maxRange, endInterval);

            intervalIndex2IntervalRange.add(new Range(startInterval, endInterval));
            intevalIndex2StartByte.add(tmpOutFile.getFilePointer());

            //System.out.println("Writing [" + startInterval + "," + endInterval + "," + description + "] to tmp file");
            //System.out.println("Saving pointer to [" + tmpOutFile.getFilePointer() + "]");

            DataUtils.writeBinaryRecord(tmpOutFile, line, fields, modifiers);
        }

        /**
         * STEP 2:
         *  - create an interval BST
         *  - put each interval into appropriate bin
         *  - create a bin -> List<line> map
         */
        System.out.println("=== STEP 2 ===");

        IntervalSearchTree ibst = new IntervalSearchTree(new Range(minRange, maxRange));

        HashMap<Integer,List<LinePlusRange>> nodeIndex2IntervalIndices = new HashMap<Integer,List<LinePlusRange>>();

        int lineNum = 0;
        for (Range r : intervalIndex2IntervalRange) {
            IntervalTreeNode n = ibst.insert(r);

            //System.out.println("Interval: " + r + "\tNode: " + n.range + "\tIndex: " + n.index);

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
        }

        //System.out.println("IBST created with: " + ibst.getNumNodes() + " nodes");

        /**
         * STEP 3:
         *  - sort each bin
         *  - write each bin to outfile
         */
        System.out.println("=== STEP 3 ===");

        Collection<List<LinePlusRange>> lineLists = (Collection<List<LinePlusRange>>) nodeIndex2IntervalIndices.values();
        for (List<LinePlusRange> lineList : lineLists) {
            Collections.sort(lineList, new LineRangeComparator());
        }

        RandomAccessFile outFile = this.openOutputFile();
        DataUtils.writeFieldsHeader(outFile, fields);
        HashMap<Integer,Long> node2startByte = writeBinsToOutfile(outFile, tmpOutFile, ibst, nodeIndex2IntervalIndices, intevalIndex2StartByte, fields, modifiers );
        outFile.close();

        /**
         * STEP 4:
         *  - write index
         */

        /**
         * TODO: write the index to an INDEX file.
         * need a node --> start byte map first!
         */
        String indexPath = outPath + indexExtension;
        RandomAccessFile indexOutFile = IOUtils.openNewFile(indexPath);

        System.out.println("=== STEP 4 ===");
        writeIntervalBSTIndex(indexOutFile, ibst, node2startByte);

        indexOutFile.close();
    }


    /**
     * INTERVAL : GENERIC
     */
    private void formatAsIntervalGeneric() throws FileNotFoundException, IOException {
        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.STRING);

        List<Object> modifiers = new ArrayList<Object>();
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);

        formatAsInterval(fields,modifiers);
    }

    private void formatAsIntervalGFF() throws FileNotFoundException, IOException {
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
    }

    private void formatAsIntervalBED() throws FileNotFoundException, IOException {
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
    }

     private HashMap<Integer,Long> writeBinsToOutfile(RandomAccessFile outFile, RandomAccessFile srcFile, IntervalSearchTree ibst, HashMap<Integer, List<LinePlusRange>> nodeIndex2IntervalIndices, List<Long> intevalIndex2StartByte, List<FieldType> fields, List<Object> modifiers) throws IOException {
         HashMap<Integer,Long> node2startByte = new HashMap<Integer, Long>();
         writeBinsToOutfile(node2startByte, outFile, srcFile, ibst.getRoot(), nodeIndex2IntervalIndices, intevalIndex2StartByte, fields, modifiers );
         return node2startByte;
     }

    private void writeBinsToOutfile(HashMap<Integer, Long> node2startByte, RandomAccessFile outFile, RandomAccessFile srcFile, IntervalTreeNode n, HashMap<Integer, List<LinePlusRange>> nodeIndex2IntervalIndices, List<Long> intevalIndex2StartByte, List<FieldType> fields, List<Object> modifiers) throws IOException {
        if (n == null) { return; }

        List<LinePlusRange> linesPlusRanges = nodeIndex2IntervalIndices.get(n.index);

        if (n.size == 0) {
            node2startByte.put(n.index, (long) -1);
        } else {
            node2startByte.put(n.index, outFile.getFilePointer());
        }
        
        //System.out.println("Node " + " index: " + n.index + " range: " + n.range + " size: " + n.size + " startByte: " +  node2startByte.get(n.index));
        System.out.println(n.index + " " + n.size + " " +  node2startByte.get(n.index));

        if (linesPlusRanges != null) {
            for (LinePlusRange lr : linesPlusRanges) {
                int intervalIndex = lr.lineNum;
                long startByte = intevalIndex2StartByte.get(intervalIndex);
                srcFile.seek(startByte);

                List<Object> rec = DataUtils.readBinaryRecord(srcFile, fields);
                DataUtils.writeBinaryRecord(outFile, rec, fields, modifiers);
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
    private void formatAsPointGeneric() throws IOException {

        /**
         * -1       Get the largest one (fixed length)
         * other    All specified length (fixed length)
         * null  :  Actual length of string (not - fixed length) NOT allowed for point!
         */
        int descriptionLength = pointGenericGetDescriptionLength();

        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.STRING);

        List<Object> modifiers = new ArrayList<Object>();
        modifiers.add(null);
        modifiers.add(descriptionLength);

        RandomAccessFile outFile = this.openOutputFile();
        DataUtils.writeFieldsHeader(outFile, fields);
        outFile.close();

        List<Object> line = null;

        outFile = this.openOutputFile();
        IOUtils.seekToEnd(outFile);

        BufferedReader inFile = this.openInputFile();
        while ((line = DataUtils.parseTxtLine(inFile, fields)) != null) {
            DataUtils.writeBinaryRecord(outFile, line, fields, modifiers);
        }
        inFile.close();

        outFile.close();
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
     * @param outraf The output file to which to write
     * @param ibst The BST to write
     * @throws IOException
     */
    private void writeIntervalBSTIndex(RandomAccessFile indexOutFile, IntervalSearchTree ibst, HashMap<Integer, Long> node2startByte) throws IOException {
        
        indexOutFile.writeInt(ibst.getNumNodes());

        List<Object> record;
        List<FieldType> fields;
        List<Object> modifiers;

        for (IntervalTreeNode n : ibst.getNodes()) {
            record = new ArrayList<Object>();
            fields = new ArrayList<FieldType>();
            modifiers = new ArrayList<Object>();

            //System.out.print(n.index + " " + n.range + " " + node2startByte.get(n.index) + " " + n.size + " " + n.subtreeSize + " ");

            // the index
            record.add(n.index); fields.add(FieldType.INTEGER); modifiers.add(null);

            // range
            record.add(n.range); fields.add(FieldType.RANGE); modifiers.add(null);

            // byte pointer
            record.add(node2startByte.get(n.index)); fields.add(FieldType.LONG); modifiers.add(null);

            // size
            record.add(n.size); fields.add(FieldType.INTEGER); modifiers.add(null);

            // subtree size
            record.add(n.subtreeSize); fields.add(FieldType.INTEGER); modifiers.add(null);

            // numchildren
            record.add(n.children.size()); fields.add(FieldType.INTEGER); modifiers.add(null);

            // index of each child
            for (IntervalTreeNode child : n.children) {
                record.add(child.index); fields.add(FieldType.INTEGER); modifiers.add(null);
            }

            DataUtils.writeBinaryRecord(indexOutFile, record, fields, modifiers);
        }
    }

    public static IntervalSearchTree readIntervalBST(String indexFileName) throws IOException {

        RandomAccessFile indexRaf = IOUtils.openFile(indexFileName, false);

        int numNodes = indexRaf.readInt();

        List<IntervalTreeNode> nodes = new ArrayList<IntervalTreeNode>(numNodes);

        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.RANGE);
        fields.add(FieldType.LONG);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);

        HashMap<Integer,List<Integer>> nodeIndex2ChildIndices = new HashMap<Integer,List<Integer>>();

        for (int i = 0; i < numNodes; i++) {

            List<Object> r1 = DataUtils.readBinaryRecord(indexRaf, fields);

            IntervalTreeNode n = new IntervalTreeNode((Range) r1.get(1), (Integer) r1.get(0));
            n.startByte = (Long) r1.get(2);
            n.size = (Integer) r1.get(3);
            n.subtreeSize = (Integer) r1.get(4);

            int numChildren = (Integer) r1.get(5);
            List<Integer> childIndices = new ArrayList<Integer>(n.size);

            for (int j = 0; j < numChildren; j++) {
                childIndices.add(indexRaf.readInt());
            }
            nodeIndex2ChildIndices.put(n.index, childIndices);

            nodes.add(n);
        }

       indexRaf.close();

       for (Integer index : nodeIndex2ChildIndices.keySet()) {
           IntervalTreeNode n = nodes.get(index);
           List<Integer> childIndices = nodeIndex2ChildIndices.get(index);


           for (Integer childIndex : childIndices) {
                n.children.add(nodes.get(childIndex));
           }
       }

       return new IntervalSearchTree(nodes);
    }


   /** FILE OPENING **/

    private RandomAccessFile openNewOutputFile() throws FileNotFoundException, IOException {
        deleteOutputFile();
        return openOutputFile();
    }


    /**
     * Open the output file
     * @return
     * @throws FileNotFoundException
     */
    private RandomAccessFile openOutputFile() throws FileNotFoundException, IOException {
        return  IOUtils.openFile(outPath,true);
    }


    private RandomAccessFile openNewTmpOutputFile() throws FileNotFoundException, IOException {
        deleteTmpOutputFile();
        return openTmpOutputFile();
    }

    /**
     * Open the output file
     * @return
     * @throws FileNotFoundException
     */
    private RandomAccessFile openTmpOutputFile() throws FileNotFoundException, IOException {
        return IOUtils.openFile(tmpOutPath, true);
    }

    private void deleteTmpOutputFile() {
         IOUtils.deleteFile(tmpOutPath);
    }

    private void deleteOutputFile() {
        IOUtils.deleteFile(outPath);
    }

    /**
     * Open the input file
     * @return
     * @throws FileNotFoundException
     */
    private BufferedReader openInputFile() throws FileNotFoundException {
        return new BufferedReader(new FileReader(inPath));
    }
}
