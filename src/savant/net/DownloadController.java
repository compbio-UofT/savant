/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.net;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import savant.view.swing.Savant;

/**
 *
 * @author mfiume
 */
public class DownloadController {

    //DownloadDialog dd;
    Queue<DownloadInfo> downloadQueue;
    boolean isDownloading = false;

    public DownloadController() {
        //dd = new DownloadDialog(Savant.getInstance(), false);
        downloadQueue = new LinkedList<DownloadInfo>();
    }

    private static DownloadController instance;
    public static DownloadController getInstance() {
        if (instance == null) {
            instance = new DownloadController();
        }
        return instance;
    }

    /*
    public void setDownloadDialogVisible(boolean arg) {
        dd.setVisible(arg);
    }
     * 
     */

    private void downloadQueuedFiles() {
        while(!downloadQueue.isEmpty()) {
            download(downloadQueue.remove(), true);
        }
    }

    private void download(DownloadInfo i, boolean d) {
        ThreadedURLDownload t = new ThreadedURLDownload(i.url, i.dir, d);
        (new Thread (t)).start();
    }

    public static class DownloadInfo {
        URL url;
        File dir;

        public DownloadInfo(URL url, File dir) {
            this.url = url;
            this.dir = dir;
        }

    }

    public void enqueueDownload(String url, File destination) {
        try {
            enqueueDownload(new URL(url), destination);
        } catch (MalformedURLException ex) {
            JOptionPane.showMessageDialog(Savant.getInstance(), "Invalid URL, download cannot continue.");
        }
    }

    public void enqueueDownload(URL url, File destination) {
        this.downloadQueue.add(new DownloadInfo(url, destination));
        downloadQueuedFiles();
    }
}
