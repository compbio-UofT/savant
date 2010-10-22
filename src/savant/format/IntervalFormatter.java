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

import savant.file.FieldType;
import savant.file.FileType;
import savant.file.SavantUnsupportedVersionException;
import savant.util.Range;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntervalFormatter extends SavantFileFormatter {

    protected int baseOffset = 0; // 0 if *input* file is 1-based; 1 if 0-based
    private static final long DEFAULT_RUN_SIZE = (long)(12.5 * Math.pow(2, 20)); // 12.5MB
    private int refnameindex;
    private int startcoordindex;
    private int endcoordindex;
    private String comment;
    private boolean isGFF = false;

    List<FieldType> writeOrderFields;
    List<Object> writeOrderModifiers;

    public IntervalFormatter(String inFile, String outFile, boolean baseOffset, FileType ft, Integer refnameindex, Integer startcoordindex, Integer endcoordindex, String comment){
        super(inFile, outFile, ft); //FileType.INTERVAL_GENERIC);
        this.setInputOneBased(baseOffset);
        this.refnameindex = refnameindex;
        this.startcoordindex = startcoordindex;
        this.endcoordindex = endcoordindex;
        this.comment = comment;
    }

    private ArrayList rearrangeList(Object l) {

        ArrayList cpy = (ArrayList) ((ArrayList) l).clone(); //new ArrayList<FieldType>(l.size());
        //System.out.println(cpy.size());
        //System.out.println(l.size());
        //Collections.copy(cpy, l);

        //System.out.println("List is of size: " + cpy.size());
        //System.out.println("Start coordinate: " + startcoordindex);
        //System.out.println("End coordinatee: " + endcoordindex);

        Object ref = cpy.get(refnameindex);
        Object strt = cpy.get(startcoordindex);
        Object end = cpy.get(endcoordindex);
        cpy.remove(refnameindex);
        cpy.add(0, ref);
        cpy.remove(startcoordindex);
        cpy.add(1,strt);
        cpy.remove(endcoordindex);
        cpy.add(2,end);
        
        return cpy;
    }

    /*
    private List<Object> rearrangeRecordList(List<Object> l) {

        List<Object> cpy = new ArrayList<Object>(l.size());
        Collections.copy(cpy, l);

        Object ref = cpy.get(refnameindex);
        Object strt = cpy.get(startcoordindex);
        Object end = cpy.get(endcoordindex);
        
        cpy.remove(refnameindex);
        cpy.add(0, ref);
        cpy.remove(startcoordindex);
        cpy.add(1,strt);
        cpy.remove(endcoordindex);
        cpy.add(2,end);
            
        return cpy;
    }
     */

    private int countFields(String path) throws FileNotFoundException, IOException {
        //System.out.println("Counting fields");
        BufferedReader br = new BufferedReader(new FileReader(path));

        String line = br.readLine();

        while(line.startsWith(comment)) {
            line = br.readLine();
        }

        //StringTokenizer st = new StringTokenizer(line,"\t");
        //int numTokens = st.countTokens();

        //System.out.println(numTokens);
        String[] ss = line.split(delimiter);
        int numTokens = ss.length;

        br.close();
        return numTokens;
    }

    private ArrayList chopOffFields(ArrayList l, int numFieldsToKeep) {

        ArrayList newList = new ArrayList();

        numFieldsToKeep = Math.min(numFieldsToKeep, l.size()); //ADDED

        for (int i = 0; i < numFieldsToKeep; i++) {
            newList.add(l.get(i));
        }

        return newList;
    }

    /**
     * Format the file at the input file path with the result being put into
     * a new file at the output file path
     * @throws IOException
     * @throws InterruptedException
     */
    public void format() throws IOException, InterruptedException, SavantUnsupportedVersionException {

        int numFields = countFields(this.inFilePath);

        // set the input file size (for tracking progress)
        this.totalBytes = new File(inFilePath).length();

       // System.out.println("File contains " + numFields + " fields");

        fields = chopOffFields((ArrayList) fields, numFields);
        modifiers = chopOffFields((ArrayList) modifiers, numFields);

        writeOrderFields = rearrangeList(fields);
        writeOrderModifiers = rearrangeList(modifiers);

        //for (FieldType f : fields) {
        //    SavantDebugger.debug("Field: " + f + "");
        //}

        // the reference names
        List<String> refnames = new ArrayList<String>();

        // map of reference name -> *data* filename
        Map<String,String> refnameToDataFileNameMap = new HashMap<String,String>();

        // map of reference name -> *index* filename
        Map<String,String> refnameToIndexFileNameMap = new HashMap<String,String>();

        this.setSubtaskStatus("Processing input file ...");
        this.incrementOverallProgress();

        // split the input file by chromosome
        Map<String, String> refToinFileMap = SavantFileFormatterUtils.splitFile(this.inFilePath,0);

        //List<String> indexFiles = new ArrayList<String>();
        //List<String> outFiles = new ArrayList<String>();
        //List<String> outIndexFiles = new ArrayList<String>();

        // counter
        //int splitfilenum = 0;

        // output file (one per split file)
        DataOutputStream outfile;

        this.incrementOverallProgress();

        int part = 0;
        int totalparts = refToinFileMap.keySet().size();

        // format each split file individually
        for (String refname : refToinFileMap.keySet()) {

            this.setSubtaskStatus("Formatting sections (part " + (++part) + " of " + totalparts + ") ..." );
            //currrefname = refname;

            // get the input file for this reference
            String file = refToinFileMap.get(refname);

            // increment ref number
            //String refname = "ref" + (++splitfilenum);
            refnames.add(refname);

            // make and save path to tmp output files
            String outPath = inFilePath + ".part_" + refname;
            String indexPath = outPath + indexExtension;
            refnameToDataFileNameMap.put(refname, outPath);
            refnameToIndexFileNameMap.put(refname, indexPath);

            //outFiles.add(outPath);
            //outIndexFiles.add(indexPath);

            // make the output and index files
            outfile = new DataOutputStream(
                new BufferedOutputStream(
                    new FileOutputStream(outPath),
                    OUTPUT_BUFFER_SIZE));
            DataOutputStream indexOutFile = new DataOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(indexPath),
                            OUTPUT_BUFFER_SIZE));

            //System.out.println("Index file " + indexPath + " opened at byte position " + indexOutFile.getFilePointer());

            // format the file, storing output in outfile and indexOutFile
            formatFile(file, outfile, indexOutFile);

            // delete the tmp input file
            File f = new File(file);
            if (!f.delete()) { f.deleteOnExit(); }
        }

        fields = writeOrderFields;
        modifiers = writeOrderModifiers;

        // analogous to writeOutputFile(), but this also concatenates the indicies
        // in the file header
        writeIntervalOutputFile(refnames, refnameToIndexFileNameMap, refnameToDataFileNameMap);
    }


    public void sortInput(int[] columns) throws IOException {

        //TODO: reenable...
        // - be careful and consistent about tmp file name
        // - remove tmp file ALWAYS (even if fail)
        // - dont do it if not necessary!!!!!
        
        /*
        ExternalSort externalSort = new ExternalSort();
        externalSort.setInFile(inFilePath);
        sortPath = inFilePath + ".sort";
        externalSort.setOutFile(sortPath);
        externalSort.setColumns(columns);
        externalSort.setNumeric(true);
        externalSort.setSeparator('\t');
        externalSort.setRunSize(DEFAULT_RUN_SIZE);
        externalSort.run();
        inFilePath = sortPath;
         */
    }


    public void formatAsIntervalBED() throws IOException, InterruptedException{
        // pre-sort by start position
        int[] columns = {1,2};
        sortInput(columns);

        log.info("Formatting as BED");

        fields = new ArrayList<FieldType>();
        fields.add(FieldType.STRING);   // reference
        fields.add(FieldType.INTEGER);  // start
        fields.add(FieldType.INTEGER);  // end
        fields.add(FieldType.STRING);   // name
        fields.add(FieldType.FLOAT);  // score
        fields.add(FieldType.STRING);   // strand
        fields.add(FieldType.INTEGER);  // thickstart
        fields.add(FieldType.INTEGER);  // thickend
        fields.add(FieldType.ITEMRGB);  // itemrgb
        fields.add(FieldType.BLOCKS);   // exonblocks

        modifiers = new ArrayList<Object>();
        modifiers.add(null);    // reference
        modifiers.add(null);    // start
        modifiers.add(null);    // end
        modifiers.add(null);    // name
        modifiers.add(null);    // score
        modifiers.add(null);    // strand
        modifiers.add(null);    // thickstart
        modifiers.add(null);    // thickend
        modifiers.add(null);    // itemrgb
        modifiers.add(null);    // exonblocks
    }

    public void formatAsIntervalGeneric() throws IOException, InterruptedException{
        // pre-sort by start position
        int[] columns = {0,1};
        sortInput(columns);

        fields = new ArrayList<FieldType>();
        fields.add(FieldType.STRING);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.STRING);

        modifiers = new ArrayList<Object>();
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
        modifiers.add(null);
    }

    public void formatAsIntervalGFF() throws IOException, InterruptedException {

        isGFF = true;

        // pre-sort by start position
        int[] columns = {3,4};
        sortInput(columns);

        fields = new ArrayList<FieldType>();
        fields.add(FieldType.STRING);   // seqname
        fields.add(FieldType.STRING);   // source
        fields.add(FieldType.STRING);   // feature
        fields.add(FieldType.INTEGER);  // start
        fields.add(FieldType.INTEGER);  // end
        fields.add(FieldType.DOUBLE);  // score
        fields.add(FieldType.STRING);   // strand
        fields.add(FieldType.STRING);   // frame
        fields.add(FieldType.STRING);   // group

        modifiers = new ArrayList<Object>();
        modifiers.add(null);    // seqname
        modifiers.add(null);    // source
        modifiers.add(null);    // feature
        modifiers.add(null);    // start
        modifiers.add(null);    // end
        modifiers.add(null);    // score
        modifiers.add(null);    // strand
        modifiers.add(null);    // frame
        modifiers.add(null);    // group
    }


    /**
     * Open the input file
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public BufferedReader openInputFile() throws FileNotFoundException {
        return new BufferedReader(new FileReader(inFilePath));
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

                /*
                System.out.println("<<D" + "\t"
                //+ this.inFilePath + "\t"
                + currrefname + "\t"
                + n.index + "\t"
                + lr.range + "\t"
                + outFile.size());
                 */

                //System.out.println(">> DATA : range=" + lr.range + " pos=" + outFile.size());

                srcFile.seek(startByte);
                List<Object> rec = SavantFileFormatterUtils.readBinaryRecord(srcFile, fields);
                SavantFileFormatterUtils.writeBinaryRecord(outFile, rearrangeList(rec), writeOrderFields, writeOrderModifiers);
            }
        }
    }

    //String currrefname = "";

    protected void writeIntervalTreeNode(IntervalTreeNode n, DataOutputStream indexOutFile) throws IOException {

        /*
        System.out.println("<<I" + "\t"
                //+ this.inFilePath + "\t"
                + currrefname + "\t"
                + n.index + "\t"
                + n.range + "\t"
                + n.startByte + "\t"
                + indexOutFile.getFilePointer());
         */
        
        //System.out.println(">> INDEX: node=" + n.index + " range=" + n.range + " pos=" + indexOutFile.getFilePointer());

        List<Object> record;
        //List<FieldType> fields;
        //List<Object> modifiers;

        record = new ArrayList<Object>();
        List<FieldType> indexNodeFields = new ArrayList<FieldType>();
        List<Object> indexNodeModifiers = new ArrayList<Object>();
        //System.outFile.print(n.index + " " + n.range + " " + node2startByte.get(n.index) + " " + n.size + " " + n.subtreeSize + " ");
        // the index
        record.add(n.index);
        indexNodeFields.add(FieldType.INTEGER);
        indexNodeModifiers.add(null);
        // range
        record.add(n.range);
        indexNodeFields.add(FieldType.RANGE);
        indexNodeModifiers.add(null);
        // byte pointer
        record.add(n.startByte);
        indexNodeFields.add(FieldType.LONG);
        indexNodeModifiers.add(null);
        // size
        record.add(n.size);
        indexNodeFields.add(FieldType.INTEGER);
        indexNodeModifiers.add(null);
        // subtree size
        record.add(n.subtreeSize);
        indexNodeFields.add(FieldType.INTEGER);
        indexNodeModifiers.add(null);
        // parent index
        if (n.parent == null) { // this is root
            record.add(-1);
            indexNodeFields.add(FieldType.INTEGER);
            indexNodeModifiers.add(null);
        } else {
            record.add(n.parent.index);
            indexNodeFields.add(FieldType.INTEGER);
            indexNodeModifiers.add(null);
        }

        SavantFileFormatterUtils.writeBinaryRecord(indexOutFile, record, indexNodeFields, indexNodeModifiers);
    }

    private void setInputOneBased(boolean inputOneBased) {
        if (inputOneBased) { this.baseOffset = 0; }
        else { this.baseOffset = 1; }
    }

    /**
     * Format a file
     * @param path Path to the file to format
     * @param outFile The output stream to send the output
     * @param indexOutFile The index file
     * @throws IOException
     * @throws InterruptedException
     */
    private void formatFile(String path, DataOutputStream outFile, DataOutputStream indexOutFile) throws IOException, InterruptedException {

        //System.out.println("Formatting split file: " + path);

        // interval index (i.e. line number) -> range map (where index is implicit in list index)
        List<Range> intervalIndex2IntervalRange = new ArrayList<Range>();

        // interval index (i.e. line number) -> start byte in data file (where index is implicit in list index)
        List<Long> intervalIndex2StartByte = new ArrayList<Long>();

        // TODO: why?
        // create a file to hold the data temporarily
        String tmpfilename = path + ".tmp";
        DataOutputStream tmpOutFile = new DataOutputStream(new FileOutputStream(tmpfilename));

         /* STEP 1:
         * Go through inFilePath and...
         *  - get max range
         *  - get description length
         *  - write intervals in binary to tmpfile
         *  - create a line number -> startbyte map (bytes correspond to tmpfile)
         *  - create a line number -> Range map
         */
        log.debug("=== STEP 1 ===");

        // the maximum and minimum range values in the input file
        long minRange = Long.MAX_VALUE;
        long maxRange = Long.MIN_VALUE;

        // open the input file
        inFileReader = new BufferedReader(new FileReader(path));

        // a line from the file
        String strLine;

        // holds parsed tokens from the line
        List<Object> line;

        int i=0;

        // keep reading lines into strLine until EOF
        while((strLine = inFileReader.readLine()) != null) {

            this.byteCount += strLine.getBytes().length;

            // skip blank lines
            if (strLine.equals("") || strLine.startsWith("track") || strLine.startsWith(comment)) { continue; }

            // tokenize the line into the respective fields
            line = SavantFileFormatterUtils.parseTxtLine(strLine, fields, isGFF);


            //for (Object o : line) {
            //    System.out.println(o);
            //}

            // adjust the start byte and endbyte offsets (for 0 vs. 1-based)
            line.set(startcoordindex, (Long)line.get(startcoordindex) + this.baseOffset);
            line.set(endcoordindex, (Long)line.get(endcoordindex) + this.baseOffset);

            // update min and max
            long startInterval = (Long)line.get(startcoordindex);
            long endInterval = (Long)line.get(endcoordindex);
            minRange = Math.min(minRange, startInterval);
            maxRange = Math.max(maxRange, endInterval);

            // add values to the map
            intervalIndex2IntervalRange.add(new Range(startInterval, endInterval));
            intervalIndex2StartByte.add((long)tmpOutFile.size());

            // write this record to the tmp file
            SavantFileFormatterUtils.writeBinaryRecord(tmpOutFile, line, fields, modifiers);

            if (++i == RECORDS_PER_INTERRUPT_CHECK) {
                 i = 0;
                 if (Thread.interrupted()) { throw new InterruptedException(); }
                 // update progress property for UI
                 this.setSubtaskProgress(this.getProgressAsInteger(byteCount, totalBytes));
            }
        }

        // close the tmp data file
        tmpOutFile.close();

        //System.out.println("Min: " + minRange);
        //System.out.println("Max: " + maxRange);

        /*
         * STEP 2:
         *  - create an interval BST
         *  - put each interval into appropriate bin
         *  - create a bin -> List<line> map
         *  - write each bin to outfile
         */
        log.debug("=== STEP 2 ===");

        //this.incrementOverallProgress();

        // re-open the tmp data file (this time we need random sources to seek to specific records)
        RandomAccessFile tmpDataFile = new RandomAccessFile(tmpfilename,"r");

        // create an empty IST
        IntervalSearchTree ist = new IntervalSearchTree(new Range(minRange, maxRange));

        // create a map of node index --> List<interval indicies>
        HashMap<Integer,List<LinePlusRange>> nodeIndex2IntervalIndices = new HashMap<Integer,List<LinePlusRange>>();

        // get the node with the smallest max value (ie. the one which will be dumped next)
        IntervalTreeNode currentSmallestNode = ist.getNodeWithSmallestMax();

        // counter
        int lineNum = 0;

        // go through each range (i.e. each interval in the input file)
        for (Range r : intervalIndex2IntervalRange) {

            //System.out.println("Adding :" + r);

            // dump nodes if we're sure no new interval can be put in them
            // ** (assumes input is sorted) **
            currentSmallestNode = ist.getNodeWithSmallestMax();
            while (r.getFrom() > currentSmallestNode.range.getTo()) {

                //System.out.println("Dumping smallest node: " + currentSmallestNode.range);

                if (currentSmallestNode.size == 0) {
                    currentSmallestNode.startByte = (long) -1;
                } else {
                    currentSmallestNode.startByte = (long) outFile.size();
                }

                // write this node to the index file
                writeIntervalTreeNode(currentSmallestNode, indexOutFile);

                // write the data in this node to the output file
                writeBinToOutfile(currentSmallestNode, tmpDataFile, outFile, nodeIndex2IntervalIndices, intervalIndex2StartByte, fields, modifiers);

                // remove node from ist
                ist.removeNode(currentSmallestNode);
                currentSmallestNode = ist.getNodeWithSmallestMax();
            } // done dumping nodes

            // insert this into the appropriate node in the tree
            IntervalTreeNode n = ist.insert(r);

            //System.out.println("Insert into: " + n.range);

            if (log.isDebugEnabled()) {
                log.debug("I " + n.index + "\t" + r);
            }

            // if new node becomes smallest, set currentSmallestNode accordingly
            if (n.range.getTo() < currentSmallestNode.range.getTo() 
                    || (    n.range.getTo() == currentSmallestNode.range.getTo()
                    &&      n.range.getFrom() > currentSmallestNode.range.getFrom() ) )
            {
                currentSmallestNode = n;
                //if (log.isDebugEnabled()) { log.debug("Smallest max:" + currentSmallestNode.range.getTo()); }
            }

            //System.out.println("Adding Interval: " + r + "\tNode: " + n.range + "\tIndex: " + n.index);

            // get list of ranges associated with this node
            List<LinePlusRange> lines;
            if (nodeIndex2IntervalIndices.containsKey(n.index)) {
                lines = nodeIndex2IntervalIndices.get(n.index);
            } else {
                lines = new ArrayList<LinePlusRange>();
            }

            // add this range to the list of ranges
            lines.add(new LinePlusRange(r,lineNum));
            nodeIndex2IntervalIndices.put(n.index, lines);

            log.debug("Adding to node " + n.index);

            // increment counter and do progress reporting
            lineNum++;
            if (lineNum % 500 == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("=========== " + ((lineNum *100) / intervalIndex2IntervalRange.size()) + "% done");
                }
                setSubtaskProgress((lineNum *100) / intervalIndex2IntervalRange.size());
                if (Thread.interrupted()) throw new InterruptedException();
            }
        }

        //System.out.println("IBST created with: " + ist.getNumNodes() + " nodes");
        //System.out.println("No intervals left to see, dumping remaining " + ist.getNumNodes() + " nodes");

        // dump the remaining nodes in the tree
        while (ist.getRoot() != null) {

            // get the smallest
            currentSmallestNode = ist.getNodeWithSmallestMax();

            //System.out.println("Dumping smallest node: " + currentSmallestNode.range);

            // set the start byte
            if (currentSmallestNode.size == 0) {

                // set to -1 if this node has no children
                currentSmallestNode.startByte = (long)-1;
            } else {

                // set to byte offset is node has children
                currentSmallestNode.startByte = (long)outFile.size();
            }

            // write this node to the index file
            writeIntervalTreeNode(currentSmallestNode, indexOutFile);

            // write the intervals in this node to the data file
            writeBinToOutfile(currentSmallestNode, tmpDataFile, outFile, nodeIndex2IntervalIndices, intervalIndex2StartByte, fields, modifiers);

            // remove the node from the tree
            ist.removeNode(currentSmallestNode);
        }

        // write a "null" terminator node ** with index of -1 ** so we know
        // there are no more nodes after this
        IntervalTreeNode nullTerminatorNode = new IntervalTreeNode(new Range(-1,-1),-1);
        nullTerminatorNode.size = -1;
        nullTerminatorNode.subtreeSize = -1;
        nullTerminatorNode.startByte = (long) -1;
        writeIntervalTreeNode(nullTerminatorNode, indexOutFile);

        setSubtaskProgress(100);

        //System.out.println("Bytes written to index: " + indexOutFile.getFilePointer());

        // close the files
        outFile.close();
        tmpDataFile.close();
        indexOutFile.close();

        // delete sorted temp file
        inFileReader.close();

        //new File(sortPath).delete();

        // delete the tmp file
        if (!(new File(tmpfilename)).delete()) { (new File(tmpfilename)).deleteOnExit(); }

        log.debug("Done formatting");
    }
}
