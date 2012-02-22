/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.pathways;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.bridgedb.bio.Organism;

/**
 *
 * @author AndrewBrook
 */
public class OrganismTableModel extends AbstractTableModel {
    private List<String> names = new ArrayList<String>();
    private String[] headers = {"Latin Name", "English Name"};

    OrganismTableModel(String[] names, boolean hasParent){
        if(hasParent) this.names.add(null);
        this.names.add(PathwaysBrowser.ALL_ORGANISMS);
        this.names.addAll(Arrays.asList(names));
    }

    @Override
    public int getRowCount() {
        return names.size();
    }

    @Override
    public int getColumnCount() {
        return headers.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        String s = names.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return s == null ? ".." : s;
            case 1:
                if(s == null) return "..";
                else if (s.equals(PathwaysBrowser.ALL_ORGANISMS)) return "";
                else return Organism.fromLatinName(s).shortName();
        }
        return null;
    }

    @Override
    public String getColumnName(int column) {
        return headers[column];
    }

    public String getEntry(int row){
        return names.get(row);
    }
}