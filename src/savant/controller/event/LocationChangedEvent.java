/*
 *    Copyright 2010-2011 University of Toronto
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

package savant.controller.event;

import savant.util.Range;

/**
 *
 * @author mfiume, tarkvara, AndrewBrook
 */
public class LocationChangedEvent {

    private final String reference;
    private final Range range;
    private final boolean newRef;

    public LocationChangedEvent(boolean newRef, String reference, Range range) {
        this.reference = reference;
        this.range = range;
        this.newRef = newRef;
    }

    public String getReference() {
        return reference;
    }

    public Range getRange() {
        return range;
    }

    public boolean isNewReference(){
        return newRef;
    }

}
