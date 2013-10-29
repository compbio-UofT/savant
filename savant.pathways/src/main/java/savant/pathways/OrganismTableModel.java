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
                else {
                    try {
                        return Organism.fromLatinName(s).shortName();
                    } catch (NullPointerException e){
                        return ""; //Fixes bug where server sends a name that isn't in current organism library. May need to update jars. 
                    }
                }
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