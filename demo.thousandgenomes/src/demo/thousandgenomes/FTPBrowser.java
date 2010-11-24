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

package demo.thousandgenomes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import savant.api.adapter.ViewTrackAdapter;
import savant.api.util.DialogUtils;
import savant.api.util.TrackUtils;

/**
 * FTP browser for loading tracks from 1000genomes.org.
 *
 * @author tarkvara
 */
public class FTPBrowser extends JPanel {
    private static final Log LOG = LogFactory.getLog(FTPBrowser.class);

    private JLabel addressLabel;
    private JTable table;
    private FTPClient ftp;

    String user = "anonymous";
    String password = "";
    String host;
    int port;
    File curDir;
    File rootDir;

    public FTPBrowser(URL rootURL) throws IOException {
        host = rootURL.getHost();
        int p = rootURL.getPort();
        port = p != -1 ? p : rootURL.getDefaultPort();
        rootDir = new File(rootURL.getPath());
        curDir = rootDir;

        setLayout(new BorderLayout());

        addressLabel = new JLabel();
        add(addressLabel, BorderLayout.NORTH);

        table = new JTable();
        updateDirectory();

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt){
                if (evt.getClickCount() == 2) {
                    int row = table.rowAtPoint(evt.getPoint());
                    try {
                        FTPFile f = ((FTPTableModel)table.getModel()).getEntry(row);
                        if (f == null) {
                            // Going up a directory.
                            curDir = curDir.getParentFile();
                            updateDirectory();
                        } else if (f.isDirectory()) {
                            curDir = new File(curDir, f.getName());
                            updateDirectory();
                        } else {
                            List<ViewTrackAdapter> tracks = TrackUtils.createTrack(new URI("ftp://" + host + new File(curDir, f.getName()).getAbsolutePath()));
                            TrackUtils.addTracks(tracks);
                        }
                    } catch (Exception x) {
                        DialogUtils.displayException("FTP Error", "Unable to process FTP request.", x);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        add(scrollPane, BorderLayout.CENTER);

        this.setPreferredSize(new Dimension(800, 500));
    }

    private void updateDirectory() throws IOException {
        FTPFile[] files = getFTPClient().listFiles(curDir.getAbsolutePath());
        addressLabel.setText("ftp://" + host + curDir.getAbsolutePath());
        table.setModel(new FTPTableModel(files, !curDir.equals(rootDir)));

        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setMaxWidth(40);           // icon
        columns.getColumn(1).setPreferredWidth(400);    // name
        columns.getColumn(2).setPreferredWidth(60);     // size
    }

    private FTPClient getFTPClient() throws IOException {
        if (ftp == null) {
            ftp = new FTPClient();
            ftp.connect(host, port);
            ftp.login(user, password);
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();
//            ftp.setSoTimeout(SOCKET_TIMEOUT);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                throw new IOException(String.format("FTP server refused connection (code %d).", reply));
            }
        }

        return ftp;
    }

    public void openConnection() throws IOException {
        updateDirectory();
    }

    public void closeConnection() throws IOException {
        if (ftp != null) {
            ftp.disconnect();
        }
    }
}

class FTPTableModel extends AbstractTableModel {
    private List<FTPFile> files = new ArrayList<FTPFile>();

    FTPTableModel(FTPFile[] files, boolean hasParent) {
        // We can't use the FTPFile array directly, because it contains some items
        // we want to suppress.  We may also want to insert a fake entry for the parent directory.
        if (hasParent) {
            this.files.add(null);
        }
        for (FTPFile f : files) {
            if (!f.getName().startsWith(".")) {
                this.files.add(f);
            }
        }
    }

    @Override
    public Class getColumnClass(int col) {
        switch (col) {
            case 0:
                return Icon.class;
            default:
                return String.class;
        }
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int col) {
        switch (col) {
            case 1:
                return "Name";
            case 2:
                return "Size";
            default:
                return null;
        }
    }

    @Override
    public int getRowCount() {
        return files.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
        FTPFile f = files.get(row);
        switch (col) {
            case 0:
                return getIcon(f);
            case 1:
                return f == null ? ".." : f.getName();
            case 2:
                return getSizeString(f);
        }
        return null;
    }

    FTPFile getEntry(int row) {
        return files.get(row);
    }

    /**
     * Get a nicely formatted string describing the file's size.
     * 
     * @param f the FTP entry whose size we're displaying
     * @return a string describing the size, or null if f is a directory
     */
    private static String getSizeString(FTPFile f) {
        if (f != null && f.isFile()) {
            long size = f.getSize();
            if (size < 1E6) {
                return size + " kB";
            } else if (size < 1E9) {
                return String.format("%.2f MB", size / 1.0E6);
            } else {
                return String.format("%.2f GB", size / 1.0E9);
            }
        }
        return null;
    }

    /**
     * Use the Swing FileSystemView to get a system icon corresponding to the given
     * file.
     *
     * @param f the FTP entry whose icon we want to retrieve
     * @return a system icon representing f
     */
    private static Icon getIcon(FTPFile f) {
        if (f == null || f.isDirectory()) {
            return FileSystemView.getFileSystemView().getSystemIcon(new File("."));
        } else {
            String name = f.getName();
            int ind = name.lastIndexOf(".");
            if (ind > 0) {
                String ext = name.substring(ind + 1);
                try {
                    File tempFile = File.createTempFile("temp_icon.", "." + ext);
                    Icon i = FileSystemView.getFileSystemView().getSystemIcon(tempFile);
                    tempFile.delete();
                    return i;
                } catch (IOException ex) {
                }
            }
        }
        return null;
    }
}
