/*
 *    Copyright 2010-2011 University of Toronto
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

package savant.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.JProgressBar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.util.MiscUtils;
import savant.view.dialog.DownloadDialog;
import savant.view.swing.Savant;
import savant.view.swing.util.DialogUtils;

/**
 *
 * @author mfiume
 */
public class ThreadedURLDownload implements Runnable {
    private static final Log LOG = LogFactory.getLog(ThreadedURLDownload.class);

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
        File destination = new File(dir.getAbsolutePath(), MiscUtils.getFilenameFromPath(url.getFile()));

        while (destination.exists()) {
            destination = new File(DialogUtils.displayInputMessage("Duplicate File", "File already exists. Please enter a new path:", destination.getPath()));
            if (destination == null) {
                return;
            }
        }

        DownloadDialog dd = null;
        if (showDownloadDialog) {
            dd = new DownloadDialog(Savant.getInstance(), false, Thread.currentThread());
            dd.setDestination(destination);
            dd.setSource(url.toExternalForm());
            dd.setVisible(true);
        }

        if (downloadFile(url, destination, dd)) {
            if (showDownloadDialog) {
                JProgressBar b = dd.getProgressBar();
                b.setIndeterminate(false);
                b.setValue(100);
                dd.setComplete();
            }

            //DialogUtils.displayMessage("Download Complete", "Download of " + url.toExternalForm() + " complete");
        } else {
            if (destFile != null && destFile.exists()) {
                destFile.delete();
            }
            DialogUtils.displayError("Sorry", "Error downloading " + url.toExternalForm() + ".\n"
                    + "Please ensure you have a working connection to the internet.");
            if (showDownloadDialog) {
                dd.dispose();
            }
        }

        //if (showDownloadDialog) {
        //    dd.dispose();
        //}
    }

    File destFile;

    private boolean downloadFile(URL u, File destFile, DownloadDialog dd) {
        OutputStream out = null;
        InputStream in = null;
        try {
            destFile.createNewFile();
            if (!destFile.exists()) {
                DialogUtils.displayError("Sorry", "Could not create output file: " + destFile.getAbsolutePath());
                return false;
            }
        } catch (IOException ex) {
            DialogUtils.displayError("Sorry", "Could not create output file: " + destFile.getAbsolutePath());
            return false;
        }
        
        try {
            out = new FileOutputStream(destFile.getAbsolutePath());

            HttpURLConnection httpConn = (HttpURLConnection) u.openConnection();
            long totalsize = httpConn.getContentLength();
            if (totalsize == -1) {
                dd.getProgressBar().setIndeterminate(true);
            }

            LOG.info("Length of " + u.getPath() + ": " + totalsize);

            in = u.openStream();

            byte[] buf = new byte[1000 * 1024]; // 1000K buffer
            int bytesRead;
            long totalread = 0;
            while ((bytesRead = in.read(buf)) != -1) {
                out.write(buf, 0, bytesRead);
                totalread += bytesRead;
                if (dd != null) {
                    if (totalsize != -1) {
                        dd.setProgress(((int) (100*(((double)totalread)/totalsize))));
                        dd.setAmountDownloaded(MiscUtils.getSophisticatedByteString(totalread) + " downloaded (" +  ((int) (100*(((double)totalread)/totalsize))) + " %)");
                    } else {
                        dd.setAmountDownloaded(MiscUtils.getSophisticatedByteString(totalread) + " downloaded");
                    }
                }
                if (Thread.interrupted()) {
                    out.close();
                    destFile.delete();
                    return false;
                }
            }

            if (totalread != totalsize) {
                LOG.warn("Error downloading file");
                out.close();
                destFile.delete();
            }
        } catch (IOException ioe) {
            return false;
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ioe) {
                return false;
            } catch (NullPointerException n) {
                return false;
            }
        }

        return true;
    }
}
