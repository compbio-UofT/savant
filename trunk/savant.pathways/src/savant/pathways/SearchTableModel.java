/*
 *    Copyright 2011 University of Toronto
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