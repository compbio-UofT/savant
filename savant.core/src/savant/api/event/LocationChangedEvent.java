/*
 *    Copyright 2010-2012 University of Toronto
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
