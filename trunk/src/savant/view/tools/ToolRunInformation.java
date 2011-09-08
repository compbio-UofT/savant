/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.view.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedOutputStream;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;
import savant.settings.BrowserSettings;

/**
 *
 * @author Marc Fiume
 */
public class ToolRunInformation {

    public enum TerminationStatus { INCOMPLETE, COMPLETE, ERROR, INTERRUPT };

    private TerminationStatus terminationStatus = TerminationStatus.INCOMPLETE;
    private Date startTime, endTime;
    private JPanel outputCanvas;
    private JTextArea outputTextArea;
    private PrintStream outstream;
    private int progress;
    private String status;
    private JProgressBar progressBar;

    public ToolRunInformation() {
        initOutputCanvas();
    }

    public void setTerminationStatus(TerminationStatus ts) {
        this.terminationStatus = ts;
    }

    public TerminationStatus getTerminationStatus() {
        return this.terminationStatus;
    }

    public PrintStream getOutputStream() { return outstream; }

    private void initOutputCanvas() {
        outputCanvas = new JPanel();
        outputCanvas.setLayout(new BorderLayout());

        outputTextArea = new JTextArea();
        outputTextArea.setWrapStyleWord(true);
        outputTextArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret)outputTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        outputCanvas.add(new JScrollPane(outputTextArea),BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        int width = 150;
        int height = 20;
        progressBar.setMinimumSize(new Dimension(width,height));
        progressBar.setMaximumSize(new Dimension(width,height));
        progressBar.setPreferredSize(new Dimension(width,height));
        progressBar.setSize(new Dimension(width,height));
        progressBar.setBorderPainted(false);
        progressBar.setIndeterminate(true);

        initOutputToTextArea(outputTextArea);
        //redirectOutputToTextArea(outputTextArea);
    }

    public JComponent getOutputCanvas() { return this.outputCanvas; }
    //public JTextArea getOutputTextArea() { return this.outputTextArea; }

    public Date getStartTime() { return this.startTime; }
    public Date getEndTime() { return this.endTime; }

    public void setStartTimeAsNow() {
        Calendar c = Calendar.getInstance();
        startTime = c.getTime();
    }

    public void setEndTimeAsNow() {
        Calendar c = Calendar.getInstance();
        endTime = c.getTime();
    }

    public void initOutputToTextArea(JTextArea ta) {
        outstream = new PrintStream(new FilteredStream(new ByteArrayOutputStream()));
    }

    public String getStatus() { return progressBar.getString(); }
    public void setStatus(String status) { progressBar.setString(status); }
    public int getProgress() { return progressBar.getValue(); }
    public void setProgress(int progress) {
        progressBar.setIndeterminate(false);
        progressBar.setValue(progress);
    }
    public JProgressBar getProgressBar() {
        return progressBar;
    }

    /*
    public void redirectOutputToTextArea(JTextArea ta) {
        outputTextArea = ta;
        PrintStream aPrintStream = new PrintStream(new FilteredStream(new ByteArrayOutputStream()));
        System.setOut(aPrintStream);
        System.setErr(aPrintStream);
    }
     */

    public String getOutput() {
        return this.outputTextArea.getText();
    }

    class FilteredStream extends BufferedOutputStream {
        public FilteredStream(OutputStream aStream) {
          super(aStream,bufferSize);
        }

        static final int bufferSize = 2048;
        int maxLines = 3000;

        @Override
        public void close() throws IOException {
            outputTextArea.setText("");
            super.close();
        }

        public void write(byte b[]) throws IOException {
          String aString = new String(b);
          dontOverflow();
          outputTextArea.append(aString);
        }

        public void write(byte b[], int off, int len) throws IOException {
          String aString = new String(b, off, len);
          dontOverflow();
          outputTextArea.append(aString);
        }

        int linesTruncated = 0;
        int timesTruncated = 0;

        public void dontOverflow() {
            int numLines = outputTextArea.getLineCount();
            if (numLines > maxLines) {
                linesTruncated += numLines;
                timesTruncated++;
                outputTextArea.setText("[ Omitting about " + (maxLines*timesTruncated) + " lines ]\n");
           }
        }
    }
}
