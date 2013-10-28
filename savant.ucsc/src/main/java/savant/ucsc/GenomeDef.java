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
package savant.ucsc;

/**
 * Class which keeps track of information about a genome in the UCSC database, most importantly
 * the name of the associated MySQL database.
 *
 * @author tarkvara
 */
public class GenomeDef {
    final String database;
    final String label;

    public GenomeDef(String database, String label) {
        this.database = database;
        this.label = label;
    }

    @Override
    public String toString() {
        return database + " - " + label;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GenomeDef) {
            return database.equals(((GenomeDef)o).database);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.database != null ? this.database.hashCode() : 0);
        return hash;
    }
    
    public String getDatabase() {
        return database;
    }
}

