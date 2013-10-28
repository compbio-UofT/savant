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
package savant.api.event;

import savant.api.adapter.GenomeAdapter;
import savant.data.types.Genome;

/**
 * Event set by the ReferenceController when the genome has changed and there is a
 * new list of references.
 *
 * @author tarkvara
 */
public class GenomeChangedEvent {
    private final GenomeAdapter oldGenome, newGenome;

    public GenomeChangedEvent(Genome oldGenome, Genome newGenome) {
        this.oldGenome = oldGenome;
        this.newGenome = newGenome;
    }

    public GenomeAdapter getOldGenome() {
        return oldGenome;
    }

    public GenomeAdapter getNewGenome() {
        return newGenome;
    }
}
