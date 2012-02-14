/*
 *    Copyright 2012 University of Toronto
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


/**
 * Event which is fired when the viewable reference and/or range has finished changing.
 * This event is not used internally by Savant itself, but is intended to simplify the life
 * of plugin authors.
 *
 * In general, plugins will want to listen to LocationChangedEvent or LocationChangeCompletedEvent,
 * but not to both.
 *
 * @author tarkvara
 */
public class LocationChangeCompletedEvent {
    
    public LocationChangeCompletedEvent() {
    }
}
