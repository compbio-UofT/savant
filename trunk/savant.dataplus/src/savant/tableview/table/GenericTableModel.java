/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.tableview.table;

import com.jidesoft.grid.DefaultContextSensitiveTableModel;
import java.util.Vector;

/**
 *
 * @author mfiume
 */
public class GenericTableModel extends DefaultContextSensitiveTableModel {

    Vector columnClasses;

    public GenericTableModel(Vector data, Vector columnNames, Vector columnClasses) {
        super(data, columnNames);
        this.columnClasses = columnClasses;
    }

    public GenericTableModel(Vector data, Vector columnNames) {
        this(data, columnNames,null);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnClasses == null || columnClasses.size() == 0) return String.class;
        return (Class) columnClasses.get(columnIndex);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
