/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util.swing;

import com.jidesoft.grid.*;
import com.jidesoft.swing.JideTabbedPane;
import java.awt.BorderLayout;

import javax.swing.*;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author mfiume
 */
public class FilterableTable extends JComponent {

    private SortableTable _table;

    public FilterableTable(Vector data, Vector columnNames, List<Class> classes) {

        GenericTableModel model = new GenericTableModel(data, columnNames, classes);

        _table = new SortableTable(model);
        _table.setClearSelectionOnTableDataChanges(false);
        _table.setRowResizable(true);
        _table.setVariousRowHeights(true);
        _table.setSelectInsertedRows(false);

        AutoFilterTableHeader _header = new AutoFilterTableHeader(_table);
        _header.setAutoFilterEnabled(true);
        _table.setTableHeader(_header);

        this.setLayout(new BorderLayout());

        this.add(_table /*new JScrollPane(_table)*/, BorderLayout.CENTER);

        JFrame fr = new JFrame();
        fr.setLayout(new BorderLayout());
        fr.add(this, BorderLayout.CENTER);
        fr.setVisible(true);
    }

    private static class GenericTableModel extends DefaultContextSensitiveTableModel {

        List<Class> classes;

        public GenericTableModel(Vector<Object> data, Vector<String> columnNames, List<Class> classes) {
            super(data, columnNames);
            this.classes = classes;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return classes.get(columnIndex);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}