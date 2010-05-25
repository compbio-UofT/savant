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
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.logging.LogFactory;
import savant.format.util.data.FieldType;
import savant.format.util.data.interval.IntervalSearchTree;
import savant.format.util.data.interval.IntervalTreeNode;
import savant.format.util.data.interval.LinePlusRange;
import savant.util.DataFormatUtils;
import savant.util.RAFUtils;
import savant.util.Range;

public class IntervalFormatter extends GenericFormatter {

    private static final long DEFAULT_RUN_SIZE = (long)(12.5 * Math.pow(2, 20)); // 12.5MB

    public IntervalFormatter(String sortPath, String inPath, int baseOffset, String outPath, DataOutputStream outFile){
        this.inFile = inPath;
        this.sortPath = sortPath;
        this.baseOffset = baseOffset;
        log = LogFactory.getLog(IntervalFormatter.class);
        this.outFile = outPath;
        this.out = outFile;
    }

    public void format() throws IOException, InterruptedException{

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
        log.debug("=== STEP 1 ===");

        int minRange = Integer.MAX_VALUE;
        int maxRange = Integer.MIN_VALUE;

        inputFile = this.openInputFile();
        String strLine;
        List<Object> line;
        while((strLine = inputFile.readLine()) != null) {

            if (strLine.equals("")) { continue; }

            log.debug(strLine);

            line = DataFormatUtils.parseTxtLine(strLine, fields);

            line.set(0, ((Integer)line.get(0)) + this.baseOffset);
            line.set(1, ((Integer)line.get(1))  + this.baseOffset);

            int startInterval = (Integer) line.get(0);
            int endInterval = (Integer) line.get(1);

            minRange = Math.min(minRange, startInterval);
            maxRange = Math.max(maxRange, endInterval);

            intervalIndex2IntervalRange.add(new Range(startInterval, endInterval));
            intevalIndex2StartByte.add((long)tmpOutFile.size());

            if (log.isDebugEnabled()) {
                log.debug("Writing [" + startInterval + "," + endInterval + "] to tmp file");
                log.debug("Saving pointer to [" + tmpOutFile.size() + "]");
            }

            DataFormatUtils.writeBinaryRecord(tmpOutFile, line, fields, modifiers);
        }

        /*
         * STEP 2:
         *  - create an interval BST
         *  - put each interval into appropriate bin
         *  - create a bin -> List<line> map
         *  - write each bin to outfile
         */
        log.debug("=== STEP 2 ===");

        DataFormatUtils.writeFieldsHeader(out, fields);
        tmpOutFile.close();

        String indexPath = outFile + indexExtension;
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

            if (log.isDebugEnabled()) {
                log.debug("Adding :" + r);
            }

            currentSmallestNode = ibst.getNodeWithSmallestMax();
            while (r.getFrom() > currentSmallestNode.range.getTo()) {
                if (currentSmallestNode.size == 0) {
                    currentSmallestNode.startByte = (long)-1;
                    //node2startByte.put(currentSmallestNode.index, (long) -1);
                } else {
                    currentSmallestNode.startByte = (long)out.size();
                    //node2startByte.put(currentSmallestNode.index, (long)out.size());
                }
                writeIntervalTreeNode(currentSmallestNode, indexOutFile);
                writeBinToOutfile(currentSmallestNode, indexFile, out, nodeIndex2IntervalIndices, intevalIndex2StartByte, fields, modifiers);

                ibst.removeNode(currentSmallestNode);
                currentSmallestNode = ibst.getNodeWithSmallestMax();
            }

            IntervalTreeNode n = ibst.insert(r);

            if (log.isDebugEnabled()) {
                log.debug("I " + n.index + "\t" + r);
            }

            if (n.range.getTo() < currentSmallestNode.range.getTo() || (n.range.getTo() == currentSmallestNode.range.getTo() && n.range.getFrom() > currentSmallestNode.range.getFrom()) )
            {
                currentSmallestNode = n;
                if (log.isDebugEnabled()) {
                    log.debug("Smallest max:" + currentSmallestNode.range.getTo());
                }
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

            log.debug("Adding to node " + n.index);

            lineNum++;

            if (lineNum % 500 == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("=========== " + ((lineNum *100) / intervalIndex2IntervalRange.size()) + "% done");
                }
                setProgress( (lineNum *100) / intervalIndex2IntervalRange.size());
                if (Thread.interrupted()) throw new InterruptedException();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("IBST created with: " + ibst.getNumNodes() + " nodes");
            log.debug("No intervals left to see, dumping remaining " + ibst.getNumNodes() + " nodes");
        }

        while (ibst.getRoot() != null) {
            currentSmallestNode = ibst.getNodeWithSmallestMax();

            if (currentSmallestNode.size == 0) {
                currentSmallestNode.startByte = (long)-1;
            } else {
                currentSmallestNode.startByte = (long)out.size();
            }
            writeIntervalTreeNode(currentSmallestNode, indexOutFile);
            writeBinToOutfile(currentSmallestNode, indexFile, out, nodeIndex2IntervalIndices, intevalIndex2StartByte, fields, modifiers);

            ibst.removeNode(currentSmallestNode);
        }

        out.close();
        indexOutFile.close();

        // delete sorted temp file
        inputFile.close();
        new File(sortPath).delete();

        log.debug("Done formatting");



    }


