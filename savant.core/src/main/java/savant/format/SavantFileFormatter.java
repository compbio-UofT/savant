/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.format;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import com.jidesoft.dialog.JideOptionPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.file.FieldType;
import savant.file.FileType;
import savant.file.SavantROFile;
import savant.util.Controller;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.util.SavantFileUtils;
import savant.util.error.report.BugReportDialog;
import savant.view.swing.Savant;


public abstract class SavantFileFormatter extends Controller<FormatEvent> {
    static final Log LOG = LogFactory.getLog(SavantFileFormatter.class);

    /** Input file. */
    protected final File inFile;

    /** Output file. */
    protected final File outFile;

    protected BufferedReader inFileReader;

    private Thread formatThread;

    // non-UI ...
    // variables to keep track of progress processing the input file(s)
    protected long totalBytes;
    protected long byteCount;

    public SavantFileFormatter(File inFile, File outFile) {
        this.inFile = inFile;
        this.outFile = outFile;
    }

    public File getInputFile() {
        return inFile;
    }

    public File getOutputFile() {
        return outFile;
    }

    public abstract void format() throws InterruptedException, IOException;

    protected void setProgress(double prog, String status) {
        fireEvent(new FormatEvent(prog, status));
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
     * Format the input file path, storing the result in the output file path
     * @throws InterruptedException
     * @throws IOException
     */
    public static SavantFileFormatter getFormatter(File inFile, File outFile, FileType inputFileType) throws IOException, SavantFileFormattingException {

        if (inputFileType == FileType.INTERVAL_BAM) {
            // Create a coverage file from a BAM file.
            return new BAMToCoverage(inFile);
        } else {
            // Format the input file in the appropriate way
            switch (inputFileType) {
                case INTERVAL_BED:
                case INTERVAL_BED1:
                case INTERVAL_GENERIC:
                case INTERVAL_GFF:
                case INTERVAL_GTF:
                case INTERVAL_PSL:
                case INTERVAL_VCF:
                case INTERVAL_KNOWNGENE:
                case INTERVAL_REFGENE:
                case INTERVAL_UNKNOWN:
                    if (!verifyTextFile(inFile, '#', true)) {
                        if (DialogUtils.askYesNo("<html>This file does not appear to be tab-delimited. Do you wish to try processing this file by interpreting runs of spaces as tabs?<br><br><i>Warning: This may or may not work correctly.</i></html>") == DialogUtils.YES) {
                            return new TabixFormatter(inFile, outFile, inputFileType, true);
                        }
                        return null;
                    }
                    return new TabixFormatter(inFile, outFile, inputFileType, false);
                case CONTINUOUS_WIG:
                    verifyTextFile(inFile, '#', false);
                    return new TDFFormatter(inFile, outFile);
                default:
                    return null;
            }
        }
    }

    /**
     * Runs the format operation in a separate thread.
     */
    public void run() {
        formatThread = new Thread("Formatter") {
            @Override
            public void run() {
                try {
                    fireEvent(new FormatEvent(FormatEvent.Type.STARTED));
                    format();
                    fireEvent(new FormatEvent(outFile));
                } catch (Throwable x) {
                    LOG.info("Formatting failed.", x.getCause());
                    fireEvent(new FormatEvent(x));
                }
            }
        };
        formatThread.start();
    }

    public void cancel() {
        formatThread.interrupt();
    }

    /**
     * Verifies that the file in question is a tab-delimited text file.
     *
     * @param f the file to be examined
     * @param lookingForTabs if true, we're looking for a tab-delimited file (false for .fasta and .wig)
     */
    private static boolean verifyTextFile(File f, char commentChar, boolean lookingForTabs) throws IOException, SavantFileFormattingException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(f));
            String line = reader.readLine();
            do {
                line = reader.readLine();
            } while (line != null && line.charAt(0) == commentChar);

            boolean tabFound = false;
            if (line != null) {
                // Found a "line" which doesn't have a comment character.
                // Look for tabs and unprintable characters.
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    if (c == '\t') {
                        tabFound = true;
                    } else if (c < ' ' || c > '~') {
                        throw new SavantFileFormattingException(String.format("%s does not appear to be a text file.", f.getName()));
                    }
                }
            }
            // Got through a whole line without finding any suspicious characters.
            if (lookingForTabs && !tabFound) {
                return false;
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return true;
    }

    public static void reportFormattingError(final Throwable x, final File inFile) {
        if (x instanceof InterruptedException) {
            DialogUtils.displayMessage("Format cancelled.");
        } else if (x instanceof SavantFileFormattingException) {
            // Not a Savant error.  They've just chosen the wrong kind of file.
            DialogUtils.displayMessage("Format Unsuccessful", x.getMessage());
        } else {
            JideOptionPane optionPane = new JideOptionPane(String.format("<html>Message: <i>%s</i><br><br>Click the <i>Details</i> button to see more information...<br><br>"
                    + "Please report any issues you experience to the to the development team.</html>", MiscUtils.getMessage(x)), JOptionPane.ERROR_MESSAGE, JideOptionPane.CLOSE_OPTION);
            optionPane.setTitle("A problem was encountered while formatting.");
            optionPane.setOptions(new String[] {});
            JButton reportButton = new JButton("Report Issue");
            ((JComponent)optionPane.getComponent(optionPane.getComponentCount() - 1)).add(reportButton);
            final JDialog dialog = optionPane.createDialog(DialogUtils.getMainWindow(), "Format Unsuccessful");
            dialog.setModal(true);
            dialog.setResizable(true);
            optionPane.setDetails(MiscUtils.getStackTrace(x));
            //optionPane.setDetailsVisible(true);
            dialog.pack();

            reportButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e2) {
                    String issue = "I am having trouble formatting my file for use with Savant.\nI have provided additional diagnostic information below.\n\n";

                    issue += "SOURCE OF FILE: [e.g. UCSC]\n\n";
                    issue += "TYPE: [e.g. BED]\n\n";
                    issue += "CONTENTS: [e.g. human genes]\n\n";
                    issue += "PATH: " + inFile + "\n\n";
                    issue += "ADDITIONAL COMMENTS:\n\n";

                    dialog.dispose();
                    new BugReportDialog(Savant.getInstance(), issue).setVisible(true);
                }

            });

            dialog.setVisible(true);
        }
    }


    public static Map<String,IntervalSearchTree> readIntervalBSTs(SavantROFile dFile) throws IOException {

        // read the refname -> index position map
        Map<String, long[]> refMap = SavantFileUtils.readReferenceMap(dFile);

        if (LOG.isDebugEnabled()) LOG.debug("\n=== DONE PARSING REF<->DATA MAP ===\n\n");

        // change the offset
        dFile.setHeaderOffset(dFile.getFilePointer());

        Map<String, IntervalSearchTree> trees = new HashMap<String, IntervalSearchTree>();

        int treenum = 0;

        if (LOG.isDebugEnabled()) LOG.debug("Number of trees to get: " + refMap.keySet().size());

        // keep track of the maximum end of tree position
        // (IMPORTANT NOTE: order of elements returned by keySet() is not gauranteed!!!)
        long maxend = Long.MIN_VALUE;
        for (String refname : refMap.keySet()) {
            long[] v = refMap.get(refname);
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
            LOG.debug("Read " + treenum + " trees (i.e. indices)");
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
}
