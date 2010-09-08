/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import savant.util.MiscUtils;
import savant.view.swing.Savant;

/**
 *
 * @author mfiume
 */
public class ThreadedURLDownload implements Runnable {

    URL url;
    File dir;
    boolean showDownloadDialog;

    public ThreadedURLDownload(URL url, File dir, boolean showDownloadDialog) {
        this.url = url;
        this.dir = dir;
        
        this.showDownloadDialog = showDownloadDialog;
    }

    @Override
    public void run() {
        String destination = dir.getAbsolutePath()
                    + System.getProperty("file.separator")
                    + MiscUtils.getFilenameFromPath(url.getFile());

        DownloadDialog dd = null;
        if (showDownloadDialog) {
            dd = new DownloadDialog(Savant.getInstance(), false, Thread.currentThread());
            dd.setDestination(destination);
            dd.setSource(url.getFile());
            dd.setVisible(true);
            //dd.validate();
        }

        if (downloadFile(url, dir.getAbsolutePath(), dd)) {
            if (showDownloadDialog) {
                JProgressBar b = dd.getProgressBar();
                b.setIndeterminate(false);
                b.setValue(100);
            }

            JOptionPane.showMessageDialog(Savant.getInstance(), "Download of " + url.getFile() + " complete");
        } else {
            if (destFile != null && destFile.exists()) {
                destFile.delete();
            }
            JOptionPane.showMessageDialog(Savant.getInstance(), "Download of " + url.getFile() + " incomplete");
        }

        if (showDownloadDialog) {
            dd.dispose();
        }
    }

    File destFile;

    private boolean downloadFile(URL u, String destDir, DownloadDialog dd) {
        OutputStream out = null;
        InputStream in = null;
        destFile = new File(destDir + System.getProperty("file.separator") + MiscUtils.getFilenameFromPath(u.getPath()));
        try {
            out = new FileOutputStream(destFile.getAbsolutePath());

            HttpURLConnection httpConn = (HttpURLConnection) u.openConnection();
            int totalsize = httpConn.getContentLength();

            in = u.openStream();

            byte[] buf = new byte[1000 * 1024]; // 1000K buffer
            int bytesRead;
            int totalread = 0;
            while ((bytesRead = in.read(buf)) != -1) {
                out.write(buf, 0, bytesRead);
                totalread += bytesRead;
                if (dd != null) {
                    dd.setProgress(((totalread*100)/totalsize));
                    dd.setAmountDownloaded(totalread + " Kb downloaded (" + ((totalread*100)/totalsize) + " %)");
                }
                if (Thread.interrupted()) {
                    return false;
                }
            }
        } catch (IOException ioe) {
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ioe) {
            } catch (NullPointerException n) {
            }
        }

        return true;
    }
}
