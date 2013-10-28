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
