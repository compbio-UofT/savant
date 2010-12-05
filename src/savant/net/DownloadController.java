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
import javax.swing.JDialog;
import javax.swing.JOptionPane;
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
        JDialog parent;
        File dir;

        public DownloadInfo(URL url, File dir, JDialog parent) {
            this.url = url;
            this.dir = dir;
            this.parent = parent;
        }

        public DownloadInfo(URL url, File dir) {
            this.url = url;
            this.dir = dir;
        }

    }

    public void enqueueDownload(String url, File destination) {
        enqueueDownload(url, destination, null);
    }

    public void enqueueDownload(String url, File destination, JDialog parent) {
        try {
            enqueueDownload(new URL(url), destination, parent);
        } catch (MalformedURLException ex) {
            JOptionPane.showMessageDialog(Savant.getInstance(), "Invalid URL, download cannot continue.");
        }
    }

    public void enqueueDownload(URL url, File destination) {
        enqueueDownload(url, destination, null);
    }

    public void enqueueDownload(URL url, File destination, JDialog parent) {
        this.downloadQueue.add(new DownloadInfo(url, destination, parent));
        downloadQueuedFiles();
    }
}
