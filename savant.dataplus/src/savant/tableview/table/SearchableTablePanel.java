/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.tableview.table;

import com.jidesoft.grid.AutoFilterTableHeader;
import com.jidesoft.grid.QuickTableFilterField;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.lucene.LuceneFilterableTableModel;
import com.jidesoft.lucene.LuceneQuickTableFilterField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author mfiume
 */
public class SearchableTablePanel extends JPanel {

    private SortableTable table;
    private JPanel fieldPanel;

    public SearchableTablePanel(Vector data, Vector columnNames) {
        this(data, columnNames, null);
    }

    public SortableTable getTable() {
        return table;
    }
    private QuickTableFilterField filterField;
    GenericTableModel model;

    public void updateData(Vector data) {
        model.getDataVector().removeAllElements();
        model.getDataVector().addAll(data);
        model.fireTableDataChanged();

        System.out.println("Actually Searching columns: ");
        for (int i : filterField.getActualSearchingColumnIndices()) {
            System.out.println("\t" + i);
        }
        System.out.println("Searching columns: ");
        for (int i : filterField.getColumnIndices()) {
            System.out.println("\t" + i);
        }
    }

    public void setTableModel(Vector data, Vector columnNames, Vector columnClasses) {
        model = new GenericTableModel(data, columnNames, columnClasses);

        int[] columns = new int[data.size()];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = i;
        }

        if (filterField == null) {
            filterField = new LuceneQuickTableFilterField(model);
            filterField.setHintText("Type here to filter");
            filterField.setColumnIndices(columns);
            //filterField.setObjectConverterManagerEnabled(true);
        } else {
            filterField.setTableModel(model);
        }

        //table.setModel(model);
        table.setModel(new LuceneFilterableTableModel(filterField.getDisplayTableModel()));
        /*
        System.out.println("Searching columns: ");
        for (int i : filterField.getActualSearchingColumnIndices()) {
            System.out.println("\t" + i);
        }
         * 
         */
    }

    public SearchableTablePanel(
            Vector data, Vector columnNames, Vector columnClasses) {

        table = new SortableTable() {

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
                //even index, selected or not selected

                if (isCellSelected(Index_row, Index_col)) {
                    comp.setBackground(new Color(75, 149, 229));

                } else {
                    if (Index_row % 2 == 0 && !isCellSelected(Index_row, Index_col)) {
                        comp.setBackground(Color.white);
                    } else {
                        comp.setBackground(new Color(242, 245, 249));
                    }
                }
                return comp;
            }
        };

        table.setClearSelectionOnTableDataChanges(true);
        table.setOptimized(true);
        table.setColumnAutoResizable(true);
        table.setAutoResort(false);
        table.setDragEnabled(false);

        table.setAutoResizeMode(SortableTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        AutoFilterTableHeader header = new AutoFilterTableHeader(table);
        header.setAutoFilterEnabled(true);
        header.setShowFilterIcon(true);
        header.setShowFilterName(true);
        table.setTableHeader(header);

        setTableModel(data, columnNames, columnClasses);

        this.setLayout(new BorderLayout(3, 3));
        fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        fieldPanel.add(filterField);

        JPanel tablePanel = new JPanel(new BorderLayout(3, 3));
        tablePanel.add(new JScrollPane(table));

        this.add(fieldPanel, BorderLayout.BEFORE_FIRST_LINE);

        this.add(tablePanel);
    }
}
