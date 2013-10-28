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
package savant.view.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.jidesoft.grid.CellStyle;
import com.jidesoft.grid.TreeTable;
import com.jidesoft.swing.TableSearchable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import savant.api.util.DialogUtils;
import savant.api.util.PluginUtils;
import savant.view.swing.model.TreeBrowserEntry;
import savant.view.swing.model.TreeBrowserModel;
import savant.util.MiscUtils;


/**
 *
 * @author mfiume
 */
public class PluginRepositoryDialog extends JDialog {
    private static final Log LOG = LogFactory.getLog(PluginRepositoryDialog.class);

    private TreeTable table;

    /**
     * Instantiate a plugin repository browser and let the user select from it.
     * Typically this is invoked from the PluginManagerDialog.
     *
     * @param parent parent window
     * @param title window title
     * @param buttonText text of button (typically "Install")
     * @param xmlFile plugin.xml file which defines repository entries
     */
    public PluginRepositoryDialog(Window parent, String title, String buttonText, File xmlFile) throws JDOMException, IOException {
        super(parent, title, Dialog.ModalityType.APPLICATION_MODAL);

        setResizable(true);
        setLayout(new BorderLayout());
        add(getCenterPanel(getDownloadTreeRows(xmlFile)), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        bottomBar.add(cancelButton);

        JButton installButton = new JButton(buttonText);
        installButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSelectedItem(false);
            }
        });
        bottomBar.add(installButton);

        add(bottomBar, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(1000, 500));
        pack();

        getRootPane().setDefaultButton(installButton);
        MiscUtils.registerCancelButton(cancelButton);
        setLocationRelativeTo(parent);
    }

    private void downloadSelectedItem(boolean ignoreBranchSelected) {
        TreeBrowserEntry r = (TreeBrowserEntry) table.getRowAt(table.getSelectedRow());
        if (r != null && r.isLeaf()) {
            // Hide the dialog when the download starts.  This makes its
            // behaviour slightly different from Install from File.
            setVisible(false);

            PluginUtils.installPlugin(r.getURL());
        } else {
            if (!ignoreBranchSelected) {
                DialogUtils.displayMessage("Please select a file");
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
                return new PluginBrowserEntry(root);
            } catch (MalformedURLException ex) {
                LOG.error(ex);
            }
        }
        return null;
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
        table = new TreeTable(new TreeBrowserModel(roots) {
            @Override
            public String[] getColumnNames() {
                return new String[] { "Name", "Description", "Web Site" };
            }

            @Override
            public CellStyle getCellStyleAt(int rowIndex, int columnIndex) {
                return null;
            }
        });
        table.setSortable(true); 
        table.setRespectRenderPreferredHeight(true);

        // configure the TreeTable
        table.setExpandAllAllowed(true);
        table.setShowTreeLines(false);
        table.setSortingEnabled(false);
        table.setRowHeight(18);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //table.expandAll();
        table.expandFirstLevel();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col == 2) {
                    Object o = table.getModel().getValueAt(table.rowAtPoint(e.getPoint()), col);
                    if (o != null && (o instanceof URL)) {
                        System.out.println(o.toString());
                        try {
                            Desktop.getDesktop().browse(((URL)o).toURI());
                        } catch (Exception x) {
                            LOG.error("Unable to open link for " + o, x);
                        }
                        return;
                    }
                }
                if (e.getClickCount() == 2) {
                    downloadSelectedItem(true);
                }
            }
        });

        // do not select row when expanding a row.
        table.setSelectRowWhenToggling(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(520);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);

        table.getColumnModel().getColumn(0).setCellRenderer(new FileRowCellRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new WebLinkRenderer());

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

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(800, 500));
        return panel;
    }
    
    private static class FileRowCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component result;
            if (value instanceof TreeBrowserEntry) {
                TreeBrowserEntry fileRow = (TreeBrowserEntry) value;
                JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                        fileRow.getName(),
                        isSelected, hasFocus, row, column);
                label.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
                result = label;
            } else {
                result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
            return result;
        }
    }

    private static class PluginBrowserEntry extends TreeBrowserEntry {
        private URL webSite;

        private PluginBrowserEntry(Element elem) throws MalformedURLException {
            super(elem.getAttributeValue("name"), null, elem.getChildText("description"), new URL(elem.getChildText("url")), null);
            String webSiteName = elem.getAttributeValue("website");
            if (webSiteName != null) {
                webSite = new URL(webSiteName);
            }
        }
            
        @Override
        public Object getValueAt(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return this;
                case 1:
                    return description;
                case 2:
                    return isLeaf ? webSite : null;
                default:
                    return null;
            }
        }
    }

    /**
     * Do we even need this class?  Maybe there's an easier way to do this with CellStyles or something.
     */
    private static class WebLinkRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            if (value != null && value instanceof URL) {
                URL url = (URL)value;
                value = String.format("<html><a href=\"%s\">%s</a></html>", url, url.getHost());
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        }
    }
}
