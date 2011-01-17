/*
 *    Copyright 2009-2011 University of Toronto
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
package savant.plugin.builtin;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.jidesoft.grid.TreeTable;
import com.jidesoft.swing.TableSearchable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import savant.api.util.DialogUtils;
import savant.data.sources.DataSource;
import savant.view.dialog.tree.TreeBrowserModel;
import savant.view.dialog.tree.TreeBrowserEntry;
import savant.settings.BrowserSettings;
import savant.util.DownloadFile;
import savant.view.swing.Savant;
import savant.view.swing.TrackFactory;

/**
 *
 * @author mfiume
 */
public class SAFEBrowser extends JDialog {

    private static final Log LOG = LogFactory.getLog(SAFEBrowser.class);
    private static final TableCellRenderer FILE_RENDERER = new FileRowCellRenderer();
    private Frame p;
    private TreeTable table;
    private URL trackPath = null;
    private static SAFEBrowser instance;

    private boolean loggedIn = false;
    private String username;
    private String password;

    public static SAFEBrowser getInstance() throws JDOMException, IOException {
        if (instance == null) {
            instance = new SAFEBrowser();
        }
        instance.trackPath = null;
        return instance;
    }

    private SAFEBrowser() throws JDOMException, IOException {
        super(Savant.getInstance(), "Savant File Exchange", true);
        init();
    }

    private void actOnSelectedItem(boolean ignoreActionOnBranch) {
        TreeBrowserEntry r = (TreeBrowserEntry) table.getRowAt(table.getSelectedRow());
        if (r != null && r.isLeaf()) {
            try {
                LOG.debug("Setting track path to " + r.getURL().toString());
                trackPath = r.getURL();
                closeDialog();
            } catch (Exception ex) {
                DialogUtils.displayMessage(String.format("Error opening URL %s: %s.", r.getURL(), ex.getLocalizedMessage()));
            }
        } else {
            if (!ignoreActionOnBranch) {
                DialogUtils.displayMessage("Please select a track.");
            }
        }
    }

    private void closeDialog() {
        this.setVisible(false);
    }

    public DataSource getDataSource() {
        if (trackPath == null) {
            LOG.error("Trackpath is null");
            return null;
        } else {
            try {
                DataSource d = TrackFactory.createDataSource(trackPath.toURI());
                return d;
            } catch (Exception ex) {
                LOG.error(String.format("Unable to create data source for %s: %s.", trackPath, ex));
                return null;
            }
        }
    }

    private static TreeBrowserEntry parseDocumentTreeRow(Element root) {
        if (root.getName().equals("branch")) {
            List<TreeBrowserEntry> children = new ArrayList<TreeBrowserEntry>();
            for (Object o : root.getChildren()) {
                Element c = (Element) o;
                children.add(parseDocumentTreeRow(c));
            }
            return new TreeBrowserEntry(root.getAttributeValue("name"), children);
        } else if (root.getName().equals("leaf")) {
            try {
                return new TreeBrowserEntry(
                        root.getAttributeValue("name"),
                        root.getChildText("type"),
                        root.getChildText("description"),
                        new URL(root.getChildText("url")),
                        root.getChildText("size"));
            } catch (MalformedURLException x) {
                LOG.error(x);
            }
        }
        return null;
    }

    /*
     * if (login()) {
        File f = DownloadFile.downloadFile(new URL("http://savantbrowser.com/safe/savantsafe.php?username=mfiume&password=fiume3640"), System.getProperty("java.io.tmpdir"));

     */

    private JPanel loginCard;
    private JPanel safeCard;
    private CardLayout layout;
    private JPanel container;

    private void init() {
        this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        this.setResizable(true);

        this.setLayout(new BorderLayout());
        container = new JPanel();
        this.add(container, BorderLayout.CENTER);


        layout = new CardLayout();
        container.setLayout(layout);
        loginCard = new SAFELoginPanel(this);
        safeCard = new JPanel();
        container.add(loginCard,"login");
        container.add(safeCard,"safe");

        this.setPreferredSize(new Dimension(800, 500));
        this.pack();

        setLocationRelativeTo(Savant.getInstance());
    }

