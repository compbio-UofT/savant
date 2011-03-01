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
package savant.agp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.w3c.dom.Element;
import savant.api.adapter.TrackAdapter;
import savant.api.util.DialogUtils;
import savant.api.util.TrackUtils;
import savant.util.MiscUtils;
import savant.view.icon.SavantIconFactory;

/**
 * FTP browser for loading tracks from 1000genomes.org.
 *
 * @author tarkvara
 */
public class HTTPBrowser extends JPanel {

    private static final Log LOG = LogFactory.getLog(HTTPBrowser.class);
    private JLabel addressLabel;
    private JTable table;
    private FTPClient ftp;
    String user = "anonymous";
    String password = "";
    private static String host;
    int port;
    private static File curDir;
    private static File rootDir;

    public HTTPBrowser(URL rootURL) throws IOException {
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
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = table.rowAtPoint(evt.getPoint());
                    try {
                        String f = ((HTTPTableModel) table.getModel()).getEntry(row);

                        if (f.equals("..")) {
                            // Going up a directory.
                            curDir = curDir.getParentFile();
                            updateDirectory();
                        } else if (!f.contains(".")) {
                            if (f.startsWith("/")) {
                                curDir = new File(f);
                            } else {
                                curDir = new File(curDir,f);
                            }
                            updateDirectory();
                        } else {

                            List<TrackAdapter> tracks = TrackUtils.createTrack(new URI(getPath() + "/" + f.replace("\\", "/")));
                            TrackUtils.addTracks(tracks);
                        }
                    } catch (Exception x) {
                        DialogUtils.displayException("AGP Plugin Error", "Unable to process request.", x);
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

        List<String> files = listFiles();
        addressLabel.setText("http://" + host + curDir.getPath().replace("\\", "/"));
        table.setModel(new HTTPTableModel(files, !curDir.equals(rootDir)));

        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setMaxWidth(40);           // icon
        columns.getColumn(1).setPreferredWidth(400);    // name
        //columns.getColumn(2).setPreferredWidth(60);     // size
    }

    public static String getPath() {
        return "http://" + host + curDir.toString().replace("\\", "/");
    }

    private List<String> listFiles() throws IOException {

        String rtparent = this.rootDir.getParent().replace("\\", "/") + "/";

        boolean atRoot = false;
        List<String> files = new ArrayList<String>();
        Source source = new Source(new URL(getPath()));
        List<net.htmlparser.jericho.Element> elementList = source.getAllElements();
        for (net.htmlparser.jericho.Element element : elementList) {
            if (element.getName().equals("a")) {
                String href = element.getAttributeValue("href");
                if (!href.equals(rtparent)) {
                    if (!href.startsWith("/")) {
                        files.add(href);
                    } else {
                       // System.out.println("not adding " + href);
                    }
                } else {
                    atRoot = true;
                }
            }
        }

        if (!atRoot) {
            files.add(0,"..");
        }

        return files;
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

/*
class HTTPFile {
public HTTPFile(String url, String name)
}
 * 
 */
class HTTPTableModel extends AbstractTableModel {

    private List<String> files = new ArrayList<String>();

    HTTPTableModel(List<String> files, boolean hasParent) {
        // We can't use the FTPFile array directly, because it contains some items
        // we want to suppress.  We may also want to insert a fake entry for the parent directory.
        for (String f : files) {
            if (f != null && !f.contains("?")) {
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
        //return 3;
        return 2;
    }

    @Override
    public String getColumnName(int col) {
        switch (col) {
            case 1:
                return "Name";
            //case 2:
            //    return "Size";
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
        String f = files.get(row);
        switch (col) {
            case 0:
                return getIcon(f);
            case 1:
                return f;
                /*
            case 2:
                if (f.equals("..") || !f.contains(".")) {
                    return "";
                } else {
                    try {
                        //HttpURLConnection httpConn = (HttpURLConnection) (new URL(HTTPBrowser.getPath() + "/" + f)).openConnection();
                        //httpConn.get
                        //long totalsize = httpConn.getContentLength();
                        //System.out.println(f + " " + totalsize);
                        //return MiscUtils.getSophisticatedByteString(totalsize);
                        return "";
                    } catch (Exception ex) {
                        return "?" + ex.getMessage();
                    }
                }
                 *
                 */
        }
        return null;
    }

    String getEntry(int row) {
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
                return String.format("%.2f kB", size / 1.0E3);
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
    private static Icon getIcon(String f) {
        try {
            if (f.contains("..") || f.contains("/")) {
                return FileSystemView.getFileSystemView().getSystemIcon(new File("."));
            } else {
                Icon i;
                if (MiscUtils.isLikelyFormattedTrack(f)) {
                    i = SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK);
                } else {
                    File tmp = File.createTempFile("temp_icon.", "." + "txt");
                    i = FileSystemView.getFileSystemView().getSystemIcon(tmp);
                    tmp.delete();
                }
                return i;
            }
        } catch (IOException ex) {
            return null;
        }

    }
}