    public void sortInput(int[] columns) throws IOException {

        ExternalSort externalSort = new ExternalSort();
        externalSort.setInFile(inFile);
        sortPath = inFile + ".sort";
        externalSort.setOutFile(sortPath);
        externalSort.setColumns(columns);
        externalSort.setNumeric(true);
        externalSort.setSeparator('\t');
        externalSort.setRunSize(DEFAULT_RUN_SIZE);
        externalSort.run();
        inFile = sortPath;

    }


    public void formatAsIntervalBED() throws IOException, InterruptedException{
        // pre-sort by start position
        int[] columns = {1,2};
        sortInput(columns);

        fields = new ArrayList<FieldType>();
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

        modifiers = new ArrayList<Object>();
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
    }

    public void formatAsIntervalGeneric() throws IOException, InterruptedException{
        // pre-sort by start position
        int[] columns = {0,1};
        sortInput(columns);

        fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.STRING);

        modifiers = new ArrayList<Object>();
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
    }

    public void formatAsIntervalGFF() throws IOException, InterruptedException {

        // pre-sort by start position
        int[] columns = {3,4};
        sortInput(columns);

        fields = new ArrayList<FieldType>();
        fields.add(FieldType.IGNORE);
        fields.add(FieldType.IGNORE);
        fields.add(FieldType.IGNORE);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.STRING);
        fields.add(FieldType.STRING);
        fields.add(FieldType.STRING);

        modifiers = new ArrayList<Object>();
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
    }


    /**
     * Open the input file
     * @return
     * @throws FileNotFoundException
     */
    public BufferedReader openInputFile() throws FileNotFoundException {
        return new BufferedReader(new FileReader(inFile));
    }

    private void writeBinToOutfile(IntervalTreeNode n, RandomAccessFile srcFile, DataOutputStream outFile, HashMap<Integer, List<LinePlusRange>> nodeIndex2IntervalIndices, List<Long> intevalIndex2StartByte, List<FieldType> fields, List<Object> modifiers) throws IOException {
        if (n == null) { return; }

        List<LinePlusRange> linesPlusRanges = nodeIndex2IntervalIndices.get(n.index);

        //System.out.println("D " + n.index + "\t" + n.range);

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

    protected void writeIntervalTreeNode(IntervalTreeNode n, RandomAccessFile indexOutFile) throws IOException {

        List<Object> record;
        //List<FieldType> fields;
        //List<Object> modifiers;

        record = new ArrayList<Object>();
        List<FieldType> fields1 = new ArrayList<FieldType>();
        List<Object> modifiers1 = new ArrayList<Object>();
        //System.out.print(n.index + " " + n.range + " " + node2startByte.get(n.index) + " " + n.size + " " + n.subtreeSize + " ");
        // the index
        record.add(n.index);
        fields1.add(FieldType.INTEGER);
        modifiers1.add(null);
        // range
        record.add(n.range);
        fields1.add(FieldType.RANGE);
        modifiers1.add(null);
        // byte pointer
        record.add(n.startByte);
        fields1.add(FieldType.LONG);
        modifiers1.add(null);
        // size
        record.add(n.size);
        fields1.add(FieldType.INTEGER);
        modifiers1.add(null);
        // subtree size
        record.add(n.subtreeSize);
        fields1.add(FieldType.INTEGER);
        modifiers1.add(null);
        // parent index
        if (n.parent == null) { // this is root
            record.add(-1);
            fields1.add(FieldType.INTEGER);
            modifiers1.add(null);
        } else {
            record.add(n.parent.index);
            fields1.add(FieldType.INTEGER);
            modifiers1.add(null);
        }

        // now: children are implicit in order...
        /*
        // numchildren
        record.add(n.children.size());
        fields1.add(FieldType.INTEGER);
        modifiers1.add(null);
        // index of each child
        for (IntervalTreeNode child : n.children) {
            record.add(child.index);
            fields1.add(FieldType.INTEGER);
            modifiers1.add(null);
        }
         */
        RAFUtils.writeBinaryRecord(indexOutFile, record, fields1, modifiers1);
    }

}
