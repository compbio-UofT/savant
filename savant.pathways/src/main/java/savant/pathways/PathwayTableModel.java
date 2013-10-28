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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;

/**
 *
 * @author AndrewBrook
 */
public class PathwayTableModel extends AbstractTableModel {
    private List<WSPathwayInfo> pathways = new ArrayList<WSPathwayInfo>();
    private String[] headers = {"ID", "Name", "Species", "Revision", "URL"};

    PathwayTableModel(WSPathwayInfo[] pathways, boolean hasParent){
        if(hasParent) this.pathways.add(null);
        this.pathways.addAll(Arrays.asList(pathways));
        Collections.sort(this.pathways, PATHWAY_COMPARATOR);
    }

    @Override
    public int getRowCount() {
        return pathways.size();
    }

    @Override
    public int getColumnCount() {
        return headers.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        WSPathwayInfo pathway = pathways.get(rowIndex);
        switch (columnIndex){
            case 0:
                return pathway == null ? ".." : pathway.getId();
            case 1:
                return pathway == null ? "" : pathway.getName();
            case 2:
                return pathway == null ? "" : pathway.getSpecies();
            case 3:
                return pathway == null ? "" : pathway.getRevision();
            case 4:
                return pathway == null ? "" : pathway.getUrl();
        }
        return null;
    }

    @Override
    public String getColumnName(int column) {
        return headers[column];
    }

    public WSPathwayInfo getEntry(int row){
        return pathways.get(row);
    }
    
    private static final Comparator<WSPathwayInfo> PATHWAY_COMPARATOR = new Comparator<WSPathwayInfo>() {
        @Override
        public int compare(WSPathwayInfo t, WSPathwayInfo t1) {
            if(t == null) return -1;
            if(t1 == null) return 1;
            if(t.getId().toLowerCase().startsWith("wp") && t1.getId().toLowerCase().startsWith("wp")){
                try {
                    Integer a = Integer.parseInt(t.getId().substring(2));
                    Integer b = Integer.parseInt(t1.getId().substring(2));
                    return a.compareTo(b);
                } catch (NumberFormatException e){}
            } 
            return t.getId().compareTo(t1.getId());
        }
    };
}
