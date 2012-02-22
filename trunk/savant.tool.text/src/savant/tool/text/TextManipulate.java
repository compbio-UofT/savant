/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.tool.text;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import savant.plugin.PluginAdapter;
import savant.plugin.ToolInformation;
import savant.plugin.ToolPlugin;
import savant.tool.text.javacsv.com.csvreader.CsvReader;
import savant.view.swing.util.DocumentViewer;

/**
 *
 * @author mfiume
 */
public class TextManipulate extends ToolPlugin {

    @Override
    public void init(PluginAdapter pluginAdapter) {
        this.setEventSubscriptionEnabled(false);
        //this.setRunnableEnabled(false);
    }

    @Override
    public ToolInformation getToolInformation() {
        return new ToolInformation(
                "Text File Manipulate",
                "Convert",
                "Performs standard operations on delimited text files (e.g. CSV, GFF, BED, etc).",
                "1.0",
                "Marc Fiume",
                "http://www.cs.utoronto.ca/~mfiume/");
    }

    @Override
    public JComponent getCanvas() {
        JTabbedPane p = new JTabbedPane();
        p.add("Cut columns", getCutColumnsPane());
        return p;
    }

    @Override
    public void runTool() throws InterruptedException {

        runCutColumns();
        //ToolRunInformation runInfo = this.getRunInformation();

        
    }

    //runInfo.setProgress(i * 100 / maxCount);
    //        runInfo.setStatus("on " + i + " of " + maxCount);
    //terminateIfInterruped();

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

    private JTextField cutColumns_inpath;
    private JTextField cutColumns_columns;
    private JTextField cutColumns_outpath;
    private char cutColumns_delimiter = '\t';

    private Component getCutColumnsPane() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));

        Component[] infilecs = new Component[3];
        infilecs[0] = new JLabel("Input file: ");
        cutColumns_inpath = new JTextField();
        infilecs[1] = cutColumns_inpath;
        infilecs[2] = new JButton("...");
        p.add(getRowComponent(infilecs));

        Component[] colcs = new Component[2];
        colcs[0] = new JLabel("Columns to remove: ");
        cutColumns_columns = new JTextField();
        colcs[1] = cutColumns_columns;
        p.add(getRowComponent(colcs));

        Component[] outfilecs = new Component[3];
        outfilecs[0] = new JLabel("Output file: ");
        cutColumns_outpath = new JTextField();
        outfilecs[1] = cutColumns_outpath;
        outfilecs[2] = new JButton("...");
        p.add(getRowComponent(outfilecs));

        p.add(Box.createGlue());

        /*
        Component[] runcs = new Component[1];
        JButton runButt = new JButton("Run");
        runButt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runCutColumns();
            }
        });
        runcs[0] = runButt;
        p.add(getRowComponent(runcs));
         */

        return p;
    }

    private void runCutColumns() {
        try {
            CsvReader r = new CsvReader(cutColumns_inpath.getText().trim(), cutColumns_delimiter);
            Set<Integer> columns = getColumnsToCut(cutColumns_columns.getText());
            BufferedWriter bw = new BufferedWriter(new FileWriter(cutColumns_outpath.getText().trim()));

            String[] record;
            while (r.readRecord()) {
                record = r.getValues();
                String cutline = "";
                for (int i = 0; i < record.length; i++) {
                    if (!columns.contains(i)) {
                        cutline += record[i] + cutColumns_delimiter;
                    }
                }
                bw.write(cutline.trim() + "\n");
            }

            r.close();
            bw.close();

        } catch (FileNotFoundException ex) {
            System.err.println("Cut columns failed");
        } catch (IOException ex) {
            System.err.println("Cut columns failed");
        }
        
        DocumentViewer v = new DocumentViewer();
        v.addDocument(cutColumns_inpath.getText().trim());
        v.addDocument(cutColumns_outpath.getText().trim());
        v.setVisible(true);
    }

    private Set<Integer> getColumnsToCut(String strcols) {
        Set<Integer> cols = new HashSet<Integer>();
        StringTokenizer st = new StringTokenizer(strcols,",");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            try {
                cols.add(Integer.parseInt(token));
            } catch(Exception e) {}
        }
        return cols;
    }

    private Component getRowComponent(Component[] components) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        for (Component c : components) {
            p.add(c);
        }
        return p;
    }



}
