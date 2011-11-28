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

package savant.sql;

import java.net.URI;


/**
 * From our MappingDialog, we want to return both a selected table and the set of
 * mapped columns.  This little class serves that purpose.
 *
 * @author tarkvara
 */
public class MappedTable extends Table {
    ColumnMapping mapping;
    String trackName;

    public MappedTable(Table t, ColumnMapping mapping, String trackName) {
        super(t.name, t.database);
        this.mapping = mapping;
        this.trackName = trackName;
    }

    /**
     * In most cases, the table name and the track name are identical.
     */
    public MappedTable(Table t, ColumnMapping mapping) {
        this(t, mapping, t.name);
    }


    /**
     * Return the table-specific URI.  This will include the full database URI with
     * the table name appended as the last component.
     */
    public URI getURI() {
        return URI.create(database.serverURI + "/" + database.name + "/" + trackName);
    }

    /**
     * We can tell whether a table has been mapped or not by looking at the mapping's format.
     * @return 
     */
    public boolean isMapped() {
        return mapping != null && mapping.format != null;
    }
    
    public ColumnMapping getMapping() {
        return mapping;
    }
}
