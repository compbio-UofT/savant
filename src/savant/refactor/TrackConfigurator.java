/*
 *    Copyright 2010 University of Toronto
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

/*
 * TrackConfigurator.java
 * Created on Jun 15, 2010
 */

package savant.refactor;

/**
 * Interface for classes which collect and pass Track-specific params to a {@Track}.
 *
 * @see Track
 * @author vwilliams
 */
public interface TrackConfigurator {

    /**
     *
     * @return the Track this configurator is responsible for
     */
    public Track getTrack();
}
