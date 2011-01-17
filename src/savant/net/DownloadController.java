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
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import javax.swing.JDialog;

/**
 *
 * @author mfiume
 */
public class DownloadController {

    //DownloadDialog dd;
    Queue<DownloadInfo> downloadQueue;
    boolean isDownloading = false;

    private DownloadController() {
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
    }

    public void enqueueDownload(URL url, File destination, JDialog parent) {
        downloadQueue.add(new DownloadInfo(url, destination, parent));
        downloadQueuedFiles();
    }
}
