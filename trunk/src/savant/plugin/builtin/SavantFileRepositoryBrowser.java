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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.jidesoft.grid.TreeTable;
import com.jidesoft.swing.TableSearchable;
import java.net.MalformedURLException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.data.types.Genome;
import savant.util.MiscUtils;
import savant.view.dialog.tree.TreeBrowserModel;
import savant.view.dialog.tree.TreeBrowserEntry;


/**
 * Dialog which allows users to browse contents of Savant data-file repository.
 *
 * @author mfiume
 */
public class SavantFileRepositoryBrowser extends JDialog {

    private static final Log LOG = LogFactory.getLog(SavantFileRepositoryBrowser.class);
    private static final TableCellRenderer FILE_RENDERER = new FileRowCellRenderer();

    private TreeTable table;
    private URL trackPath = null;
    private static SavantFileRepositoryBrowser instance;

    public static SavantFileRepositoryBrowser getInstance() {
        if (instance == null) {
            instance = new SavantFileRepositoryBrowser(DialogUtils.getMainWindow());
        }
        instance.trackPath = null;
        return instance;
    }

    private SavantFileRepositoryBrowser(Window parent) {
        super(parent, "Public Savant File Repository Browser", Dialog.ModalityType.APPLICATION_MODAL);

        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setResizable(true);
        setLayout(new BorderLayout());
        add(getCenterPanel(getDownloadTreeRows()), BorderLayout.CENTER);

        JMenuBar bottombar = new JMenuBar();
        bottombar.setAlignmentX(RIGHT_ALIGNMENT);
        bottombar.add(Box.createHorizontalGlue());
        JButton openbutt = new JButton("Load Track");
        openbutt.putClientProperty("JButton.buttonType", "default");
        openbutt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actOnSelectedItem(false);
            }
        });
        bottombar.add(openbutt);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.putClientProperty("JButton.buttonType", "default");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }
        });
        bottombar.add(cancelButton);

        add(bottombar, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(800, 500));
        pack();

        setLocationRelativeTo(parent);
    }

    private void actOnSelectedItem(boolean ignoreActionOnBranch) {
        TreeBrowserEntry r = (TreeBrowserEntry) table.getRowAt(table.getSelectedRow());
        if (r != null && r.isLeaf()) {
            try {
                LOG.info("Setting track path to " + r.getURL());
                trackPath = r.getURL();
                closeDialog();
            } catch (Exception ex) {
                DialogUtils.displayMessage(String.format("Error opening URL %s: %s.", r.getURL(), ex));
            }
        } else {
            if (!ignoreActionOnBranch) {
                DialogUtils.displayMessage("Please select a track.");
            }
        }
    }

    private void closeDialog() {
        setVisible(false);
    }

    public URL getTrackPath() {
        return trackPath;
    }

    private static class FileRowCellRenderer extends DefaultTableCellRenderer {

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

    private static List<TreeBrowserEntry> getDownloadTreeRows() {
        Genome[] genomes = Genome.getDefaultGenomes();
        List<TreeBrowserEntry> roots = new ArrayList<TreeBrowserEntry>(genomes.length);
        try {
            for (Genome g: genomes) {
                Genome.Auxiliary[] auxes = g.getAuxiliaries();
                if (auxes.length > 0) {
                    List<TreeBrowserEntry> auxEntries = new ArrayList<TreeBrowserEntry>(auxes.length);
                    for (Genome.Auxiliary aux: g.getAuxiliaries()) {
                        auxEntries.add(new TreeBrowserEntry(MiscUtils.getFileName(aux.uri), aux.type.toString(), aux.description, aux.uri.toURL(), "0"));
                    }
                    roots.add(new TreeBrowserEntry(g.getDescription(), auxEntries));
                }
            }
        } catch (MalformedURLException ignored) {
        }
        return roots;
    }

    public final Component getCenterPanel(List<TreeBrowserEntry> roots) {
        table = new TreeTable(new TreeBrowserModel(roots) {
            @Override
            public String[] getColumnNames() {
                return new String[] { "Name", "Description" };
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
        table.expandFirstLevel();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    actOnSelectedItem(true);
                }
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
