/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.net;

import com.jidesoft.grid.TreeTable;
import com.jidesoft.swing.TableSearchable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import savant.settings.DirectorySettings;
import savant.util.MiscUtils;

/**
 *
 * @author mfiume
 */
public class DownloadTreeList extends JDialog implements MouseListener {

    private static DownloadTreeRow parseDocumentTreeRow(Element root) {
        if (root.getName().equals("branch")) {
            List<DownloadTreeRow> children = new ArrayList<DownloadTreeRow>();
            for (Object o : root.getChildren()) {
                Element c = (Element) o;
                children.add(parseDocumentTreeRow(c));
            }
            return new DownloadTreeRow(root.getAttributeValue("name"), children);
        } else if (root.getName().equals("leaf")) {
            return new DownloadTreeRow(
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

    Frame p;
    String saveToDirectory;

    public DownloadTreeList(Frame parent, boolean modal, String title, File xmlfile, String destDir) throws JDOMException, IOException {
        this(parent, modal, title, getDownloadTreeRows(xmlfile), destDir);
    }

    public DownloadTreeList(
            Frame parent,
            boolean modal,
            String title,
            List<DownloadTreeRow> roots,
            String dir) {
        
        super(parent, title, modal);

        saveToDirectory = dir;
        p = parent;
        this.addMouseListener(this);
        this.setResizable(true);
        this.setLayout(new BorderLayout());
        this.add(getCenterPanel(roots), BorderLayout.CENTER);
        
        JToolBar bottombar = new JToolBar();
        bottombar.setFloatable(false);
        bottombar.setAlignmentX(RIGHT_ALIGNMENT);
        JButton downbutt = new JButton("Download");
        downbutt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadTreeRow r  = (DownloadTreeRow) _table.getRowAt(_table.getSelectedRow());
                if (r != null && r.isLeaf()) {
                    DownloadController.getInstance().enqueueDownload(r.getURL(), new File(saveToDirectory));
                    System.out.println(r.getURL());
                } else {
                    JOptionPane.showMessageDialog(p, "Please select a file");
                }
            }

        });
        bottombar.add(downbutt);
        this.add(bottombar, BorderLayout.NORTH);

        this.setPreferredSize(new Dimension(800, 500));
        this.pack();
    }
    public TreeTable _table;
    protected AbstractTableModel _tableModel;
    protected int _pattern;

    static final TableCellRenderer FILE_RENDERER = new FileRowCellRenderer();
    //static final TableCellRenderer FILE_SIZE_RENDERER = new FileSizeCellRenderer();
    //static final TableCellRenderer FILE_DATE_RENDERER = new FileDateCellRenderer();
    //private TreeTableModel createTableModel(List<DownloadTreeRow> roots) {
    //    return new DownloadTreeListTableModel(roots);
    //}

    public static class FileRowCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof DownloadTreeRow) {
                DownloadTreeRow fileRow = (DownloadTreeRow) value;
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

    private static List<DownloadTreeRow> getDownloadTreeRows(File f) throws JDOMException, IOException {
        List<DownloadTreeRow> roots = new ArrayList<DownloadTreeRow>();
        Document d = new SAXBuilder().build(f);
        Element root = d.getRootElement();
        DownloadTreeRow treeroot = parseDocumentTreeRow(root);
        roots.add(treeroot);
        return roots;
    }

    public Component getCenterPanel(List<DownloadTreeRow> roots) {
        _tableModel = new DownloadTreeListTableModel(roots);
        _table = new TreeTable(_tableModel);
        _table.setSortable(true);
        _table.setRespectRenderPreferredHeight(true);

        // configure the TreeTable
        _table.setExpandAllAllowed(true);
        _table.setShowTreeLines(false);
        _table.setSortingEnabled(false);
        _table.setRowHeight(18);
        _table.setShowGrid(false);
        _table.setIntercellSpacing(new Dimension(0, 0));
        //_table.expandFirstLevel();

        // do not select row when expanding a row.
        _table.setSelectRowWhenToggling(false);

        _table.getColumnModel().getColumn(0).setPreferredWidth(200);
        _table.getColumnModel().getColumn(1).setPreferredWidth(300);
        _table.getColumnModel().getColumn(2).setPreferredWidth(50);
        _table.getColumnModel().getColumn(3).setPreferredWidth(100);
        _table.getColumnModel().getColumn(4).setPreferredWidth(50);

        _table.getColumnModel().getColumn(0).setCellRenderer(FILE_RENDERER);
        //_table.getColumnModel().getColumn(1).setCellRenderer(FILE_SIZE_RENDERER);
        //_table.getColumnModel().getColumn(3).setCellRenderer(FILE_DATE_RENDERER);

        // add searchable feature
        TableSearchable searchable = new TableSearchable(_table) {

            @Override
            protected String convertElementToString(Object item) {
                if (item instanceof DownloadTreeRow) {
                    return ((DownloadTreeRow) item).getType();
                }
                return super.convertElementToString(item);
            }
        };
        searchable.setMainIndex(0); // only search for name column

        JScrollPane scrollPane = new JScrollPane(_table);
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(800, 500));
        return panel;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        /*
        JPopupMenu menu = new JPopupMenu();
        JMenuItem mi = new JMenuItem("Download");
        menu.add(mi);
        menu.show(this, e.getX(), e.getY());
         *
         */
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
}
