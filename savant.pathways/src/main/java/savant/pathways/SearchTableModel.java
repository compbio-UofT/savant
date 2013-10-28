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
import org.pathvisio.wikipathways.webservice.WSSearchResult;

/**
 *
 * @author AndrewBrook
 */
public class SearchTableModel extends AbstractTableModel {
    private List<WSSearchResult> pathways = new ArrayList<WSSearchResult>();
    private String[] headers = {"Search Relevance", "ID", "Name", "Species", "Revision", "URL"};

    SearchTableModel(WSSearchResult[] pathways){
        this.pathways.addAll(Arrays.asList(pathways));
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
        WSSearchResult pathway = pathways.get(rowIndex);
        switch (columnIndex){
            case 0:
                return pathway.getScore();
            case 1:
                return pathway.getId();
            case 2:
                return pathway.getName();
            case 3:
                return pathway.getSpecies();
            case 4:
                return pathway.getRevision();
            case 5:
                return pathway.getUrl();
        }
        return null;
    }

    @Override
    public String getColumnName(int column) {
        return headers[column];
    }

    public WSSearchResult getEntry(int row){
        return pathways.get(row);
    }
}