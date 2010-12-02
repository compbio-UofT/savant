/*
 *    Copyright 2009-2010 University of Toronto
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.jidesoft.grid.TreeTable;
import com.jidesoft.swing.TableSearchable;
import java.net.URI;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import savant.api.util.DialogUtils;
import savant.data.sources.DataSource;
import savant.net.TreeListTableModel;
import savant.net.TreeRow;
import savant.settings.BrowserSettings;
import savant.util.DownloadFile;
import savant.view.swing.Savant;
import savant.view.swing.TrackFactory;

/**
 *
 * @author mfiume
 */
public class SavantFileRepositoryBrowser extends JDialog {
    private static final Log LOG = LogFactory.getLog(SavantFileRepositoryBrowser.class);

    private static final TableCellRenderer FILE_RENDERER = new FileRowCellRenderer();

    private Frame p;
    private TreeTable table;

    private String trackpath = null;


    private static SavantFileRepositoryBrowser instance;

    public static SavantFileRepositoryBrowser getInstance() throws JDOMException, IOException {
        if (instance == null) {
            instance = new SavantFileRepositoryBrowser();
        }
        instance.trackpath = null;
        return instance;
    }

    private SavantFileRepositoryBrowser() throws JDOMException, IOException {
        this(
                Savant.getInstance(),
                true,
                "Savant File Repository Browser",
                getDownloadTreeRows(DownloadFile.downloadFile(new URL(BrowserSettings.url_data), System.getProperty("java.io.tmpdir")))
                );
    }

    private SavantFileRepositoryBrowser(
            Frame parent,
            boolean modal,
            String title,
            List<TreeRow> roots) {

        super(parent, title, modal);

        setLocationRelativeTo(parent);

        this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        p = parent;
        this.setResizable(true);
        this.setLayout(new BorderLayout());
        this.add(getCenterPanel(roots), BorderLayout.CENTER);

        JToolBar bottombar = new JToolBar();
        bottombar.setFloatable(false);
        bottombar.setAlignmentX(RIGHT_ALIGNMENT);
        bottombar.add(Box.createHorizontalGlue());
        JButton openbutt = new JButton("Load Track");
        openbutt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TreeRow r  = (TreeRow) table.getRowAt(table.getSelectedRow());
                if (r != null && r.isLeaf()) {
                    try {
                        System.out.println("Setting track path to " + r.getURL().toString());
                        trackpath = r.getURL().toString();
                        closeDialog();
                    } catch (Exception ex) {
                        DialogUtils.displayMessage("Error opening URL: " + r.getURL());
                    }
                } else {
                    DialogUtils.displayMessage("Please select a track");
                }
            }

        });
        bottombar.add(openbutt);
        
        this.add(bottombar, BorderLayout.SOUTH);

        this.setPreferredSize(new Dimension(800, 500));
        this.pack();
    }

    private void closeDialog() {
        this.setVisible(false);
    }

    public DataSource getDataSource() {
        if (trackpath == null) {
            System.out.println("Trackpath is null");
            return null;
        }
        else {
            try {
                DataSource d = TrackFactory.createDataSource(new URI(trackpath));
                System.out.println("Datasource is " + d);
                return d;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    private static TreeRow parseDocumentTreeRow(Element root) {
        if (root.getName().equals("branch")) {
            List<TreeRow> children = new ArrayList<TreeRow>();
            for (Object o : root.getChildren()) {
                Element c = (Element) o;
                children.add(parseDocumentTreeRow(c));
            }
            return new TreeRow(root.getAttributeValue("name"), children);
        } else if (root.getName().equals("leaf")) {
            return new TreeRow(
                    root.getAttributeValue("name"),
                    root.getChildText("type"),
                    root.getChildText("description"),
                    root.getChildText("url"),
                    root.getChildText("size")
                    );
        } else {
            return null;
        }
    }

    public static class FileRowCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof TreeRow) {
                TreeRow fileRow = (TreeRow) value;
                JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                        fileRow.getName(),
                        isSelected, hasFocus, row, column);
                try {
                    label.setIcon(fileRow.getIcon());
                }
                catch (Exception e) {
                    //System.out.println(fileRow.getFile().getAbsolutePath());
                }
                label.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
                return label;
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private static List<TreeRow> getDownloadTreeRows(File f) throws JDOMException, IOException {
        List<TreeRow> roots = new ArrayList<TreeRow>();
        Document d = new SAXBuilder().build(f);
        Element root = d.getRootElement();
        TreeRow treeroot = parseDocumentTreeRow(root);
        roots.add(treeroot);
        return roots;
    }

    public final Component getCenterPanel(List<TreeRow> roots) {
        table = new TreeTable(new TreeListTableModel(roots));
        table.setSortable(true);
        table.setRespectRenderPreferredHeight(true);

        // configure the TreeTable
        table.setExpandAllAllowed(true);
        table.setShowTreeLines(false);
        table.setSortingEnabled(false);
        table.setRowHeight(18);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        //table.expandAll();
        table.expandFirstLevel();

        // do not select row when expanding a row.
        table.setSelectRowWhenToggling(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        //table.getColumnModel().getColumn(3).setPreferredWidth(100);
        //table.getColumnModel().getColumn(4).setPreferredWidth(50);

        table.getColumnModel().getColumn(0).setCellRenderer(FILE_RENDERER);

        // add searchable feature
        TableSearchable searchable = new TableSearchable(table) {

            @Override
            protected String convertElementToString(Object item) {
                if (item instanceof TreeRow) {
                    return ((TreeRow) item).getType();
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
