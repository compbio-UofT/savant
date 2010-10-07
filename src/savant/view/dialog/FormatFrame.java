/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FormatFrame.java
 *
 * Created on Oct 4, 2010, 7:14:37 PM
 */
package savant.view.dialog;

import com.jidesoft.dialog.JideOptionPane;
import java.io.IOException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.format.DataFormatter;
import savant.format.DataFormatterThread;
import savant.format.FormatProgressListener;
import savant.util.MiscUtils;
import savant.view.icon.SavantIconFactory;
import savant.view.swing.Savant;

/**
 *
 * @author mfiume
 */
public class FormatFrame extends javax.swing.JFrame implements FormatProgressListener {

    private static final Log log = LogFactory.getLog(DataFormatForm.class);
    //DataFormatter dataFormatter;
    Thread formatThread;
    String inputFilePath;
    String outputFilePath;

    /** Creates new form FormatFrame */
    public FormatFrame(DataFormatter df) {
        initComponents();
         this.setIconImage(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.LOGO).getImage());

        DataFormatterThread dft = new DataFormatterThread(df);
        dft.setFormatFrame(this);
        this.formatThread = new Thread(dft);

        df.addProgressListener(this);

        this.inputFilePath = df.getInputFilePath();
        this.outputFilePath = df.getOutputFilePath();

        this.label_src.setText(shorten(df.getInputFilePath()));
        this.label_dest.setText(shorten(df.getOutputFilePath()));
        //this.label_type.setText(df.getInputFileType().toString());

        setSubtaskStatus("");
        setSubtaskProgress(0);
        setOverallProgress(0);
        //setOverallProgress(0,1);

        formatThread.start();
    }

    public void setSubtaskStatus(String msg) {
        if (msg != null) {
            this.label_status.setText(msg);
        }
    }

    int overallprogress_at;
    //int overallprogress_total;

    private void setTitle() {
        this.setTitle("("  + subtaskprogress + "% done subtask" + /*"/" + overallprogress_total +*/ ") Formatting " + inputFilePath);
    }
    
    int subtaskprogress;

    public void setSubtaskProgress(Integer p) {
        if (p == null) {
            //this.progress_current.setIndeterminate(true);
        } else if (p >= 0 && p <= 100) {
            subtaskprogress = p;
            this.progress_current.setIndeterminate(false);
            this.progress_current.setValue(p);
            setTitle();
        }
    }

    public void setFormatComplete() {
        this.setTitle("(100%) Format of " + this.inputFilePath + " complete");
        this.label_overallstatus.setText("Format complete.");
    }

    public void setOverallProgress(int at) {
        if (at != overallprogress_at) {
            setSubtaskProgress(0);
        }
        overallprogress_at = at;
        setTitle();
        //this.label_overallstatus.setText("Status: ");
        //this.label_overallstatus.setText("Performing task " + at + ":");
    }


    /*public void setOverallProgress(int at, int total) {
        if (at != overallprogress_at) {
            setSubtaskProgress(0);
        }
        overallprogress_at = at;
        overallprogress_total = total;
        setTitle();
        this.label_overallstatus.setText("Performing task " + at + " of " + total + " ...");
    }*/

    private void cancelTask() {
        int result =JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel?",
                "Confirm cancel",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            formatThread.interrupt();
            this.dispose();
        }
    }

    @Override
    public void taskProgressUpdate(Integer progress, String status) {
        setSubtaskProgress(progress);
        setSubtaskStatus(status);
    }

    @Override
    public void incrementOverallProgress() {
        this.setOverallProgress(overallprogress_at+1);
    }

    /*
    @Override
    public void overallProgressUpdate(int at, int total) {
        //setOverallProgress(at, total);
        setOverallProgress(at);
    }

    @Override
    public void overallProgressUpdate(int at) {
        setOverallProgress(at);
    }
     * 
     */

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel5 = new javax.swing.JLabel();
        label_src = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        label_status = new javax.swing.JLabel();
        progress_current = new javax.swing.JProgressBar();
        jButton1 = new javax.swing.JButton();
        label_dest = new javax.swing.JLabel();
        label_overallstatus = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Formatting ...");
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        setResizable(false);

        jLabel5.setText("Destination: ");

        label_src.setText("filename1");

        jLabel3.setText("Formatting: ");

        label_status.setText("status ...");

        jButton1.setText("Cancel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        label_dest.setText("filename2");

        label_overallstatus.setText("Status: ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel3))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(label_src, javax.swing.GroupLayout.PREFERRED_SIZE, 398, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(label_dest, javax.swing.GroupLayout.PREFERRED_SIZE, 398, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(label_overallstatus)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(label_status, javax.swing.GroupLayout.PREFERRED_SIZE, 433, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 477, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(progress_current, javax.swing.GroupLayout.DEFAULT_SIZE, 477, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(label_src))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(label_dest))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(label_overallstatus)
                    .addComponent(label_status))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progress_current, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        cancelTask();
}//GEN-LAST:event_jButton1ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel label_dest;
    private javax.swing.JLabel label_overallstatus;
    private javax.swing.JLabel label_src;
    private javax.swing.JLabel label_status;
    private javax.swing.JProgressBar progress_current;
    // End of variables declaration//GEN-END:variables

    public void notifyOfTermination(boolean wasFormatSuccessful, Exception e) {
        if (wasFormatSuccessful) {
            int result = JOptionPane.showConfirmDialog(this, "Format successful. Open track now?", "Format Successful", JOptionPane.YES_NO_OPTION);
            this.setVisible(false);
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Savant.getInstance().addTrackFromFile(outputFilePath);
                } catch (IOException ex) {
                }
            }
            this.dispose();
        } else if (e instanceof InterruptedException) {
            JOptionPane.showMessageDialog(this, "Format cancelled.", "Format Cancelled", 0);
        } else {
                JideOptionPane optionPane = new JideOptionPane("Click \"Details\" button to see more information ... \n\n"
                        + "Problems formatting files? Please copy these details and email them to savant@cs.toronto.edu \n"
                        + "along with the file you are trying to format (if it is under 10MB in size). The Savant Team will \n"
                        + "be happy to help troubleshoot the issue with you.", JOptionPane.ERROR_MESSAGE, JideOptionPane.CLOSE_OPTION);
                optionPane.setTitle("A problem was encountered while formatting.");
                JDialog dialog = optionPane.createDialog(this, "Format unsuccessful");
                dialog.setModal(true);
                dialog.setResizable(true);
                optionPane.setDetails(MiscUtils.getStackTrace(e));
                //optionPane.setDetailsVisible(true);
                dialog.pack();
                dialog.setVisible(true);
                this.dispose();
        }
    }

    private String shorten(String string) {
        int maxlen = 50;
        if (string.length() > maxlen) {
            return "..." + string.substring(string.length()-maxlen);
        } else {
            return string;
        }
    }


}