    void initSafe(final String username, final String password) throws MalformedURLException, JDOMException, IOException {

        safeCard.removeAll();
        safeCard.setLayout(new BorderLayout());

        File f = DownloadFile.downloadFile(new URL(BrowserSettings.safe + "?type=list&username=" + username + "&password=" + password), System.getProperty("java.io.tmpdir"));

        if (!wereCredentialsValid(f)) {
            DialogUtils.displayMessage("Login failed.");
            return;
        }

        final Component mainp = getCenterPanel(getDownloadTreeRows(f));
        safeCard.add(mainp, BorderLayout.CENTER);

        JToolBar bottombar = new JToolBar();
        bottombar.setFloatable(false);
        bottombar.setAlignmentX(RIGHT_ALIGNMENT);
        bottombar.add(Box.createHorizontalGlue());

        /*
        JButton refbutt = new JButton("Refresh");
        refbutt.putClientProperty( "JButton.buttonType", "default" );
        refbutt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    System.out.println("Refreshing");
                    safeCard.remove(mainp);
                    File f = DownloadFile.downloadFile(new URL("http://savantbrowser.com/safe/savantsafe.php?username=" + username + "&password=" + password), System.getProperty("java.io.tmpdir"));
                    Component newmainp = getCenterPanel(getDownloadTreeRows(f));
                    safeCard.add(newmainp, BorderLayout.CENTER);
                    container.invalidate();
                    System.out.println("Done Refreshing");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        bottombar.add(refbutt);
         *
         */

        JButton addgroupbutt = new JButton("Create group");
        addgroupbutt.putClientProperty( "JButton.buttonType", "default" );
        addgroupbutt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    addGroup(username, password);
                } catch (Exception ex) {
                    LOG.error("Unable to create group: " + ex.getLocalizedMessage());
                }
            }
        });
        bottombar.add(addgroupbutt);

        JButton logoutbutt = new JButton("Logout");
        logoutbutt.putClientProperty( "JButton.buttonType", "default" );
        logoutbutt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                layout.show(container, "login");
            }
        });
        bottombar.add(logoutbutt);

        JButton openbutt = new JButton("Load Track");
        openbutt.putClientProperty( "JButton.buttonType", "default" );
        openbutt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                actOnSelectedItem(false);
            }
        });
        bottombar.add(openbutt);

        safeCard.add(bottombar, BorderLayout.SOUTH);

        layout.show(container, "safe");
    }

    private boolean wereCredentialsValid(File f) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = br.readLine();
            if (line.contains("branch")){
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
    }

    public static void addGroup(String username, String password) {
        (new AddSAFEGroup(Savant.getInstance(), true, username, password)).setVisible(true);
    }

    public static class FileRowCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof TreeBrowserEntry) {
                TreeBrowserEntry fileRow = (TreeBrowserEntry) value;
                JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                        fileRow.getName(),
                        isSelected, hasFocus, row, column);
                try {
                    label.setIcon(fileRow.getIcon());
                } catch (Exception e) {
                    //System.out.println(fileRow.getFile().getAbsolutePath());
                }
                label.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
                return label;
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private static List<TreeBrowserEntry> getDownloadTreeRows(File f) throws JDOMException, IOException {
        List<TreeBrowserEntry> roots = new ArrayList<TreeBrowserEntry>();
        Document d = new SAXBuilder().build(f);
        Element root = d.getRootElement();
        TreeBrowserEntry treeroot = parseDocumentTreeRow(root);
        roots.add(treeroot);
        return roots;
    }

    public final Component getCenterPanel(List<TreeBrowserEntry> roots) {
        table = new TreeTable(new TreeBrowserModel(roots));
        table.setSortable(false);
        table.setRespectRenderPreferredHeight(true);

        // configure the TreeTable
        table.setExpandAllAllowed(true);
        table.setShowTreeLines(false);
        table.setSortingEnabled(false);
        table.setRowHeight(18);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.expandFirstLevel();
        table.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    actOnSelectedItem(true);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        // do not select row when expanding a row.
        table.setSelectRowWhenToggling(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        //table.getColumnModel().getColumn(1).setPreferredWidth(400);
        //table.getColumnModel().getColumn(2).setPreferredWidth(100);
        //table.getColumnModel().getColumn(3).setPreferredWidth(100);
        //table.getColumnModel().getColumn(4).setPreferredWidth(50);

        table.getColumnModel().getColumn(0).setCellRenderer(FILE_RENDERER);

        // add searchable feature
        TableSearchable searchable = new TableSearchable(table) {

            @Override
            protected String convertElementToString(Object item) {
                if (item instanceof TreeBrowserEntry) {
                    return ((TreeBrowserEntry) item).getType();
                }
                return super.convertElementToString(item);
            }
        };
        searchable.setMainIndex(0); // only search for name column

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(800, 500));
        return panel;
    }
}
