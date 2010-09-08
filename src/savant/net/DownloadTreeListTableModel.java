/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.net;

import com.jidesoft.grid.CellStyle;
import com.jidesoft.grid.StyleModel;
import com.jidesoft.grid.TreeTableModel;

import java.awt.*;
import java.util.List;
import javax.swing.JButton;

public class DownloadTreeListTableModel extends TreeTableModel implements StyleModel {
    static final protected String[] COLUMN_NAMES = {"Name", "Description", "Type", "Filename", "Size"}; //, "Download"};

    public DownloadTreeListTableModel(List rows) {
        super(rows);
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    static final Color BACKGROUND = new Color(247, 247, 247);
    static final CellStyle CELL_STYLE = new CellStyle();

    static {
        CELL_STYLE.setBackground(BACKGROUND);
    }

    public CellStyle getCellStyleAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return CELL_STYLE;
        }
        else {
            return null;
        }
    }

    public boolean isCellStyleOn() {
        return true;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return DownloadTreeRow.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            case 3:
                return String.class;
            case 4:
                return String.class;
            //case 4:
            //    return JButton.class;
        }
        return super.getColumnClass(columnIndex);
    }
   
}
