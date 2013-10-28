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

import savant.api.adapter.RangeAdapter;
import savant.util.Range;

/**
 * Event which is fired when the viewable reference and/or range is changed.
 * In general, code will want to listen to LocationChangedEvent or LocationChangeCompletedEvent,
 * but not to both.
 *
 * @author mfiume, tarkvara, AndrewBrook
 */
public class LocationChangedEvent {
    
    private final String reference;
    private final Range range;
    private final boolean newReference;

    public LocationChangedEvent(boolean newRef, String ref, Range r) {
        this.newReference = newRef;
        this.reference = ref;
        this.range = r;
    }

    /**
     * The reference to which the location is being changed.
     */
    public String getReference() {
        return reference;
    }

    /**
     * The range to which the location is being changed.
     */
    public RangeAdapter getRange() {
        return range;
    }

    /**
     * Is the location being changed to a new reference?
     */
    public boolean isNewReference() {
        return newReference;
    }
}
