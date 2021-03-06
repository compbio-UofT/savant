/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.thousandgenomes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
import javax.swing.JTextField;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import savant.api.adapter.TrackAdapter;
import savant.api.util.DialogUtils;
import savant.api.util.TrackUtils;

/**
 * FTP browser for loading tracks from 1000genomes.org.
 *
 * @author tarkvara
 */
public class FTPBrowser extends JPanel {
    private static final Log LOG = LogFactory.getLog(FTPBrowser.class);

    private JTextField addressField;
    private JTable table;
    private FTPClient ftp;

    String user = "anonymous";
    String password = "";
    String host;
    int port;
    File curDir;
    File rootDir;

    TableCellRenderer iconCellRenderer = new IconRenderer();
    TableCellRenderer defaultCellRenderer = new DefaultTableCellRenderer();

    public FTPBrowser(URL rootURL) throws IOException {
        setRoot(rootURL);

        setLayout(new BorderLayout());

        addressField = new JTextField();
        addressField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    setRoot(new URL(addressField.getText()));
                    updateDirectory();
                } catch (Exception x) {
                    DialogUtils.displayException("1000 Genomes Plugin", "Unable to change root directory", x);
                }
            }
            
        });
        add(addressField, BorderLayout.NORTH);

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
                            TrackUtils.createTrack(new URI("ftp://" + host + new File(curDir, f.getName()).getPath().replace("\\", "/")));
                        }
                    } catch (Throwable x) {
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

    public String getRoot() throws MalformedURLException {
        return new URL("ftp", host, port, rootDir.getPath()).toString();
    }

    private void setRoot(URL rootURL) {
        host = rootURL.getHost();
        int p = rootURL.getPort();
        port = p != -1 ? p : rootURL.getDefaultPort();
        rootDir = new File(rootURL.getPath());
        curDir = rootDir;
    }

    private void updateDirectory() throws IOException {
        FTPFile[] files = getFTPClient().listFiles(curDir.getPath().replace("\\", "/"));
        addressField.setText("ftp://" + host + curDir.getPath().replace("\\", "/"));
        table.setModel(new FTPTableModel(files, !curDir.equals(rootDir)));

        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setMaxWidth(40);           // icon
        columns.getColumn(1).setPreferredWidth(400);    // name
        columns.getColumn(2).setPreferredWidth(60);     // size

        //Renderers are being set to null (somehow) on Linux only. Replacing them here.
        columns.getColumn(0).setCellRenderer(iconCellRenderer);
        columns.getColumn(1).setCellRenderer(defaultCellRenderer);
        columns.getColumn(2).setCellRenderer(defaultCellRenderer);
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

/*
 * This class is copied straight from JTable.
 * On Linux, renderers are being removed when TableModel set. Using this to
 * force icon rendering for first column. 
 */
class IconRenderer extends DefaultTableCellRenderer.UIResource {
    public IconRenderer() {
        super();
        setHorizontalAlignment(JLabel.CENTER);
    }
    @Override
    public void setValue(Object value) { setIcon((value instanceof Icon) ? (Icon)value : null); }
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
